package com.scottlogic.weather.owmadapter.impl;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.ResponseEntity;
import akka.http.javadsl.model.StatusCode;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.stream.Materializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lightbend.lagom.internal.jackson.JacksonObjectMapperProvider;
import com.lightbend.lagom.javadsl.api.deser.DeserializationException;
import com.lightbend.lagom.javadsl.api.deser.ExceptionMessage;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.api.transport.TransportException;
import com.scottlogic.weather.owmadapter.api.message.Unauthorized;
import com.scottlogic.weather.owmadapter.api.message.internal.ErrorResponse;
import com.scottlogic.weather.owmadapter.api.message.internal.OwmCurrentWeatherResponse;
import com.scottlogic.weather.owmadapter.api.message.internal.OwmWeatherForecastResponse;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.libs.concurrent.MaterializerProvider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

@Singleton
class OwmClient {
	private static final String CURRENT_WEATHER_SEGMENT = "weather";
	private static final String WEATHER_FORECAST_SEGMENT = "forecast";
	private static final int REQUEST_TIMEOUT_SECS = 30;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Http http;
	private final Materializer materializer;
	private final ObjectMapper objectMapper;
	private final String baseUrl;
	private final String apiKey;

	@Inject
	OwmClient(final ActorSystem actorSystem, final Http http, final Config config) {
		this.http = http;
		this.materializer = new MaterializerProvider(actorSystem).get();
		this.objectMapper = JacksonObjectMapperProvider.get(actorSystem).objectMapper();

		final Config owmConfig = config.getConfig("source.owm");
		// TODO Onboard user with this API key, and store in entity.
		this.apiKey = owmConfig.getString("apiKey");

		final String url = owmConfig.getString("url");
		this.baseUrl = url + (url.endsWith("/") ? "" : "/");
		try {
			// Perform some simple URL validation.
			new URL(this.baseUrl).toURI();
		} catch (MalformedURLException | URISyntaxException e) {
			throw new RuntimeException("OpenWeatherMap URL in config is not valid", e);
		}
	}

	OwmCurrentWeatherResponse getCurrentWeather(final String location) throws TransportException {
		return getWeather(
				currentWeatherUrl(location),
				OwmCurrentWeatherResponse.class
		);
	}

	OwmWeatherForecastResponse getWeatherForecast(final String location) throws TransportException {
		return getWeather(
				weatherForecastUrl(location),
				OwmWeatherForecastResponse.class
		);
	}

	<T> T getWeather(final String url, final Class<T> responseClass) throws TransportException {
		try {
			return this.http.singleRequest(HttpRequest.create(url))
					.thenApply(httpResponse -> {
						if (httpResponse.status().isSuccess()) {
							return unmarshallWeatherResponse(httpResponse.entity(), responseClass);
						}
						throw transportExceptionFromFailureResponse(httpResponse.status(), httpResponse.entity());
					})
					.toCompletableFuture().get(REQUEST_TIMEOUT_SECS, SECONDS);
		} catch (final ExecutionException e) {
			final Throwable cause = e.getCause();
			throw (TransportException.class.isAssignableFrom(cause.getClass()))
					? (TransportException) e.getCause()
					: internalServerError(e);
		} catch (final InterruptedException | TimeoutException e) {
			throw requestTimedOut(e);
		}
	}

	private <T> T unmarshallWeatherResponse(final ResponseEntity entity, final Class<T> clazz) throws DeserializationException {
		try {
			final String jsonResponse = Unmarshaller.entityToString()
					.unmarshal(entity, materializer)
					.toCompletableFuture().get(5, SECONDS);

			return this.objectMapper.readValue(jsonResponse, clazz);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw deserializationException("Failed to unmarshall weather data response entity", e);
		} catch (IOException e) {
			throw deserializationException("Failed to parse weather data response body", e);
		}
	}

	private TransportException transportExceptionFromFailureResponse(final StatusCode statusCode, final HttpEntity errorEntity) {
		try {
			final String jsonResponse = Unmarshaller.entityToString()
					.unmarshal(errorEntity, materializer)
					.toCompletableFuture().get(5, SECONDS);
			final ErrorResponse error = this.objectMapper.readValue(jsonResponse, ErrorResponse.class);

			// Unauthorized exception not implemented in Lagom, for some reason (risk of info leak?)
			if (StatusCodes.UNAUTHORIZED.equals(statusCode)) {
				return new Unauthorized(error.getMessage());
			}

			// For some status codes, there is an exception class named after the reason code (but
			// with whitespace removed). If not, a generic TransportException will be constructed.
			final String exceptionClassName = statusCode.reason().replace(" ", "");
			return TransportException.fromCodeAndMessage(
					TransportErrorCode.fromHttp(statusCode.intValue()),
					new ExceptionMessage(exceptionClassName, error.getMessage())
			);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			return deserializationException("Failed to unmarshall error response entity", e);
		} catch (IOException e) {
			return deserializationException("Failed to parse error response body", e);
		}
	}

	private DeserializationException deserializationException(final String message, final Exception e) {
		log.error(message, e);
		return new DeserializationException(message);
	}

	private TransportException requestTimedOut(final Exception e) {
		return TransportException.fromCodeAndMessage(
				TransportErrorCode.fromHttp(StatusCodes.REQUEST_TIMEOUT.intValue()),
				constructExceptionMessage(e)
		);
	}

	private TransportException internalServerError(final Throwable e) {
		return TransportException.fromCodeAndMessage(
				TransportErrorCode.InternalServerError,
				constructExceptionMessage(e)
		);
	}

	private ExceptionMessage constructExceptionMessage(final Throwable e) {
		return new ExceptionMessage(
				e.getClass().getSimpleName(),
				e.getMessage()
		);
	}

	private String currentWeatherUrl(final String location) throws TransportException {
		return weatherUrl(CURRENT_WEATHER_SEGMENT, location);
	}

	private String weatherForecastUrl(final String location) throws TransportException {
		return weatherUrl(WEATHER_FORECAST_SEGMENT, location);
	}

	private String weatherUrl(final String segment, final String location) throws TransportException {
		try {
			final String locationEncoded = URLEncoder.encode(location, StandardCharsets.UTF_8.name());
			return this.baseUrl +
					segment +
					"?units=metric&appid=" + this.apiKey +
					"&q=" + locationEncoded;
		} catch (UnsupportedEncodingException e) {
			log.error("Problem encoding URL for OpenWeatherMap", e);
			throw internalServerError(e);
		}
	}
}

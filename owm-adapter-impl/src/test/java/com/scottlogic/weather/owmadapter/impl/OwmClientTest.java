package com.scottlogic.weather.owmadapter.impl;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.ImmutableList;
import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.scottlogic.weather.owmadapter.api.message.Unauthorized;
import com.scottlogic.weather.owmadapter.api.message.internal.Locale;
import com.scottlogic.weather.owmadapter.api.message.internal.OwmWeatherResponse;
import com.scottlogic.weather.owmadapter.api.message.internal.Temperature;
import com.scottlogic.weather.owmadapter.api.message.internal.Weather;
import com.scottlogic.weather.owmadapter.api.message.internal.Wind;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@DisplayName("Tests for handling success and failure responses from OpenWeatherMap")
class OwmClientTest {
	private static Config configValid;
	private static Config configBadApiKey;
	private static ActorSystem actorSystem;

	@Mock private Http http;

	private OwmClient sut;

	@BeforeAll
	static void beforeAll() {
		configValid = ConfigFactory.parseResources("valid.conf");
		configBadApiKey = ConfigFactory.parseResources("bad_apikey.conf");
		actorSystem = ActorSystem.create("OwnClientIT");
	}

	@AfterAll
	static void afterAll() {
		TestKit.shutdownActorSystem(actorSystem);
		actorSystem = null;
		configValid = null;
		configBadApiKey = null;
	}

	@BeforeEach
	void beforeEach() {
		initMocks(this);
	}

	@Test
	void getCurrentWeather_200Response_ReturnsWeatherData() {
		final OwmWeatherResponse expectedResponse = generateOwmWeatherResponse();
		when(
				http.singleRequest(ArgumentMatchers.any(HttpRequest.class))
		).thenReturn(
				httpSuccessResponseWithEntity(expectedResponse)
		);

		sut = new OwmClient(actorSystem, http, configValid);
		final OwmWeatherResponse response = sut.getCurrentWeather("anywhere");

		assertThat(response, is(expectedResponse));
	}

	@Test
	void getCurrentWeather_401Response_ThrowsUnauthorized() {
		final String failureMessage = "whoops apocalypse";
		when(
				http.singleRequest(ArgumentMatchers.any(HttpRequest.class))
		).thenReturn(
				httpFailureResponseWithStatus(401, failureMessage)
		);

		sut = new OwmClient(actorSystem, http, configBadApiKey);

		final Unauthorized exception = assertThrows(
				Unauthorized.class, () -> sut.getCurrentWeather("anywhere")
		);
		assertThat(exception.getMessage().toLowerCase(), is(failureMessage));
	}

	@Test
	void getCurrentWeather_404Response_ThrowsNotFound() {
		final String failureMessage = "apocalypse now";
		when(
				http.singleRequest(ArgumentMatchers.any(HttpRequest.class))
		).thenReturn(
				httpFailureResponseWithStatus(404, failureMessage)
		);

		sut = new OwmClient(actorSystem, http, configValid);

		final NotFound exception = assertThrows(
				NotFound.class, () -> sut.getCurrentWeather("Shoogly")
		);
		assertThat(exception.getMessage().toLowerCase(), containsString(failureMessage));
	}

	@Test
	void constructor_ConfigNotFound_ThrowsRuntimeException() {
		final RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> new OwmClient(actorSystem, http, ConfigFactory.empty())
		);
		assertThat(exception.getMessage().toLowerCase(), containsString("no configuration setting found for key 'source'"));
	}

	private OwmWeatherResponse generateOwmWeatherResponse() {
		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		return OwmWeatherResponse.builder()
				.id(12345)
				.name("anywhere")
				.measuredAt(now)
				.localeData(Locale.builder()
						.sunrise(now.minus(90, ChronoUnit.MINUTES))
						.sunset(now.plus(180, ChronoUnit.MINUTES))
						.countryCode("AA")
						.build()
				)
				.weather(ImmutableList.of(Weather.builder()
						.id(500)
						.description("crappy pissy rain")
						.build()
				))
				.temperature(Temperature.builder()
						.tempMin(BigDecimal.ZERO)
						.temp(new BigDecimal("9.6"))
						.tempMax(new BigDecimal("10.4"))
						.build()
				)
				.wind(Wind.builder()
						.speed(new BigDecimal("42.8"))
						.fromDegrees((short) 359)
						.build()
				)
				.build();
	}

	private CompletionStage<HttpResponse> httpSuccessResponseWithEntity(final OwmWeatherResponse responseEntity) {
		return CompletableFuture.completedFuture(
				HttpResponse.create()
						.withStatus(200)
						.withEntity(HttpEntities.create(
								owmWeatherResponseToEntityString(responseEntity)
						))
		);
	}

	private String owmWeatherResponseToEntityString(final OwmWeatherResponse weather) {
		final LocalDateTime sunrise = weather.getLocaleData().getSunrise();
		final LocalDateTime sunset = weather.getLocaleData().getSunset();
		final LocalDateTime measuredAt = weather.getMeasuredAt();

		return "{" +
				"\"coord\":{\"lon\":-3.2,\"lat\":55.95}," +
				"\"weather\":[{" +
					"\"id\":" + weather.getWeather().get(0).getId() +
					",\"description\":\"" + weather.getWeather().get(0).getDescription() + "\",\"icon\":\"03d\"" +
				"}]," +
				"\"base\":\"stations\"," +
				"\"main\":{" +
					"\"temp\":" + weather.getTemperature().getTemp().toPlainString() +
					",\"pressure\":1011" +
					",\"humidity\":71" +
					",\"temp_min\":" + weather.getTemperature().getTempMin().toPlainString() +
					",\"temp_max\":" + weather.getTemperature().getTempMax().toPlainString() +
				"}," +
				"\"visibility\":10000," +
				"\"wind\":{" +
					"\"speed\":" + weather.getWind().getSpeed().toPlainString() +
					",\"deg\":" + weather.getWind().getFromDegrees() +
				"}," +
				"\"clouds\":{\"all\":40}," +
				"\"dt\":" + measuredAt.toInstant(ZoneId.systemDefault().getRules().getOffset(measuredAt)).getEpochSecond() +
				",\"sys\":{" +
					"\"type\":1,\"id\":5122,\"message\":0.0032" +
					",\"country\":\"" + weather.getLocaleData().getCountryCode() + "\"" +
					",\"sunrise\":" + sunrise.toInstant(ZoneId.systemDefault().getRules().getOffset(sunrise)).getEpochSecond() +
					",\"sunset\":" + sunset.toInstant(ZoneId.systemDefault().getRules().getOffset(sunset)).getEpochSecond() +
				"}," +
				"\"id\":" + weather.getId() +
				",\"name\":\"" + weather.getName() + "\"," +
				"\"cod\":200" +
				"}";
	}

	private CompletionStage<HttpResponse> httpFailureResponseWithStatus(final int statusCode, final String message) {
		return CompletableFuture.completedFuture(
				HttpResponse.create()
						.withStatus(statusCode)
						.withEntity(HttpEntities.create(
								"{\"cod\":\"" + statusCode +
								"\",\"message\":\"" + message + "\"}"))
		);
	}
}
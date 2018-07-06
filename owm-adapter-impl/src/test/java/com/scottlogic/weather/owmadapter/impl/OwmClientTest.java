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
import com.scottlogic.weather.owmadapter.api.message.internal.City;
import com.scottlogic.weather.owmadapter.api.message.internal.Coordinates;
import com.scottlogic.weather.owmadapter.api.message.internal.Forecast;
import com.scottlogic.weather.owmadapter.api.message.internal.Locale;
import com.scottlogic.weather.owmadapter.api.message.internal.OwmCurrentWeatherResponse;
import com.scottlogic.weather.owmadapter.api.message.internal.OwmWeatherForecastResponse;
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
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

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
		final OwmCurrentWeatherResponse expectedResponse = generateOwmCurrentWeatherResponse();
		when(
				http.singleRequest(ArgumentMatchers.any(HttpRequest.class))
		).thenReturn(
				httpSuccessResponseWithEntity(
						owmCurrentWeatherResponseToEntityString(expectedResponse)
				)
		);

		sut = new OwmClient(actorSystem, http, configValid);
		final OwmCurrentWeatherResponse response = sut.getCurrentWeather("anywhere");

		assertThat(response, is(expectedResponse));
	}

	@Test
	void getCurrentWeather_401Response_ThrowsUnauthorized() {
		final String failureMessage = "Apocalypse Now";
		when(
				http.singleRequest(ArgumentMatchers.any(HttpRequest.class))
		).thenReturn(
				httpFailureResponseWithStatus(401, failureMessage)
		);

		sut = new OwmClient(actorSystem, http, configBadApiKey);

		final Unauthorized exception = assertThrows(
				Unauthorized.class, () -> sut.getCurrentWeather("anywhere")
		);
		assertThat(exception.getMessage(), is(failureMessage));
	}

	@Test
	void getCurrentWeather_404Response_ThrowsNotFound() {
		final String failureMessage = "Whoops Apocalypse";
		when(
				http.singleRequest(ArgumentMatchers.any(HttpRequest.class))
		).thenReturn(
				httpFailureResponseWithStatus(404, failureMessage)
		);

		sut = new OwmClient(actorSystem, http, configValid);

		final NotFound exception = assertThrows(
				NotFound.class, () -> sut.getCurrentWeather("Shoogly")
		);
		assertThat(exception.getMessage(), is(failureMessage));
	}

	@Test
	void getWeatherForecast_200Response_ReturnsWeatherData() {
		final OwmWeatherForecastResponse expectedResponse = generateOwmWeatherForecastResponse();
		when(
				http.singleRequest(ArgumentMatchers.any(HttpRequest.class))
		).thenReturn(
				httpSuccessResponseWithEntity(
						owmWeatherForecastResponseToEntityString(expectedResponse)
				)
		);

		sut = new OwmClient(actorSystem, http, configValid);
		final OwmWeatherForecastResponse response = sut.getWeatherForecast("anywhere");

		assertThat(response, is(expectedResponse));
	}

	@Test
	void getWeatherForecast_401Response_ThrowsUnauthorized() {
		final String failureMessage = "Apocalypse, CA";
		when(
				http.singleRequest(ArgumentMatchers.any(HttpRequest.class))
		).thenReturn(
				httpFailureResponseWithStatus(401, failureMessage)
		);

		sut = new OwmClient(actorSystem, http, configBadApiKey);

		final Unauthorized exception = assertThrows(
				Unauthorized.class, () -> sut.getWeatherForecast("anywhere")
		);
		assertThat(exception.getMessage(), is(failureMessage));
	}

	@Test
	void getWeatherForecast_404Response_ThrowsNotFound() {
		final String failureMessage = "X-Men: Apocalypse";
		when(
				http.singleRequest(ArgumentMatchers.any(HttpRequest.class))
		).thenReturn(
				httpFailureResponseWithStatus(404, failureMessage)
		);

		sut = new OwmClient(actorSystem, http, configValid);

		final NotFound exception = assertThrows(
				NotFound.class, () -> sut.getWeatherForecast("Shoogly")
		);
		assertThat(exception.getMessage(), is(failureMessage));
	}

	@Test
	void constructor_ConfigNotFound_ThrowsRuntimeException() {
		final RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> new OwmClient(actorSystem, http, ConfigFactory.empty())
		);
		assertThat(exception.getMessage().toLowerCase(), containsString("no configuration setting found for key 'source'"));
	}

	private OwmCurrentWeatherResponse generateOwmCurrentWeatherResponse() {
		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		return OwmCurrentWeatherResponse.builder()
				.id(12345)
				.name("anywhere")
				.coordinates(Coordinates.builder()
						.longitude(2.0)
						.latitude(60.0)
						.build()
				)
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

	private OwmWeatherForecastResponse generateOwmWeatherForecastResponse() {
		final Instant firstMeasured = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		final Forecast templateForecast = generateForecast(firstMeasured);

		return OwmWeatherForecastResponse.builder()
				.city(City.builder()
						.id(264371)
						.name("Athens")
						.countryCode("GR")
						.coordinates(Coordinates.builder()
								.longitude(37.98)
								.latitude(23.73)
								.build()
						)
						.build()
				)
				.forecasts(ImmutableList.of(
						templateForecast,
						templateForecast.withMeasuredAt(firstMeasured.plus(3, ChronoUnit.HOURS)),
						templateForecast.withMeasuredAt(firstMeasured.plus(6, ChronoUnit.HOURS))
				))
				.build();
	}

	private Forecast generateForecast(final Instant firstMeasured) {
		return Forecast.builder()
				.measuredAt(firstMeasured)
				.weather(ImmutableList.of(Weather.builder()
						.id(999)
						.description("surface of the sun hot")
						.build())
				)
				.temperature(Temperature.builder()
						.tempMin(new BigDecimal("28.0"))
						.temp(new BigDecimal("35.2"))
						.tempMax(new BigDecimal("40.7"))
						.build()
				)
				.wind(Wind.builder()
						.fromDegrees((short) 180)
						.speed(new BigDecimal("0.3"))
						.build()
				)
				.build();
	}

	private CompletionStage<HttpResponse> httpSuccessResponseWithEntity(final String responseEntityAsString) {
		return CompletableFuture.completedFuture(
				HttpResponse.create()
						.withStatus(200)
						.withEntity(HttpEntities.create(responseEntityAsString))
		);
	}

	private String owmCurrentWeatherResponseToEntityString(final OwmCurrentWeatherResponse current) {
		return "{" +
				"\"coord\":{" +
					"\"lon\":" + current.getCoordinates().getLongitude() +
					",\"lat\":" + current.getCoordinates().getLatitude() +
				"}," +
				"\"weather\":[{" +
					"\"id\":" + current.getWeather().get(0).getId() +
					",\"description\":\"" + current.getWeather().get(0).getDescription() + "\",\"icon\":\"03d\"" +
				"}]," +
				"\"base\":\"stations\"," +
				"\"main\":{" +
					"\"temp\":" + current.getTemperature().getTemp().toPlainString() +
					",\"pressure\":1011" +
					",\"humidity\":71" +
					",\"temp_min\":" + current.getTemperature().getTempMin().toPlainString() +
					",\"temp_max\":" + current.getTemperature().getTempMax().toPlainString() +
				"}," +
				"\"visibility\":10000," +
				"\"wind\":{" +
					"\"speed\":" + current.getWind().getSpeed().toPlainString() +
					",\"deg\":" + current.getWind().getFromDegrees() +
				"}," +
				"\"clouds\":{\"all\":40}," +
				"\"dt\":" + current.getMeasuredAt().getEpochSecond() +
				",\"sys\":{" +
					"\"type\":1,\"id\":5122,\"message\":0.0032" +
					",\"country\":\"" + current.getLocaleData().getCountryCode() + "\"" +
					",\"sunrise\":" + current.getLocaleData().getSunrise().getEpochSecond() +
					",\"sunset\":" + current.getLocaleData().getSunset().getEpochSecond() +
				"}," +
				"\"id\":" + current.getId() +
				",\"name\":\"" + current.getName() + "\"," +
				"\"cod\":200" +
				"}";
	}

	private String owmWeatherForecastResponseToEntityString(final OwmWeatherForecastResponse forecast) {
		return "{" +
				"\"cod\":\"200\"" +
				",\"message\":0.0001" +
				",\"cnt\":" + forecast.getForecasts().size() +
				",\"city\":{" +
					"\"id\":" + forecast.getCity().getId() +
					",\"name\":\"" + forecast.getCity().getName() + "\"" +
					",\"country\":\"" + forecast.getCity().getCountryCode() + "\"" +
					",\"population\":1000000" +
					",\"coord\":{" +
						"\"lat\":" + forecast.getCity().getCoordinates().getLatitude() +
						",\"lon\":" + forecast.getCity().getCoordinates().getLongitude() +
					"}" +
				"}" +
				",\"list\":[" +
				forecast.getForecasts().parallelStream()
						.map(this::forecastToString)
						.collect(Collectors.joining(",")) +
				"]" +
				"}";
	}

	private String forecastToString(final Forecast forecast) {
		return "{" +
				"\"dt\":" + forecast.getMeasuredAt().getEpochSecond() +
				",\"main\":{" +
				"\"temp\":" + forecast.getTemperature().getTemp().toPlainString() +
				",\"temp_min\":" + forecast.getTemperature().getTempMin().toPlainString() +
				",\"temp_max\":" + forecast.getTemperature().getTempMax().toPlainString() +
				",\"humidity\":85" +
				"}" +
				",\"weather\":[{" +
				"\"id\":" + forecast.getWeather().get(0).getId() +
				",\"description\":\"" + forecast.getWeather().get(0).getDescription() + "\",\"icon\":\"03d\"" +
				"}]" +
				",\"clouds\":{\"all\":25}" +
				",\"wind\":{" +
				"\"speed\":" + forecast.getWind().getSpeed().toPlainString() +
				",\"deg\":" + forecast.getWind().getFromDegrees() +
				"}" +
				",\"rain\":{\"3h\":0.2450}" +
				",\"sys\":{\"pod\":\"d\"}" +
				",\"dt_txt\": \"2018-01-01 12:00:00\"" + // value doesn't matter, it's not used
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
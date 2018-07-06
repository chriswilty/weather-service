package com.scottlogic.weather.owmadapter.impl;

import com.google.common.collect.ImmutableList;
import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.lightbend.lagom.javadsl.api.transport.TransportException;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.owmadapter.api.message.Sun;
import com.scottlogic.weather.owmadapter.api.message.WeatherData;
import com.scottlogic.weather.owmadapter.api.message.internal.City;
import com.scottlogic.weather.owmadapter.api.message.internal.Coordinates;
import com.scottlogic.weather.owmadapter.api.message.internal.Forecast;
import com.scottlogic.weather.owmadapter.api.message.internal.Locale;
import com.scottlogic.weather.owmadapter.api.message.internal.OwmCurrentWeatherResponse;
import com.scottlogic.weather.owmadapter.api.message.internal.OwmWeatherForecastResponse;
import com.scottlogic.weather.owmadapter.api.message.internal.Temperature;
import com.scottlogic.weather.owmadapter.api.message.internal.Weather;
import com.scottlogic.weather.owmadapter.api.message.internal.Wind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@DisplayName("Tests for the OWM Adapter implementation")
class OwmAdapterTest {
	@Mock private OwmClient owmClient;

	private OwmAdapter sut;

	@BeforeEach
	void beforeEach() {
		initMocks(this);
		sut = new OwmAdapterImpl(owmClient);
	}

	@Test
	void getCurrentWeather_Success_RespondsWithWeatherData() throws Exception {
		final String location = "Anywhere";
		final OwmCurrentWeatherResponse owmResponse = generateOwmCurrentWeatherResponse();
		final WeatherData expectedResult = generateWeatherDataFrom(owmResponse);

		when(owmClient.getCurrentWeather(location)).thenReturn(owmResponse);

		final WeatherData response = sut.getCurrentWeather(location).invoke()
				.toCompletableFuture().get(5, SECONDS);

		assertThat(response, is(expectedResult));
	}

	@Test
	void getCurrentWeather_TransportException_PropagatesSameExceptionToCaller() {
		final String location = "Somewhere";
		final TransportException expectedException = new NotFound("whoops");

		when(owmClient.getCurrentWeather(location)).thenThrow(expectedException);

		final TransportException result = assertThrows(TransportException.class, () ->
				sut.getCurrentWeather(location).invoke()
						.toCompletableFuture().get(5, SECONDS)
		);

		assertThat(result, is(expectedException));
	}

	@Test
	void getForecastWeather_Success_RespondsWithListOfWeatherData() throws Exception {
		final String location = "Anywhere";
		final OwmWeatherForecastResponse owmResponse = generateOwmForecastWeatherResponse();
		final List<WeatherData> expectedResult = generateWeatherDataFrom(owmResponse);

		when(owmClient.getForecastWeather(location)).thenReturn(owmResponse);

		final List<WeatherData> response = sut.getWeatherForecast(location).invoke()
				.toCompletableFuture().get(5, SECONDS);

		assertThat(response, is(expectedResult));
	}

	@Test
	void getForecastWeather_TransportException_PropagatesSameExceptionToCaller() {
		final String location = "Somewhere";
		final TransportException expectedException = new NotFound("whoops");

		when(owmClient.getForecastWeather(location)).thenThrow(expectedException);

		final TransportException result = assertThrows(TransportException.class, () ->
				sut.getWeatherForecast(location).invoke()
						.toCompletableFuture().get(5, SECONDS)
		);

		assertThat(result, is(expectedException));
	}

	private WeatherData generateWeatherDataFrom(final OwmCurrentWeatherResponse owmResponse) {
		final ZoneId zoneId = ZoneId.of(TimezoneMapper.latLngToTimezoneString(
				owmResponse.getCoordinates().getLatitude(),
				owmResponse.getCoordinates().getLongitude()
		));

		return WeatherData.builder()
				.id(owmResponse.getId())
				.location(owmResponse.getName() + ", " + owmResponse.getLocaleData().getCountryCode())
				.measured(OffsetDateTime.ofInstant(owmResponse.getMeasuredAt(), zoneId))
				.weather(transformWeather(owmResponse.getWeather().get(0)))
				.temperature(transformTemperature(owmResponse.getTemperature()))
				.wind(transformWind(owmResponse.getWind()))
				.sun(Sun.builder()
						.sunrise(OffsetDateTime.ofInstant(owmResponse.getLocaleData().getSunrise(), zoneId))
						.sunset(OffsetDateTime.ofInstant(owmResponse.getLocaleData().getSunset(), zoneId))
						.build()
				)
				.build();
	}

	private List<WeatherData> generateWeatherDataFrom(final OwmWeatherForecastResponse owmResponse) {
		final City city = owmResponse.getCity();
		final int id = city.getId();
		final String name = city.getName() + ", " + city.getCountryCode();
		final ZoneId zoneId = ZoneId.of(TimezoneMapper.latLngToTimezoneString(
				city.getCoordinates().getLatitude(),
				city.getCoordinates().getLongitude()
		));

		return owmResponse.getForecasts().parallelStream()
				.map(forecast -> WeatherData.builder()
						.id(id)
						.location(name)
						.measured(OffsetDateTime.ofInstant(forecast.getMeasuredAt(), zoneId))
						.weather(transformWeather(forecast.getWeather().get(0)))
						.temperature(transformTemperature(forecast.getTemperature()))
						.wind(transformWind(forecast.getWind()))
						.build()
				)
				.collect(Collectors.toList());
	}

	private OwmCurrentWeatherResponse generateOwmCurrentWeatherResponse() {
		final Instant timeNow = Instant.parse("2018-06-21T13:00:00Z")
				.minus(3, HOURS); // Helsinki is 3 hours ahead of UTC at the above time.
		final Instant sunrise = timeNow.minus(10, HOURS);
		final Instant sunset = timeNow.plus(10, HOURS);

		return OwmCurrentWeatherResponse.builder()
				.id(12345)
				.name("Helsinki")
				.coordinates(Coordinates.builder()
						.longitude(24.94)
						.latitude(60.17)
						.build()
				)
				.measuredAt(timeNow)
				.localeData(Locale.builder()
						.sunrise(sunrise)
						.sunset(sunset)
						.countryCode("FI")
						.build()
				)
				.weather(ImmutableList.of(Weather.builder()
						.id(500)
						.description("crappy pissy rain")
						.build()
				))
				.temperature(Temperature.builder()
						.tempMin(BigDecimal.ZERO)
						.temp(BigDecimal.ONE)
						.tempMax(BigDecimal.TEN)
						.build()
				)
				.wind(Wind.builder()
						.speed(BigDecimal.TEN)
						.fromDegrees((short) 359)
						.build()
				)
				.build();
	}

	private OwmWeatherForecastResponse generateOwmForecastWeatherResponse() {
		final Instant timeNow = Instant.parse("2018-06-21T13:00:00Z")
				.minus(3, HOURS); // Helsinki is 3 hours ahead of UTC at the above time.
		final Instant firstMeasured = timeNow.truncatedTo(ChronoUnit.HOURS);
		final Forecast templateForecast = generateForecast(firstMeasured);

		return OwmWeatherForecastResponse.builder()
				.city(City.builder()
						.id(12345)
						.name("Helsinki")
						.countryCode("FI")
						.coordinates(Coordinates.builder()
								.longitude(24.94)
								.latitude(60.17)
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
						.id(200)
						.description("clement for the time of year")
						.build())
				)
				.temperature(Temperature.builder()
						.tempMin(new BigDecimal("22.0"))
						.temp(new BigDecimal("24.8"))
						.tempMax(new BigDecimal("26.5"))
						.build()
				)
				.wind(Wind.builder()
						.fromDegrees((short) 225)
						.speed(new BigDecimal("1.6"))
						.build()
				)
				.build();
	}

	private com.scottlogic.weather.owmadapter.api.message.Weather transformWeather(final Weather weather) {
		return com.scottlogic.weather.owmadapter.api.message.Weather.builder()
				.id(weather.getId())
				.description(weather.getDescription())
				.build();
	}

	private com.scottlogic.weather.owmadapter.api.message.Temperature transformTemperature(final Temperature temperature) {
		return com.scottlogic.weather.owmadapter.api.message.Temperature.builder()
				.minimum(temperature.getTempMin())
				.current(temperature.getTemp())
				.maximum(temperature.getTempMax())
				.build();
	}

	private com.scottlogic.weather.owmadapter.api.message.Wind transformWind(final Wind wind) {
		return com.scottlogic.weather.owmadapter.api.message.Wind.builder()
				.fromDegrees(wind.getFromDegrees())
				.speed(wind.getSpeed())
				.build();
	}
}

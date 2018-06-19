package com.scottlogic.weather.owmadapter.impl;

import com.google.common.collect.ImmutableList;
import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.lightbend.lagom.javadsl.api.transport.TransportException;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.owmadapter.api.message.Sun;
import com.scottlogic.weather.owmadapter.api.message.WeatherData;
import com.scottlogic.weather.owmadapter.api.message.internal.Coordinates;
import com.scottlogic.weather.owmadapter.api.message.internal.Locale;
import com.scottlogic.weather.owmadapter.api.message.internal.OwmWeatherResponse;
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
		final OwmWeatherResponse owmResponse = generateOwmWeatherResponse();
		final WeatherData expectedResult = generateWeatherDataFrom(owmResponse);

		when(owmClient.getCurrentWeather(location)).thenReturn(owmResponse);

		final WeatherData response = sut.getCurrentWeather(location).invoke().toCompletableFuture().get(5, SECONDS);
		assertThat(response, is(expectedResult));
	}

	@Test
	void getCurrentWeather_TransportException_PropagatesSameExceptionThroughService() {
		final String location = "Somewhere";
		final TransportException expectedException = new NotFound("whoops");

		when(owmClient.getCurrentWeather(location)).thenThrow(expectedException);

		final TransportException result = assertThrows(TransportException.class, () ->
				sut.getCurrentWeather(location).invoke().toCompletableFuture().get(5, SECONDS)
		);
		assertThat(result, is(expectedException));
	}

	private WeatherData generateWeatherDataFrom(final OwmWeatherResponse owmResponse) {
		final double latitude = owmResponse.getCoordinates().getLatitude();
		final double longitude = owmResponse.getCoordinates().getLongitude();
		final String zoneId = TimezoneMapper.latLngToTimezoneString(latitude, longitude);

		return WeatherData.builder()
				.id(owmResponse.getId())
				.name(owmResponse.getName() + ", " + owmResponse.getLocaleData().getCountryCode())
				.measured(OffsetDateTime.ofInstant(owmResponse.getMeasuredAt(), ZoneId.of(zoneId)))
				.weather(com.scottlogic.weather.owmadapter.api.message.Weather.builder()
						.id(owmResponse.getWeather().get(0).getId())
						.description(owmResponse.getWeather().get(0).getDescription())
						.build()
				)
				.temperature(com.scottlogic.weather.owmadapter.api.message.Temperature.builder()
						.minimum(owmResponse.getTemperature().getTempMin())
						.current(owmResponse.getTemperature().getTemp())
						.maximum(owmResponse.getTemperature().getTempMax())
						.build()
				)
				.wind(com.scottlogic.weather.owmadapter.api.message.Wind.builder()
						.speed(owmResponse.getWind().getSpeed())
						.fromDegrees(owmResponse.getWind().getFromDegrees())
						.build()
				)
				.sun(Sun.builder()
						.sunrise(OffsetDateTime.ofInstant(owmResponse.getLocaleData().getSunrise(), ZoneId.of(zoneId)))
						.sunset(OffsetDateTime.ofInstant(owmResponse.getLocaleData().getSunset(), ZoneId.of(zoneId)))
						.build()
				)
				.build();
	}

	private OwmWeatherResponse generateOwmWeatherResponse() {
		final Instant timeNow = Instant.parse("2018-06-21T13:00:00Z")
				.minus(3, HOURS); // Helsinki is 3 hours ahead of UTC at the above time.
		final Instant sunrise = timeNow.minus(10, HOURS);
		final Instant sunset = timeNow.plus(10, HOURS);

		return OwmWeatherResponse.builder()
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
}

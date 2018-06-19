package com.scottlogic.weather.weatherservice.impl;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.owmadapter.api.message.Sun;
import com.scottlogic.weather.owmadapter.api.message.Temperature;
import com.scottlogic.weather.owmadapter.api.message.Unauthorized;
import com.scottlogic.weather.owmadapter.api.message.Weather;
import com.scottlogic.weather.owmadapter.api.message.WeatherData;
import com.scottlogic.weather.owmadapter.api.message.Wind;
import com.scottlogic.weather.weatherservice.api.message.WeatherDataResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Tests for the WeatherService implementation")
class WeatherServiceTest {
	private WeatherServiceImpl sut;

	@BeforeEach
	void beforeEach() {
		sut = new WeatherServiceImpl(new OwmAdapterStub());
	}

	@Test
	void getCurrentWeather_LocationFound_RespondsWithWeatherData() throws Exception {
		final String location = "Edinburgh,UK";
		final WeatherDataResponse result = sut.getCurrentWeather(location).invoke().toCompletableFuture().get(5, SECONDS);

		assertThat(result.getLocation(), is(location));
	}
	
	@Test
	void getCurrentWeather_AdapterThrowsUnauthorized() {
		assertThrows(Unauthorized.class, () ->
				sut.getCurrentWeather(OwmAdapterStub.LOCATION_401).invoke().toCompletableFuture().get(5, SECONDS)
		);
	}

	@Test
	void getCurrentWeather_AdapterThrowsNotFound() {
		assertThrows(NotFound.class, () ->
				sut.getCurrentWeather(OwmAdapterStub.LOCATION_404).invoke().toCompletableFuture().get(5, SECONDS)
		);
	}

	/**
	 * Stub implementation of OWM Adapter service, returning faked data.
	 */
	class OwmAdapterStub implements OwmAdapter {
		static final String LOCATION_401 = "Anywhere,KP";
		static final String LOCATION_404 = "Trumpsbrain,US";

		@Override
		public ServiceCall<NotUsed, WeatherData> getCurrentWeather(final String location) {
			return request -> {
				switch (location) {
					case LOCATION_401:
						throw new Unauthorized("denied");
					case LOCATION_404:
						throw new NotFound("no sir");
					default:
						return CompletableFuture.completedFuture(generateWeatherData(location));
				}
			};
		}

		private WeatherData generateWeatherData(final String location) {
			final LocalDateTime now = LocalDateTime.now();
			return WeatherData.builder()
					.id(1234567)
					.name(location)
					.measured(now)
					.weather(Weather.builder()
							.id(101)
							.description("crappy pissy rain")
							.build()
					)
					.temperature(Temperature.builder()
							.current(new BigDecimal("10.0"))
							.minimum(new BigDecimal("9.5"))
							.maximum(new BigDecimal("10.5"))
							.build()
					)
					.wind(Wind.builder()
							.fromDegrees((short) 0)
							.speed(new BigDecimal("34.5"))
							.build()
					)
					.sun(Sun.builder()
							.sunrise(now.withHour(6).truncatedTo(HOURS))
							.sunset(now.withHour(18).truncatedTo(HOURS))
							.build()
					)
					.build();
		}
	}
}
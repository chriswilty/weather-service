package com.scottlogic.weather.weatherservice.impl.stub;

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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

import static java.time.temporal.ChronoUnit.HOURS;

/**
 * Stub implementation of OWM Adapter service, returning faked data.
 */
public class OwmAdapterStub implements OwmAdapter {
	public static final String LOCATION_401 = "Anywhere,KP";
	public static final String LOCATION_404 = "Trumpsbrain,US";

	@Override
	public ServiceCall<NotUsed, WeatherData> getCurrentWeather(final String location) {
		// TODO Include a case for Service Unavailable? Work out what happens when OWM is unreachable.
		// Then devise a supervision / retry strategy within the flow.
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
		final OffsetDateTime now = OffsetDateTime.now();
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

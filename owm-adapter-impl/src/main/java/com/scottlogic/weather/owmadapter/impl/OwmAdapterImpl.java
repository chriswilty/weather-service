package com.scottlogic.weather.owmadapter.impl;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.owmadapter.api.message.Temperature;
import com.scottlogic.weather.owmadapter.api.message.Weather;
import com.scottlogic.weather.owmadapter.api.message.WeatherData;
import com.scottlogic.weather.owmadapter.api.message.Wind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class OwmAdapterImpl implements OwmAdapter {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public ServiceCall<NotUsed, WeatherData> getCurrentWeather(final String location) {
		return request -> {
			log.info("Received request for current weather in [{}]", location);

			// TODO Real call to OpenWeatherMap API.
			return completedFuture(
					WeatherData.builder()
							.id(1234567)
							.name(location)
							.weather(Weather.builder()
									.id((short) 522)
									.description("heavy intensity shower rain")
									.build()
							)
							.temperature(Temperature.builder()
									.current(new BigDecimal("18.5"))
									.minimum(new BigDecimal("11.0"))
									.maximum(new BigDecimal("19.8"))
									.build()
							)
							.wind(Wind.builder()
									.fromDegrees((short) 60)
									.speed(new BigDecimal("10.0"))
									.build()
							)
							.build()
			);
		};
	}
}

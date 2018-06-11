package com.scottlogic.weather.impl;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.scottlogic.weather.api.WeatherService;
import com.scottlogic.weather.api.message.Temperature;
import com.scottlogic.weather.api.message.Weather;
import com.scottlogic.weather.api.message.WeatherDataResponse;
import com.scottlogic.weather.api.message.Wind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Implementation of the WeatherService.
 */
public class WeatherServiceImpl implements WeatherService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public ServiceCall<NotUsed, WeatherDataResponse> getCurrentWeather(final String location) {
		return request -> {
			log.info("Received request for current weather in [{}]", location);

			// TODO Real service call to OWD adapter.
			return completedFuture(
					WeatherDataResponse.builder()
							.location(location)
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

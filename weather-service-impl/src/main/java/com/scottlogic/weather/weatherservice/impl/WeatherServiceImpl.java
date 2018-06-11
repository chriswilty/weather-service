package com.scottlogic.weather.weatherservice.impl;

import akka.NotUsed;
import com.google.inject.Inject;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.owmadapter.api.message.WeatherData;
import com.scottlogic.weather.weatherservice.api.WeatherService;
import com.scottlogic.weather.weatherservice.api.message.Temperature;
import com.scottlogic.weather.weatherservice.api.message.Weather;
import com.scottlogic.weather.weatherservice.api.message.WeatherDataResponse;
import com.scottlogic.weather.weatherservice.api.message.Wind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the WeatherService.
 */
public class WeatherServiceImpl implements WeatherService {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final OwmAdapter owmAdapterService;

	@Inject
	public WeatherServiceImpl(final OwmAdapter owmAdapter) {
		this.owmAdapterService = owmAdapter;
	}

	@Override
	public ServiceCall<NotUsed, WeatherDataResponse> getCurrentWeather(final String location) {
		return request -> {
			log.info("Received request for current weather in [{}]", location);

			return owmAdapterService.getCurrentWeather(location).invoke()
					.thenApply(this::transformWeatherData);
		};
	}

	private WeatherDataResponse transformWeatherData(final WeatherData weatherData) {
		return WeatherDataResponse.builder()
				.location(weatherData.getName())
				.weather(Weather.builder()
						.id(weatherData.getWeather().getId())
						.description(weatherData.getWeather().getDescription())
						.build()
				)
				.temperature(Temperature.builder()
						.current(weatherData.getTemperature().getCurrent())
						.minimum(weatherData.getTemperature().getMinimum())
						.maximum(weatherData.getTemperature().getMaximum())
						.build()
				)
				.wind(Wind.builder()
						.fromDegrees(weatherData.getWind().getFromDegrees())
						.speed(weatherData.getWind().getSpeed())
						.build()
				)
				.build();
	}
}

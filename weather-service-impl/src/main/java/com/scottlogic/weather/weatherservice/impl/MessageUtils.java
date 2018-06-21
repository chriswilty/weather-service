package com.scottlogic.weather.weatherservice.impl;

import com.scottlogic.weather.owmadapter.api.message.WeatherData;
import com.scottlogic.weather.weatherservice.api.message.Sun;
import com.scottlogic.weather.weatherservice.api.message.Temperature;
import com.scottlogic.weather.weatherservice.api.message.Weather;
import com.scottlogic.weather.weatherservice.api.message.WeatherDataResponse;
import com.scottlogic.weather.weatherservice.api.message.Wind;

public class MessageUtils {

	public static WeatherDataResponse transformWeatherData(final WeatherData weatherData) {
		return WeatherDataResponse.builder()
				.location(weatherData.getName())
				.measured(weatherData.getMeasured())
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
				.sun(Sun.builder()
						.sunrise(weatherData.getSun().getSunrise())
						.sunset(weatherData.getSun().getSunset())
						.build()
				)
				.build();
	}

	private MessageUtils() {
		throw new UnsupportedOperationException("Utility class, should not be instantiated!");
	}
}

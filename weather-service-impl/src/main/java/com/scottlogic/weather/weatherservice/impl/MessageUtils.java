package com.scottlogic.weather.weatherservice.impl;

import com.scottlogic.weather.owmadapter.api.message.WeatherData;
import com.scottlogic.weather.weatherservice.api.message.CurrentWeatherResponse;
import com.scottlogic.weather.weatherservice.api.message.Sun;
import com.scottlogic.weather.weatherservice.api.message.Temperature;
import com.scottlogic.weather.weatherservice.api.message.Weather;
import com.scottlogic.weather.weatherservice.api.message.WeatherForecastResponse;
import com.scottlogic.weather.weatherservice.api.message.WeatherSnapshot;
import com.scottlogic.weather.weatherservice.api.message.Wind;

import java.util.List;
import java.util.stream.Collectors;

public class MessageUtils {

	public static CurrentWeatherResponse weatherDataToCurrentWeatherResponse(final WeatherData current) {
		return CurrentWeatherResponse.builder()
				.location(current.getLocation())
				.current(weatherDataToWeatherSnapshot(current))
				.build();
	}

	public static WeatherForecastResponse weatherDataToWeatherForecastResponse(
			final WeatherData current,
			final List<WeatherData> forecasts
	) {
		return WeatherForecastResponse.builder()
				.location(current.getLocation())
				.current(weatherDataToWeatherSnapshot(current))
				.forecast(forecasts.parallelStream()
						.map(MessageUtils::weatherDataToWeatherSnapshot)
						.collect(Collectors.toList())
				)
				.build();
	}

	private static WeatherSnapshot weatherDataToWeatherSnapshot(WeatherData weatherData) {
		return WeatherSnapshot.builder()
				.measured(weatherData.getMeasured())
				.weather(transformWeather(weatherData.getWeather()))
				.temperature(transformTemperature(weatherData.getTemperature()))
				.wind(transformWind(weatherData.getWind()))
				.sun(transformSun(weatherData.getSun()))
				.build();
	}

	private static Weather transformWeather(final com.scottlogic.weather.owmadapter.api.message.Weather weather) {
		return Weather.builder()
				.id(weather.getId())
				.description(weather.getDescription())
				.build();
	}

	private static Temperature transformTemperature(final com.scottlogic.weather.owmadapter.api.message.Temperature temperature) {
		return Temperature.builder()
				.current(temperature.getCurrent())
				.minimum(temperature.getMinimum())
				.maximum(temperature.getMaximum())
				.humidity(temperature.getHumidity())
				.build();
	}

	private static Wind transformWind(final com.scottlogic.weather.owmadapter.api.message.Wind wind) {
		return Wind.builder()
				.fromDegrees(wind.getFromDegrees())
				.speed(wind.getSpeed())
				.build();
	}

	private static Sun transformSun(final com.scottlogic.weather.owmadapter.api.message.Sun sun) {
		return sun == null
				? null
				: Sun.builder()
						.sunrise(sun.getSunrise())
						.sunset(sun.getSunset())
						.build();
	}

	private MessageUtils() {
		throw new UnsupportedOperationException("Utility class, should not be instantiated!");
	}
}

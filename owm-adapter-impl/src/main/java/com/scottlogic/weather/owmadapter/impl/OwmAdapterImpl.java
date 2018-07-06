package com.scottlogic.weather.owmadapter.impl;

import akka.NotUsed;
import com.google.inject.Inject;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.owmadapter.api.message.Sun;
import com.scottlogic.weather.owmadapter.api.message.Temperature;
import com.scottlogic.weather.owmadapter.api.message.Weather;
import com.scottlogic.weather.owmadapter.api.message.WeatherData;
import com.scottlogic.weather.owmadapter.api.message.Wind;
import com.scottlogic.weather.owmadapter.api.message.internal.City;
import com.scottlogic.weather.owmadapter.api.message.internal.OwmCurrentWeatherResponse;
import com.scottlogic.weather.owmadapter.api.message.internal.OwmWeatherForecastResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class OwmAdapterImpl implements OwmAdapter {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final OwmClient owmClient;

	@Inject
	public OwmAdapterImpl(final OwmClient owmClient) {
		this.owmClient = owmClient;
	}

	@Override
	public ServiceCall<NotUsed, WeatherData> getCurrentWeather(final String location) {
		return request -> {
			log.info("Received request for current weather in [{}]", location);

			final WeatherData response = this.transformOwmCurrentWeatherData(
					this.owmClient.getCurrentWeather(location)
			);

			log.info("Sending current weather response for [{}]", response.getLocation());
			return CompletableFuture.completedFuture(response);
		};
	}

	@Override
	public ServiceCall<NotUsed, List<WeatherData>> getWeatherForecast(String location) {
		return request -> {
			log.info("Received request for weather forecast for [{}]", location);

			final List<WeatherData> response = this.transformOwmWeatherForecastData(
					this.owmClient.getWeatherForecast(location)
			);

			log.info("Sending weather forecast response for [{}]", response.get(0).getLocation());
			return CompletableFuture.completedFuture(response);
		};
	}

	private WeatherData transformOwmCurrentWeatherData(final OwmCurrentWeatherResponse owmResponse) {
		final String location = owmResponse.getName() + ", " + owmResponse.getLocaleData().getCountryCode();
		final String zoneId = TimezoneMapper.latLngToTimezoneString(
				owmResponse.getCoordinates().getLatitude(),
				owmResponse.getCoordinates().getLongitude()
		);
		log.info("TimeZone is " + zoneId);

		return WeatherData.builder()
				.id(owmResponse.getId())
				.location(location)
				.measured(instantToOffsetDateTime(owmResponse.getMeasuredAt(), zoneId))
				.weather(transformWeather(owmResponse.getWeather().get(0))) // OWM can return more than one; just use first
				.temperature(transformTemperature(owmResponse.getTemperature()))
				.wind(transformWind(owmResponse.getWind()))
				.sun(Sun.builder()
						.sunrise(instantToOffsetDateTime(owmResponse.getLocaleData().getSunrise(), zoneId))
						.sunset(instantToOffsetDateTime(owmResponse.getLocaleData().getSunset(), zoneId))
						.build()
				)
				.build();
	}

	private List<WeatherData> transformOwmWeatherForecastData(final OwmWeatherForecastResponse owmResponse) {
		final City city = owmResponse.getCity();
		final int id = city.getId();
		final String location = city.getName() + ", " + city.getCountryCode();
		final String zoneId = TimezoneMapper.latLngToTimezoneString(
				city.getCoordinates().getLatitude(),
				city.getCoordinates().getLongitude()
		);

		return owmResponse.getForecasts().parallelStream()
				.map(forecast -> WeatherData.builder()
						.id(id)
						.location(location)
						.measured(instantToOffsetDateTime(forecast.getMeasuredAt(), zoneId))
						.weather(transformWeather(forecast.getWeather().get(0))) // OWM can return more than one; just use first
						.temperature(transformTemperature(forecast.getTemperature()))
						.wind(transformWind(forecast.getWind()))
						.build()
				)
				.collect(Collectors.toList());
	}

	private Weather transformWeather(final com.scottlogic.weather.owmadapter.api.message.internal.Weather owmWeather) {
		return Weather.builder()
				.id(owmWeather.getId())
				.description(owmWeather.getDescription())
				.build();
	}

	private Temperature transformTemperature(final com.scottlogic.weather.owmadapter.api.message.internal.Temperature temp) {
		return Temperature.builder()
				.minimum(temp.getTempMin())
				.current(temp.getTemp())
				.maximum(temp.getTempMax())
				.build();
	}

	private Wind transformWind(final com.scottlogic.weather.owmadapter.api.message.internal.Wind wind) {
		return Wind.builder()
				.fromDegrees(wind.getFromDegrees())
				.speed(wind.getSpeed())
				.build();
	}

	private OffsetDateTime instantToOffsetDateTime(final Instant instant, final String zoneId) {
		return OffsetDateTime.ofInstant(instant, ZoneId.of(zoneId));
	}
}

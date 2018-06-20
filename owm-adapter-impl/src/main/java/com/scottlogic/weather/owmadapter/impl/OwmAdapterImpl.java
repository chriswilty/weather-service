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
import com.scottlogic.weather.owmadapter.api.message.internal.OwmWeatherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

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

			final WeatherData response = this.transformOwmWeatherData(
					this.owmClient.getCurrentWeather(location)
			);

			log.info("Sending current weather data response for [{}]", response.getName());
			return CompletableFuture.completedFuture(response);
		};
	}

	private WeatherData transformOwmWeatherData(final OwmWeatherResponse owmResponse) {
		final double latitude = owmResponse.getCoordinates().getLatitude();
		final double longitude = owmResponse.getCoordinates().getLongitude();
		final String zoneId = TimezoneMapper.latLngToTimezoneString(latitude, longitude);
		log.info("TimeZone is " + zoneId);

		return WeatherData.builder()
				.id(owmResponse.getId())
				.name(owmResponse.getName() + ", " + owmResponse.getLocaleData().getCountryCode())
				.measured(instantToOffsetDateTime(owmResponse.getMeasuredAt(), zoneId))
				.weather(
						transformWeather(owmResponse.getWeather().get(0))
				)
				.temperature(Temperature.builder()
						.current(owmResponse.getTemperature().getTemp())
						.minimum(owmResponse.getTemperature().getTempMin())
						.maximum(owmResponse.getTemperature().getTempMax())
						.build()
				)
				.wind(Wind.builder()
						.speed(owmResponse.getWind().getSpeed())
						.fromDegrees(owmResponse.getWind().getFromDegrees())
						.build()
				)
				.sun(Sun.builder()
						.sunrise(instantToOffsetDateTime(owmResponse.getLocaleData().getSunrise(), zoneId))
						.sunset(instantToOffsetDateTime(owmResponse.getLocaleData().getSunset(), zoneId))
						.build()
				)
				.build();
	}

	private Weather transformWeather(final com.scottlogic.weather.owmadapter.api.message.internal.Weather owmWeather) {
		return Weather.builder()
				.id(owmWeather.getId())
				.description(owmWeather.getDescription())
				.build();
	}

	private OffsetDateTime instantToOffsetDateTime(final Instant instant, final String zoneId) {
		return ZonedDateTime.ofInstant(instant, ZoneId.of(zoneId)).toOffsetDateTime();
	}
}

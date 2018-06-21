package com.scottlogic.weather.weatherservice.impl;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.google.inject.Inject;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.weatherservice.api.WeatherService;
import com.scottlogic.weather.weatherservice.api.message.WeatherDataResponse;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the WeatherService.
 */
public class WeatherServiceImpl implements WeatherService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	// No user sessions for now; just one entity:
	private final String entityId = "default";

	private final OwmAdapter owmAdapterService;
	private final PersistentEntityRegistryFacade persistentEntityRegistryFacade;

	@Inject
	public WeatherServiceImpl(final OwmAdapter owmAdapter, final PersistentEntityRegistryFacade persistentEntityRegistryFacade) {
		this.owmAdapterService = owmAdapter;
		this.persistentEntityRegistryFacade = persistentEntityRegistryFacade;
		persistentEntityRegistryFacade.register(WeatherEntity.class);
	}

	@Override
	public ServiceCall<NotUsed, WeatherDataResponse> currentWeather(final String location) {
		return request -> {
			log.info("Received request for current weather in [{}]", location);

			return this.owmAdapterService.getCurrentWeather(location).invoke()
					.thenApply(MessageUtils::transformWeatherData)
					.thenApply(this::logResponse);
		};
	}

	@Override
	public ServiceCall<NotUsed, Source<WeatherDataResponse, ?>> currentWeatherStream() {
		return request -> {
			log.info("Received request for stream of current weather");

			return this.persistentEntityRegistryFacade.sendCommandToPersistentEntity(
					WeatherEntity.class,
					entityId,
					new WeatherCommand.GetCurrentWeatherStream()
			);
		};
	}

	private WeatherDataResponse logResponse(final WeatherDataResponse response) {
		log.info("Sending current weather data response for [{}]", response.getLocation());
		return response;
	}
}

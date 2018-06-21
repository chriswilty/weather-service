package com.scottlogic.weather.weatherservice.impl;

import akka.stream.javadsl.Source;
import com.google.inject.Inject;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.weatherservice.api.message.WeatherDataResponse;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletionStage;

public class SourceGenerator {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final OwmAdapter owmAdapter;
	private final PersistentEntityRegistryFacade persistentEntityRegistryFacade;

	@Inject
	public SourceGenerator(final OwmAdapter owmAdapter, final PersistentEntityRegistryFacade persistentEntityRegistryFacade) {
		this.owmAdapter = owmAdapter;
		this.persistentEntityRegistryFacade = persistentEntityRegistryFacade;
		persistentEntityRegistryFacade.register(WeatherEntity.class);
	}

	public CompletionStage<Source<WeatherDataResponse, ?>> getSourceOfCurrentWeatherData(final String entityId) {
		return this.persistentEntityRegistryFacade.sendCommandToPersistentEntity(
				WeatherEntity.class,
				entityId,
				new WeatherCommand.GetWeatherStreamParameters()
		).thenApply(weatherStreamParameters ->
				Source.cycle(() -> weatherStreamParameters.getLocations().iterator())
						.mapAsync(3, this::getCurrentWeatherForLocation)
						.throttle(1, Duration.of(weatherStreamParameters.getEmitFrequencySeconds(), ChronoUnit.SECONDS))
						.watchTermination((source, future) -> {
							future.whenComplete((done, throwable) -> {
								if (throwable != null) {
									log.error("Stream of weather data terminated with error", throwable);
								} else {
									log.info("Stream of weather data closed following successful completion");
								}
							});
							return source;
						})
		);
	}

	private CompletionStage<WeatherDataResponse> getCurrentWeatherForLocation(final String location) {
		return this.owmAdapter
				.getCurrentWeather(location).invoke()
				.thenApply(MessageUtils::transformWeatherData);
	}
}

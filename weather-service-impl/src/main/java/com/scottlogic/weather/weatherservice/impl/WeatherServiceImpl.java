package com.scottlogic.weather.weatherservice.impl;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.google.inject.Inject;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.transport.BadRequest;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.weatherservice.api.WeatherService;
import com.scottlogic.weather.weatherservice.api.message.AddLocationRequest;
import com.scottlogic.weather.weatherservice.api.message.SetEmitFrequencyRequest;
import com.scottlogic.weather.weatherservice.api.message.WeatherDataResponse;
import com.scottlogic.weather.weatherservice.api.message.WeatherStreamParameters;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.AddLocation;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.ChangeEmitFrequency;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.GetWeatherStreamParameters;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.RemoveLocation;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherEntity;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the WeatherService.
 */
public class WeatherServiceImpl implements WeatherService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	// No user sessions for now; just one entity:
	private final String entityId = "default";

	private final OwmAdapter owmAdapterService;
	private final StreamGeneratorFactory streamGeneratorFactory;
	private final PersistentEntityRegistryFacade entityRegistryFacade;

	@Inject
	public WeatherServiceImpl(
			final OwmAdapter owmAdapter,
			final StreamGeneratorFactory streamGeneratorFactory,
			final PersistentEntityRegistryFacade entityRegistryFacade
	) {
		this.owmAdapterService = owmAdapter;
		this.streamGeneratorFactory = streamGeneratorFactory;
		this.entityRegistryFacade = entityRegistryFacade;
		this.entityRegistryFacade.register(WeatherEntity.class);
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
			return CompletableFuture.completedFuture(this.streamGeneratorFactory.get(entityId).getSourceOfCurrentWeatherData());
		};
	}

	@Override
	public ServiceCall<NotUsed, WeatherStreamParameters> weatherStreamParameters() {
		return request -> {
			log.info("Received request for stream parameters");
			return this.entityRegistryFacade.sendCommandToPersistentEntity(
					WeatherEntity.class,
					entityId,
					new GetWeatherStreamParameters()
			).thenApply(this::logResponse);
		};
	}

	@Override
	public ServiceCall<SetEmitFrequencyRequest, Done> setEmitFrequency() {
		return request -> {
			checkNonNullRequest(request, "{ frequency: number }");

			final int frequency = request.getFrequency();
			log.info("Received request to set emit frequency to [{}]", frequency);

			if (frequency < 1) {
				badRequest("Frequency must be greater than zero");
			}

			return this.entityRegistryFacade.sendCommandToPersistentEntity(
					WeatherEntity.class,
					entityId,
					new ChangeEmitFrequency(frequency)
			);
		};
	}

	@Override
	public ServiceCall<AddLocationRequest, Done> addLocation() {
		return request -> {
			checkNonNullRequest(request, "{ location: string }");

			final String location = StringUtils.strip(request.getLocation());
			log.info("Received request to add location [{}]", location);

			if (StringUtils.isEmpty(location)) {
				badRequest("Location must be given");
			}

			return this.entityRegistryFacade.sendCommandToPersistentEntity(
					WeatherEntity.class,
					entityId,
					new AddLocation(location)
			);
		};
	}

	@Override
	public ServiceCall<NotUsed, Done> removeLocation(final String location) {
		return request -> {
			log.info("Received request to remove location [{}]", location);

			return this.entityRegistryFacade.sendCommandToPersistentEntity(
					WeatherEntity.class,
					entityId,
					new RemoveLocation(location)
			);
		};
	}

	private <T> T logResponse(final T response) {
		log.info("Sending response: {}", response);
		return response;
	}

	private <R> void checkNonNullRequest(final R request, final String expectedShape) throws BadRequest {
		if (request == null) {
			badRequest("No request body found, expected " + expectedShape);
		}
	}

	private void badRequest(final String message) {
		log.warn(message);
		throw new BadRequest(message);
	}
}

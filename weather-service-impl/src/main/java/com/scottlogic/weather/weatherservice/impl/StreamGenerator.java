package com.scottlogic.weather.weatherservice.impl;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Scheduler;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Source;
import com.google.inject.Inject;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.weatherservice.api.message.WeatherDataResponse;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.GetWeatherStreamParameters;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.FiniteDuration;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class StreamGenerator {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final OwmAdapter owmAdapter;
	private final PersistentEntityRegistryFacade persistentEntityRegistryFacade;
	private final Scheduler scheduler;
	private final ExecutionContext executor;

	private Optional<Cancellable> weatherStreamTask;

	@Inject
	public StreamGenerator(
			final OwmAdapter owmAdapter,
			final PersistentEntityRegistryFacade persistentEntityRegistryFacade,
			final ActorSystem actorSystem
	) {
		this.owmAdapter = owmAdapter;
		this.persistentEntityRegistryFacade = persistentEntityRegistryFacade;
		this.scheduler = actorSystem.scheduler();
		this.executor = actorSystem.dispatcher();
		this.weatherStreamTask = Optional.empty();

	}

	public Source<WeatherDataResponse, ?> getSourceOfCurrentWeatherData(final String entityId) {
		return Source.<WeatherDataResponse>actorRef(5, OverflowStrategy.dropHead())
				.watchTermination((source, future) -> {
					future.whenComplete((done, throwable) -> {
						this.weatherStreamTask.ifPresent(Cancellable::cancel);
						if (throwable != null) {
							log.error("Stream of weather data terminated with error", throwable);
						} else {
							log.info("Stream of weather data closed following successful completion");
						}
					});
					return source;
				}).mapMaterializedValue(actorRef -> {
					this.scheduler.scheduleOnce(
							FiniteDuration.Zero(),
							() -> fetchWeatherDataAndPushIntoStream(entityId, actorRef, -1),
							this.executor
					);
					return actorRef;
				});
	}

	private void fetchWeatherDataAndPushIntoStream(final String entityId, final ActorRef streamActor, final int previousIndex) {
		this.persistentEntityRegistryFacade.sendCommandToPersistentEntity(
				WeatherEntity.class,
				entityId,
				new GetWeatherStreamParameters()
		).thenApply(weatherStreamParameters -> {
			final int locationIndex = (previousIndex + 1) % weatherStreamParameters.getLocations().size();
			// Schedule next invocation of this method.
			this.weatherStreamTask = Optional.of(
					this.scheduler.scheduleOnce(
							FiniteDuration.apply(weatherStreamParameters.getEmitFrequencySeconds(), TimeUnit.SECONDS),
							() -> fetchWeatherDataAndPushIntoStream(entityId, streamActor, locationIndex),
							this.executor
					)
			);
			return weatherStreamParameters.getLocations().get(locationIndex);
		}).thenCompose(
				this::getCurrentWeatherForLocation
		).thenAccept(weatherDataResponse ->
				streamActor.tell(weatherDataResponse, ActorRef.noSender())
		).exceptionally(e -> {
			log.error("Error encountered while fetching weather data for stream", e);
			return null;
		});
	}

	private CompletionStage<WeatherDataResponse> getCurrentWeatherForLocation(final String location) {
		return this.owmAdapter
				.getCurrentWeather(location).invoke()
				.thenApply(MessageUtils::transformWeatherData);
	}
}

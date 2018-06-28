package com.scottlogic.weather.weatherservice.impl;

import akka.actor.ActorRef;
import akka.stream.KillSwitch;
import akka.stream.KillSwitches;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.inject.Inject;
import com.lightbend.lagom.javadsl.pubsub.PubSubRef;
import com.lightbend.lagom.javadsl.pubsub.PubSubRegistry;
import com.lightbend.lagom.javadsl.pubsub.TopicId;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.weatherservice.api.message.StreamParametersUpdated;
import com.scottlogic.weather.weatherservice.api.message.WeatherDataResponse;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.GetWeatherStreamParameters;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StreamGenerator {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final OwmAdapter owmAdapter;
	private final PersistentEntityRegistryFacade persistentEntityRegistryFacade;
	private final Materializer materializer;
	private final PubSubRef<StreamParametersUpdated> streamParametersPubSub;
	private final String entityId;

	private Optional<KillSwitch> sourceKillSwitch;

	@Inject
	public StreamGenerator(
			final OwmAdapter owmAdapter,
			final PersistentEntityRegistryFacade persistentEntityRegistryFacade,
			final Materializer materializer,
			final PubSubRegistry pubSubRegistry,
			final String entityId
	) {
		this.owmAdapter = owmAdapter;
		this.persistentEntityRegistryFacade = persistentEntityRegistryFacade;
		this.materializer = materializer;
		this.entityId = entityId;
		this.streamParametersPubSub = pubSubRegistry.refFor(TopicId.of(StreamParametersUpdated.class, entityId));
		this.sourceKillSwitch = Optional.empty();
	}

	public Source<WeatherDataResponse, ?> getSourceOfCurrentWeatherData() {
		return Source.<WeatherDataResponse>actorRef(5, OverflowStrategy.dropHead())
				.mapMaterializedValue(actorRef -> {
					this.sourceKillSwitch = generateCancellableStreamOfWeatherData(
							sourceOfCurrentWeatherData(entityId),
							actorRef
					);
					this.streamParametersPubSub.subscriber().runForeach(
							parametersUpdated -> {
								log.info("Received notification of stream parameter changes");
								this.terminateWeatherSourceAndReplaceKillSwitch(
										generateCancellableStreamOfWeatherData(
												sourceOfCurrentWeatherData(
														parametersUpdated.getEmitFrequencySecs(),
														parametersUpdated.getLocations()
												),
												actorRef
										)
								);
							},
							materializer
					);
					return actorRef;
				}).watchTermination((source, future) -> {
					// This is termination of the ActorRef source.
					future.whenComplete((done, throwable) -> {
						if (throwable != null) {
							log.error("Stream of weather data terminated with error", throwable);
						} else {
							log.info("Stream of weather data closed following successful completion");
						}
						// Always terminate the upstream source cleanly.
						this.terminateWeatherSourceAndReplaceKillSwitch(Optional.empty());
					});
					return source;
				});
	}

	private Optional<KillSwitch> generateCancellableStreamOfWeatherData(
			final Source<WeatherDataResponse, ?> source,
			final ActorRef actorRef
	) {
		return Optional.of(
				source.viaMat(KillSwitches.single(), Keep.right())
						.toMat(sinkIntoActorRef(actorRef), Keep.left())
						.run(this.materializer)
		);
	}

	private Source<WeatherDataResponse, ?> sourceOfCurrentWeatherData(final String entityId)
			throws InterruptedException, ExecutionException, TimeoutException {
		return this.persistentEntityRegistryFacade.sendCommandToPersistentEntity(
				WeatherEntity.class,
				entityId,
				new GetWeatherStreamParameters()
		).thenApply(streamParameters ->
				sourceOfCurrentWeatherData(streamParameters.getEmitFrequencySeconds(), streamParameters.getLocations())
		).toCompletableFuture().get(5, TimeUnit.SECONDS);
	}

	private Source<WeatherDataResponse, ?> sourceOfCurrentWeatherData(final int frequency, final List<String> locations) {
		return Source.cycle(locations::iterator)
				.mapAsync(3, this::getCurrentWeatherForLocation)
				.throttle(1, Duration.of(frequency, ChronoUnit.SECONDS));
	}

	private Sink<WeatherDataResponse, ?> sinkIntoActorRef(final ActorRef actorRef) {
		return Sink.foreach(weather -> actorRef.tell(weather, ActorRef.noSender()));
	}

	private void terminateWeatherSourceAndReplaceKillSwitch(final Optional<KillSwitch> replacement) {
		this.sourceKillSwitch.ifPresent(killSwitch -> {
			killSwitch.shutdown();
			log.info("Upstream weather source terminated successfully");
			this.sourceKillSwitch = Optional.empty();
		});
		this.sourceKillSwitch = replacement;
	}

	private CompletionStage<WeatherDataResponse> getCurrentWeatherForLocation(final String location) {
		return this.owmAdapter
				.getCurrentWeather(location).invoke()
				.thenApply(MessageUtils::transformWeatherData);
	}
}

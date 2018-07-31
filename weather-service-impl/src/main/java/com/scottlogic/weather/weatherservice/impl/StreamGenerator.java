package com.scottlogic.weather.weatherservice.impl;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.japi.function.Creator;
import akka.japi.function.Function;
import akka.japi.function.Function2;
import akka.stream.Graph;
import akka.stream.KillSwitch;
import akka.stream.KillSwitches;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.inject.Inject;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.weatherservice.api.message.CurrentWeatherResponse;
import com.scottlogic.weather.weatherservice.api.message.StreamParametersUpdated;
import com.scottlogic.weather.weatherservice.api.message.WeatherForecastResponse;
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
	private final PubSubRegistryFacade pubSubRegistryFacade;
	private final Materializer materializer;
	private final String entityId;

	private final Function<String, CompletionStage<CurrentWeatherResponse>> getCurrentWeather;
	private final Function<String, CompletionStage<WeatherForecastResponse>> getWeatherForecast;

	private KillSwitch sourceKillSwitch;

	@Inject
	public StreamGenerator(
			final OwmAdapter owmAdapter,
			final PersistentEntityRegistryFacade persistentEntityRegistryFacade,
			final PubSubRegistryFacade pubSubRegistryFacade,
			final Materializer materializer,
			final String entityId
	) {
		this.owmAdapter = owmAdapter;
		this.persistentEntityRegistryFacade = persistentEntityRegistryFacade;
		this.pubSubRegistryFacade = pubSubRegistryFacade;
		this.materializer = materializer;
		this.entityId = entityId;

		this.getCurrentWeather = location -> this.owmAdapter.getCurrentWeatherByName(location).invoke()
				.thenApply(MessageUtils::weatherDataToCurrentWeatherResponse);
		this.getWeatherForecast = location -> this.owmAdapter.getCurrentWeatherByName(location).invoke()
				.thenCombine(
						this.owmAdapter.getWeatherForecastByName(location).invoke(),
						MessageUtils::weatherDataToWeatherForecastResponse
				);
	}

	/**
	 * <p>
	 *   Generates a source of weather data that will react to stream parameter changes in realtime.
	 * </p>
	 * <p>
	 *   The concept is to create a "source" of weather data which can be reconstructed on-demand,
	 *   and a "sink" which will be a stream passed back to (and eventually terminated by) the
	 *   client. Incoming messages from the source will be pushed into the sink as they arrive;
	 *   because our source and sink are independent, we can reconstruct our source at any stage
	 *   without affecting the open stream to the client.
	 * </p>
	 * <ul>
	 * <li>
	 *   The source returned to the caller (client) is created using
	 *   {@link Source#actorRef(int, OverflowStrategy) Source.actorRef}. Messages can be pushed to
	 *   the actor, which in turn pushes them into the materialized stream. We need to use
	 *   mapMaterializedValue to get hold of a reference to the ActorRef, so that we can push
	 *   weather data responses as they come back from the Adapter service. Note that we could just
	 *   as easily use {@link Source#queue(int, OverflowStrategy) Source.queue}, if we expected our
	 *   service to be consumed by another service and wanted to be able to react to backpressure.
	 * </li>
	 * <li>
	 *   For the weather data source, we use {@link Source#cycle(Creator) Source.cycle} to
	 *   continuously loop through the configured locations, mapping each to a call to the Adapter
	 *   service to fetch weather data, using a couple of
	 *   {@link Source#throttle(int, Duration) Source.throttle}s to regulate the frequency of
	 *   requests to the Adapter (and therefore to OpenWeatherMap) and emission of weather data to
	 *   the client.
	 * </li>
	 * <li>
	 *   The source is constructed first time by asking the WeatherEntity for the current stream
	 *   parameters (locations and emission frequency), and then we subscribe to parameter update
	 *   messages from the WeatherEntity so that we can rebuild the source on demand, whenever the
	 *   user modifies the parameters.
	 * </li>
	 * <li>
	 *   In order to be able to close the existing weather data stream whenever the parameters are
	 *   changed, we use {@link Source#viaMat(Graph, Function2) Source.viaMat} with a
	 *   {@link KillSwitches#single() KillSwitch flow stage} when we materialize the source. This
	 *   gives us back a KillSwitch reference, which will allow us to kill the stream by sending a
	 *   termination message; we can then reconstruct the source and materialize it into a new
	 *   KillSwitch, ready for the next update.
	 * </li>
	 * </ul>
	 *
	 * @return a {@link Source} of weather data
	 */
	public Source<CurrentWeatherResponse, ?> getSourceOfCurrentWeatherData() {
		return this.getSourceOfWeatherData(this.getCurrentWeather);
	}

	public Source<WeatherForecastResponse, ?> getSourceOfWeatherForecastData() {
		return this.getSourceOfWeatherData(this.getWeatherForecast);
	}

	private <T> Source<T, ?> getSourceOfWeatherData(final Function<String, CompletionStage<T>> getWeatherFunction) {
		return Source.<T>actorRef(3, OverflowStrategy.dropHead())
				.mapMaterializedValue(actorRef -> {
					// Set up the (responsive) weather data stream.
					generateCancellableStreamOfWeatherData(
							sourceOfWeatherData(this.entityId, getWeatherFunction),
							actorRef
					);
					subscribeToStreamParameterUpdates(getWeatherFunction, actorRef);
					return actorRef;
				})
				.watchTermination((source, future) -> {
					// This is termination of the downstream ActorRef sink.
					future.whenComplete((done, throwable) -> {
						// Always terminate the upstream source cleanly.
						terminateWeatherSource();
						logStreamTermination(throwable);
					});
					return source;
				});
	}

	private <T> void subscribeToStreamParameterUpdates(
			final Function<String, CompletionStage<T>> getWeatherFunction,
			final ActorRef actorRef
	) {
		this.pubSubRegistryFacade
				.subscribe(StreamParametersUpdated.class, Optional.of(this.entityId))
				.runForeach(
						update -> {
							log.info("Received notification of stream parameter changes, rebuilding source...");
							terminateWeatherSource();
							generateCancellableStreamOfWeatherData(
									sourceOfWeatherData(
											update.getLocations(),
											update.getEmitFrequencySecs(),
											getWeatherFunction
									),
									actorRef
							);
						},
						materializer
				);
	}

	private <T> void generateCancellableStreamOfWeatherData(
			final Source<T, NotUsed> source,
			final ActorRef actorRef
	) {
		this.sourceKillSwitch = source.viaMat(KillSwitches.single(), Keep.right())
				.toMat(sinkIntoActorRef(actorRef), Keep.left())
				.run(this.materializer);
	}

	private <T> Sink<T, ?> sinkIntoActorRef(final ActorRef actorRef) {
		return Sink.foreach(weather -> actorRef.tell(weather, ActorRef.noSender()));
	}

	private <T> Source<T, NotUsed> sourceOfWeatherData(
			final String entityId,
			final Function<String, CompletionStage<T>> getWeatherFunction
	) throws InterruptedException, ExecutionException, TimeoutException {
		return this.persistentEntityRegistryFacade.sendCommandToPersistentEntity(
				WeatherEntity.class,
				entityId,
				new GetWeatherStreamParameters()
		).thenApply(
				streamParameters -> sourceOfWeatherData(
						streamParameters.getLocations(),
						streamParameters.getEmitFrequencySeconds(),
						getWeatherFunction
				)
		).toCompletableFuture().get(5, TimeUnit.SECONDS);
	}

	private <T> Source<T, NotUsed> sourceOfWeatherData(
			final List<String> locations,
			final int frequency,
			final Function<String, CompletionStage<T>> getWeatherFunction
	) {
		// OWM free tier only allows 60 API calls per minute, so throttle to one per second.
		// In case OWM is slow, allow our own buffer of up to three concurrent requests.
		return Source.cycle(locations::iterator)
				.throttle(1, Duration.of(1, ChronoUnit.SECONDS))
				.mapAsync(3, getWeatherFunction)
				.throttle(1, Duration.of(frequency, ChronoUnit.SECONDS));
	}

	private void terminateWeatherSource() {
		if (this.sourceKillSwitch != null) {
			this.sourceKillSwitch.shutdown();
			this.sourceKillSwitch = null;
			log.info("Upstream weather source terminated successfully");
		}
	}

	private void logStreamTermination(final Throwable throwable) {
		if (throwable != null) {
			log.error("Client stream of weather data terminated with error", throwable);
		} else {
			log.info("Client stream of weather data closed successfully");
		}
	}
}

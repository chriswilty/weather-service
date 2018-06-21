package com.scottlogic.weather.weatherservice.impl.entity;

import akka.stream.javadsl.Source;
import com.google.inject.Inject;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.weatherservice.api.message.WeatherDataResponse;
import com.scottlogic.weather.weatherservice.impl.MessageUtils;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.GetCurrentWeatherStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * This is an event sourced entity. It has a state, {@link WeatherState}, which stores the current
 * list of weather locations and stream push frequency.
 * <p>
 * Event sourced entities are interacted with by sending them commands. This entity currently
 * supports one read-only command: {@link GetCurrentWeatherStream}.
 * </p>
 * <p>
 * Commands that are not read-only are translated to events, which are then persisted by the entity.
 * Each event has an event handler registered for it, which simply applies the event to the current
 * state. This is done when the event is first created (during command processing), and also when
 * the entity is rehydrated from the database - events are replayed in original order to recreate
 * the state of the entity.
 * </p>
 * <p>
 * This entity currently does not persist any events.
 */
public class WeatherEntity extends PersistentEntity<WeatherCommand, WeatherEvent, WeatherState> {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private final OwmAdapter owmAdapter;

	@Inject
	public WeatherEntity(final OwmAdapter owmAdapter) {
		this.owmAdapter = owmAdapter;
	}

	/**
	 * An entity can define different behaviours for different states, but it will
	 * always start with an initial behaviour. This entity only has one behaviour.
	 */
	@Override
	public Behavior initialBehavior(Optional<WeatherState> snapshotState) {
		final BehaviorBuilder b = newBehaviorBuilder(
				snapshotState.orElse(WeatherState.INITIAL_STATE)
		);

		b.setReadOnlyCommandHandler(GetCurrentWeatherStream.class, (cmd, ctx) -> {
			log.info("Received command to get a stream of weather data");
			ctx.reply(this.getSourceOfCurrentWeatherData());
		});

		return b.build();
	}

	private Source<WeatherDataResponse, ?> getSourceOfCurrentWeatherData() {
		return Source.cycle(() -> state().getLocations().iterator())
				.mapAsync(3, this::getCurrentWeatherForLocation)
				.throttle(1, Duration.of(state().getEmitFrequencySecs(), ChronoUnit.SECONDS))
				.watchTermination((source, future) -> {
					future.whenComplete((done, throwable) -> {
						if (throwable != null) {
							log.error("Stream of weather data terminated with error", throwable);
						} else {
							log.info("Stream of weather data closed following successful completion");
						}
					});
					return source;
				});
	}

	private CompletionStage<WeatherDataResponse> getCurrentWeatherForLocation(final String location) {
		return this.owmAdapter.getCurrentWeather(location).invoke()
				.thenApply(MessageUtils::transformWeatherData);
	}
}

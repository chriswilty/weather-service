package com.scottlogic.weather.weatherservice.impl.entity;

import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.scottlogic.weather.weatherservice.api.message.internal.WeatherStreamParameters;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.GetWeatherStreamParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * This is an event sourced entity. It has a state, {@link WeatherState}, which stores the current
 * list of weather locations and stream push frequency.
 * <p>
 * Event sourced entities are interacted with by sending them commands. This entity currently
 * supports one read-only command: {@link GetWeatherStreamParameters}.
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

	/**
	 * An entity can define different behaviours for different states, but it will
	 * always start with an initial behaviour. This entity only has one behaviour.
	 */
	@Override
	public Behavior initialBehavior(Optional<WeatherState> snapshotState) {
		final BehaviorBuilder b = newBehaviorBuilder(
				snapshotState.orElse(WeatherState.INITIAL_STATE)
		);

		b.setReadOnlyCommandHandler(GetWeatherStreamParameters.class, (cmd, ctx) -> {
			log.info("Received command to get weather stream parameters");

			ctx.reply(
					WeatherStreamParameters.builder()
							.emitFrequencySeconds(state().getEmitFrequencySecs())
							.locations(state().getLocations())
							.build()
			);
		});

		return b.build();
	}

}

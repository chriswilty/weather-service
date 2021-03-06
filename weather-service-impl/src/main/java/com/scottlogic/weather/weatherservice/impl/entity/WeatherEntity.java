package com.scottlogic.weather.weatherservice.impl.entity;

import akka.Done;
import com.google.inject.Inject;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.scottlogic.weather.weatherservice.api.message.StreamParametersUpdated;
import com.scottlogic.weather.weatherservice.api.message.WeatherStreamParameters;
import com.scottlogic.weather.weatherservice.impl.PubSubRegistryFacade;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.AddLocation;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.ChangeEmitFrequency;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.GetWeatherStreamParameters;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.RemoveLocation;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherEvent.EmitFrequencyChanged;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherEvent.LocationAdded;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherEvent.LocationRemoved;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableList.toImmutableList;

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
	private final PubSubRegistryFacade pubSubRegistryFacade;

	@Inject
	public WeatherEntity(final PubSubRegistryFacade pubSubRegistryFacade) {
		this.pubSubRegistryFacade = pubSubRegistryFacade;
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

		b.setCommandHandler(ChangeEmitFrequency.class, (cmd, ctx) -> {
			log.info("Received command to change emit frequency to [{}] seconds", cmd.getFrequencySeconds());
			return ctx.<EmitFrequencyChanged>thenPersist(
					new EmitFrequencyChanged(cmd.getFrequencySeconds()),
					this.<EmitFrequencyChanged>afterStreamParameterChangesPersisted(ctx)
			);
		});
		b.setEventHandler(
				EmitFrequencyChanged.class,
				evt -> state().withEmitFrequencySecs(evt.getFrequency())
		);

		b.setCommandHandler(AddLocation.class, (cmd, ctx) -> {
			log.info("Received command to add location [{}]", cmd.getLocation());
			return ctx.<LocationAdded>thenPersist(
					new LocationAdded(cmd.getLocation()),
					this.<LocationAdded>afterStreamParameterChangesPersisted(ctx)
			);
		});
		b.setEventHandler(
				LocationAdded.class,
				evt -> state().toBuilder()
						.location(evt.getLocation())
						.build()
		);

		b.setCommandHandler(RemoveLocation.class, (cmd, ctx) -> {
			final String location = cmd.getLocation();
			log.info("Received command to remove location [{}]", location);

			if (state().getLocations().contains(location)) {
				return ctx.<LocationRemoved>thenPersist(
						new LocationRemoved(cmd.getLocation()),
						this.<LocationRemoved>afterStreamParameterChangesPersisted(ctx)
				);
			} else {
				ctx.reply(Done.getInstance());
				return ctx.done();
			}
		});
		b.setEventHandler(
				LocationRemoved.class,
				evt -> state().withLocations(
						state().getLocations().stream()
								.filter(loc -> !loc.equals(evt.getLocation()))
								.collect(toImmutableList())
				)
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

	private <E extends WeatherEvent> Consumer<E> afterStreamParameterChangesPersisted(ReadOnlyCommandContext<Done> ctx) {
		return (E event) -> {
			ctx.reply(Done.getInstance());
			publishStreamParameters();
		};
	}

	private void publishStreamParameters() {
		log.info("Publishing change in stream parameters");
		pubSubRegistryFacade.publish(
				StreamParametersUpdated.class,
				StreamParametersUpdated.builder()
						.locations(state().getLocations())
						.emitFrequencySecs(state().getEmitFrequencySecs())
						.build(),
				Optional.of(entityId())
		);
	}

}

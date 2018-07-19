package com.scottlogic.weather.weatherservice.impl.entity;

import akka.Done;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.ImmutableList;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver.Outcome;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

@DisplayName("Tests for the persistent entity storing weather stream parameters")
class WeatherEntityTest {
	private static ActorSystem system;

	private final String entityId = "default";
	@Mock private PubSubRegistryFacade pubSubRegistryFacade;
	private PersistentEntityTestDriver<WeatherCommand, WeatherEvent, WeatherState> testDriver;

	@BeforeAll
	static void setup() {
		system = ActorSystem.create("WeatherEntityTest");
	}

	@AfterAll
	static void teardown() {
		TestKit.shutdownActorSystem(system);
		system = null;
	}

	@BeforeEach
	void beforeEach() {
		initMocks(this);
		testDriver = new PersistentEntityTestDriver<>(system, new WeatherEntity(pubSubRegistryFacade), entityId);
	}

	@Test
	void commandGetWeatherStreamParameters_ReturnsValuesFromCurrentState() {
		final WeatherState initialState = testDriver.initialize(Optional.empty()).state();

		final Outcome<WeatherEvent, WeatherState> outcome = testDriver.run(new GetWeatherStreamParameters());
		assertThat(outcome.issues(), is(empty()));
		assertThat(outcome.events(), is(empty()));
		assertThat(outcome.getReplies(), hasSize(1));

		final WeatherStreamParameters reply = (WeatherStreamParameters) outcome.getReplies().get(0);
		assertThat(reply.getEmitFrequencySeconds(), is(initialState.getEmitFrequencySecs()));
		assertThat(reply.getLocations(), is(initialState.getLocations()));
	}

	@Test
	void commandChangeEmitFrequency_PersistsEventAndUpdatesState() {
		final WeatherState initialState = testDriver.initialize(Optional.empty()).state();
		final int frequency = initialState.getEmitFrequencySecs() + new Random().nextInt(10) + 1;

		final Outcome<WeatherEvent, WeatherState> outcome = testDriver.run(new ChangeEmitFrequency(frequency));

		assertThat(outcome.issues(), is(empty()));

		final List<WeatherEvent> events = outcome.events();
		assertThat(events, hasSize(1));
		final EmitFrequencyChanged event = (EmitFrequencyChanged) events.get(0);
		assertThat(event.getFrequency(), is(frequency));

		final WeatherState nextState = outcome.state();
		assertThat(nextState.getLocations(), is(initialState.getLocations()));
		assertThat(nextState.getEmitFrequencySecs(), is(frequency));

		assertThat(outcome.getReplies(), hasSize(1));
		assertThat((Done) outcome.getReplies().get(0), isA(Done.class));
	}

	@Test
	void commandChangeEmitFrequency_PublishesStreamParameters() {
		final WeatherState initialState = testDriver.initialize(Optional.empty()).state();
		final int frequency = 99;
		final StreamParametersUpdated expectedMessage = StreamParametersUpdated.builder()
				.emitFrequencySecs(frequency)
				.locations(initialState.getLocations())
				.build();

		testDriver.run(new ChangeEmitFrequency(frequency));

		verify(pubSubRegistryFacade).publish(
				StreamParametersUpdated.class,
				expectedMessage,
				Optional.of(entityId)
		);
	}

	@Test
	void commandAddLocation_PersistsEventAndUpdatesState() {
		final WeatherState initialState = testDriver.initialize(Optional.empty()).state();
		final String location = "Somewhere";
		final List<String> expectedLocations = ImmutableList.<String>builder()
				.addAll(initialState.getLocations())
				.add(location)
				.build();

		final Outcome<WeatherEvent, WeatherState> outcome = testDriver.run(new AddLocation(location));

		assertThat(outcome.issues(), is(empty()));

		final List<WeatherEvent> events = outcome.events();
		assertThat(events, hasSize(1));
		final LocationAdded event = (LocationAdded) events.get(0);
		assertThat(event.getLocation(), is(location));

		final WeatherState nextState = outcome.state();
		assertThat(nextState.getEmitFrequencySecs(), is(initialState.getEmitFrequencySecs()));
		assertThat(nextState.getLocations(), is(expectedLocations));

		assertThat(outcome.getReplies(), hasSize(1));
		assertThat((Done) outcome.getReplies().get(0), isA(Done.class));
	}

	@Test
	void commandAddLocation_PublishesStreamParameters() {
		final WeatherState initialState = testDriver.initialize(Optional.empty()).state();
		final String location = "Somewhere Else, GB";
		final StreamParametersUpdated expectedMessage = StreamParametersUpdated.builder()
				.emitFrequencySecs(initialState.getEmitFrequencySecs())
				.locations(initialState.getLocations())
				.location(location)
				.build();

		testDriver.run(new AddLocation(location));

		verify(pubSubRegistryFacade).publish(
				StreamParametersUpdated.class,
				expectedMessage,
				Optional.of(entityId)
		);
	}

	@Test
	void commandRemoveLocation_LocationFound_PersistsEventAndUpdatesState_RepliesWithDone() {
		final WeatherState initialState = testDriver.initialize(Optional.empty()).state();
		final String location = initialState.getLocations().get(1);
		final List<String> expectedLocations = initialState.getLocations().stream()
				.filter(item -> !location.equals(item))
				.collect(toImmutableList());

		final Outcome<WeatherEvent, WeatherState> outcome = testDriver.run(new RemoveLocation(location));

		assertThat(outcome.issues(), is(empty()));

		final List<WeatherEvent> events = outcome.events();
		assertThat(events, hasSize(1));
		final LocationRemoved event = (LocationRemoved) events.get(0);
		assertThat(event.getLocation(), is(location));

		final WeatherState nextState = outcome.state();
		assertThat(nextState.getEmitFrequencySecs(), is(initialState.getEmitFrequencySecs()));
		assertThat(nextState.getLocations(), is(expectedLocations));

		assertThat(outcome.getReplies(), hasSize(1));
		assertThat((Done) outcome.getReplies().get(0), isA(Done.class));
	}

	@Test
	void commandRemoveLocation_LocationFound_PublishesStreamParameters() {
		final WeatherState initialState = testDriver.initialize(Optional.empty()).state();
		final List<String> locations = new ArrayList<>(initialState.getLocations());
		final String location = locations.remove(locations.size() - 2);
		final StreamParametersUpdated expectedMessage = StreamParametersUpdated.builder()
				.emitFrequencySecs(initialState.getEmitFrequencySecs())
				.locations(locations)
				.build();

		testDriver.run(new RemoveLocation(location));

		verify(pubSubRegistryFacade).publish(
				StreamParametersUpdated.class,
				expectedMessage,
				Optional.of(entityId)
		);
	}

	@Test
	void commandRemoveLocation_LocationNotFound_NoEventOrStateUpdateOrMessagePublished_RepliesWithDone() {
		final WeatherState initialState = testDriver.initialize(Optional.empty()).state();
		final String location = "Trumpsbrain, US";

		final Outcome<WeatherEvent, WeatherState> outcome = testDriver.run(new RemoveLocation(location));

		assertThat(outcome.issues(), is(empty()));
		assertThat(outcome.events(), is(empty()));
		assertThat(outcome.state(), is(initialState));
		assertThat(outcome.getReplies(), hasSize(1));
		assertThat((Done) outcome.getReplies().get(0), isA(Done.class));

		verifyZeroInteractions(pubSubRegistryFacade);
	}

}
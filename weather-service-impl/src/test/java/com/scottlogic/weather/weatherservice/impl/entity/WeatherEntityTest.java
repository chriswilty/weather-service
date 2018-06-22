package com.scottlogic.weather.weatherservice.impl.entity;

import akka.Done;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.ImmutableList;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver.Outcome;
import com.scottlogic.weather.weatherservice.api.message.WeatherStreamParameters;
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

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;

@DisplayName("Tests for the persistent entity storing weather stream parameters")
class WeatherEntityTest {
	private static ActorSystem system;

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
		testDriver = new PersistentEntityTestDriver<>(system, new WeatherEntity(), "default");
	}

	@Test
	void commandGetWeatherStreamParameters_ReturnsValuesFromCurrentState() {
		final WeatherState initialState = testDriver.initialize(Optional.empty()).state();

		final Outcome<WeatherEvent, WeatherState> outcomeOne = testDriver.run(new GetWeatherStreamParameters());
		assertThat(outcomeOne.issues(), is(empty()));
		assertThat(outcomeOne.events(), is(empty()));
		assertThat(outcomeOne.getReplies(), hasSize(1));

		final WeatherStreamParameters replyOne = (WeatherStreamParameters) outcomeOne.getReplies().get(0);
		assertThat(replyOne.getEmitFrequencySeconds(), is(initialState.getEmitFrequencySecs()));
		assertThat(replyOne.getLocations(), is(initialState.getLocations()));
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
	void commandRemoveLocation_LocationNotFound_NoEventOrStateUpdate_RepliesWithDone() {
		final WeatherState initialState = testDriver.initialize(Optional.empty()).state();
		final String location = "Trumpsbrain, US";

		final Outcome<WeatherEvent, WeatherState> outcome = testDriver.run(new RemoveLocation(location));

		assertThat(outcome.issues(), is(empty()));
		assertThat(outcome.events(), is(empty()));
		assertThat(outcome.state(), is(initialState));
		assertThat(outcome.getReplies(), hasSize(1));
		assertThat((Done) outcome.getReplies().get(0), isA(Done.class));
	}

}
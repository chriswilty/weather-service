package com.scottlogic.weather.weatherservice.impl.entity;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver.Outcome;
import com.scottlogic.weather.weatherservice.api.message.internal.WeatherStreamParameters;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.GetWeatherStreamParameters;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

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

}
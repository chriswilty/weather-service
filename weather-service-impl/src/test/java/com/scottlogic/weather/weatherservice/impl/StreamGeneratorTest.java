package com.scottlogic.weather.weatherservice.impl;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber.Probe;
import akka.stream.testkit.javadsl.TestSink;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.ImmutableList;
import com.scottlogic.weather.weatherservice.api.message.WeatherDataResponse;
import com.scottlogic.weather.weatherservice.api.message.WeatherStreamParameters;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.GetWeatherStreamParameters;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherEntity;
import com.scottlogic.weather.weatherservice.impl.stub.OwmAdapterStub;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class StreamGeneratorTest {

	private static ActorSystem system;
	private static Materializer materializer;

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final String entityId = "default";

	@Mock private PersistentEntityRegistryFacade registryFacade;

	private StreamGenerator sut;

	@BeforeAll
	static void setup() {
		system = ActorSystem.create("WeatherEntityTest");
		materializer = ActorMaterializer.create(system);
	}

	@AfterAll
	static void teardown() {
		TestKit.shutdownActorSystem(system);
		system = null;
	}

	@BeforeEach
	void beforeEach() {
		initMocks(this);
		sut = new StreamGenerator(new OwmAdapterStub(), registryFacade, system);
	}

	@Test
	void getSourceOfCurrentWeatherData_ReturnsStreamOfDataForLocationsInEntityState() {
		final int emitFrequency = 2;
		final List<String> locations = ImmutableList.of("London, UK", "Paris, FR", "New York, US");
		final FiniteDuration safeDuration = FiniteDuration.apply(emitFrequency + 1, TimeUnit.SECONDS);

		when(registryFacade.sendCommandToPersistentEntity(
				eq(WeatherEntity.class),
				eq(entityId),
				any(GetWeatherStreamParameters.class)
		)).thenReturn(
				CompletableFuture.completedFuture(
						WeatherStreamParameters.builder()
								.emitFrequencySeconds(emitFrequency)
								.locations(locations)
								.build()
				)
		);

		final Source<WeatherDataResponse, ?> source = sut.getSourceOfCurrentWeatherData(entityId);
		final Probe<WeatherDataResponse> probe = source.runWith(TestSink.probe(system), materializer);
		probe.request(4);

		final WeatherDataResponse elementOne = probe.expectNext(safeDuration);
		log.info("Stream emitted " + elementOne);
		final WeatherDataResponse elementTwo = probe.expectNext(safeDuration);
		log.info("Stream emitted " + elementTwo);
		final WeatherDataResponse elementThree = probe.expectNext(safeDuration);
		log.info("Stream emitted " + elementThree);
		final WeatherDataResponse elementFour = probe.expectNext(safeDuration);
		log.info("Stream emitted " + elementFour);
		probe.cancel();

		assertThat(elementOne.getLocation(), is(locations.get(0)));
		assertThat(elementTwo.getLocation(), is(locations.get(1)));
		assertThat(elementThree.getLocation(), is(locations.get(2)));
		assertThat(elementFour.getLocation(), is(locations.get(0)));
	}
}
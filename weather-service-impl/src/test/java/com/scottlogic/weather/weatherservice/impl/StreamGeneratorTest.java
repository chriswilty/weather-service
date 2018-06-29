package com.scottlogic.weather.weatherservice.impl;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.DelayOverflowStrategy;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber;
import akka.stream.testkit.javadsl.TestSink;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.ImmutableList;
import com.scottlogic.weather.weatherservice.api.message.StreamParametersUpdated;
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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class StreamGeneratorTest {

	private static ActorSystem system;
	private static Materializer materializer;

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final String entityId = "default";

	@Mock private PersistentEntityRegistryFacade persistentEntityRegistryFacade;
	@Mock private PubSubRegistryFacade pubSubRegistryFacade;

	private StreamGenerator sut;

	@BeforeAll
	static void setup() {
		system = ActorSystem.create("StreamGeneratorTest");
		materializer = ActorMaterializer.create(system);
	}

	@AfterAll
	static void teardown() {
		TestKit.shutdownActorSystem(system);
		materializer = null;
		system = null;
	}

	@BeforeEach
	void beforeEach() {
		initMocks(this);
		sut = new StreamGenerator(
				new OwmAdapterStub(),
				persistentEntityRegistryFacade,
				pubSubRegistryFacade,
				materializer,
				entityId
		);
	}

	@Test
	void getSourceOfCurrentWeatherData_ReturnsStreamOfDataForLocationsInEntityState() {
		final int emitFrequency = 2;
		final List<String> locations = ImmutableList.of("London, UK", "Paris, FR", "New York, US");
		final FiniteDuration safeDuration = FiniteDuration.apply(emitFrequency + 1, TimeUnit.SECONDS);

		when(persistentEntityRegistryFacade.sendCommandToPersistentEntity(
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

		doReturn(Source.maybe())
				.when(pubSubRegistryFacade).subscribe(
						StreamParametersUpdated.class,
						Optional.of(entityId)
				);

		final Source<WeatherDataResponse, ?> source = sut.getSourceOfCurrentWeatherData();
		final TestSubscriber.Probe<WeatherDataResponse> probe = source.runWith(TestSink.probe(system), materializer);
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

	@Test
	void getSourceOfCurrentWeatherData_StreamParametersUpdated_RestartsStreamOfWeatherData() {
		final int originalEmitFrequency = 2;
		final List<String> originalLocations = ImmutableList.of("London, UK", "Paris, FR", "New York, US");
		final FiniteDuration originalSafeDuration = FiniteDuration.apply(originalEmitFrequency + 1, TimeUnit.SECONDS);
		final int newEmitFrequency = 1;
		final List<String> newLocations = ImmutableList.of("Helsinki, FI", "Stockholm, SE");
		final FiniteDuration newSafeDuration = FiniteDuration.apply(newEmitFrequency + 1, TimeUnit.SECONDS);

		final Source<StreamParametersUpdated, ?> parameterChangesSource = Source.single(
				StreamParametersUpdated.builder()
						.emitFrequencySecs(newEmitFrequency)
						.locations(newLocations)
						.build()
		).delay(Duration.of(1, ChronoUnit.SECONDS), DelayOverflowStrategy.backpressure());

		when(persistentEntityRegistryFacade.sendCommandToPersistentEntity(
				eq(WeatherEntity.class),
				eq(entityId),
				any(GetWeatherStreamParameters.class)
		)).thenReturn(
				CompletableFuture.completedFuture(
						WeatherStreamParameters.builder()
								.emitFrequencySeconds(originalEmitFrequency)
								.locations(originalLocations)
								.build()
				)
		);

		doReturn(parameterChangesSource)
				.when(pubSubRegistryFacade).subscribe(
				StreamParametersUpdated.class,
				Optional.of(entityId)
		);

		final TestSubscriber.Probe<WeatherDataResponse> probe = sut.getSourceOfCurrentWeatherData()
				.runWith(TestSink.probe(system), materializer);
		probe.request(4);

		// Fetch one element ...
		final WeatherDataResponse elementOne = probe.expectNext(originalSafeDuration);
		log.info("Stream emitted " + elementOne);

		// ... and expect the (delayed) StreamParameterChanges message to have caused the stream to restart.
		final WeatherDataResponse elementTwo = probe.expectNext(newSafeDuration);
		log.info("Stream emitted " + elementTwo);
		final WeatherDataResponse elementThree = probe.expectNext(newSafeDuration);
		log.info("Stream emitted " + elementThree);
		final WeatherDataResponse elementFour = probe.expectNext(newSafeDuration);
		log.info("Stream emitted " + elementFour);
		probe.cancel();

		assertThat(elementOne.getLocation(), is(originalLocations.get(0)));
		assertThat(elementTwo.getLocation(), is(newLocations.get(0)));
		assertThat(elementThree.getLocation(), is(newLocations.get(1)));
		assertThat(elementFour.getLocation(), is(newLocations.get(0)));
	}
}
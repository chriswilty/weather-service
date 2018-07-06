package com.scottlogic.weather.weatherservice.impl;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.DelayOverflowStrategy;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber.Probe;
import akka.stream.testkit.javadsl.TestSink;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.ImmutableList;
import com.scottlogic.weather.weatherservice.api.message.CurrentWeatherResponse;
import com.scottlogic.weather.weatherservice.api.message.StreamParametersUpdated;
import com.scottlogic.weather.weatherservice.api.message.WeatherForecastResponse;
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
import static org.hamcrest.Matchers.hasSize;
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
	private final int emitFrequency = 2;
	private final int updatedEmitFrequency = 1;
	private final List<String> locations = ImmutableList.of("Edinburgh, UK", "Stockholm, SE", "Vancouver, CA");
	private final List<String> updatedLocations = ImmutableList.of("London, GB", "Paris, FR");
	private final FiniteDuration safeDuration = FiniteDuration.apply(emitFrequency + 1, TimeUnit.SECONDS);
	private final FiniteDuration updatedSafeDuration = FiniteDuration.apply(updatedEmitFrequency + 1, TimeUnit.SECONDS);

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
		stubPersistentEntityRegistryMock();
		stubPubSubRegistryMock(Source.maybe());

		final Probe<CurrentWeatherResponse> probe = sut.getSourceOfCurrentWeatherData()
				.runWith(TestSink.probe(system), materializer);
		probe.request(4);

		final CurrentWeatherResponse elementOne = probe.expectNext(safeDuration);
		log.info("Stream emitted " + elementOne);
		final CurrentWeatherResponse elementTwo = probe.expectNext(safeDuration);
		log.info("Stream emitted " + elementTwo);
		final CurrentWeatherResponse elementThree = probe.expectNext(safeDuration);
		log.info("Stream emitted " + elementThree);
		final CurrentWeatherResponse elementFour = probe.expectNext(safeDuration);
		log.info("Stream emitted " + elementFour);
		probe.cancel();

		assertThat(elementOne.getLocation(), is(locations.get(0)));
		assertThat(elementTwo.getLocation(), is(locations.get(1)));
		assertThat(elementThree.getLocation(), is(locations.get(2)));
		assertThat(elementFour.getLocation(), is(locations.get(0)));
	}

	@Test
	void getSourceOfCurrentWeatherData_StreamParametersUpdated_RestartsStreamOfWeatherData() {
		final Source<StreamParametersUpdated, ?> parameterChangesSource = Source.single(
				StreamParametersUpdated.builder()
						.emitFrequencySecs(updatedEmitFrequency)
						.locations(updatedLocations)
						.build()
		).delay(Duration.of(1, ChronoUnit.SECONDS), DelayOverflowStrategy.backpressure());

		stubPersistentEntityRegistryMock();
		stubPubSubRegistryMock(parameterChangesSource);

		final Probe<CurrentWeatherResponse> probe = sut.getSourceOfCurrentWeatherData()
				.runWith(TestSink.probe(system), materializer);
		probe.request(4);

		// Fetch one element ...
		final CurrentWeatherResponse elementOne = probe.expectNext(safeDuration);
		log.info("Stream emitted " + elementOne);

		// ... and expect the (delayed) StreamParameterChanges message to have caused the stream to restart.
		final CurrentWeatherResponse elementTwo = probe.expectNext(updatedSafeDuration);
		log.info("Stream emitted " + elementTwo);
		final CurrentWeatherResponse elementThree = probe.expectNext(updatedSafeDuration);
		log.info("Stream emitted " + elementThree);
		final CurrentWeatherResponse elementFour = probe.expectNext(updatedSafeDuration);
		log.info("Stream emitted " + elementFour);
		probe.cancel();

		assertThat(elementOne.getLocation(), is(locations.get(0)));
		assertThat(elementTwo.getLocation(), is(updatedLocations.get(0)));
		assertThat(elementThree.getLocation(), is(updatedLocations.get(1)));
		assertThat(elementFour.getLocation(), is(updatedLocations.get(0)));
	}

	@Test
	void getSourceOfWeatherForecastData_ReturnsStreamOfDataForLocationsInEntityState() {
		stubPersistentEntityRegistryMock();
		stubPubSubRegistryMock(Source.maybe());

		final Probe<WeatherForecastResponse> probe = sut.getSourceOfWeatherForecastData()
				.runWith(TestSink.probe(system), materializer);
		probe.request(4);

		final WeatherForecastResponse elementOne = probe.expectNext(safeDuration);
		log.info("Stream emitted " + elementOne);
		final WeatherForecastResponse elementTwo = probe.expectNext(safeDuration);
		log.info("Stream emitted " + elementTwo);
		final WeatherForecastResponse elementThree = probe.expectNext(safeDuration);
		log.info("Stream emitted " + elementThree);
		final WeatherForecastResponse elementFour = probe.expectNext(safeDuration);
		log.info("Stream emitted " + elementFour);
		probe.cancel();

		assertThat(elementOne.getLocation(), is(locations.get(0)));
		assertThat(elementOne.getForecast(), hasSize(40));
		assertThat(elementTwo.getLocation(), is(locations.get(1)));
		assertThat(elementTwo.getForecast(), hasSize(40));
		assertThat(elementThree.getLocation(), is(locations.get(2)));
		assertThat(elementThree.getForecast(), hasSize(40));
		assertThat(elementFour.getLocation(), is(locations.get(0)));
		assertThat(elementFour.getForecast(), hasSize(40));
	}

	@Test
	void getSourceOfWeatherForecastData_StreamParametersUpdated_RestartsStreamOfWeatherData() {
		final Source<StreamParametersUpdated, ?> parameterChangesSource = Source.single(
				StreamParametersUpdated.builder()
						.emitFrequencySecs(updatedEmitFrequency)
						.locations(updatedLocations)
						.build()
		).delay(Duration.of(1, ChronoUnit.SECONDS), DelayOverflowStrategy.backpressure());

		stubPersistentEntityRegistryMock();
		stubPubSubRegistryMock(parameterChangesSource);

		final Probe<WeatherForecastResponse> probe = sut.getSourceOfWeatherForecastData()
				.runWith(TestSink.probe(system), materializer);
		probe.request(4);

		// Fetch one element ...
		final WeatherForecastResponse elementOne = probe.expectNext(safeDuration);
		log.info("Stream emitted " + elementOne);

		// ... and expect the (delayed) StreamParameterChanges message to have caused the stream to restart.
		final WeatherForecastResponse elementTwo = probe.expectNext(updatedSafeDuration);
		log.info("Stream emitted " + elementTwo);
		final WeatherForecastResponse elementThree = probe.expectNext(updatedSafeDuration);
		log.info("Stream emitted " + elementThree);
		final WeatherForecastResponse elementFour = probe.expectNext(updatedSafeDuration);
		log.info("Stream emitted " + elementFour);
		probe.cancel();

		assertThat(elementOne.getLocation(), is(locations.get(0)));
		assertThat(elementOne.getForecast(), hasSize(40));
		assertThat(elementTwo.getLocation(), is(updatedLocations.get(0)));
		assertThat(elementTwo.getForecast(), hasSize(40));
		assertThat(elementThree.getLocation(), is(updatedLocations.get(1)));
		assertThat(elementThree.getForecast(), hasSize(40));
		assertThat(elementFour.getLocation(), is(updatedLocations.get(0)));
		assertThat(elementFour.getForecast(), hasSize(40));
	}

	private void stubPersistentEntityRegistryMock() {
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
	}

	private void stubPubSubRegistryMock(final Source<StreamParametersUpdated, ?> parameterUpdates) {
		doReturn(parameterUpdates)
				.when(pubSubRegistryFacade).subscribe(
						StreamParametersUpdated.class,
						Optional.of(entityId)
				);
	}
}
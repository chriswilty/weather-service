package com.scottlogic.weather.weatherservice.impl.entity;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber.Probe;
import akka.stream.testkit.javadsl.TestSink;
import akka.testkit.javadsl.TestKit;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver.Outcome;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.weatherservice.api.message.WeatherDataResponse;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.GetCurrentWeatherStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import play.api.libs.concurrent.MaterializerProvider;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.MockitoAnnotations.initMocks;

@DisplayName("Tests for the persistent entity storing weather stream parameters")
class WeatherEntityTest {
	private static ActorSystem system;
	private static Materializer materializer;

	@Mock
	private OwmAdapter owmAdapter;

	private PersistentEntityTestDriver<WeatherCommand, WeatherEvent, WeatherState> testDriver;

	@BeforeAll
	static void setup() {
		system = ActorSystem.create("WeatherEntityTest");
		materializer = new MaterializerProvider(system).get();
	}

	@AfterAll
	static void teardown() {
		TestKit.shutdownActorSystem(system);
		system = null;
		materializer = null;
	}

	@BeforeEach
	void beforeEach() {
		initMocks(this);
		testDriver = new PersistentEntityTestDriver<>(system, new WeatherEntity(owmAdapter), "default");
	}

	@Disabled(
			"This is broken because akka Source is not serializable. The implementation is also " +
			"broken in a cluster, because if the request arrived at a different instance to the " +
			"one in which the entity is located, then the reply containing the source would need " +
			"to be serialized for sending back to the original instance. " +
			"We need to rethink this approach... Probably it would be better for a dedicated " +
			"stream generator class to query the entity for its current state when creating a " +
			"stream, and subscribe to events that change the state. The entity would then not " +
			"need to know about our adapter service(s), and its job of persisting state would be " +
			"far clearer."
	)
	@Test
	void commandGetCurrentWeatherStream_PersistsZeroEventsAndReturnsASource() {
		final List<String> locations = testDriver.initialize(Optional.empty()).state().getLocations();
		final Outcome<WeatherEvent, WeatherState> result = testDriver.run(new GetCurrentWeatherStream());

		assertThat(result.issues(), is(empty()));
		assertThat(result.events(), is(empty()));
		assertThat(result.getReplies(), hasSize(1));

		final Source<WeatherDataResponse, ?> source = (Source<WeatherDataResponse, ?>) result.getReplies().get(0);
		final Probe<WeatherDataResponse> probe = source.runWith(TestSink.probe(system), materializer);

		probe.request(3);
		final WeatherDataResponse responseOne = probe.expectNext(FiniteDuration.apply(4, TimeUnit.SECONDS));
		final WeatherDataResponse responseTwo = probe.expectNext(FiniteDuration.apply(4, TimeUnit.SECONDS));
		final WeatherDataResponse responseThree = probe.expectNext(FiniteDuration.apply(4, TimeUnit.SECONDS));
		probe.cancel();

		assertThat(responseOne.getLocation(), is(locations.get(0)));
		assertThat(responseTwo.getLocation(), is(locations.get(1)));
		assertThat(responseThree.getLocation(), is(locations.get(2)));
	}

}
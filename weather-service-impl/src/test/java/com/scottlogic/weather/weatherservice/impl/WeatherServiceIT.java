package com.scottlogic.weather.weatherservice.impl;

import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.lightbend.lagom.javadsl.api.transport.TransportException;
import com.lightbend.lagom.javadsl.testkit.ServiceTest.TestServer;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.owmadapter.api.message.Unauthorized;
import com.scottlogic.weather.weatherservice.api.WeatherService;
import com.scottlogic.weather.weatherservice.api.message.CurrentWeatherResponse;
import com.scottlogic.weather.weatherservice.api.message.WeatherForecastResponse;
import com.scottlogic.weather.weatherservice.impl.stub.OwmAdapterStub;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.bind;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.startServer;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeatherServiceIT {

	private static TestServer server;

	private WeatherService weatherService;

	@BeforeAll
	static void beforeAll() {
		server = startServer(defaultSetup()
				.withCassandra()
				.configureBuilder(b -> b.overrides(
						bind(OwmAdapter.class).to(OwmAdapterStub.class),
						bind(StreamGeneratorFactory.class).toSelf(),
						bind(PersistentEntityRegistryFacade.class).toSelf()
				))
		);
	}

	@AfterAll
	static void afterAll() {
		if (server != null) {
			server.stop();
			server = null;
		}

	}

	@BeforeEach
	void beforeTest() {
	    weatherService = server.client(WeatherService.class);
	}

	@Test
	void currentWeather_ValidLocationName_RespondsWithWeatherData() throws Exception {
		final String location = "Stockholm, SE";

		final CurrentWeatherResponse response = weatherService.currentWeather(location).invoke()
				.toCompletableFuture().get(5, SECONDS);

		assertThat(response.getLocation(), is(location));
	}

	@Test
	void currentWeather_ValidLocationId_RespondsWithWeatherData() throws Exception {
		final int location = 564378;

		final CurrentWeatherResponse response = weatherService.currentWeather(Integer.toString(location)).invoke()
				.toCompletableFuture().get(5, SECONDS);

		assertThat(response.getId(), is(location));
	}

	@Test
	void currentWeather_SimulateBadApiKey_ThrowsUnauthorized() {
		final ExecutionException executionException = assertThrows(ExecutionException.class, () ->
				weatherService.currentWeather(OwmAdapterStub.LOCATION_401).invoke()
						.toCompletableFuture().get(5, SECONDS)
		);

		assertThat(
				((TransportException) executionException.getCause()).exceptionMessage().name(),
				is(Unauthorized.class.getSimpleName())
		);
		// This only works because of static injection for our exception serializer ... Yikes!
		assertThat(((Unauthorized) executionException.getCause()), isA(Unauthorized.class));
	}

	@Test
	void currentWeather_SimulateUnknownLocation_ThrowsNotFound() {
	    final ExecutionException executionException = assertThrows(ExecutionException.class, () ->
				weatherService.currentWeather(OwmAdapterStub.LOCATION_404).invoke()
						.toCompletableFuture().get(5, SECONDS)
		);

	    assertThat(((NotFound) executionException.getCause()), isA(NotFound.class));
	}

	@Test
	void weatherForecast_ValidLocationName_RespondsWithWeatherData() throws Exception {
	    final String location = "Helsinki, FI";

	    final WeatherForecastResponse response = weatherService.weatherForecast(location).invoke()
			    .toCompletableFuture().get(5, SECONDS);

		assertThat(response.getLocation(), is(location));
		assertThat(response.getForecast(), hasSize(40));
	}

	@Test
	void weatherForecast_ValidLocationId_RespondsWithWeatherData() throws Exception {
		final int location = 9823682;

		final WeatherForecastResponse response = weatherService.weatherForecast(Integer.toString(location)).invoke()
				.toCompletableFuture().get(5, SECONDS);

		assertThat(response.getId(), is(location));
		assertThat(response.getForecast(), hasSize(40));
	}

	@Test
	void weatherForecast_SimulateBadApiKey_ThrowsUnauthorized() {
		final ExecutionException executionException = assertThrows(ExecutionException.class, () ->
				weatherService.weatherForecast(OwmAdapterStub.LOCATION_401).invoke()
						.toCompletableFuture().get(5, SECONDS)
		);

		assertThat(
				((TransportException) executionException.getCause()).exceptionMessage().name(),
				is(Unauthorized.class.getSimpleName())
		);
		// This only works because of static injection for our exception serializer ... Yikes!
		assertThat(((Unauthorized) executionException.getCause()), isA(Unauthorized.class));
	}

	@Test
	void weatherForecast_SimulateUnknownLocation_ThrowsNotFound() {
		final ExecutionException executionException = assertThrows(ExecutionException.class, () ->
				weatherService.weatherForecast(OwmAdapterStub.LOCATION_404).invoke()
						.toCompletableFuture().get(5, SECONDS)
		);

		assertThat(((NotFound) executionException.getCause()), isA(NotFound.class));
	}

	// For brevity, omitting stream tests which are together covered by WeatherServiceTest and
	// StreamGeneratorTest; I intend to write standalone integration tests at a later date.
}

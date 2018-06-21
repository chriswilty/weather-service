package com.scottlogic.weather.weatherservice.impl;

import akka.stream.javadsl.Source;
import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.scottlogic.weather.owmadapter.api.message.Unauthorized;
import com.scottlogic.weather.weatherservice.api.message.WeatherDataResponse;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.GetCurrentWeatherStream;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherEntity;
import com.scottlogic.weather.weatherservice.impl.stub.OwmAdapterStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@DisplayName("Tests for the WeatherService implementation")
class WeatherServiceTest {

	private final String entityId = "default";

	@Mock private PersistentEntityRegistryFacade registryFacade;

	private WeatherServiceImpl sut;

	@BeforeEach
	void beforeEach() {
		initMocks(this);
		sut = new WeatherServiceImpl(new OwmAdapterStub(), registryFacade);
	}

	@Test
	void getCurrentWeather_LocationFound_RespondsWithWeatherData() throws Exception {
		final String location = "Edinburgh,UK";
		final WeatherDataResponse result = sut.currentWeather(location).invoke().toCompletableFuture().get(5, SECONDS);

		assertThat(result.getLocation(), is(location));
	}
	
	@Test
	void getCurrentWeather_AdapterThrowsUnauthorized() {
		assertThrows(Unauthorized.class, () ->
				sut.currentWeather(OwmAdapterStub.LOCATION_401).invoke().toCompletableFuture().get(5, SECONDS)
		);
	}

	@Test
	void getCurrentWeather_AdapterThrowsNotFound() {
		assertThrows(NotFound.class, () ->
				sut.currentWeather(OwmAdapterStub.LOCATION_404).invoke().toCompletableFuture().get(5, SECONDS)
		);
	}

	@Test
	void getWeatherDataStream_SendsCommandToWeatherEntityAndGetsBackASource() throws Exception {
		final Source<WeatherDataResponse, ?> expectedSource = Source.empty();

		when(registryFacade.sendCommandToPersistentEntity(
				eq(WeatherEntity.class),
				eq(entityId),
				any(GetCurrentWeatherStream.class))
		).thenReturn(
				CompletableFuture.completedFuture(expectedSource)
		);

		final Source<WeatherDataResponse, ?> result = sut.currentWeatherStream().invoke().toCompletableFuture().get(5, SECONDS);
		assertThat(result, is(expectedSource));
	}
}
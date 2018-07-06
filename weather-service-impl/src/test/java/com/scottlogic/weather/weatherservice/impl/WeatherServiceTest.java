package com.scottlogic.weather.weatherservice.impl;

import akka.Done;
import akka.stream.javadsl.Source;
import com.lightbend.lagom.javadsl.api.transport.BadRequest;
import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.scottlogic.weather.owmadapter.api.message.Unauthorized;
import com.scottlogic.weather.weatherservice.api.message.AddLocationRequest;
import com.scottlogic.weather.weatherservice.api.message.CurrentWeatherResponse;
import com.scottlogic.weather.weatherservice.api.message.SetEmitFrequencyRequest;
import com.scottlogic.weather.weatherservice.api.message.WeatherForecastResponse;
import com.scottlogic.weather.weatherservice.api.message.WeatherStreamParameters;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.AddLocation;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.ChangeEmitFrequency;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.GetWeatherStreamParameters;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.RemoveLocation;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherEntity;
import com.scottlogic.weather.weatherservice.impl.stub.OwmAdapterStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@DisplayName("Tests for the WeatherService implementation")
class WeatherServiceTest {

	private final String entityId = "default";
	private final CompletableFuture<Done> doneFuture = CompletableFuture.completedFuture(Done.getInstance());

	@Mock private StreamGeneratorFactory streamGeneratorFactory;
	@Mock private StreamGenerator streamGenerator;
	@Mock private PersistentEntityRegistryFacade registryFacade;

	private WeatherServiceImpl sut;

	@BeforeEach
	void beforeEach() {
		initMocks(this);
		when(streamGeneratorFactory.get(entityId)).thenReturn(streamGenerator);
		sut = new WeatherServiceImpl(new OwmAdapterStub(), streamGeneratorFactory, registryFacade);
	}

	@Test
	void currentWeather_LocationFound_RespondsWithCurrentWeather() throws Exception {
		final String location = "Edinburgh,UK";
		final CurrentWeatherResponse result = sut.currentWeather(location).invoke().toCompletableFuture().get(5, SECONDS);

		assertThat(result.getLocation(), is(location));
	}
	
	@Test
	void currentWeather_AdapterThrowsUnauthorized() {
		assertThrows(Unauthorized.class, () ->
				sut.currentWeather(OwmAdapterStub.LOCATION_401).invoke().toCompletableFuture().get(5, SECONDS)
		);
	}

	@Test
	void currentWeather_AdapterThrowsNotFound() {
		assertThrows(NotFound.class, () ->
				sut.currentWeather(OwmAdapterStub.LOCATION_404).invoke().toCompletableFuture().get(5, SECONDS)
		);
	}

	@Test
	void currentWeatherStream_InvokesSourceGeneratorAndGetsBackASource() throws Exception {
		final Source<WeatherForecastResponse, ?> expectedResponse = Source.empty();
		doReturn(expectedResponse).when(streamGenerator).getSourceOfCurrentWeatherData();

		final Source<CurrentWeatherResponse, ?> result = sut.currentWeatherStream().invoke()
				.toCompletableFuture().get(5, SECONDS);

		assertThat(result, is(expectedResponse));
	}

	@Test
	void weatherForecast_LocationFound_RespondsWithCurrentWeatherAndForecast() throws Exception {
		final String location = "Helsinki, FI";
		final WeatherForecastResponse result = sut.weatherForecast(location).invoke()
				.toCompletableFuture().get(5, SECONDS);

		assertThat(result.getLocation(), is(location));
		assertThat(result.getForecast().size(), is(40)); // 5 days, 8 forecasts per day
	}

	@Test
	void weatherForecastStream_InvokesSourceGeneratorAndGetsBackASource() throws Exception {
		final Source<WeatherForecastResponse, ?> expectedResponse = Source.maybe();
		doReturn(expectedResponse).when(streamGenerator).getSourceOfWeatherForecastData();

		final Source<WeatherForecastResponse, ?> result = sut.weatherForecastStream().invoke()
				.toCompletableFuture().get(5, SECONDS);

		assertThat(result, is(expectedResponse));
	}

	@Test
	void weatherForecast_AdapterThrowsUnauthorized() {
		assertThrows(Unauthorized.class, () ->
				sut.weatherForecast(OwmAdapterStub.LOCATION_401).invoke().toCompletableFuture().get(5, SECONDS)
		);
	}

	@Test
	void weatherForecast_AdapterThrowsNotFound() {
		assertThrows(NotFound.class, () ->
				sut.weatherForecast(OwmAdapterStub.LOCATION_404).invoke().toCompletableFuture().get(5, SECONDS)
		);
	}

	@Test
	void weatherStreamParameters_SendsCommandToEntity() throws Exception {
		final WeatherStreamParameters expectedResponse = WeatherStreamParameters.builder()
				.location("Aaaaa, AA")
				.location("Bbbbbbb")
				.emitFrequencySeconds(20)
				.build();

		when(registryFacade.sendCommandToPersistentEntity(
				eq(WeatherEntity.class),
				eq(entityId),
				any(GetWeatherStreamParameters.class)
		)).thenReturn(
				CompletableFuture.completedFuture(expectedResponse)
		);

		final WeatherStreamParameters result = sut.weatherStreamParameters().invoke().toCompletableFuture().get(5, SECONDS);
		assertThat(result, is(expectedResponse));
	}

	@Test
	void setEmitFrequency_ValidRequest_SendsCommandToEntity() throws Exception {
		final int frequency = 999;
		final ArgumentCaptor<ChangeEmitFrequency> captor = ArgumentCaptor.forClass(ChangeEmitFrequency.class);

		when(registryFacade.sendCommandToPersistentEntity(
				eq(WeatherEntity.class),
				eq(entityId),
				captor.capture()
		)).thenReturn(doneFuture);

		sut.setEmitFrequency().invoke(new SetEmitFrequencyRequest(frequency)).toCompletableFuture().get(5, SECONDS);

		final ChangeEmitFrequency command = captor.getValue();
		assertThat(command.getFrequencySeconds(), is(frequency));
	}

	@Test
	void setEmitFrequency_ValueOne_ThrowsBadRequest() {
		assertThrows(BadRequest.class, () ->
				sut.setEmitFrequency().invoke(new SetEmitFrequencyRequest(1)).toCompletableFuture().get(5, SECONDS)
		);
	}

	@Test
	void setEmitFrequency_ValueZero_ThrowsBadRequest() {
		assertThrows(BadRequest.class, () ->
				sut.setEmitFrequency().invoke(new SetEmitFrequencyRequest(0)).toCompletableFuture().get(5, SECONDS)
		);
	}

	@Test
	void setEmitFrequency_ValueLessThanZero_ThrowsBadRequest() {
		assertThrows(BadRequest.class, () ->
				sut.setEmitFrequency().invoke(new SetEmitFrequencyRequest(-1)).toCompletableFuture().get(5, SECONDS)
		);
	}

	@Test
	void setEmitFrequency_EmptyRequestBody_ThrowsBadRequest() {
		assertThrows(BadRequest.class, () ->
				sut.setEmitFrequency().invoke(null).toCompletableFuture().get(5, SECONDS)
		);
	}

	@Test
	void addLocation_ValidRequest_SendsCommandToEntity()  throws Exception {
		final String location = "Somewhere";
		final ArgumentCaptor<AddLocation> captor = ArgumentCaptor.forClass(AddLocation.class);

		when(registryFacade.sendCommandToPersistentEntity(
				eq(WeatherEntity.class),
				eq(entityId),
				captor.capture()
		)).thenReturn(doneFuture);

		sut.addLocation().invoke(new AddLocationRequest(location)).toCompletableFuture().get(5, SECONDS);

		final AddLocation command = captor.getValue();
		assertThat(command.getLocation(), is(location));
	}

	@Test
	void addLocation_EmptyLocationInRequest_ThrowsBadRequest() {
		assertThrows(BadRequest.class, () ->
				sut.addLocation().invoke(new AddLocationRequest("")).toCompletableFuture().get(5, SECONDS)
		);
	}

	@Test
	void addLocation_BlankLocationInRequest_ThrowsBadRequest() {
		assertThrows(BadRequest.class, () ->
				sut.addLocation().invoke(new AddLocationRequest(" ")).toCompletableFuture().get(5, SECONDS)
		);
	}

	@Test
	void addLocation_EmptyRequestBody_ThrowsBadRequest() {
		assertThrows(BadRequest.class, () ->
				sut.addLocation().invoke(null).toCompletableFuture().get(5, SECONDS)
		);
	}

	@Test
	void removeLocation_AlwaysSuccessful() throws Exception {
		final String location = "Anywhere";
		final ArgumentCaptor<RemoveLocation> captor = ArgumentCaptor.forClass(RemoveLocation.class);

		when(registryFacade.sendCommandToPersistentEntity(
				eq(WeatherEntity.class),
				eq(entityId),
				captor.capture()
		)).thenReturn(doneFuture);

		sut.removeLocation(location).invoke().toCompletableFuture().get(5, SECONDS);

		final RemoveLocation command = captor.getValue();
		assertThat(command.getLocation(), is(location));
	}
}
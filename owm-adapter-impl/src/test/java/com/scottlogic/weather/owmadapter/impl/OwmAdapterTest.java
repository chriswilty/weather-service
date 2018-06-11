package com.scottlogic.weather.owmadapter.impl;

import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.owmadapter.api.message.WeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@DisplayName("Tests for the OWM Adapter implementation")
class OwmAdapterTest {
	private OwmAdapter sut;

	@BeforeEach
	void beforeEach() {
		sut = new OwmAdapterImpl();
	}

	@Test
	void getCurrentWeather_LocationFound_RespondsWithWeatherData() throws Exception {
		final String location = "Edinburgh, UK";

		final WeatherData response = sut.getCurrentWeather(location).invoke().toCompletableFuture().get(5, SECONDS);
		assertThat(response.getName(), is(location));
	}
}

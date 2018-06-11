package com.scottlogic.weather.impl;

import com.scottlogic.weather.api.message.WeatherDataResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@DisplayName("Tests for the WeatherService implementation")
public class WeatherServiceImplTest {
	private WeatherServiceImpl sut;

	@BeforeEach
	public void beforeEach() {
		sut = new WeatherServiceImpl();
	}

	@Test
	public void getWeatherData_LocationFound_ReturnsWeatherData() throws Exception {
		final String location = "Edinburgh,UK";
		final WeatherDataResponse result = sut.getCurrentWeather(location).invoke().toCompletableFuture().get(5, SECONDS);

		assertThat(result.getLocation(), is(location));
	}
}
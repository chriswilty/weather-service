package com.scottlogic.weather.owmadapter.api.message.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonDeserialize
public class OwmForecastWeatherResponse {
	private final City city;

	@JsonProperty("list")
	private final List<Forecast> forecasts;
}

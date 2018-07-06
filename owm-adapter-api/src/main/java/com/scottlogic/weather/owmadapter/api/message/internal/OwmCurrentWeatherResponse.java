package com.scottlogic.weather.owmadapter.api.message.internal;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
@JsonDeserialize
public class OwmCurrentWeatherResponse {
	int id;
	String name;

	@JsonProperty("dt")
	@JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
	Instant measuredAt;

	@JsonProperty("sys")
	Locale localeData;

	@JsonProperty("coord")
	Coordinates coordinates;

	List<Weather> weather;

	@JsonProperty("main")
	Temperature temperature;

	Wind wind;
}

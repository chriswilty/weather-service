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
public class OwmWeatherResponse {

	private final int id;
	private final String name;

	@JsonProperty("dt")
	@JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
	private final Instant measuredAt;

	@JsonProperty("sys")
	private final Locale localeData;

	@JsonProperty("coord")
	private final Coordinates coordinates;

	private final List<Weather> weather;

	@JsonProperty("main")
	private final Temperature temperature;

	private final Wind wind;
}

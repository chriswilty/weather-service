package com.scottlogic.weather.owmadapter.api.message.internal;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.time.Instant;
import java.util.List;

@Value
@Builder
@Wither
public class Forecast {
	@JsonProperty("dt")
	@JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
	Instant measuredAt;

	List<Weather> weather;

	@JsonProperty("main")
	Temperature temperature;

	Wind wind;
}

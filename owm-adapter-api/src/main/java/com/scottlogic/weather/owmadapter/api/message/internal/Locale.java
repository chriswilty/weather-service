package com.scottlogic.weather.owmadapter.api.message.internal;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class Locale {
	@JsonProperty("country")
	String countryCode;

	@JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
	Instant sunrise;

	@JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
	Instant sunset;
}

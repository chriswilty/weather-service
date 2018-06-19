package com.scottlogic.weather.owmadapter.api.message.internal;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Value
@Builder
public class Locale {

	@JsonProperty("country")
	private final String countryCode;

	@JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
	private final Instant sunrise;

	@JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
	private final Instant sunset;

	public LocalDateTime getSunrise() {
		return LocalDateTime.ofInstant(sunrise, ZoneId.systemDefault());
	}

	public LocalDateTime getSunset() {
		return LocalDateTime.ofInstant(sunset, ZoneId.systemDefault());
	}
}

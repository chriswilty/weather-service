package com.scottlogic.weather.owmadapter.api.message;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class Sun {
	private final OffsetDateTime sunrise;
	private final OffsetDateTime sunset;
}

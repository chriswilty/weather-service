package com.scottlogic.weather.weatherservice.api.message;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class Sun {
	OffsetDateTime sunrise;
	OffsetDateTime sunset;
}

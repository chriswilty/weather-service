package com.scottlogic.weather.weatherservice.api.message;

import lombok.Value;

@Value
public class SetEmitFrequencyRequest {
	private final int frequency;
}

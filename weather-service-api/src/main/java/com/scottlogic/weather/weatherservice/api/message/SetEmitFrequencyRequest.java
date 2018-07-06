package com.scottlogic.weather.weatherservice.api.message;

import lombok.Value;

@Value
public class SetEmitFrequencyRequest {
	int frequency;
}

package com.scottlogic.weather.api.message;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Weather {
	private final short id;
	private final String description;
}

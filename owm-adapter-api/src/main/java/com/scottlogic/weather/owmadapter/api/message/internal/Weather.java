package com.scottlogic.weather.owmadapter.api.message.internal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Weather {
	private final int id;
	private final String description;
}

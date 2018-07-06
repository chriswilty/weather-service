package com.scottlogic.weather.owmadapter.api.message;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Weather {
	int id;
	String description;
}

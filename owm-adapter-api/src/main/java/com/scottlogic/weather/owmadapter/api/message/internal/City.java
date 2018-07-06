package com.scottlogic.weather.owmadapter.api.message.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class City {
	int id;
	String name;

	@JsonProperty("coord")
	Coordinates coordinates;

	@JsonProperty("country")
	String countryCode;
}

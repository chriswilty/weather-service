package com.scottlogic.weather.owmadapter.api.message.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class City {

	private final int id;
	private final String name;

	@JsonProperty("coord")
	private final Coordinates coordinates;

	@JsonProperty("country")
	private final String countryCode;

}

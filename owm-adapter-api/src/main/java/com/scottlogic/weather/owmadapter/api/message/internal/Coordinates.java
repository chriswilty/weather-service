package com.scottlogic.weather.owmadapter.api.message.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Coordinates {

	@JsonProperty("lon")
	private final double longitude;

	@JsonProperty("lat")
	private final double latitude;
}

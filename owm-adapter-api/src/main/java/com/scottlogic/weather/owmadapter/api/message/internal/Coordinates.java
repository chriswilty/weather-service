package com.scottlogic.weather.owmadapter.api.message.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Coordinates {
	@JsonProperty("lon")
	double longitude;

	@JsonProperty("lat")
	double latitude;
}

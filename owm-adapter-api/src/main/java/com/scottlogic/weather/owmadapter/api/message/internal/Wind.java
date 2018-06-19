package com.scottlogic.weather.owmadapter.api.message.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class Wind {

	private final BigDecimal speed;

	@JsonProperty("deg")
	private final short fromDegrees;
}

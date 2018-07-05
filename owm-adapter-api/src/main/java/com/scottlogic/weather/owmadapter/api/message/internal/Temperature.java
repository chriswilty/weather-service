package com.scottlogic.weather.owmadapter.api.message.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class Temperature {

	private final BigDecimal temp;

	@JsonProperty("temp_min")
	private final BigDecimal tempMin;

	@JsonProperty("temp_max")
	private final BigDecimal tempMax;

	// TODO!
	//private final short humidity; // percent
}

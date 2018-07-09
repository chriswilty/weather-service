package com.scottlogic.weather.owmadapter.api.message.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class Temperature {
	BigDecimal temp;

	@JsonProperty("temp_min")
	BigDecimal tempMin;

	@JsonProperty("temp_max")
	BigDecimal tempMax;

	short humidity; // percent
}

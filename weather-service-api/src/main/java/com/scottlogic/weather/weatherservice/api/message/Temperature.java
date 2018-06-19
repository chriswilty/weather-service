package com.scottlogic.weather.weatherservice.api.message;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.scottlogic.weather.weatherservice.api.serialization.BigDecimalToStringSerializer;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class Temperature {

	@JsonSerialize(using = BigDecimalToStringSerializer.class)
	private final BigDecimal current;

	@JsonSerialize(using = BigDecimalToStringSerializer.class)
	private final BigDecimal minimum;

	@JsonSerialize(using = BigDecimalToStringSerializer.class)
	private final BigDecimal maximum;
	
}

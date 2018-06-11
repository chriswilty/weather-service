package com.scottlogic.weather.weatherservice.api.message;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class Temperature {
	private final BigDecimal current;
	private final BigDecimal minimum;
	private final BigDecimal maximum;
}

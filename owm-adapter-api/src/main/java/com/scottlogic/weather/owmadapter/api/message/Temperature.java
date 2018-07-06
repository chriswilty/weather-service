package com.scottlogic.weather.owmadapter.api.message;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class Temperature {
	BigDecimal current;
	BigDecimal minimum;
	BigDecimal maximum;
}

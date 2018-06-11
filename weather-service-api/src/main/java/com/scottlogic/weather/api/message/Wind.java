package com.scottlogic.weather.api.message;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class Wind {
	private final BigDecimal speed;
	private final short fromDegrees;
}

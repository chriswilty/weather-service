package com.scottlogic.weather.weatherservice.api.message;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.scottlogic.weather.common.serializer.BigDecimalSerializer;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class Temperature {

	@JsonSerialize(using = BigDecimalSerializer.class)
	private final BigDecimal current;

	@JsonSerialize(using = BigDecimalSerializer.class)
	private final BigDecimal minimum;

	@JsonSerialize(using = BigDecimalSerializer.class)
	private final BigDecimal maximum;
}

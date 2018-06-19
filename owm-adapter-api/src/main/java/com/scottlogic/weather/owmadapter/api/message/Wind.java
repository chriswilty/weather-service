package com.scottlogic.weather.owmadapter.api.message;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkState;

@Value
@Builder
public class Wind {

	private final BigDecimal speed;
	private final short fromDegrees;

	public Wind(final BigDecimal speed, final short fromDegrees) {
		checkState(
				fromDegrees >= 0 && fromDegrees < 360,
				"fromDegrees must be between 0 and 359 degrees"
		);

		this.speed = speed;
		this.fromDegrees = fromDegrees;
	}
}

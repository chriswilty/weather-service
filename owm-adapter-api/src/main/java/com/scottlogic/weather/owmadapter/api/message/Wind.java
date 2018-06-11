package com.scottlogic.weather.owmadapter.api.message;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkState;

@Value
@Builder
public class Wind {
	private final short fromDegrees;
	private final BigDecimal speed;

	public Wind(final short fromDegrees, final BigDecimal speed) {
		checkState(
				fromDegrees >= 0 && fromDegrees < 360,
				"fromDegrees must be between 0 and 359 degrees"
		);

		this.fromDegrees = fromDegrees;
		this.speed = speed;
	}
}

package com.scottlogic.weather.weatherservice.api.message;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.scottlogic.weather.weatherservice.api.serialization.BigDecimalToStringSerializer;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkState;

@Value
@Builder
public class Wind {

	@JsonSerialize(using = BigDecimalToStringSerializer.class)
	private final BigDecimal speed;

	private final short fromDegrees;

	public Wind(final BigDecimal speed, final short fromDegrees) {
		// Yes, OWM sometimes sends 360 degrees:
		checkState(
				fromDegrees >= 0 && fromDegrees <= 360,
				"fromDegrees should be between 0 and 360, but received [" + fromDegrees + "]"
		);

		this.speed = speed;
		this.fromDegrees = fromDegrees;
	}
}

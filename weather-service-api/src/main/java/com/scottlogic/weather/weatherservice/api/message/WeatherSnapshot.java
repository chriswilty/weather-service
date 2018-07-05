package com.scottlogic.weather.weatherservice.api.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.time.OffsetDateTime;

@Value
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class WeatherSnapshot {
	OffsetDateTime measured;
	Weather weather;
	Temperature temperature;
	Wind wind;

	@NonFinal Sun sun; // Can be null! Lombok does not support Optionals.
}

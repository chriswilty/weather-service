package com.scottlogic.weather.weatherservice.api.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
@JsonSerialize
public class WeatherDataResponse implements Jsonable {

	private final String location;

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	private final LocalDateTime measured;

	private final Weather weather;

	private final Temperature temperature;

	private final Wind wind;

	private final Sun sun;
}

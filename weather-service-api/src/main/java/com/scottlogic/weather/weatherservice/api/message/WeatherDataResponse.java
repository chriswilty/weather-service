package com.scottlogic.weather.weatherservice.api.message;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonSerialize
public class WeatherDataResponse implements Jsonable {
	private final String location;
	private final Weather weather;
	private final Temperature temperature;
	private final Wind wind;
}

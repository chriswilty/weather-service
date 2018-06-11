package com.scottlogic.weather.owmadapter.api.message;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonSerialize
public class WeatherData implements Jsonable {
	private final int id;
	private final String name;
	private final Weather weather;
	private final Temperature temperature;
	private final Wind wind;
}

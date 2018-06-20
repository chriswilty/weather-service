package com.scottlogic.weather.owmadapter.api.message;

import com.lightbend.lagom.serialization.Jsonable;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class WeatherData implements Jsonable {
	private final int id;
	private final String name;
	private final OffsetDateTime measured;
	private final Weather weather;
	private final Temperature temperature;
	private final Wind wind;
	private final Sun sun;
}

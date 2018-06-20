package com.scottlogic.weather.weatherservice.api.message;

import com.lightbend.lagom.serialization.Jsonable;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class WeatherDataResponse implements Jsonable {
	private final String location;
	private final OffsetDateTime measured;
	private final Weather weather;
	private final Temperature temperature;
	private final Wind wind;
	private final Sun sun;
}

package com.scottlogic.weather.owmadapter.api.message;

import com.lightbend.lagom.serialization.Jsonable;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.Wither;

import java.time.OffsetDateTime;

@Value
@Wither
@Builder(toBuilder = true)
public class WeatherData implements Jsonable {
	int id;
	String location;
	OffsetDateTime measured;
	Weather weather;
	Temperature temperature;
	Wind wind;

	@NonFinal Sun sun; // Can be null! Lombok does not support Optionals.
}

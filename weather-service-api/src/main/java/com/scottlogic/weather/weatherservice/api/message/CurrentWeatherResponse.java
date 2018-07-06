package com.scottlogic.weather.weatherservice.api.message;

import com.lightbend.lagom.serialization.Jsonable;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CurrentWeatherResponse implements Jsonable {
	String location;
	WeatherSnapshot current;
}

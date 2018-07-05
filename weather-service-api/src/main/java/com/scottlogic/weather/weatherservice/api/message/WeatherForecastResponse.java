package com.scottlogic.weather.weatherservice.api.message;

import com.lightbend.lagom.serialization.Jsonable;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class WeatherForecastResponse implements Jsonable {
	String location;
	WeatherSnapshot current;
	List<WeatherSnapshot> forecast;
}

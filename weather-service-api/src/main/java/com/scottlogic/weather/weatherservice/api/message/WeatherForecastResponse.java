package com.scottlogic.weather.weatherservice.api.message;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class WeatherForecastResponse implements WeatherResponse {
	int id;
	String location;
	WeatherSnapshot current;
	List<WeatherSnapshot> forecast;
}

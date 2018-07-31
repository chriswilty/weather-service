package com.scottlogic.weather.weatherservice.api.message;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CurrentWeatherResponse implements WeatherResponse {
	int id;
	String location;
	WeatherSnapshot current;
}

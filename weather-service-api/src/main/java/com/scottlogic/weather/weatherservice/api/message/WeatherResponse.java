package com.scottlogic.weather.weatherservice.api.message;

import com.lightbend.lagom.serialization.Jsonable;

public interface WeatherResponse extends Jsonable {
	int getId();
	String getLocation();
	WeatherSnapshot getCurrent();
}

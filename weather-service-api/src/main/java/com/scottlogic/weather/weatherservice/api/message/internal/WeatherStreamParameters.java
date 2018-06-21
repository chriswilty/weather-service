package com.scottlogic.weather.weatherservice.api.message.internal;

import com.lightbend.lagom.serialization.Jsonable;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class WeatherStreamParameters implements Jsonable {

	short emitFrequencySeconds;

	@Singular
	@NonNull
	List<String> locations;
}

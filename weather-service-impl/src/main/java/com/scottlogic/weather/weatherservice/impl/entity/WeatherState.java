package com.scottlogic.weather.weatherservice.impl.entity;

import com.lightbend.lagom.serialization.CompressedJsonable;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.List;

@Value
@Wither
@Builder
public final class WeatherState implements CompressedJsonable {

	public static final WeatherState INITIAL_STATE = WeatherState.builder()
			.emitFrequencySecs((short) 3)
			.location("Edinburgh, GB")
			.location("London, GB")
			.location("Falkirk, GB")
			.build();

	short emitFrequencySecs;

	@NonNull
	@Singular
	List<String> locations;
}

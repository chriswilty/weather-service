package com.scottlogic.weather.weatherservice.api.message;

import com.lightbend.lagom.serialization.CompressedJsonable;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class StreamParametersUpdated implements CompressedJsonable {
	int emitFrequencySecs;

	@NonNull
	@Singular
	List<String> locations;
}

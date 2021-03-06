package com.scottlogic.weather.weatherservice.impl.entity;

import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventShards;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTagger;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.Value;

public interface WeatherEvent extends Jsonable, AggregateEvent<WeatherEvent> {

	/**
	 * Tags are used for getting and publishing streams of events. Each event will have this tag,
	 * and in this case, we are partitioning the tags into 4 shards, which means we can have 4
	 * concurrent processors/publishers of events. We don't anticipate huge demand for weather data.
	 */
	AggregateEventShards<WeatherEvent> TAG = AggregateEventTag.sharded(WeatherEvent.class, 4);

	@Override
	default AggregateEventTagger<WeatherEvent> aggregateTag() {
		return TAG;
	}

	/**
	 * Event representing a change in frequency (seconds) at which weather data should be emitted.
	 */
	@Value
	final class EmitFrequencyChanged implements WeatherEvent {
		private final int frequency;
	}

	/**
	 * Event representing addition of a location for streaming weather data.
	 */
	@Value
	final class LocationAdded implements WeatherEvent {
		private final String location;
	}

	/**
	 * Event representing removal of a location for streaming weather data.
	 */
	@Value
	final class LocationRemoved implements WeatherEvent {
		private final String location;
	}
}

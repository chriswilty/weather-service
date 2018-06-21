package com.scottlogic.weather.weatherservice.impl.entity;

import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventShards;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTagger;
import com.lightbend.lagom.serialization.Jsonable;

public interface WeatherEvent extends Jsonable, AggregateEvent<WeatherEvent> {

	/**
	 * Tags are used for getting and publishing streams of events. Each event
	 * will have this tag, and in this case, we are partitioning the tags into
	 * 4 shards, which means we can have 4 concurrent processors/publishers of
	 * events. We don't anticipate huge demand for weather data :)
	 */
	AggregateEventShards<WeatherEvent> TAG = AggregateEventTag.sharded(WeatherEvent.class, 4);

	@Override
	default AggregateEventTagger<WeatherEvent> aggregateTag() {
		return TAG;
	}

	// No persisted events yet...
}

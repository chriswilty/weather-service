package com.scottlogic.weather.weatherservice.impl.entity;

import akka.stream.javadsl.Source;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.serialization.Jsonable;
import com.scottlogic.weather.weatherservice.api.message.WeatherDataResponse;

public interface WeatherCommand extends Jsonable {

	class GetCurrentWeatherStream implements WeatherCommand, PersistentEntity.ReplyType<Source<WeatherDataResponse, ?>> {
		// This is a read-only command, with no parameters.
	}
}

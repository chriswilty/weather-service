package com.scottlogic.weather.weatherservice.impl.entity;

import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.serialization.Jsonable;
import com.scottlogic.weather.weatherservice.api.message.internal.WeatherStreamParameters;
import lombok.Value;

public interface WeatherCommand extends Jsonable {

	@Value
	class GetWeatherStreamParameters implements WeatherCommand, PersistentEntity.ReplyType<WeatherStreamParameters> {
		// This is a read-only command, with no parameters.
	}

}

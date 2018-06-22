package com.scottlogic.weather.weatherservice.impl.entity;

import akka.Done;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.serialization.Jsonable;
import com.scottlogic.weather.weatherservice.api.message.WeatherStreamParameters;
import lombok.Value;

public interface WeatherCommand extends Jsonable {

	/**
	 * A read-only command to get the current streaming parameters.
	 */
	@Value
	class GetWeatherStreamParameters implements WeatherCommand, PersistentEntity.ReplyType<WeatherStreamParameters> {}

	/**
	 * A command to set the frequency of emission of streamed weather data.
	 * <p>
	 *   Reply type is {@link akka.Done}, which is sent back to the caller
	 *   when all events emitted by this command are successfully persisted.
	 * </p>
	 */
	@Value
	class ChangeEmitFrequency implements WeatherCommand, PersistentEntity.ReplyType<Done> {
		private final int frequencySeconds;
	}

	/**
	 * A command to add a location to retrieve streaming weather data for.
	 * <p>
	 *   Reply type is {@link akka.Done}, which is sent back to the caller
	 *   when all events emitted by this command are successfully persisted.
	 * </p>
	 */
	@Value
	class AddLocation implements WeatherCommand, PersistentEntity.ReplyType<Done> {
		private final String location;
	}

	/**
	 * A command to remove a location from the list for streaming weather data.
	 * <p>
	 *   Reply type is {@link akka.Done}, which is sent back to the caller
	 *   when all events emitted by this command are successfully persisted.
	 * </p>
	 */
	@Value
	class RemoveLocation implements WeatherCommand, PersistentEntity.ReplyType<Done> {
		private final String location;
	}

}

package com.scottlogic.weather.weatherservice.api;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.scottlogic.weather.weatherservice.api.message.WeatherDataResponse;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;
import static com.lightbend.lagom.javadsl.api.transport.Method.GET;

/**
 * The weather service interface.
 * <p>
 * Describes everything Lagom needs for serving and consuming this service.
 */
public interface WeatherService extends Service {

	ServiceCall<NotUsed, WeatherDataResponse> getCurrentWeather(String location);

	@Override
	default Descriptor descriptor() {
		return named("weather-service")
				.withCalls(
						restCall(GET, "/api/weather-service/current/:location", this::getCurrentWeather)
				)
				.withAutoAcl(true);
	}
}

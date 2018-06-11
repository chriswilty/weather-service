package com.scottlogic.weather.api;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.scottlogic.weather.api.message.WeatherDataResponse;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.transport.Method.GET;

/**
 * The weather service interface.
 * <p>
 * This describes everything Lagom needs to know about how to serve and consume the WeatherService.
 */
public interface WeatherService extends Service {

	ServiceCall<NotUsed, WeatherDataResponse> getCurrentWeather(String location);

	@Override
	default Descriptor descriptor() {
		return named("weather")
				.withCalls(
						Service.restCall(GET, "/api/weather-service/current/:location", this::getCurrentWeather)
				)
				.withAutoAcl(true);
	}
}

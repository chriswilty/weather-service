package com.scottlogic.weather.owmadapter.api;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.scottlogic.weather.owmadapter.api.message.WeatherData;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;
import static com.lightbend.lagom.javadsl.api.transport.Method.GET;

/**
 * The OpenWeatherMap Adapter interface.
 * <p>
 * Describes everything Lagom needs for serving and consuming this adapter service.
 */
public interface OwmAdapter extends Service {

	ServiceCall<NotUsed, WeatherData> getCurrentWeather(String location);

	@Override
	default Descriptor descriptor() {
		return named("owm-adapter")
				.withCalls(
						restCall(GET, "/api/owm-adapter/current/:location", this::getCurrentWeather)
				)
				.withAutoAcl(true);
	}


}

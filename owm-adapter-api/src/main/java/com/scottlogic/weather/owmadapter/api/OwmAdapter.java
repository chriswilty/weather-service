package com.scottlogic.weather.owmadapter.api;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.scottlogic.weather.owmadapter.api.message.WeatherData;

import java.util.List;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;
import static com.lightbend.lagom.javadsl.api.transport.Method.GET;

/**
 * The OpenWeatherMap Adapter interface.
 * <p>
 * Describes everything Lagom needs for serving and consuming this adapter service.
 * </p>
 */
public interface OwmAdapter extends Service {

	@Override
	default Descriptor descriptor() {
		return named("owm-adapter")
				.withCalls(
						restCall(GET, "/api/owm-adapter/is-alive", this::isAlive),
						restCall(GET, "/api/owm-adapter/current?location", this::getCurrentWeatherByName),
						restCall(GET, "/api/owm-adapter/current/:id", this::getCurrentWeatherById),
						restCall(GET, "/api/owm-adapter/forecast?location", this::getWeatherForecastByName),
						restCall(GET, "/api/owm-adapter/forecast/:id", this::getWeatherForecastById)
				)
				.withAutoAcl(true);
	}

	ServiceCall<NotUsed, String> isAlive();
	ServiceCall<NotUsed, WeatherData> getCurrentWeatherByName(String location);
	ServiceCall<NotUsed, WeatherData> getCurrentWeatherById(int id);
	ServiceCall<NotUsed, List<WeatherData>> getWeatherForecastByName(String location);
	ServiceCall<NotUsed, List<WeatherData>> getWeatherForecastById(int location);

}

package com.scottlogic.weather.weatherservice.api;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.scottlogic.weather.weatherservice.api.message.AddLocationRequest;
import com.scottlogic.weather.weatherservice.api.message.SetEmitFrequencyRequest;
import com.scottlogic.weather.weatherservice.api.message.WeatherDataResponse;
import com.scottlogic.weather.weatherservice.api.message.WeatherStreamParameters;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;
import static com.lightbend.lagom.javadsl.api.transport.Method.DELETE;
import static com.lightbend.lagom.javadsl.api.transport.Method.GET;
import static com.lightbend.lagom.javadsl.api.transport.Method.POST;
import static com.lightbend.lagom.javadsl.api.transport.Method.PUT;

/**
 * The weather service interface.
 * <p>
 * Describes everything Lagom needs for serving and consuming this service.
 */
public interface WeatherService extends Service {

	ServiceCall<NotUsed, WeatherDataResponse> currentWeather(String location);
	ServiceCall<NotUsed, Source<WeatherDataResponse, ?>> currentWeatherStream();
	ServiceCall<NotUsed, WeatherStreamParameters> weatherStreamParameters();
	ServiceCall<SetEmitFrequencyRequest, Done> setEmitFrequency();
	ServiceCall<AddLocationRequest, Done> addLocation();
	ServiceCall<NotUsed, Done> removeLocation(String location);

	@Override
	default Descriptor descriptor() {
		return named("weather-service")
				.withCalls(
						restCall(GET, "/api/weather-service/current/:location", this::currentWeather),
						restCall(GET, "/api/weather-service/streaming/current", this::currentWeatherStream),
						restCall(GET, "/api/weather-service/streaming/parameters", this::weatherStreamParameters),
						restCall(PUT, "/api/weather-service/streaming/parameters/emit-frequency", this::setEmitFrequency),
						restCall(POST, "/api/weather-service/streaming/parameters/locations", this::addLocation),
						restCall(DELETE, "/api/weather-service/streaming/parameters/locations/:location", this::removeLocation)
				)
				.withAutoAcl(true);
	}
}

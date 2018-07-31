package com.scottlogic.weather.weatherservice.impl;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.google.inject.Inject;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.transport.BadRequest;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.owmadapter.api.message.WeatherData;
import com.scottlogic.weather.weatherservice.api.WeatherService;
import com.scottlogic.weather.weatherservice.api.message.AddLocationRequest;
import com.scottlogic.weather.weatherservice.api.message.CurrentWeatherResponse;
import com.scottlogic.weather.weatherservice.api.message.SetEmitFrequencyRequest;
import com.scottlogic.weather.weatherservice.api.message.WeatherForecastResponse;
import com.scottlogic.weather.weatherservice.api.message.WeatherResponse;
import com.scottlogic.weather.weatherservice.api.message.WeatherStreamParameters;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.AddLocation;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.ChangeEmitFrequency;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.GetWeatherStreamParameters;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherCommand.RemoveLocation;
import com.scottlogic.weather.weatherservice.impl.entity.WeatherEntity;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Implementation of the WeatherService.
 */
public class WeatherServiceImpl implements WeatherService {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Pattern positiveNumberMatcher = Pattern.compile("\\d+");

	// No user sessions for now; just one entity:
	private final String entityId = "default";

	private final OwmAdapter owmAdapter;
	private final StreamGeneratorFactory streamGeneratorFactory;
	private final PersistentEntityRegistryFacade entityRegistryFacade;

	@Inject
	public WeatherServiceImpl(
			final OwmAdapter owmAdapter,
			final StreamGeneratorFactory streamGeneratorFactory,
			final PersistentEntityRegistryFacade entityRegistryFacade
	) {
		this.owmAdapter = owmAdapter;
		this.streamGeneratorFactory = streamGeneratorFactory;
		this.entityRegistryFacade = entityRegistryFacade;
		this.entityRegistryFacade.register(WeatherEntity.class);
	}

	@Override
	public ServiceCall<NotUsed, String> isAlive() {
		return request -> completedFuture(
				"Service \"" + descriptor().name() + "\" is alive: " +
						OffsetDateTime.now().format(DateTimeFormatter.ofPattern("EEEE dd MMM uuuu HH:mm:ss Z"))
		);
	}

	@Override
	public ServiceCall<NotUsed, CurrentWeatherResponse> currentWeather(final String location) {
		return positiveNumberMatcher.matcher(location).matches()
				? currentWeatherById(Integer.parseInt(location))
				: currentWeatherByName(location);
	}

	@Override
	public ServiceCall<NotUsed, WeatherForecastResponse> weatherForecast(final String location) {
		return positiveNumberMatcher.matcher(location).matches()
				? weatherForecastById(Integer.parseInt(location))
				: weatherForecastByName(location);
	}

	@Override
	public ServiceCall<NotUsed, Source<CurrentWeatherResponse, ?>> currentWeatherStream() {
		return request -> {
			log.info("Received request for stream of current weather");
			return completedFuture(this.streamGeneratorFactory.get(entityId).getSourceOfCurrentWeatherData());
		};
	}

	@Override
	public ServiceCall<NotUsed, Source<WeatherForecastResponse, ?>> weatherForecastStream() {
		return request -> {
			log.info("Received request for stream of forecast weather");
			return completedFuture(this.streamGeneratorFactory.get(entityId).getSourceOfWeatherForecastData());
		};
	}

	@Override
	public ServiceCall<NotUsed, WeatherStreamParameters> weatherStreamParameters() {
		return request -> {
			log.info("Received request for stream parameters");
			return this.entityRegistryFacade.sendCommandToPersistentEntity(
					WeatherEntity.class,
					entityId,
					new GetWeatherStreamParameters()
			).thenApply(this::logGenericResponse);
		};
	}

	@Override
	public ServiceCall<SetEmitFrequencyRequest, Done> setEmitFrequency() {
		return request -> {
			checkNonNullRequest(request, "{ frequency: number }");

			final int frequency = request.getFrequency();
			log.info("Received request to set emit frequency to [{}]", frequency);

			if (frequency < 2) {
				badRequest("Frequency must be greater than 1");
			}

			return this.entityRegistryFacade.sendCommandToPersistentEntity(
					WeatherEntity.class,
					entityId,
					new ChangeEmitFrequency(frequency)
			);
		};
	}

	@Override
	public ServiceCall<AddLocationRequest, Done> addLocation() {
		return request -> {
			checkNonNullRequest(request, "{ location: string }");

			final String location = StringUtils.strip(request.getLocation());
			log.info("Received request to add location [{}]", location);

			if (StringUtils.isEmpty(location)) {
				badRequest("Location must be given");
			}

			return this.entityRegistryFacade.sendCommandToPersistentEntity(
					WeatherEntity.class,
					entityId,
					new AddLocation(location)
			);
		};
	}

	@Override
	public ServiceCall<NotUsed, Done> removeLocation(final String location) {
		return request -> {
			log.info("Received request to remove location [{}]", location);

			return this.entityRegistryFacade.sendCommandToPersistentEntity(
					WeatherEntity.class,
					entityId,
					new RemoveLocation(location)
			);
		};
	}

	private ServiceCall<NotUsed, CurrentWeatherResponse> currentWeatherByName(final String name) {
		return request -> {
			log.info("Received request for current weather in [{}]", name);
			return this.owmAdapter.getCurrentWeatherByName(name).invoke()
					.thenApply(MessageUtils::weatherDataToCurrentWeatherResponse)
					.thenApply(this::logWeatherResponse);
		};
	}

	private ServiceCall<NotUsed, CurrentWeatherResponse> currentWeatherById(final int id) {
		return request -> {
			log.info("Received request for current weather for location [{}]", id);
			return this.owmAdapter.getCurrentWeatherById(id).invoke()
					.thenApply(MessageUtils::weatherDataToCurrentWeatherResponse)
					.thenApply(this::logWeatherResponse);
		};
	}

	private ServiceCall<NotUsed, WeatherForecastResponse> weatherForecastByName(final String name) {
		return request -> {
			log.info("Received request for weather forecast for [{}]", name);

			final CompletionStage<WeatherData> current = this.owmAdapter.getCurrentWeatherByName(name).invoke();
			final CompletionStage<List<WeatherData>> forecast = this.owmAdapter.getWeatherForecastByName(name).invoke();

			return current.thenCombine(forecast, MessageUtils::weatherDataToWeatherForecastResponse)
					.thenApply(this::logWeatherResponse);
		};
	}

	private ServiceCall<NotUsed, WeatherForecastResponse> weatherForecastById(final int id) {
		return request -> {
			log.info("Received request for weather forecast for location [{}]", id);

			final CompletionStage<WeatherData> current = this.owmAdapter.getCurrentWeatherById(id).invoke();
			final CompletionStage<List<WeatherData>> forecast = this.owmAdapter.getWeatherForecastById(id).invoke();

			return current.thenCombine(forecast, MessageUtils::weatherDataToWeatherForecastResponse)
					.thenApply(this::logWeatherResponse);
		};
	}

	private <T extends WeatherResponse> T logWeatherResponse(final T response) {
		log.info(
				"Sending {} for [{} ({})]",
				response instanceof CurrentWeatherResponse ? "current weather" : "weather forecast",
				response.getLocation(),
				response.getId()
		);
		return response;
	}

	private <T> T logGenericResponse(final T response) {
		log.info("Sending response: {}", response);
		return response;
	}

	private <R> void checkNonNullRequest(final R request, final String expectedShape) throws BadRequest {
		if (request == null) {
			badRequest("No request body found, expected " + expectedShape);
		}
	}

	private void badRequest(final String message) {
		log.warn(message);
		throw new BadRequest(message);
	}
}

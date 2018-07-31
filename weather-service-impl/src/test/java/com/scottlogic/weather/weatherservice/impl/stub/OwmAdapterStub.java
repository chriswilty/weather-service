package com.scottlogic.weather.weatherservice.impl.stub;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.owmadapter.api.message.Sun;
import com.scottlogic.weather.owmadapter.api.message.Temperature;
import com.scottlogic.weather.owmadapter.api.message.Unauthorized;
import com.scottlogic.weather.owmadapter.api.message.Weather;
import com.scottlogic.weather.owmadapter.api.message.WeatherData;
import com.scottlogic.weather.owmadapter.api.message.Wind;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Stub implementation of OWM Adapter service, returning faked data.
 */
public class OwmAdapterStub implements OwmAdapter {
	public static final String LOCATION_401 = "Anywhere,KP";
	public static final String LOCATION_404 = "Trumpsbrain,US";

	private static Random random = new Random();

	@Override
	public ServiceCall<NotUsed, String> isAlive() {
		return request -> completedFuture("yes");
	}

	@Override
	public ServiceCall<NotUsed, WeatherData> getCurrentWeatherByName(final String location) {
		// TODO Include a case for Service Unavailable? Work out what happens when OWM is unreachable.
		// Then devise a supervision / retry strategy within the flow.
		return request -> {
			switch (location) {
				case LOCATION_401:
					throw new Unauthorized("denied");
				case LOCATION_404:
					throw new NotFound("no sir");
				default:
					return completedFuture(generateCurrentWeatherData(location));
			}
		};
	}

	@Override
	public ServiceCall<NotUsed, WeatherData> getCurrentWeatherById(final int id) {
		return request -> completedFuture(generateCurrentWeatherData(id));
	}

	@Override
	public ServiceCall<NotUsed, List<WeatherData>> getWeatherForecastByName(final String location) {
		return request -> {
			switch (location) {
				case LOCATION_401:
					throw new Unauthorized("denied");
				case LOCATION_404:
					throw new NotFound("no sir");
				default:
					return completedFuture(generateWeatherForecastData(location));
			}
		};
	}

	@Override
	public ServiceCall<NotUsed, List<WeatherData>> getWeatherForecastById(final int location) {
		return request -> completedFuture(generateWeatherForecastData(location));
	}

	private List<WeatherData> generateWeatherForecastData(final int id) {
		return generateWeatherForecastData(id, "Nowhere, US");
	}

	private List<WeatherData> generateWeatherForecastData(final String location) {
		return generateWeatherForecastData(1234567, location);
	}

	private List<WeatherData> generateWeatherForecastData(final int id, final String location) {
		final OffsetDateTime firstReading = OffsetDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1);

		return IntStream.range(0, 40)
				.mapToObj(i ->
						generateCurrentWeatherData(id, location).toBuilder()
								.sun(null)
								.measured(firstReading.plusHours(i * 3))
								.build()
				)
				.collect(Collectors.toList());
	}

	private WeatherData generateCurrentWeatherData(final int id) {
		return generateCurrentWeatherData(id, "Nowhere, US");
	}

	private WeatherData generateCurrentWeatherData(final String location) {
		return generateCurrentWeatherData(1234567, location);
	}

	private WeatherData generateCurrentWeatherData(final int id, final String location) {
		final OffsetDateTime now = OffsetDateTime.now();
		final short windDirection = (short) random.nextInt(360);
		final BigDecimal windSpeed = BigDecimal.valueOf(random.nextInt(400))
				.setScale(1)
				.divide(BigDecimal.TEN);

		return WeatherData.builder()
				.id(id)
				.location(location)
				.measured(now.truncatedTo(ChronoUnit.HOURS))
				.weather(Weather.builder()
						.id(101)
						.description("crappy pissy rain")
						.build()
				)
				.temperature(Temperature.builder()
						.current(new BigDecimal("10.0"))
						.minimum(new BigDecimal("9.5"))
						.maximum(new BigDecimal("10.5"))
						.build()
				)
				.wind(Wind.builder()
						.fromDegrees(windDirection)
						.speed(windSpeed)
						.build()
				)
				.sun(Sun.builder()
						.sunrise(now.withHour(6).truncatedTo(HOURS))
						.sunset(now.withHour(18).truncatedTo(HOURS))
						.build()
				)
				.build();
	}
}

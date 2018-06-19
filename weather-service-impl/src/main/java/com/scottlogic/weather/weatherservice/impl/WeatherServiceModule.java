package com.scottlogic.weather.weatherservice.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;
import com.scottlogic.weather.weatherservice.api.WeatherService;

/**
 * The module that binds the WeatherService so that it can be served.
 */
public class WeatherServiceModule extends AbstractModule implements ServiceGuiceSupport {

	@Override
	protected void configure() {
		bindService(WeatherService.class, WeatherServiceImpl.class);
		bindClient(OwmAdapter.class);
	}
}

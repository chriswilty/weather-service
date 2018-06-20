package com.scottlogic.weather.owmadapter.impl;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;

/**
 * The module that binds the OwmAdapter so that it can be served.
 */
public class OwmAdapterModule extends AbstractModule implements ServiceGuiceSupport {

	@Override
	protected void configure() {
		bindService(OwmAdapter.class, OwmAdapterImpl.class);
	}

	@Provides
	Http http(final ActorSystem actorSystem) {
		return Http.get(actorSystem);
	}
}

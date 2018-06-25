package com.scottlogic.weather.weatherservice.impl;

import akka.actor.ActorSystem;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;

@Singleton
public class StreamGeneratorFactory {

	private final OwmAdapter owmAdapter;
	private final PersistentEntityRegistryFacade registryFacade;
	private final ActorSystem actorSystem;

	@Inject
	public StreamGeneratorFactory(
			final OwmAdapter owmAdapter, final PersistentEntityRegistryFacade registryFacade, final ActorSystem actorSystem
	) {
		this.owmAdapter = owmAdapter;
		this.registryFacade = registryFacade;
		this.actorSystem = actorSystem;
	}

	public StreamGenerator get() {
		return new StreamGenerator(owmAdapter, registryFacade, actorSystem);
	}
}

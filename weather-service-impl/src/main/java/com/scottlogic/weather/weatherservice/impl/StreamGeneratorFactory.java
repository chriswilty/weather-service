package com.scottlogic.weather.weatherservice.impl;

import akka.stream.Materializer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;

@Singleton
public class StreamGeneratorFactory {

	private final OwmAdapter owmAdapter;
	private final PersistentEntityRegistryFacade persistentEntityRegistryFacade;
	private final PubSubRegistryFacade pubSubRegistryFacade;
	private final Materializer materializer;

	@Inject
	public StreamGeneratorFactory(
			final OwmAdapter owmAdapter,
			final PersistentEntityRegistryFacade persistentEntityRegistryFacade,
			PubSubRegistryFacade pubSubRegistryFacade,
			final Materializer materializer
	) {
		this.owmAdapter = owmAdapter;
		this.persistentEntityRegistryFacade = persistentEntityRegistryFacade;
		this.pubSubRegistryFacade = pubSubRegistryFacade;
		this.materializer = materializer;
	}

	public StreamGenerator get(final String entityId) {
		return new StreamGenerator(owmAdapter, persistentEntityRegistryFacade, pubSubRegistryFacade, materializer, entityId);
	}
}

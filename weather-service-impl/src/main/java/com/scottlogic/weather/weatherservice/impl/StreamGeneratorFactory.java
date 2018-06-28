package com.scottlogic.weather.weatherservice.impl;

import akka.stream.Materializer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lightbend.lagom.javadsl.pubsub.PubSubRegistry;
import com.scottlogic.weather.owmadapter.api.OwmAdapter;

@Singleton
public class StreamGeneratorFactory {

	private final OwmAdapter owmAdapter;
	private final PersistentEntityRegistryFacade registryFacade;
	private final Materializer materializer;
	private final PubSubRegistry pubSubRegistry;

	@Inject
	public StreamGeneratorFactory(
			final OwmAdapter owmAdapter,
			final PersistentEntityRegistryFacade registryFacade,
			final Materializer materializer,
			final PubSubRegistry pubSubRegistry
			) {
		this.owmAdapter = owmAdapter;
		this.registryFacade = registryFacade;
		this.materializer = materializer;
		this.pubSubRegistry = pubSubRegistry;
	}

	public StreamGenerator get(final String entityId) {
		return new StreamGenerator(owmAdapter, registryFacade, materializer, pubSubRegistry, entityId);
	}
}

package com.scottlogic.weather.weatherservice.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity.ReplyType;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;

import java.util.concurrent.CompletionStage;

@Singleton
public class PersistentEntityRegistryFacade {
	private final PersistentEntityRegistry persistentEntityRegistry;

	@Inject
	public PersistentEntityRegistryFacade(final PersistentEntityRegistry persistentEntityRegistry) {
		this.persistentEntityRegistry = persistentEntityRegistry;
	}

	public <C, E, S, T extends PersistentEntity<C,E,S>> void register(final Class<T> clazz) {
		this.persistentEntityRegistry.register(clazz);
	}

	public <R, C, E, S, D extends ReplyType<R>, P extends PersistentEntity<C,E,S>> CompletionStage<R> sendCommandToPersistentEntity(
			final Class<P> clazz,
			final String id,
			final D command
	) {
		return this.persistentEntityRegistry.refFor(clazz, id).ask(command);
	}
}

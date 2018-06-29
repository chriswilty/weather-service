package com.scottlogic.weather.weatherservice.impl;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lightbend.lagom.javadsl.pubsub.PubSubRegistry;
import com.lightbend.lagom.javadsl.pubsub.TopicId;

import java.util.Optional;

@Singleton
public class PubSubRegistryFacade {

	private final PubSubRegistry pubSubRegistry;

	@Inject
	public PubSubRegistryFacade(final PubSubRegistry pubSubRegistry) {
		this.pubSubRegistry = pubSubRegistry;
	}

	public <T> void publish(final Class<T> messageClass, final T message, final Optional<String> topicSuffix) {
		this.pubSubRegistry.refFor(
				TopicId.of(messageClass, topicSuffix.orElse(""))
		).publish(message);
	}

	public <T> Source<T, NotUsed> subscribe(final Class<T> messageClass, final Optional<String> topicSuffix) {
		return this.pubSubRegistry.refFor(
				TopicId.of(messageClass, topicSuffix.orElse(""))
		).subscriber();
	}
}

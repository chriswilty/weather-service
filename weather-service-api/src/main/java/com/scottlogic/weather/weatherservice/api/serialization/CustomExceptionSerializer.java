package com.scottlogic.weather.weatherservice.api.serialization;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lightbend.lagom.javadsl.api.deser.RawExceptionMessage;
import com.lightbend.lagom.javadsl.api.transport.TransportException;
import com.lightbend.lagom.javadsl.jackson.JacksonExceptionSerializer;
import com.scottlogic.weather.owmadapter.api.message.Unauthorized;
import play.Environment;

import java.util.Objects;

@Singleton
public class CustomExceptionSerializer extends JacksonExceptionSerializer {

	private static CustomExceptionSerializer instance = null;

	// Using static injection... Evil but (currently) necessary!
	// Lagom currently provides no way to plug in an exception serializer that extends the default
	// JacksonExceptionSerializer. The service descriptor takes an INSTANCE of an exception
	// serializer rather than a CLASS, and therefore an exception serializer cannot be injected.
	// Because we cannot make use of injection, we cannot use e.g. Config or any other injectable
	// types in our exception serializer. JacksonExceptionSerializer injects Environment, so that
	// it can tell whether or not the service is running in PROD, and thereby hide or expose details
	// of exceptions. But this means that we cannot extend JacksonExceptionSerializer because we
	// have no way to get hold of a correctly configured instance of Environment.
	//
	// There is an open issue in github for allowing injection of serializers:
	// https://github.com/lagom/lagom/issues/1072
	@Inject
	private static Environment environment;

	public static CustomExceptionSerializer getInstance() {
		if (instance == null) {
			instance = new CustomExceptionSerializer();
		}
		return instance;
	}

	@Override
	public Throwable deserialize(final RawExceptionMessage message) {
		final Throwable throwable = super.deserialize(message);

		if (throwable instanceof TransportException && Objects.equals(
				((TransportException) throwable).errorCode(),
				Unauthorized.ERROR_CODE)
		) {
			return new Unauthorized(throwable.getMessage());
		}

		return throwable;
	}

	// Singleton class: use getInstance()
	private CustomExceptionSerializer() {
		super(environment);
	}

}

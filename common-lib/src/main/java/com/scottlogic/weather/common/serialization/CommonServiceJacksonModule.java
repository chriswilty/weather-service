package com.scottlogic.weather.common.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class CommonServiceJacksonModule extends SimpleModule {

	@Override
	public void setupModule(final SetupContext setupContext) {
		final ObjectMapper objectMapper = setupContext.getOwner();

		objectMapper
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
				.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
				.setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));

		/*
		  Note that enabling JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN does not have the
		  expected effect of outputting BigDecimals as Strings, because by default,
		  JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS is disabled, which is fine for all other
		  numerics but it interferes with our desired BigDecimal output. Therefore, I have written a
		  custom serializer (for use with @JsonSerialize) for all BigDecimal fields which will be
		  emitted to the client:
		  com.scottlogic.weather.weatherservice.api.serialization.BigDecimalToStringSerializer
		  It's fine to serialize BigDecimals as numbers in intra-service messages, so this custom
		  serializer is only needed in weather-service-api.
		 */
	}
}

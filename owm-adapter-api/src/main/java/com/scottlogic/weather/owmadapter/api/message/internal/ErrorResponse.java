package com.scottlogic.weather.owmadapter.api.message.internal;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Value;

@Value
@JsonDeserialize
public class ErrorResponse {
	int cod;
	String message;
}

package com.scottlogic.weather.owmadapter.api.message;

import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.api.transport.TransportException;

public final class Unauthorized extends TransportException {
	public static final TransportErrorCode ERROR_CODE = TransportErrorCode.fromHttp(401);

	public Unauthorized(final String message) {
		super(ERROR_CODE, message);
	}
}

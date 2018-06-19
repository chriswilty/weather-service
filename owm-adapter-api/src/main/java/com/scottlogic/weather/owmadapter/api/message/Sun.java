package com.scottlogic.weather.owmadapter.api.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class Sun {

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	private final LocalDateTime sunrise;

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	private final LocalDateTime sunset;
}

package ru.pobopo.smart.thing.gateway.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Processed server exception")
public class ErrorResponse {
    private String message;
}

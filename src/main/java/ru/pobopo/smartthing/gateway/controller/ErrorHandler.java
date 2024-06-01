package ru.pobopo.smartthing.gateway.controller;

import com.fasterxml.jackson.databind.util.ExceptionUtil;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import ru.pobopo.smartthing.gateway.controller.model.ErrorResponse;
import ru.pobopo.smartthing.gateway.exception.AccessDeniedException;
import ru.pobopo.smartthing.gateway.exception.BadRequestException;
import ru.pobopo.smartthing.gateway.exception.DeviceApiException;
import ru.pobopo.smartthing.gateway.exception.DeviceSettingsException;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse forbidden(AccessDeniedException exception) {
        return new ErrorResponse(exception.getMessage(), ExceptionUtils.getStackTrace(exception));
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(BadRequestException exception) {
        log.error("Bad request: {}", exception.getMessage());
        return new ErrorResponse(exception.getMessage(), null);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse validationError(ValidationException exception) {
        log.error("Validation error: {}", exception.getMessage());
        return new ErrorResponse(exception.getMessage(), null);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse apiError(DeviceApiException exception) {
        log.error("Device api exception", exception);
        return new ErrorResponse(exception.getMessage(), null);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse unavailable(ResourceAccessException exception) {
        return new ErrorResponse(exception.getMessage(), ExceptionUtils.getStackTrace(exception));
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse deviceSettings(DeviceSettingsException exception) {
        log.error("Device settings exception: {}", exception.getMessage(), exception.getCause());
        return new ErrorResponse(exception.getMessage(), ExceptionUtils.getStackTrace(exception));
    }
}

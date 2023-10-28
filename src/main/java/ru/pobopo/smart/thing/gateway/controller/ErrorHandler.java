package ru.pobopo.smart.thing.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.pobopo.smart.thing.gateway.controller.model.ErrorResponse;
import ru.pobopo.smart.thing.gateway.exception.AccessDeniedException;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse forbidden(AccessDeniedException exception) {
        return new ErrorResponse(exception.getMessage());
    }
}

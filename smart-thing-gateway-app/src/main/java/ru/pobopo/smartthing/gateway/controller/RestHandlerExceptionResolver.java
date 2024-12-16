package ru.pobopo.smartthing.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;
import ru.pobopo.smartthing.gateway.exception.AccessDeniedException;
import ru.pobopo.smartthing.gateway.exception.BadRequestException;
import ru.pobopo.smartthing.gateway.exception.DeviceSettingsException;
import ru.pobopo.smartthing.gateway.exception.ForbiddenCloudEndpointException;

import java.io.IOException;

@Component
public class RestHandlerExceptionResolver extends AbstractHandlerExceptionResolver {
    @Override
    protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            if (ex instanceof ForbiddenCloudEndpointException || ex instanceof AccessDeniedException) {
                return handleException(response, ex, 403);
            }
            if (ex instanceof BadRequestException || ex instanceof ValidationException) {
                return handleException(response, ex, 400);
            }
            if (ex instanceof ResourceAccessException) {
                return handleException(response, ex, 503);
            }
            if (ex instanceof DeviceSettingsException) {
                return handleException(response, ex, 500);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private ModelAndView handleException(HttpServletResponse response, Exception ex, int code) throws IOException {
        response.sendError(code, ex.getMessage());
        return new ModelAndView();
    }
}

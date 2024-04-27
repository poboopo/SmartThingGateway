package ru.pobopo.smart.thing.gateway.stomp.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smart.thing.gateway.exception.LogoutException;
import ru.pobopo.smart.thing.gateway.exception.MissingValueException;
import ru.pobopo.smart.thing.gateway.service.CloudService;
import ru.pobopo.smartthing.model.InternalHttpResponse;
import ru.pobopo.smartthing.model.stomp.GatewayCommand;
import ru.pobopo.smartthing.model.stomp.GatewayCommandMessage;
import ru.pobopo.smartthing.model.stomp.ResponseMessage;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class GatewayCommandProcessor implements MessageProcessor {
    private final CloudService cloudService;
    private final RestTemplate restTemplate;
    private final String gatewayPort;

    @Override
    public ResponseMessage process(Object payload) {
        GatewayCommandMessage gatewayCommand = (GatewayCommandMessage) payload;

        ResponseMessage response = new ResponseMessage();
        response.setRequestId(gatewayCommand.getId());

        if (StringUtils.isBlank(gatewayCommand.getCommand())) {
            response.setSuccess(false);
            response.setError("Command is missing!");
            return response;
        }

        switch (gatewayCommand.getCommand()) {
            case "ping" -> response.setResponse(InternalHttpResponse.builder().data("pong").build());
            case "request" -> {
                try {
                    response.setResponse(internalRequest(gatewayCommand.getParameters()));
                } catch (MissingValueException exception) {
                    response.setError(exception.getMessage());
                    response.setSuccess(false);
                }
            }
            case "logout" -> {
                log.info("Logout event! Removing token from config.");
                cloudService.logout();
                throw new LogoutException();
            }
            default -> {
                response.setError("Unknown command");
                response.setSuccess(false);
            }
        }

        return response;
    }

    private InternalHttpResponse internalRequest(Map<String, Object> parameters) throws MissingValueException {
        String url = (String) parameters.get("url");
        if (StringUtils.isEmpty(url)) {
            throw new MissingValueException("Url param is missing!");
        }
        String method = (String) parameters.get("method");
        if (StringUtils.isEmpty(method)) {
            throw new MissingValueException("Method param is missing!");
        }
        HttpEntity<?> entity = new HttpEntity<>(parameters.get("data"));

        // add some role for internal requests?
        // so for example we gonna set /cloud/* as LOCAL and we not gonna be able to call it from outside
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + gatewayPort + url,
                HttpMethod.valueOf(method),
                entity,
                String.class
        );
        return new InternalHttpResponse(response.getStatusCode().value(), response.getBody(), response.getHeaders().toSingleValueMap());
    }
}

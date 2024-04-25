package ru.pobopo.smart.thing.gateway.stomp.processor;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smart.thing.gateway.exception.BadRequestException;
import ru.pobopo.smart.thing.gateway.exception.LogoutException;
import ru.pobopo.smart.thing.gateway.exception.MissingValueException;
import ru.pobopo.smart.thing.gateway.jobs.DevicesSearchJob;
import ru.pobopo.smart.thing.gateway.stomp.message.GatewayCommand;
import ru.pobopo.smart.thing.gateway.stomp.message.MessageResponse;
import ru.pobopo.smart.thing.gateway.service.CloudService;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class GatewayCommandProcessor implements MessageProcessor {
    private final CloudService cloudService;
    private final RestTemplate restTemplate;
    @Value("${server.port}")
    private String gatewayPort;

    @Override
    public MessageResponse process(Object payload) {
        GatewayCommand gatewayCommand = (GatewayCommand) payload;

        MessageResponse response = new MessageResponse();
        response.setRequestId(gatewayCommand.getRequestId());

        if (StringUtils.isBlank(gatewayCommand.getCommand())) {
            response.setSuccess(false);
            response.setError("Command is missing!");
            return response;
        }

        switch (gatewayCommand.getCommand()) {
            case "ping" -> response.setResponse("pong");
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

    private ResponseEntity<String> internalRequest(Map<String, Object> parameters) throws MissingValueException {
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
        return restTemplate.exchange("http://localhost:" + gatewayPort + url,  HttpMethod.valueOf(method), entity, String.class);
    }
}

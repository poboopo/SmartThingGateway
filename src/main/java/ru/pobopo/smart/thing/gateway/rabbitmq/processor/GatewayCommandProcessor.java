package ru.pobopo.smart.thing.gateway.rabbitmq.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.pobopo.smart.thing.gateway.exception.MissingValueException;
import ru.pobopo.smart.thing.gateway.jobs.DeviceSearchJob;
import ru.pobopo.smart.thing.gateway.rabbitmq.message.GatewayCommand;
import ru.pobopo.smart.thing.gateway.rabbitmq.message.MessageResponse;

@Slf4j
public class GatewayCommandProcessor implements MessageProcessor {
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final DeviceSearchJob searchJob;

    public GatewayCommandProcessor(DeviceSearchJob searchJob) {
        this.searchJob = searchJob;
    }

    @Override
    public MessageResponse process(String message) throws Exception {
        GatewayCommand gatewayCommand = objectMapper.readValue(message, GatewayCommand.class);
        if (StringUtils.isBlank(gatewayCommand.getRequestId())) {
            throw new MissingValueException("Request id is missing!");
        }

        MessageResponse response = new MessageResponse();

        if (StringUtils.isBlank(gatewayCommand.getCommand())) {
            response.setSuccess(false);
            response.setResponse("Command is missing!");
            return response;
        }

        switch (gatewayCommand.getCommand()) {
            case "ping" -> response.setResponse("pong");
            case "search" -> response.setResponse(searchDevices());
            default -> {
                response.setResponse("Unknown command");
                response.setSuccess(false);
            }
        }

        return response;
    }

    private String searchDevices() throws JsonProcessingException {
        return objectMapper.writeValueAsString(searchJob.getRecentFoundDevices());
    }
}

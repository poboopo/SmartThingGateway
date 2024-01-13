package ru.pobopo.smart.thing.gateway.stomp.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.pobopo.smart.thing.gateway.exception.LogoutException;
import ru.pobopo.smart.thing.gateway.jobs.DeviceSearchJob;
import ru.pobopo.smart.thing.gateway.model.CloudAuthInfo;
import ru.pobopo.smart.thing.gateway.stomp.message.GatewayCommand;
import ru.pobopo.smart.thing.gateway.stomp.message.MessageResponse;
import ru.pobopo.smart.thing.gateway.service.CloudService;
import ru.pobopo.smart.thing.gateway.service.ConfigurationService;

@Slf4j
public class GatewayCommandProcessor implements MessageProcessor {
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final DeviceSearchJob searchJob;
    private final ConfigurationService configurationService;
    private final CloudService cloudService;

    public GatewayCommandProcessor(DeviceSearchJob searchJob, ConfigurationService configurationService, CloudService cloudService) {
        this.searchJob = searchJob;
        this.configurationService = configurationService;
        this.cloudService = cloudService;
    }

    @Override
    public MessageResponse process(Object payload) throws Exception {
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
            case "search" -> response.setResponse(searchDevices());
            case "logout" -> {
                log.info("Logout event! Removing token from config.");
                CloudAuthInfo authInfo = configurationService.getCloudAuthInfo();
                authInfo.setToken(null);
                configurationService.updateCloudAuthInfo(authInfo);
                cloudService.clearAuthorization();
                throw new LogoutException();
            }
            default -> {
                response.setError("Unknown command");
                response.setSuccess(false);
            }
        }

        return response;
    }

    private String searchDevices() throws JsonProcessingException {
        return objectMapper.writeValueAsString(searchJob.getRecentFoundDevices());
    }
}

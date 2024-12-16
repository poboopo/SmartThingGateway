package ru.pobopo.smartthing.gateway.stomp.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import ru.pobopo.smartthing.gateway.exception.LogoutException;
import ru.pobopo.smartthing.gateway.service.cloud.CloudApiService;
import ru.pobopo.smartthing.model.InternalHttpResponse;
import ru.pobopo.smartthing.model.stomp.GatewayCommandMessage;

@Slf4j
@RequiredArgsConstructor
public class GatewayCommandProcessor implements MessageProcessor {
    private final CloudApiService cloudService;

    @Override
    public InternalHttpResponse process(Object payload) throws Exception {
        GatewayCommandMessage gatewayCommand = (GatewayCommandMessage) payload;

        if (gatewayCommand.getCommand() == null) {
            return new InternalHttpResponse(HttpStatus.BAD_REQUEST, "Command is missing!", null);
        }

        switch (gatewayCommand.getCommand()) {
            case PING -> {
                return new InternalHttpResponse(HttpStatus.OK, "ping", null);
            }
            case LOGOUT -> {
                log.info("Logout event! Removing token from config.");
                cloudService.removeCloudCache();
                throw new LogoutException();
            }
            default -> throw new Exception("Unknown command");
        }
    }

}

package ru.pobopo.smart.thing.gateway.stomp.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.pobopo.smart.thing.gateway.exception.LogoutException;
import ru.pobopo.smart.thing.gateway.service.CloudService;
import ru.pobopo.smartthing.model.stomp.GatewayCommandMessage;
import ru.pobopo.smartthing.model.stomp.ResponseMessage;

@Slf4j
@RequiredArgsConstructor
public class GatewayCommandProcessor implements MessageProcessor {
    private final CloudService cloudService;

    @Override
    public Object process(Object payload) throws Exception {
        GatewayCommandMessage gatewayCommand = (GatewayCommandMessage) payload;

        ResponseMessage response = new ResponseMessage();

        if (gatewayCommand.getCommand() == null) {
            response.setSuccess(false);
            response.setError("Command is missing!");
            return response;
        }

        switch (gatewayCommand.getCommand()) {
            case PING -> {
                return "pong";
            }
            case LOGOUT -> {
                log.info("Logout event! Removing token from config.");
                cloudService.logout();
                throw new LogoutException();
            }
            default -> throw new Exception("Unknown command");
        }
    }

}

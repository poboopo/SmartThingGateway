package ru.pobopo.smart.thing.gateway.stomp.message;

import java.util.Map;
import lombok.Data;

@Data
public class GatewayCommand extends BaseMessage {
    private String command;
    private Map<String, Object> parameters;
}

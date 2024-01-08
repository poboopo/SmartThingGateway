package ru.pobopo.smart.thing.gateway.stomp.message;

import lombok.Data;

@Data
public class BaseMessage {
    private GatewayMessageType type;
    private String requestId;
}

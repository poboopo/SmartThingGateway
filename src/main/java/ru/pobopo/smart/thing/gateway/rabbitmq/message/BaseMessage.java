package ru.pobopo.smart.thing.gateway.rabbitmq.message;

import lombok.Data;

@Data
public class BaseMessage {
    private String requestId;
}

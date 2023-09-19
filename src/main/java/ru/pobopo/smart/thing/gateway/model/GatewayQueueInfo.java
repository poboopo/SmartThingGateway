package ru.pobopo.smart.thing.gateway.model;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class GatewayQueueInfo {
    private String queueIn;
    private String queueOut;
}

package ru.pobopo.smart.thing.gateway.model;

import lombok.Data;

@Data
public class GatewayCloudConfig {
    private String brokerIp;
    private int brokerPort;
    private String queueIn;
    private String queueOut;
    private String queueNotification;
}

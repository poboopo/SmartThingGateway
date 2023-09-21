package ru.pobopo.smart.thing.gateway.model;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class GatewayInfo {
    private String id;
    private String name;
    private String description;
    private String queueIn;
    private String queueOut;
}

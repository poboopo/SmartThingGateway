package ru.pobopo.smart.thing.gateway.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
public class CloudIdentity {
    private GatewayInfo gateway;
    private UserInfo user;
}

package ru.pobopo.smart.thing.gateway.model;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AuthenticatedCloudUser {
    private GatewayInfo gateway;
    private UserInfo user;
}

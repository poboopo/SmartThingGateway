package ru.pobopo.smart.thing.gateway.model;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AuthorizedCloudUser {
    private GatewayInfo gateway;
    private UserInfo user;
}

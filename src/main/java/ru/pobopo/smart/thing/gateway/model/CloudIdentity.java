package ru.pobopo.smart.thing.gateway.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
@Schema(description = "Cached cloud identity")
public class CloudIdentity {
    @Schema(description = "Current gateway information")
    private GatewayInfo gateway;
    @Schema(description = "Current authenticated user information")
    private UserInfo user;
}

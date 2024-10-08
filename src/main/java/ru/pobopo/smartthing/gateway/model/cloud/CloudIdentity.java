package ru.pobopo.smartthing.gateway.model.cloud;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import ru.pobopo.smartthing.gateway.model.UserInfo;
import ru.pobopo.smartthing.model.GatewayInfo;

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

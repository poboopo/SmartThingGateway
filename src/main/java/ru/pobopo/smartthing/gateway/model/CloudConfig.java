package ru.pobopo.smartthing.gateway.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Schema(description = "Cloud connection configurations")
public class CloudConfig {
    @Schema(description = "Authentication token")
    private String token;
    @Schema(description = "Cloud app ip")
    private String cloudIp;
    @Schema(description = "Cloud app port")
    private int cloudPort;
}

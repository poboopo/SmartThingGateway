package ru.pobopo.smart.thing.gateway.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Notification")
public class Notification {
    @Schema(description = "Notification message")
    private String message;
    @Schema(description = "Notification type", example = "info")
    private NotificationType type;
}

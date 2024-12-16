package ru.pobopo.smartthing.gateway.model.cloud;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CloudConnectionStatusMessage {
    private CloudConnectionStatus status;
}

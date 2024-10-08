package ru.pobopo.smartthing.gateway.model.ota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.pobopo.smartthing.gateway.service.ota.OtaFirmwareTaskStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtaUploadProgressShort {
    private OtaFirmwareTaskStatus status;
    private Integer progress;
}

package ru.pobopo.smartthing.gateway.model.ota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.pobopo.smartthing.gateway.service.ota.OtaFirmwareTaskStatus;
import ru.pobopo.smartthing.model.DeviceInfo;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class OtaFirmwareUploadProgress {
    private UUID taskId;
    private DeviceInfo device;
    private OtaFirmwareInfo firmware;
    private OtaFirmwareTaskStatus status;
    private int progress;
}

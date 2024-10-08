package ru.pobopo.smartthing.gateway.model.ota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OtaFirmwareInfo {
    private UUID id;
    private String board;
    private String type;
    private String version;
    private String fileName;
    private String fileChecksum;
}

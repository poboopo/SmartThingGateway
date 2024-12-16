package ru.pobopo.smartthing.gateway.model.ota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.pobopo.smartthing.annotation.FileRepoId;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OtaFirmwareInfo {
    @FileRepoId
    private UUID id;
    private String board;
    private String type;
    private String version;
    private String fileName;
    private String fileChecksum;
}

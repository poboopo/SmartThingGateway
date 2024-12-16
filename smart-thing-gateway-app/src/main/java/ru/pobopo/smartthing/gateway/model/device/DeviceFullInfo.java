package ru.pobopo.smartthing.gateway.model.device;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DeviceFullInfo {
    private String ip;
    private String name;
    private String version;
    private String type;
    @JsonProperty("chip_model")
    private String chipModel;
    @JsonProperty("chip_revision")
    private String chipRevision;
}

package ru.pobopo.smartthing.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("device.search")
public class DeviceSearchProperties {
    private String group;
    private int port;
}

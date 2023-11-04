package ru.pobopo.smart.thing.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.hc.core5.http.Header;
import org.springframework.http.HttpHeaders;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceResponse {
    private int code;
    private String body;
    private HttpHeaders headers;
}

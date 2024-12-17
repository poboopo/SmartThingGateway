package ru.pobopo.smartthing.gateway.stomp.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import ru.pobopo.smartthing.gateway.service.device.DeviceRequestService;
import ru.pobopo.smartthing.model.InternalHttpResponse;
import ru.pobopo.smartthing.model.stomp.DeviceRequestMessage;

@Slf4j
@RequiredArgsConstructor
public class DeviceRequestMessageProcessor implements MessageProcessor {

    private final DeviceRequestService apiService;

    @Override
    public InternalHttpResponse process(Object payload) throws Exception {
        DeviceRequestMessage request = (DeviceRequestMessage) payload;
        ResponseEntity<String> response = apiService.execute(request.getRequest());
        return InternalHttpResponse.builder()
                .data(response.getBody())
                .status(response.getStatusCode())
                .headers(response.getHeaders())
                .build();
    }

}

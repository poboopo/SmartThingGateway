package ru.pobopo.smart.thing.gateway.stomp.processor;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.pobopo.smart.thing.gateway.exception.MissingValueException;
import ru.pobopo.smart.thing.gateway.model.DeviceResponse;
import ru.pobopo.smart.thing.gateway.stomp.message.DeviceRequestMessage;
import ru.pobopo.smart.thing.gateway.stomp.message.MessageResponse;
import ru.pobopo.smart.thing.gateway.service.DeviceService;

@Slf4j
public class DeviceRequestMessageProcessor implements MessageProcessor {
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final DeviceService deviceService;

    @Autowired
    public DeviceRequestMessageProcessor(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    public MessageResponse process(Object payload) throws Exception {
        DeviceRequestMessage request = (DeviceRequestMessage) payload;
        if (StringUtils.isBlank(request.getRequestId())) {
            throw new MissingValueException("Request id is missing!");
        }

        MessageResponse response = new MessageResponse();
        response.setRequestId(request.getRequestId());

        try {
            DeviceResponse deviceResponse = deviceService.sendRequest(request);
            response.setResponse(objectMapper.writeValueAsString(deviceResponse));
        } catch (Exception exception) {
            response.setSuccess(false);
            response.setResponse(exception.getMessage());
        }

        return response;
    }

}

package ru.pobopo.smart.thing.gateway.stomp.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.pobopo.smart.thing.gateway.exception.MissingValueException;
import ru.pobopo.smart.thing.gateway.service.DeviceApiService;
import ru.pobopo.smart.thing.gateway.stomp.message.DeviceRequestMessage;
import ru.pobopo.smart.thing.gateway.stomp.message.MessageResponse;

@Slf4j
public class DeviceRequestMessageProcessor implements MessageProcessor {

    private final DeviceApiService apiService;

    @Autowired
    public DeviceRequestMessageProcessor(DeviceApiService apiService) {
        this.apiService = apiService;
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
            response.setResponse(apiService.execute(request.getRequest()));
        } catch (Exception exception) {
            response.setSuccess(false);
            response.setError(exception.getMessage());
        }

        return response;
    }

}

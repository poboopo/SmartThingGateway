package ru.pobopo.smartthing.gateway.stomp.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import ru.pobopo.smartthing.gateway.exception.MissingValueException;
import ru.pobopo.smartthing.gateway.service.DeviceApiService;
import ru.pobopo.smartthing.model.InternalHttpResponse;
import ru.pobopo.smartthing.model.stomp.DeviceRequestMessage;
import ru.pobopo.smartthing.model.stomp.ResponseMessage;

@Slf4j
public class DeviceRequestMessageProcessor implements MessageProcessor {

    private final DeviceApiService apiService;

    @Autowired
    public DeviceRequestMessageProcessor(DeviceApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public InternalHttpResponse process(Object payload) throws Exception {
        DeviceRequestMessage request = (DeviceRequestMessage) payload;
        return apiService.execute(request.getRequest());
    }

}

package ru.pobopo.smartthing.gateway.stomp.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.AbstractMessageConverter;
import ru.pobopo.smartthing.model.stomp.BaseMessage;
import ru.pobopo.smartthing.model.stomp.DeviceRequestMessage;
import ru.pobopo.smartthing.model.stomp.GatewayCommandMessage;
import ru.pobopo.smartthing.model.stomp.GatewayRequestMessage;

import java.io.IOException;

public class BaseMessageConverter extends AbstractMessageConverter {
    private final ObjectMapper objectMapper;

    public BaseMessageConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
        byte[] payload = (byte[]) message.getPayload();
        try {
            BaseMessage base = objectMapper.readValue(payload, BaseMessage.class);
            switch (base.getType()) {
                case DEVICE_REQUEST -> {
                    return objectMapper.readValue(payload, DeviceRequestMessage.class);
                }
                case GATEWAY_COMMAND -> {
                    return objectMapper.readValue(payload, GatewayCommandMessage.class);
                }
                case GATEWAY_REQUEST -> {
                    return objectMapper.readValue(payload, GatewayRequestMessage.class);
                }
                default -> {
                    return base;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return clazz == BaseMessage.class;
    }
}

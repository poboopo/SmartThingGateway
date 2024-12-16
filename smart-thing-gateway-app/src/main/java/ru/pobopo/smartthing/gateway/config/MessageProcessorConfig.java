package ru.pobopo.smartthing.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smartthing.gateway.service.cloud.CloudApiService;
import ru.pobopo.smartthing.gateway.service.device.DeviceApiService;
import ru.pobopo.smartthing.gateway.stomp.MessageProcessorFactory;
import ru.pobopo.smartthing.gateway.stomp.processor.DeviceRequestMessageProcessor;
import ru.pobopo.smartthing.gateway.stomp.processor.GatewayCommandProcessor;
import ru.pobopo.smartthing.gateway.stomp.processor.GatewayRequestProcessor;
import ru.pobopo.smartthing.model.stomp.MessageType;

@Configuration
public class MessageProcessorConfig {

    @Bean
    public MessageProcessorFactory messageProcessorFactory(
            CloudApiService cloudService,
            DeviceApiService deviceApiService,
            RestTemplate restTemplate,
            @Value("${server.port}") String serverPort
    ) {
        MessageProcessorFactory messageProcessorFactory = new MessageProcessorFactory();
        messageProcessorFactory.addProcessor(MessageType.DEVICE_REQUEST, new DeviceRequestMessageProcessor(
                deviceApiService
        ));
        messageProcessorFactory.addProcessor(MessageType.GATEWAY_COMMAND, new GatewayCommandProcessor(
                cloudService
        ));
        messageProcessorFactory.addProcessor(MessageType.GATEWAY_REQUEST, new GatewayRequestProcessor(
                restTemplate,
                serverPort
        ));
        return messageProcessorFactory;
    }
}

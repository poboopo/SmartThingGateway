package ru.pobopo.smartthing.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smartthing.gateway.service.cloud.CloudApiService;
import ru.pobopo.smartthing.gateway.service.device.DeviceRequestService;
import ru.pobopo.smartthing.gateway.stomp.MessageProcessorFactory;
import ru.pobopo.smartthing.gateway.stomp.processor.DeviceRequestMessageProcessor;
import ru.pobopo.smartthing.gateway.stomp.processor.GatewayCommandProcessor;
import ru.pobopo.smartthing.gateway.stomp.processor.GatewayRequestProcessor;
import ru.pobopo.smartthing.model.stomp.MessageType;

@Configuration
public class MessageProcessorConfig {

    // todo use application context to inject beans
    // todo why create object by hand in the first place?
    // i think there is was a problem with beans resolve

    @Bean
    public MessageProcessorFactory messageProcessorFactory(
            CloudApiService cloudService,
            DeviceRequestService deviceRequestService,
            RestTemplate restTemplate,
            @Value("${server.port}") String serverPort
    ) {
        MessageProcessorFactory messageProcessorFactory = new MessageProcessorFactory();
        messageProcessorFactory.addProcessor(MessageType.DEVICE_REQUEST, new DeviceRequestMessageProcessor(
                deviceRequestService
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

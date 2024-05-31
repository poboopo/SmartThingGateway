package ru.pobopo.smart.thing.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smart.thing.gateway.service.DeviceApiService;
import ru.pobopo.smart.thing.gateway.stomp.MessageProcessorFactory;
import ru.pobopo.smart.thing.gateway.stomp.processor.DeviceRequestMessageProcessor;
import ru.pobopo.smart.thing.gateway.stomp.processor.GatewayCommandProcessor;
import ru.pobopo.smart.thing.gateway.service.CloudService;
import ru.pobopo.smart.thing.gateway.stomp.processor.GatewayRequestProcessor;
import ru.pobopo.smartthing.model.stomp.MessageType;

@Configuration
public class MessageProcessorConfig {

    @Bean
    public MessageProcessorFactory messageProcessorFactory(
            CloudService cloudService,
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

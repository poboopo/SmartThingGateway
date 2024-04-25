package ru.pobopo.smart.thing.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smart.thing.gateway.jobs.DevicesSearchJob;
import ru.pobopo.smart.thing.gateway.service.DeviceApiService;
import ru.pobopo.smart.thing.gateway.stomp.MessageProcessorFactory;
import ru.pobopo.smart.thing.gateway.stomp.message.GatewayMessageType;
import ru.pobopo.smart.thing.gateway.stomp.processor.DeviceRequestMessageProcessor;
import ru.pobopo.smart.thing.gateway.stomp.processor.GatewayCommandProcessor;
import ru.pobopo.smart.thing.gateway.service.CloudService;

@Configuration
public class MessageProcessorConfig {

    @Bean
    public MessageProcessorFactory messageProcessorFactory(
            DevicesSearchJob searchJob,
            CloudService cloudService,
            DeviceApiService deviceApiService,
            RestTemplate restTemplate
    ) {
        MessageProcessorFactory messageProcessorFactory = new MessageProcessorFactory();
        messageProcessorFactory.addProcessor(GatewayMessageType.DEVICE_REQUEST, new DeviceRequestMessageProcessor(
                deviceApiService
        ));
        messageProcessorFactory.addProcessor(GatewayMessageType.GATEWAY_COMMAND, new GatewayCommandProcessor(
                cloudService,
                restTemplate
        ));
        return messageProcessorFactory;
    }
}

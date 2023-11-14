package ru.pobopo.smart.thing.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.pobopo.smart.thing.gateway.jobs.DeviceSearchJob;
import ru.pobopo.smart.thing.gateway.rabbitmq.MessageProcessorFactory;
import ru.pobopo.smart.thing.gateway.rabbitmq.message.GatewayMessageType;
import ru.pobopo.smart.thing.gateway.rabbitmq.processor.DeviceRequestMessageProcessor;
import ru.pobopo.smart.thing.gateway.rabbitmq.processor.GatewayCommandProcessor;
import ru.pobopo.smart.thing.gateway.service.CloudService;
import ru.pobopo.smart.thing.gateway.service.ConfigurationService;
import ru.pobopo.smart.thing.gateway.service.DeviceService;

@Configuration
public class MessageProcessorConfig {

    @Bean
    public MessageProcessorFactory messageProcessorFactory(
            DeviceSearchJob searchJob,
            DeviceService deviceService,
            ConfigurationService configurationService,
            CloudService cloudService
    ) {
        MessageProcessorFactory messageProcessorFactory = new MessageProcessorFactory();
        messageProcessorFactory.addProcessor(GatewayMessageType.DEVICE_REQUEST, new DeviceRequestMessageProcessor(deviceService));
        messageProcessorFactory.addProcessor(GatewayMessageType.GATEWAY_COMMAND, new GatewayCommandProcessor(
                searchJob,
                configurationService,
                cloudService
        ));
        return messageProcessorFactory;
    }
}

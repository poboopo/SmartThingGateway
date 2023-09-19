package ru.pobopo.smart.thing.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.pobopo.smart.thing.gateway.jobs.DeviceSearchJob;
import ru.pobopo.smart.thing.gateway.rabbitmq.MessageProcessorFactory;
import ru.pobopo.smart.thing.gateway.rabbitmq.message.GatewayMessageType;
import ru.pobopo.smart.thing.gateway.rabbitmq.processor.DeviceRequestProcessor;
import ru.pobopo.smart.thing.gateway.rabbitmq.processor.GatewayCommandProcessor;

@Configuration
public class MessageProcessorConfig {

    @Bean
    public MessageProcessorFactory messageProcessorFactory(DeviceSearchJob searchJob) {
        MessageProcessorFactory messageProcessorFactory = new MessageProcessorFactory();
        messageProcessorFactory.addProcessor(GatewayMessageType.DEVICE_REQUEST, new DeviceRequestProcessor());
        messageProcessorFactory.addProcessor(GatewayMessageType.GATEWAY_COMMAND, new GatewayCommandProcessor(searchJob));
        return messageProcessorFactory;
    }
}

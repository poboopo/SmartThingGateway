package ru.pobopo.smart.thing.gateway.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.model.GatewayInfo;
import ru.pobopo.smart.thing.gateway.model.GatewayQueueInfo;
import ru.pobopo.smart.thing.gateway.rabbitmq.MessageConsumer;
import ru.pobopo.smart.thing.gateway.rabbitmq.MessageProcessorFactory;
import ru.pobopo.smart.thing.gateway.service.CloudService;

@Component
@Slf4j
public class CommandsConsumer implements Runnable {
    private final CloudService cloudService;
    private final String token;
    private final String brokerUrl;
    private final MessageProcessorFactory messageProcessorFactory;


    @Autowired
    public CommandsConsumer(CloudService cloudService, Environment environment, MessageProcessorFactory messageProcessorFactory) {
        this.cloudService = cloudService;
        this.messageProcessorFactory = messageProcessorFactory;
        this.token = environment.getProperty("TOKEN");
        this.brokerUrl = environment.getProperty("BROKER_URL");
    }

    @Override
    public void run() {
        if (StringUtils.isBlank(token)) {
            log.error("Token is missing!");
            return;
        }

        if (StringUtils.isBlank(brokerUrl)) {
            log.error("Broker url is missing!");
            return;
        }

        GatewayInfo info = cloudService.getGatewayInfo();
        if (info == null) {
            return;
        }
        log.info("Loaded gateway info: {}", info);

        GatewayQueueInfo queueInfo = cloudService.getQueueInfo();
        if (queueInfo == null) {
            return;
        }
        log.info("Loaded gateway queue info: {}", queueInfo);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(brokerUrl);
        factory.setUsername(info.getId());
        factory.setPassword(token);

        try (Connection connection = factory.newConnection()) {
            Channel channel = connection.createChannel();
            log.info("Connected to rabbit");

            channel.basicConsume(
                queueInfo.getQueueIn(),
                true,
                "commands_consumer",
                new MessageConsumer(channel, messageProcessorFactory, queueInfo.getQueueOut())
            );
            log.info("Subscribed to queue {}", queueInfo.getQueueIn());

            for(;;){}
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}

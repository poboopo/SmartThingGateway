package ru.pobopo.smart.thing.gateway.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.event.CloudInfoLoadedEvent;
import ru.pobopo.smart.thing.gateway.model.GatewayInfo;
import ru.pobopo.smart.thing.gateway.service.CloudService;

@Component
@Slf4j
public class CommandsConsumer {
    private final CloudService cloudService;
    private String token;
    private  String brokerIp;
    private final MessageProcessorFactory messageProcessorFactory;
    private Connection connection;
    private Channel channel;

    @Autowired
    public CommandsConsumer(CloudService cloudService, Environment environment, MessageProcessorFactory messageProcessorFactory) {
        this.cloudService = cloudService;
        this.messageProcessorFactory = messageProcessorFactory;
        this.token = environment.getProperty("TOKEN");
        this.brokerIp = environment.getProperty("BROKER_URL");
    }

    @EventListener
    @Order(1)
    public void connect(CloudInfoLoadedEvent event) throws IOException, TimeoutException {
        closeConnection();

        token = event.getToken();
        brokerIp = event.getBrokerIp();
        log.info("Token and broker ip were updated!");

        if (StringUtils.isBlank(token)) {
            log.error("Token is missing!");
            return;
        }

        if (StringUtils.isBlank(brokerIp)) {
            log.error("Broker ip is missing!");
            return;
        }

        GatewayInfo info = cloudService.getGatewayInfo();
        if (info == null) {
            log.error("No gateway info were loaded, leaving...");
            return;
        }
        log.info("Loaded gateway info: {}", info);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(brokerIp);
        factory.setUsername(info.getId());
        factory.setPassword(token);

        connection = factory.newConnection();
        this.channel = connection.createChannel();
        log.info("Connected to rabbit");

        channel.basicConsume(
            info.getQueueIn(),
            true,
            "commands_consumer",
            new MessageConsumer(channel, messageProcessorFactory, info.getQueueOut())
        );
        log.info("Subscribed to queue {}", info.getQueueIn());
    }


    @PreDestroy
    public void closeConnection() throws IOException, TimeoutException {
        if (channel == null) {
            return;
        }
        channel.close();
        log.info("Channel were closed");

        if (connection == null) {
            return;
        }
        connection.close();
        log.info("Connection were closed");
    }
}

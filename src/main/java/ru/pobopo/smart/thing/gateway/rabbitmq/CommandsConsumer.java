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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.event.CloudConnectionEvent;
import ru.pobopo.smart.thing.gateway.event.CloudConnectionEventType;
import ru.pobopo.smart.thing.gateway.event.CloudInfoLoadedEvent;
import ru.pobopo.smart.thing.gateway.event.RabbitConnectionCloseEvent;
import ru.pobopo.smart.thing.gateway.model.GatewayInfo;
import ru.pobopo.smart.thing.gateway.service.CloudService;

@Component
@Slf4j
public class CommandsConsumer {
    private static final String CONSUMER_TAG = "gateway_commands_consumer";

    private final MessageProcessorFactory messageProcessorFactory;
    private final CloudService cloudService;
    private final RabbitExceptionHandler exceptionHandler;
    private final ApplicationEventPublisher applicationEventPublisher;

    private String token;
    private  String brokerIp;
    private Connection connection;
    private Channel channel;

    @Autowired
    public CommandsConsumer(
        CloudService cloudService,
        Environment environment,
        MessageProcessorFactory messageProcessorFactory,
        RabbitExceptionHandler exceptionHandler,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.cloudService = cloudService;
        this.messageProcessorFactory = messageProcessorFactory;
        this.token = environment.getProperty("TOKEN");
        this.brokerIp = environment.getProperty("BROKER_URL");
        this.exceptionHandler = exceptionHandler;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener
    @Order(1)
    public void connect(CloudInfoLoadedEvent event) throws IOException, TimeoutException {
        cleanup();

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
        sendEvent(CloudConnectionEventType.GATEWAY_INFO_LOADED);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(brokerIp);
        factory.setUsername(info.getId());
        factory.setPassword(token);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setExceptionHandler(exceptionHandler);

        connection = factory.newConnection();
        this.channel = connection.createChannel();
        log.info("Connected to rabbit");
        sendEvent(CloudConnectionEventType.CONNECTED_TO_BROKER);

        channel.basicConsume(
            info.getQueueIn(),
            true,
            CONSUMER_TAG,
            new MessageConsumer(channel, messageProcessorFactory, info.getQueueOut())
        );
        log.info("Subscribed to queue {}", info.getQueueIn());
        sendEvent(CloudConnectionEventType.SUBSCRIBED_TO_QUEUE);
    }

    @EventListener
    public void connectionEvent(RabbitConnectionCloseEvent event) throws IOException, TimeoutException {
        log.warn("Closing rabbitMq channel and connection");
        cleanup();
    }


    @PreDestroy
    public void cleanup() throws IOException, TimeoutException {
        if (channel != null) {
            channel.basicCancel(CONSUMER_TAG);
            if (channel.isOpen()) {
                channel.close();
                log.warn("Channel were closed");
            }
        }

        if (connection != null && connection.isOpen()) {
            connection.close();
            log.warn("Connection were closed");
        }

        sendEvent(CloudConnectionEventType.CONNECTION_CLOSED);
    }

    private void sendEvent(CloudConnectionEventType eventType) {
        applicationEventPublisher.publishEvent(new CloudConnectionEvent(this, eventType));
    }
}

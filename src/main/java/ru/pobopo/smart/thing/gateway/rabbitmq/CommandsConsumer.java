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
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.event.*;
import ru.pobopo.smart.thing.gateway.exception.ConfigurationException;
import ru.pobopo.smart.thing.gateway.model.CloudInfo;
import ru.pobopo.smart.thing.gateway.model.GatewayInfo;
import ru.pobopo.smart.thing.gateway.service.ConfigurationService;

@Component
@Slf4j
public class CommandsConsumer {
    private static final String CONSUMER_TAG = "gateway_commands_consumer";

    private final MessageProcessorFactory messageProcessorFactory;
    private final RabbitExceptionHandler exceptionHandler;
    private final ConfigurationService configurationService;

    private Connection connection;
    private Channel channel;

    @Autowired
    public CommandsConsumer(
        MessageProcessorFactory messageProcessorFactory,
        RabbitExceptionHandler exceptionHandler,
        ConfigurationService configurationService
    ) {
        this.messageProcessorFactory = messageProcessorFactory;
        this.exceptionHandler = exceptionHandler;
        this.configurationService = configurationService;
    }

    @EventListener
    public void connectionClosedEvent(RabbitConnectionCloseEvent event) throws IOException, TimeoutException {
        log.warn("Closing rabbitMq channel and connection");
        cleanup();
    }

    @EventListener
    public void connect(AuthorizedEvent event) throws IOException, TimeoutException, InterruptedException {
        cleanup();
        try {
            GatewayInfo gatewayInfo = event.getAuthorizedCloudUser().getGateway();
            if (gatewayInfo == null) {
                throw new ConfigurationException("Gateway info is missing");
            }

            connection = getConnectionFactory(gatewayInfo).newConnection();
            this.channel = connection.createChannel();
            log.info("Connected to rabbit");

            channel.basicConsume(
                    gatewayInfo.getQueueIn(),
                    true,
                    CONSUMER_TAG,
                    new MessageConsumer(channel, messageProcessorFactory, gatewayInfo.getQueueOut())
            );
            log.info("Subscribed to queue {}", gatewayInfo.getQueueIn());
        } catch (ConfigurationException exception) {
            log.error("Failed to configure rabbitmq connection: {}", exception.getMessage());
        }
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
    }

    private ConnectionFactory getConnectionFactory(GatewayInfo gatewayInfo) throws ConfigurationException {
        CloudInfo cloudInfo = configurationService.getCloudInfo();
        if (cloudInfo == null) {
            throw new ConfigurationException("Cloud info missing");
        }

        String token = cloudInfo.getToken();
        String brokerIp = cloudInfo.getCloudIp();

        if (StringUtils.isBlank(token)) {
            throw new ConfigurationException("Token is missing");
        }
        if (StringUtils.isBlank(brokerIp)) {
            throw new ConfigurationException("Broker ip is missing");
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(brokerIp);
        factory.setUsername(gatewayInfo.getId());
        factory.setPassword(token);
        factory.setExceptionHandler(exceptionHandler);

        return factory;
    }
}

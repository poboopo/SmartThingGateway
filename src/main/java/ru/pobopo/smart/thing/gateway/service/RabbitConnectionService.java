package ru.pobopo.smart.thing.gateway.service;

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
import ru.pobopo.smart.thing.gateway.exception.AccessDeniedException;
import ru.pobopo.smart.thing.gateway.exception.ConfigurationException;
import ru.pobopo.smart.thing.gateway.model.CloudAuthInfo;
import ru.pobopo.smart.thing.gateway.model.GatewayConfig;
import ru.pobopo.smart.thing.gateway.model.GatewayInfo;
import ru.pobopo.smart.thing.gateway.rabbitmq.MessageConsumer;
import ru.pobopo.smart.thing.gateway.rabbitmq.MessageProcessorFactory;
import ru.pobopo.smart.thing.gateway.rabbitmq.RabbitExceptionHandler;

@Component
@Slf4j
public class RabbitConnectionService {
    private static final String CONSUMER_TAG = "gateway_commands_consumer";

    private final MessageProcessorFactory messageProcessorFactory;
    private final RabbitExceptionHandler exceptionHandler;
    private final ConfigurationService configurationService;
    private final CloudService cloudService;

    private Connection connection;
    private Channel channel;

    @Autowired
    public RabbitConnectionService(
        MessageProcessorFactory messageProcessorFactory,
        RabbitExceptionHandler exceptionHandler,
        ConfigurationService configurationService,
        CloudService cloudService
    ) {
        this.messageProcessorFactory = messageProcessorFactory;
        this.exceptionHandler = exceptionHandler;
        this.configurationService = configurationService;
        this.cloudService = cloudService;
    }

    public boolean isConnected() {
        return channel != null && channel.isOpen();
    }

    @EventListener
    public void connectionClosedEvent(RabbitConnectionCloseEvent event) throws IOException, TimeoutException {
        log.warn("Closing rabbitMq channel and connection");
        cleanup();
    }

    @EventListener
    public void connect(AuthorizedEvent event) throws IOException, TimeoutException {
        connect(event.getAuthorizedCloudUser().getGateway());
    }

    public boolean connect(GatewayInfo gatewayInfo) throws IOException, TimeoutException {
        cleanup();
        try {
            if (gatewayInfo == null) {
                throw new ConfigurationException("Gateway info is missing");
            }

            GatewayConfig config = cloudService.getGatewayConfig();
            if (config == null) {
                throw new ConfigurationException("Gateway config is missing");
            }

            log.info("Using gateway config: {}", config);

            connection = getConnectionFactory(gatewayInfo, config).newConnection();
            this.channel = connection.createChannel();
            log.info("Connected to rabbit message broker");

            channel.basicConsume(
                    config.getQueueIn(),
                    true,
                    CONSUMER_TAG,
                    new MessageConsumer(channel, messageProcessorFactory, config.getQueueOut())
            );
            return true;
        } catch (ConfigurationException exception) {
            log.error("Failed to configure rabbitmq connection: {}", exception.getMessage());
        } catch (AccessDeniedException e) {
            log.error("Failed to authenticate!");
        }
        return false;
    }

    @PreDestroy
    public void cleanup() throws IOException, TimeoutException {
        if (channel != null && channel.isOpen()) {
            channel.basicCancel(CONSUMER_TAG);
            channel.close();
            log.warn("Channel were closed");
        }

        if (connection != null && connection.isOpen()) {
            connection.close();
            log.warn("Connection were closed");
        }
    }

    private ConnectionFactory getConnectionFactory(GatewayInfo gatewayInfo, GatewayConfig gatewayConfig) throws ConfigurationException {
        CloudAuthInfo cloudInfo = configurationService.getCloudAuthInfo();
        if (cloudInfo == null) {
            throw new ConfigurationException("Cloud info missing");
        }

        String token = cloudInfo.getToken();
        String brokerIp = gatewayConfig.getBrokerIp();
        int port = gatewayConfig.getBrokerPort();

        if (StringUtils.isBlank(token)) {
            throw new ConfigurationException("Token is missing");
        }
        if (StringUtils.isBlank(brokerIp)) {
            throw new ConfigurationException("Broker ip is missing");
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(brokerIp);
        factory.setPort(port);
        factory.setUsername(gatewayInfo.getId());
        factory.setPassword(token);
        factory.setExceptionHandler(exceptionHandler);

        log.info(
                "Message broker connection factory: host={} port={} username={}",
                factory.getHost(),
                factory.getPort(),
                factory.getUsername()
        );

        return factory;
    }
}

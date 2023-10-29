package ru.pobopo.smart.thing.gateway.rabbitmq;

import com.rabbitmq.client.AuthenticationFailureException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.impl.DefaultExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.event.RabbitConnectionCloseEvent;
import ru.pobopo.smart.thing.gateway.exception.LogoutException;

@Component
@Slf4j
public class RabbitExceptionHandler extends DefaultExceptionHandler {
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public RabbitExceptionHandler(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void handleConsumerException(Channel channel, Throwable exception, Consumer consumer, String consumerTag, String methodName) {
        super.handleConsumerException(channel, exception, consumer, consumerTag, methodName);
        if (exception instanceof LogoutException) {
            publishEvent();
        }
    }

    @Override
    public void handleConnectionRecoveryException(Connection connection, Throwable throwable) {
        checkException(throwable);
    }

    @Override
    public void handleChannelRecoveryException(Channel channel, Throwable throwable) {
        checkException(throwable);
    }

    private void checkException(Throwable throwable) {
        if (throwable instanceof AuthenticationFailureException) {
            log.error("Authorization failed! Bad token?");
            publishEvent();
        } else {
            log.debug("Recovery exception: {}", throwable.getMessage());
        }
    }

    private void publishEvent() {
        log.info("Publishing RabbitConnectionCloseEvent");
        applicationEventPublisher.publishEvent(new RabbitConnectionCloseEvent(this));
    }
}

package ru.pobopo.smart.thing.gateway.rabbitmq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import ru.pobopo.smart.thing.gateway.rabbitmq.message.MessageResponse;
import ru.pobopo.smart.thing.gateway.rabbitmq.processor.MessageProcessor;

@Slf4j
public class MessageConsumer extends DefaultConsumer {
    private final String queueOut;

    private final MessageProcessorFactory messageProcessorFactory;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public MessageConsumer(Channel channel, MessageProcessorFactory messageProcessorFactory, String queueOut) {
        super(channel);
        this.messageProcessorFactory = messageProcessorFactory;
        this.queueOut = queueOut;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
        throws IOException {
        String message = new String(body);
        log.info("Got new message! {}", message);
        MessageProcessor processor = messageProcessorFactory.getProcessor(properties.getType());
        if (processor == null) {
            log.error("Can't find message processor for type {}", properties.getType());
            return;
        }

        try {
            MessageResponse messageResponse = processor.process(message);
            if (messageResponse != null) {
                String response = objectMapper.writeValueAsString(messageResponse);
                log.info("Sending response to queue {}: {}", queueOut, response);
                getChannel().basicPublish("", queueOut, null, response.getBytes());
            } else {
                log.warn("Empty message response!");
            }
        } catch (Exception exception) {
            log.error("Failed to process message", exception);
        }
//                        channel.basicAck(envelope.getDeliveryTag(), false);
    }
}

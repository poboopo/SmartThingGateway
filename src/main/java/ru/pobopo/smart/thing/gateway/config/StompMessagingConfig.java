package ru.pobopo.smart.thing.gateway.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import ru.pobopo.smartthing.model.stomp.BaseMessage;
import ru.pobopo.smartthing.model.stomp.DeviceRequestMessage;
import ru.pobopo.smartthing.model.stomp.GatewayCommand;

import static ru.pobopo.smart.thing.gateway.service.MessageBrokerService.CONNECTION_STATUS_TOPIC;
import static ru.pobopo.smart.thing.gateway.service.NotificationService.NOTIFICATION_TOPIC;

@Configuration
@EnableWebSocketMessageBroker
public class StompMessagingConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(
                "/devices",
                NOTIFICATION_TOPIC,
                CONNECTION_STATUS_TOPIC
        );
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*");
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        messageConverters.add(messageConverter());
        return false;
    }

    @Bean
    public WebSocketStompClient stompClient() {
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setMessageConverter(new BaseMessageConverter(objectMapper()));
        stompClient.setTaskScheduler(taskScheduler());
        return stompClient;
    }

    private ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10);
        taskScheduler.initialize();
        return taskScheduler;
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.findAndRegisterModules();
        return mapper;
    }

    private MessageConverter messageConverter() {
        DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
        resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper());
        converter.setContentTypeResolver(resolver);
        return converter;
    }

    static class BaseMessageConverter extends AbstractMessageConverter {
        private final ObjectMapper objectMapper;

        public BaseMessageConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
            byte[] payload = (byte[]) message.getPayload();
            try {
                BaseMessage base = objectMapper.readValue(payload, BaseMessage.class);
                switch (base.getType()) {
                    case DEVICE_REQUEST -> {
                        return objectMapper.readValue(payload, DeviceRequestMessage.class);
                    }
                    case GATEWAY_COMMAND -> {
                        return objectMapper.readValue(payload, GatewayCommand.class);
                    }
                    default -> {
                        return base;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected boolean supports(Class<?> clazz) {
            return clazz == BaseMessage.class;
        }
    }
}

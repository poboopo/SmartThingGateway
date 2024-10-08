package ru.pobopo.smartthing.gateway.config;

import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.*;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class StompMessagingConfig implements WebSocketMessageBrokerConfigurer {
    public static final String NOTIFICATION_TOPIC = "/notifications";
    public static final String CONNECTION_STATUS_TOPIC = "/connection/status";
    public static final String DASHBOARD_TOPIC_PREFIX = "/dashboard";
    public static final String DEVICES_TOPIC = "/devices";
    public static final String OTA_PROGRESS_TOPIC = "/ota";

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(
                DEVICES_TOPIC,
                NOTIFICATION_TOPIC,
                CONNECTION_STATUS_TOPIC,
                DASHBOARD_TOPIC_PREFIX,
                OTA_PROGRESS_TOPIC
        ).setTaskScheduler(taskScheduler());
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/smt-ws").setAllowedOrigins("*");
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        messageConverters.add(messageConverter());
        return false;
    }
    private MessageConverter messageConverter() {
        DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
        resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper());
        converter.setContentTypeResolver(resolver);
        return converter;
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

}

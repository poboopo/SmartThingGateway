package ru.pobopo.smartthing.gateway;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.pobopo.smartthing.gateway.model.CloudConnectionStatus;
import ru.pobopo.smartthing.gateway.service.CloudService;
import ru.pobopo.smartthing.gateway.service.MessageBrokerService;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@SpringBootApplication
public class SmartThingGatewayApp {
    public static final Path DEFAULT_APP_DIR = Paths.get(System.getProperty("user.home"), ".smartthing");

    public static void main(String[] args) {
        SpringApplication.run(SmartThingGatewayApp.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.findAndRegisterModules();
        return mapper;
    }

    @Bean
    CommandLineRunner run(
            CloudService cloudService,
            MessageBrokerService brokerService
    )  {
        return args -> {
            try {
                cloudService.login();
            } catch (Throwable exception) {
                log.error("Failed to login in cloud: {}", exception.getMessage());
            }
            try {
                brokerService.connect(CloudConnectionStatus.NOT_CONNECTED);
            } catch (Throwable exception) {
                log.error("Failed to connect to cloud: {}", exception.getMessage());
            }
        };
    }
}

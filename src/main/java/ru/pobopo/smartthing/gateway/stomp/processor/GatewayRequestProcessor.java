package ru.pobopo.smartthing.gateway.stomp.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smartthing.gateway.exception.MissingValueException;
import ru.pobopo.smartthing.model.InternalHttpResponse;
import ru.pobopo.smartthing.model.stomp.GatewayRequestMessage;

@Slf4j
@RequiredArgsConstructor
public class GatewayRequestProcessor implements MessageProcessor {
    private final RestTemplate restTemplate;
    private final String gatewayPort;

    @Override
    public InternalHttpResponse process(Object payload) throws Exception {
        GatewayRequestMessage requestMessage = (GatewayRequestMessage) payload;
        log.info("Processing gateway request {}", requestMessage);
        if (StringUtils.isEmpty(requestMessage.getUrl())) {
            throw new MissingValueException("Url param is missing!");
        }
        if (StringUtils.isEmpty(requestMessage.getMethod())) {
            throw new MissingValueException("Method param is missing!");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("smt-cloud-request", "true");
        HttpEntity<?> entity = new HttpEntity<>(requestMessage.getData(), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "http://localhost:" + gatewayPort + requestMessage.getUrl(),
                    HttpMethod.valueOf(requestMessage.getMethod()),
                    entity,
                    String.class
            );
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            return new InternalHttpResponse(
                    response.getStatusCode(),
                    response.getBody(),
                    httpHeaders // this done bcs of "Multiple cross-origin headers" error
            );
        } catch (HttpServerErrorException | HttpClientErrorException exception) {
            log.error("Request failed: {} {}", exception.getMessage(), exception.getStatusCode());
            return new InternalHttpResponse(
                    exception.getStatusCode(),
                    exception.getResponseBodyAs(String.class),
                    new HttpHeaders()
            );
        }
    }
}

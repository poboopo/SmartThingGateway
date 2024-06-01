package ru.pobopo.smartthing.gateway.stomp.processor;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smartthing.gateway.exception.MissingValueException;
import ru.pobopo.smartthing.model.InternalHttpResponse;
import ru.pobopo.smartthing.model.stomp.GatewayRequestMessage;

@RequiredArgsConstructor
public class GatewayRequestProcessor implements MessageProcessor {
    private final RestTemplate restTemplate;
    private final String gatewayPort;

    @Override
    public Object process(Object payload) throws Exception {
        GatewayRequestMessage requestMessage = (GatewayRequestMessage) payload;
        if (StringUtils.isEmpty(requestMessage.getUrl())) {
            throw new MissingValueException("Url param is missing!");
        }
        if (StringUtils.isEmpty(requestMessage.getMethod())) {
            throw new MissingValueException("Method param is missing!");
        }
        HttpEntity<?> entity = new HttpEntity<>(requestMessage.getData());

        // todo add some role for internal requests?
        // so for example we gonna set /cloud/* as LOCAL and we not gonna be able to call it from outside
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + gatewayPort + requestMessage.getUrl(),
                HttpMethod.valueOf(requestMessage.getMethod()),
                entity,
                String.class
        );
        return new InternalHttpResponse(response.getStatusCode().value(), response.getBody(), response.getHeaders().toSingleValueMap());

    }
}

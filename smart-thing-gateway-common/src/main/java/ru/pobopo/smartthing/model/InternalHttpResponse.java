package ru.pobopo.smartthing.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InternalHttpResponse implements Serializable {
    @JsonSerialize(using = HttpStatusCodeSerializer.class)
    private HttpStatusCode status;
    private String data;
    private HttpHeaders headers;

    public ResponseEntity<String> toResponseEntity() {
        return new ResponseEntity<>(data, headers, status);
    }
}

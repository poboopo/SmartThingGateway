package ru.pobopo.smartthing.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.http.HttpStatusCode;

import java.io.IOException;

public class HttpStatusCodeSerializer extends JsonSerializer<HttpStatusCode> {
    @Override
    public void serialize(HttpStatusCode httpStatusCode, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeObject(httpStatusCode.value());
    }
}

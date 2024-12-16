package ru.pobopo.smartthing.model.stomp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.pobopo.smartthing.model.InternalHttpResponse;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseMessage {
    private UUID requestId;
    private InternalHttpResponse data;
    private String error;
    private String stack;
    private boolean success = true;
}

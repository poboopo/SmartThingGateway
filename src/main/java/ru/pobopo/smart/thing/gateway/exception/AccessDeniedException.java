package ru.pobopo.smart.thing.gateway.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AccessDeniedException extends Exception {
    private String message;
}

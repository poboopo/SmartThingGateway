package ru.pobopo.smart.thing.gateway.exception;

public class DeviceSettingsException extends Exception {
    public DeviceSettingsException(String message) {
        super(message);
    }

    public DeviceSettingsException(String message, Throwable cause) {
        super(message, cause);
    }
}

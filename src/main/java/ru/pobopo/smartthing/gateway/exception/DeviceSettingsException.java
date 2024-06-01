package ru.pobopo.smartthing.gateway.exception;

public class DeviceSettingsException extends Exception {
    public DeviceSettingsException(String message) {
        super(message);
    }

    public DeviceSettingsException(String message, Throwable cause) {
        super(message, cause);
    }
}

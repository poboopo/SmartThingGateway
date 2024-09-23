package ru.pobopo.smartthing.gateway.device.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.pobopo.smartthing.model.DeviceInfo;

@Slf4j
public abstract class DeviceApi {
    public abstract boolean accept(DeviceInfo deviceInfo);

    public abstract ResponseEntity<String> health(DeviceInfo info);

    public abstract ResponseEntity<String> getInfo(DeviceInfo info);

    public ResponseEntity<String> getActions(DeviceInfo info) {
        log.info("Calling default getActions method");
        return new ResponseEntity<>("[]", HttpStatus.OK);
    }
    public ResponseEntity<String> callAction(DeviceInfo info, String action) {
        log.info("Calling default callAction method");
        return new ResponseEntity<>("Actions not supported", HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<String> getSensors(DeviceInfo info) {
        log.info("Calling default getSensors method");
        return new ResponseEntity<>("[]", HttpStatus.OK);
    }

    public ResponseEntity<String> getStates(DeviceInfo info) {
        log.info("Calling default getStates method");
        return new ResponseEntity<>("[]", HttpStatus.OK);
    }
}

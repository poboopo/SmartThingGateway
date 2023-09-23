package ru.pobopo.smart.thing.gateway.event;

import org.springframework.context.ApplicationEvent;
import ru.pobopo.smart.thing.gateway.model.CloudInfo;

public class CloudInfoLoadedEvent extends ApplicationEvent {
    private final CloudInfo cloudInfo;

    public CloudInfoLoadedEvent(Object source, String token, String cloudUrl, String brokerIp) {
        super(source);
        this.cloudInfo = new CloudInfo(token, cloudUrl, brokerIp);
    }

    public CloudInfoLoadedEvent(Object source, CloudInfo info) {
        super(source);
        this.cloudInfo = info;
    }

    public String getToken() {
        return cloudInfo.getToken();
    }

    public String getCloudUrl() {
        return cloudInfo.getCloudUrl();
    }

    public String getBrokerIp() {
        return cloudInfo.getBrokerIp();
    }

    public CloudInfo getCloudInfo() {
        return cloudInfo;
    }
}

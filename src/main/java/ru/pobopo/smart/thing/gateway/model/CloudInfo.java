package ru.pobopo.smart.thing.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CloudInfo {
    private String token;
    private String cloudUrl;
    private String brokerIp;
}

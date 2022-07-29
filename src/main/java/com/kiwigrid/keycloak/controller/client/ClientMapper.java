package com.kiwigrid.keycloak.controller.client;

import java.util.Map;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode
public class ClientMapper {

    private String name;
    private String protocolMapper;
    private Map<String, String> config;
}
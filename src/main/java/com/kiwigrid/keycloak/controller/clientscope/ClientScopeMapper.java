package com.kiwigrid.keycloak.controller.clientscope;

import java.util.Map;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode
public class ClientScopeMapper {
    private String name;
    private String protocolMapper;
    private Map<String, String> config;
}

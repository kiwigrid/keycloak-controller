package com.kiwigrid.keycloak.controller.clientscope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode(callSuper = false)
public class ClientScopeSpec {
    private String keycloak = "default";
    private String realm;

    private String id;
    private String name;
    private String description;
    private List<ClientScopeMapper> mappers = new ArrayList<>();
    private String protocol;
    protected Map<String, String> attributes;
}

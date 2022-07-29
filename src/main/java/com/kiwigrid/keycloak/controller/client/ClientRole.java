package com.kiwigrid.keycloak.controller.client;

import java.util.List;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode
public class ClientRole {

    private String name;
    private List<String> realmRoles;
}

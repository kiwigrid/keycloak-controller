package com.kiwigrid.keycloak.controller.keycloak;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode(callSuper = false)
public class KeycloakSpec {
    private String url;
    private String realm = "master";
    private String clientId = "admin-cli";
    private String username = "admin";
    private String passwordSecretNamespace = "default";
    private String passwordSecretName = "keycloak-http";
    private String passwordSecretKey = "password";

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}

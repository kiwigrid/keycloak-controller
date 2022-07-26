package com.kiwigrid.keycloak.controller.keycloak;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode(callSuper = false)
public class KeycloakResourceStatus {
    private String timestamp;
    private String version;
    private String error;

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}

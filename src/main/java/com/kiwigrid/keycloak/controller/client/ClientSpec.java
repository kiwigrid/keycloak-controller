package com.kiwigrid.keycloak.controller.client;

import java.util.ArrayList;
import java.util.List;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode(callSuper = false)
public class ClientSpec {
    private String keycloak = "default";
    private String realm;
    private String clientId;
    private ClientType clientType;
    private String name;
    private Boolean enabled;
    private Boolean directAccessGrantsEnabled;
    private Boolean standardFlowEnabled;
    private Boolean implicitFlowEnabled;
    private Boolean serviceAccountsEnabled;
    private List<String> defaultRoles = new ArrayList<>();
    private List<String> defaultClientScopes = new ArrayList<>();
    private List<String> optionalClientScopes = new ArrayList<>();
    private List<String> webOrigins = new ArrayList<>();
    private List<String> redirectUris = new ArrayList<>();
    private String secretNamespace;
    private String secretName;
    private String secretKey = "secret";
    private List<ClientMapper> mapper = new ArrayList<>();
    private List<ClientRole> roles = new ArrayList<>();
    private List<String> serviceAccountRealmRoles = new ArrayList<>();

}
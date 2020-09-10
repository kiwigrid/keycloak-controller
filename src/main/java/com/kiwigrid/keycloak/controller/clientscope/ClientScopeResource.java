package com.kiwigrid.keycloak.controller.clientscope;

import com.kiwigrid.keycloak.controller.client.ClientResource;
import com.kiwigrid.keycloak.controller.client.ClientType;
import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionSpec;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionStatus;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.fabric8.kubernetes.client.CustomResourceList;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode(of = "spec", callSuper = false)
public class ClientScopeResource extends CustomResource {

    public static final CustomResourceDefinition DEFINITION = new CustomResourceDefinitionBuilder()
            .withNewSpec()
            .withScope("Namespaced")
            .withGroup("k8s.kiwigrid.com")
            .withVersion("v1beta1")
            .withNewNames()
            .withKind("KeycloakClientScope")
            .withSingular("keycloakclientscope")
            .withPlural("keycloakclientscopes")
            .withShortNames("kccs")
            .endNames()
            .endSpec().build();


    private ClientScopeResource.ClientScopeResourceSpec spec = new ClientScopeResource.ClientScopeResourceSpec();
    private ClientScopeResource.ClientScopeResourceStatus status = new ClientScopeResource.ClientScopeResourceStatus();

    @lombok.Getter
    @lombok.Setter
    @lombok.EqualsAndHashCode(callSuper = false)
    public static class ClientScopeResourceSpec extends CustomResourceDefinitionSpec {
        private String keycloak = "default";
        private String realm;

        private String id;
        private String name;
        private String description;
        private List<ClientScopeResource.ClientScopeMapper> mappers = new ArrayList<>();
        private String protocol;
        protected Map<String, String> attributes;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.EqualsAndHashCode
    public static class ClientScopeMapper {

        private String name;
        private String protocolMapper;
        private Map<String, String> config;
    }

    @lombok.Getter
    @lombok.Setter
    public static class ClientScopeResourceStatus extends CustomResourceDefinitionStatus {
        private String timestamp;
        private String error;
    }

    public static class ClientScopeResourceList extends CustomResourceList<ClientScopeResource> {}

    public static class ClientScopeResourceDoneable extends CustomResourceDoneable<ClientScopeResource> {
        public ClientScopeResourceDoneable(ClientScopeResource resource, Function<ClientScopeResource, ClientScopeResource> function) {
            super(resource, function);
        }
    }
}

package com.kiwigrid.keycloak.controller.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionSpec;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionStatus;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.fabric8.kubernetes.client.CustomResourceList;

@SuppressWarnings("serial")
@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode(of = "spec", callSuper = false)
public class ClientResource extends CustomResource {

	public static final CustomResourceDefinition DEFINITION = new CustomResourceDefinitionBuilder()
			.withNewSpec()
			.withScope("Namespaced")
			.withGroup("k8s.kiwigrid.com")
			.withVersion("v1beta1")
			.withNewNames()
			.withKind("KeycloakClient")
			.withSingular("keycloakclient")
			.withPlural("keycloakclients")
			.withShortNames("kcc")
			.endNames()
			.endSpec().build();

	private ClientResourceSpec spec = new ClientResourceSpec();
	private ClientResourceStatus status = new ClientResourceStatus();

	@lombok.Getter
	@lombok.Setter
	@lombok.EqualsAndHashCode(callSuper = false)
	public static class ClientResourceSpec extends CustomResourceDefinitionSpec {

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

	@lombok.Getter
	@lombok.Setter
	@lombok.EqualsAndHashCode
	public static class ClientMapper {

		private String name;
		private String protocolMapper;
		private Map<String, String> config;
	}

	@lombok.Getter
	@lombok.Setter
	@lombok.EqualsAndHashCode
	public static class ClientRole {

		private String name;
		private List<String> realmRoles;
	}

	@lombok.Getter
	@lombok.Setter
	public static class ClientResourceStatus extends CustomResourceDefinitionStatus {

		private String timestamp;
		private String error;
	}

	public static class ClientResourceList extends CustomResourceList<ClientResource> {}

	public static class ClientResourceDoneable extends CustomResourceDoneable<ClientResource> {
		public ClientResourceDoneable(ClientResource resource, Function<ClientResource, ClientResource> function) {
			super(resource, function);
		}
	}
}
package com.kiwigrid.keycloak.controller.keycloak;

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
public class KeycloakResource extends CustomResource {

	public static final CustomResourceDefinition DEFINITION = new CustomResourceDefinitionBuilder()
			.withNewSpec()
			.withScope("Cluster")
			.withGroup("k8s.kiwigrid.com")
			.withVersion("v1beta1")
			.withNewNames()
			.withKind("Keycloak")
			.withSingular("keycloak")
			.withPlural("keycloaks")
			.withShortNames("kc")
			.endNames()
			.endSpec().build();

	private KeycloakResourceSpec spec = new KeycloakResourceSpec();
	private KeycloakResourceStatus status = new KeycloakResourceStatus();

	@lombok.Getter
	@lombok.Setter
	@lombok.EqualsAndHashCode(callSuper = false)
	public static class KeycloakResourceSpec extends CustomResourceDefinitionSpec {

		private String url;
		private String realm = "master";
		private String clientId = "admin-cli";
		private String username = "admin";
		private String passwordSecretNamespace = "default";
		private String passwordSecretName = "keycloak-http";
		private String passwordSecretKey = "password";
	}

	@lombok.Getter
	@lombok.Setter
	public static class KeycloakResourceStatus extends CustomResourceDefinitionStatus {

		private String timestamp;
		private String version;
		private String error;
	}

	public static class KeycloakResourceList extends CustomResourceList<KeycloakResource> {}

	public static class KeycloakResourceDoneable extends CustomResourceDoneable<KeycloakResource> {
		public KeycloakResourceDoneable(KeycloakResource resource,
				Function<KeycloakResource, KeycloakResource> function) {
			super(resource, function);
		}
	}
}
package com.kiwigrid.keycloak.controller.realm;

import java.util.ArrayList;
import java.util.List;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionSpec;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionStatus;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.fabric8.kubernetes.client.CustomResourceList;

@SuppressWarnings("serial")
@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode(of = "spec", callSuper = false)
public class RealmResource extends CustomResource {

	public static final CustomResourceDefinition DEFINITION = new CustomResourceDefinitionBuilder()
			.withNewSpec()
			.withScope("Cluster")
			.withGroup("k8s.kiwigrid.com")
			.withVersion("v1beta1")
			.withNewNames()
			.withKind("KeycloakRealm")
			.withSingular("keycloakrealm")
			.withPlural("keycloakrealms")
			.withShortNames("kcr")
			.endNames()
			.endSpec().build();

	private RealmResourceSpec spec = new RealmResourceSpec();
	private RealmResourceStatus status = new RealmResourceStatus();

	@lombok.Getter
	@lombok.Setter
	@lombok.EqualsAndHashCode(callSuper = true)
	public static class RealmResourceSpec extends CustomResourceDefinitionSpec {

		private String keycloak = "keycloak";
		private String realm;
		private List<String> roles = new ArrayList<>();
	}

	@lombok.Getter
	@lombok.Setter
	public static class RealmResourceStatus extends CustomResourceDefinitionStatus {

		private String timestamp;
		private String error;
	}

	public static class RealmResourceList extends CustomResourceList<RealmResource> {}

	public static class RealmResourceDoneable extends CustomResourceDoneable<RealmResource> {
		public RealmResourceDoneable(RealmResource resource, Function<RealmResource, RealmResource> function) {
			super(resource, function);
		}
	}
}
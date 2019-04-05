package com.kiwigrid.keycloak.client.controller.k8s;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.NoArgsConstructor
public class KeycloakClientRegistrationCRD extends CustomResourceDefinition {

	public static final String VERSION = "v1beta1";
	public static final String GROUP = "k8s.kiwigrid.com";
	public static final String SCOPE = "CLUSTER";
	public static final String SPEC_KIND = "KeycloakClientRegistrationCRD";
	public static final String SPEC_SINGULAR = "keycloakclientregistration";
	public static final String SPEC_PLURAL = "keycloakclientregistrations";
}

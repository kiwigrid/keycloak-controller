package com.kiwigrid.keycloak.client.controller;

import com.kiwigrid.keycloak.client.controller.k8s.crd.KcrSpec;
import org.keycloak.representations.idm.ClientRepresentation;

public class RegistrarHelper {

	/**
	 * Transform a {@link KcrSpec} to a {@link ClientRepresentation} instance, which is  accepted by Keycloak API.
	 *
	 * @param keycloakClientRegistrationSpec The client controller CRD spec
	 * @return A {@link ClientRepresentation} instance
	 */
	public static ClientRepresentation transformKeycloakClientRegistrationSpec(KcrSpec keycloakClientRegistrationSpec) {
		ClientRepresentation clientRepresentation = new ClientRepresentation();
		clientRepresentation.setClientId(keycloakClientRegistrationSpec.getClientId());
		clientRepresentation.setRedirectUris(keycloakClientRegistrationSpec.getRedirectUris());
		clientRepresentation.setWebOrigins(keycloakClientRegistrationSpec.getWebOrigins());
		clientRepresentation.setDescription("Created via keycloak-client-controller");

		switch (keycloakClientRegistrationSpec.getAccessType()) {
		case "confidential":
			clientRepresentation.setPublicClient(false);
			clientRepresentation.setBearerOnly(false);
			clientRepresentation.setServiceAccountsEnabled(keycloakClientRegistrationSpec.isServiceAccountsEnabled());
			break;
		case "bearer-only":
			clientRepresentation.setPublicClient(false);
			clientRepresentation.setBearerOnly(true);
			break;
		case "public":
			clientRepresentation.setPublicClient(true);
			clientRepresentation.setBearerOnly(false);
		default:
			break;
		}

		return clientRepresentation;
	}
}

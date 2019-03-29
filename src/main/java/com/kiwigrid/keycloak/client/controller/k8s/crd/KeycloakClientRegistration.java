package com.kiwigrid.keycloak.client.controller.k8s.crd;

import io.fabric8.kubernetes.client.CustomResource;

@lombok.Getter
@lombok.Setter
public class KeycloakClientRegistration extends CustomResource {

	private KcrSpec spec;

	@Override
	public String toString() {
		return "KeycloakClientRegistration{" +
				"apiVersion='" + getApiVersion() + '\'' +
				", metadata=" + getMetadata() +
				", spec=" + spec +
				'}';
	}
}

package com.kiwigrid.keycloak.client.controller.k8s.crd;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class KcrDoneable extends CustomResourceDoneable<KeycloakClientRegistration> {
	public KcrDoneable(KeycloakClientRegistration resource, Function function) {
		super(resource, function);
	}
}

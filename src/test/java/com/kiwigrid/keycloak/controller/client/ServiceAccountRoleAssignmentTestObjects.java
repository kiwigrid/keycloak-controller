package com.kiwigrid.keycloak.controller.client;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.representations.idm.RoleRepresentation;

public class ServiceAccountRoleAssignmentTestObjects {

	List<RoleRepresentation> toRoleRepresentations(
			List<String> givenServiceAccountRoleNames)
	{
		return givenServiceAccountRoleNames
				.stream()
				.map(this::toRoleRepresentation)
				.collect(Collectors.toList());
	}

	private RoleRepresentation toRoleRepresentation(String nameName) {
		var keycloakRoleRepresentation = new RoleRepresentation();
		keycloakRoleRepresentation.setName(nameName);
		return keycloakRoleRepresentation;
	}

	com.kiwigrid.keycloak.controller.client.ClientResource createK8sClientResourceWith(List<String> rolesToAssign) {
		var clientResourceSpecification = new com.kiwigrid.keycloak.controller.client.ClientSpec();
		clientResourceSpecification.setServiceAccountRealmRoles(rolesToAssign);

		var clientResource = new com.kiwigrid.keycloak.controller.client.ClientResource();
		clientResource.setSpec(clientResourceSpecification);
		return clientResource;
	}
}

package com.kiwigrid.keycloak.controller.client;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

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
		var clientResourceSpecification = new com.kiwigrid.keycloak.controller.client.ClientResource.ClientResourceSpec();
		clientResourceSpecification.setServiceAccountRealmRoles(rolesToAssign);

		var clientResource = new com.kiwigrid.keycloak.controller.client.ClientResource();
		clientResource.setSpec(clientResourceSpecification);
		return clientResource;
	}
}

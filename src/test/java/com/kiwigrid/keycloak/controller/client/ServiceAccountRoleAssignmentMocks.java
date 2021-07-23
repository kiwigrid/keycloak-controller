package com.kiwigrid.keycloak.controller.client;

import java.util.List;

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

public class ServiceAccountRoleAssignmentMocks {
	org.keycloak.admin.client.resource.ClientResource createKeycloakClientResourceMock() {
		var keycloakClientResource = Mockito.mock(org.keycloak.admin.client.resource.ClientResource.class);
		var userRepresentation = Mockito.mock(UserRepresentation.class);

		given(keycloakClientResource.getServiceAccountUser())
				.willReturn(userRepresentation);
		given(userRepresentation.getId())
				.willReturn("some-keycloak-service-account-user-id");
		given(keycloakClientResource.getServiceAccountUser())
				.willReturn(userRepresentation);

		return keycloakClientResource;
	}

	RealmResource createKeycloakRealmResourceMockWith(
			org.keycloak.admin.client.resource.ClientResource keycloakClientResource,
			List<RoleRepresentation> serviceAccountRolesToAssign,
			List<RoleRepresentation> serviceAccountRealmRoles)
	{
		var keycloakRealmResource = Mockito.mock(RealmResource.class);
		var keycloakClientsResource = Mockito.mock(ClientsResource.class);
		var keycloakUsersResource = Mockito.mock(UsersResource.class);
		var keycloakUserResource = Mockito.mock(UserResource.class);
		var keycloakRolesResource = Mockito.mock(RolesResource.class);
		var keycloakRoleMappingResource = Mockito.mock(RoleMappingResource.class);
		var keycloakRoleMappingsRepresentation = Mockito.mock(MappingsRepresentation.class);
		var keycloakRoleScopeResource = Mockito.mock(RoleScopeResource.class);

		given(keycloakRealmResource.clients())
				.willReturn(keycloakClientsResource);
		given(keycloakClientsResource.get(anyString()))
				.willReturn(keycloakClientResource);
		given(keycloakRealmResource.users())
				.willReturn(keycloakUsersResource);
		given(keycloakUsersResource.get(anyString()))
				.willReturn(keycloakUserResource);
		given(keycloakUserResource.roles())
				.willReturn(keycloakRoleMappingResource);
		given(keycloakRoleMappingResource.getAll())
				.willReturn(keycloakRoleMappingsRepresentation);
		given(keycloakRoleMappingResource.realmLevel())
				.willReturn(keycloakRoleScopeResource);
		given(keycloakRoleMappingsRepresentation.getRealmMappings())
				.willReturn(serviceAccountRolesToAssign);
		given(keycloakRealmResource.roles())
				.willReturn(keycloakRolesResource);
		given(keycloakRolesResource.list())
				.willReturn(serviceAccountRealmRoles);

		doAnswer(invocationOnMock -> {
			RoleRepresentation roleRepresentation = invocationOnMock.getArgument(0);
			return serviceAccountRealmRoles.add(roleRepresentation);
		}).when(keycloakRolesResource).create(any(RoleRepresentation.class));

		doAnswer(invocationOnMock -> {
			List<RoleRepresentation> roleRepresentations = invocationOnMock.getArgument(0);
			return serviceAccountRolesToAssign.addAll(roleRepresentations);
		}).when(keycloakRoleScopeResource).add(anyList());

		return keycloakRealmResource;
	}
}

package com.kiwigrid.keycloak.controller.client;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ServiceAccountRoleAssignmentSynchronizerTest {

	private static final ServiceAccountRoleAssignmentTestObjects testObjects = new ServiceAccountRoleAssignmentTestObjects();
	private static final String CLIENT_UUID = "some-client-uuid";

	@Test
	public void givenAssignedRolesAreNotUnassignedAndCreatedAndReassignedIgnoringCase() {
		// Arrange
		List<String> serviceAccountRoleNamesToAssign = List.of("hero", "CITIZEN");
		com.kiwigrid.keycloak.controller.client.ClientResource k8sClientResourceDefinition = testObjects
				.createK8sClientResourceWith(serviceAccountRoleNamesToAssign);

		var realmResource = mock(RealmResource.class);
		var rolesResource = mock(RolesResource.class);
		when(realmResource.roles()).thenReturn(rolesResource);

		var roleMappingResource = mock(RoleMappingResource.class);
		var roleScopeResource = mock(RoleScopeResource.class);
		when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

		ServiceAccountRoleAssignment serviceAccountRoleAssignment = mock(ServiceAccountRoleAssignment.class);
		when(serviceAccountRoleAssignment.getServiceAccountRoleMappingsFor(realmResource, CLIENT_UUID))
				.thenReturn(roleMappingResource);
		when(serviceAccountRoleAssignment.findAssignedRolesToRemoveWith(realmResource,
				k8sClientResourceDefinition,
				CLIENT_UUID)).thenReturn(Collections.emptyList());
		when(serviceAccountRoleAssignment.findRolesToAssignWith(realmResource,
				k8sClientResourceDefinition,
				CLIENT_UUID)).thenReturn(Collections.emptyList());
		when(serviceAccountRoleAssignment.findRequestedRolesToCreateWith(realmResource,
				k8sClientResourceDefinition)).thenReturn(
				Collections.emptyList());

		ServiceAccountRoleAssignmentSynchronizer synchronizer = new ServiceAccountRoleAssignmentSynchronizer(
				serviceAccountRoleAssignment);

		// Act
		synchronizer.manageServiceAccountRealmRoles(realmResource, k8sClientResourceDefinition, CLIENT_UUID);

		// Assert/Verify
		verifyNoInteractions(rolesResource, roleScopeResource);
	}

	@Test
	public void givenUnassignedRoleToAssignThatDoesNotExistInRealmIsCreatedAndAssignedIgnoringCase() {
		// Arrange
		var serviceAccountRoleAssignment = mock(ServiceAccountRoleAssignment.class);

		var realmResource = mock(RealmResource.class);
		var rolesResource = mock(RolesResource.class);
		when(realmResource.roles()).thenReturn(rolesResource);

		var roleMappingResource = mock(RoleMappingResource.class);
		var roleScopeResource = mock(RoleScopeResource.class);
		when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

		var k8sClientResource = testObjects.createK8sClientResourceWith(List.of("CITIZEN", "HERO"));

		when(serviceAccountRoleAssignment.getServiceAccountRoleMappingsFor(realmResource, CLIENT_UUID))
				.thenReturn(roleMappingResource);
		when(serviceAccountRoleAssignment.findAssignedRolesToRemoveWith(realmResource, k8sClientResource, CLIENT_UUID))
				.thenReturn(Collections.emptyList());

		List<RoleRepresentation> rolesToAdd = testObjects.toRoleRepresentations(List.of("HERO"));
		when(serviceAccountRoleAssignment.findRequestedRolesToCreateWith(realmResource, k8sClientResource))
				.thenReturn(rolesToAdd);
		when(serviceAccountRoleAssignment.findRolesToAssignWith(realmResource, k8sClientResource, CLIENT_UUID))
				.thenReturn(rolesToAdd);

		var synchronizer = new ServiceAccountRoleAssignmentSynchronizer(serviceAccountRoleAssignment);

		// Act
		synchronizer.manageServiceAccountRealmRoles(realmResource, k8sClientResource, CLIENT_UUID);

		// Assert
		InOrder inOrder = Mockito.inOrder(roleScopeResource, rolesResource);

		inOrder.verify(roleScopeResource, times(0)).remove(anyList());
		inOrder.verify(rolesResource).create(rolesToAdd.get(0));
		inOrder.verify(roleScopeResource).add(rolesToAdd);
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	public void givenUnassignedRoleToAssignThatExistsInRealmIsNotCreatedButAssignedIgnoringCase() {
		// Arrange
		ServiceAccountRoleAssignment serviceAccountRoleAssignment = mock(ServiceAccountRoleAssignment.class);
		List<String> serviceAccountRoleNamesToAssign = List.of("CITIZEN", "hero");
		List<RoleRepresentation> serviceAccountRolesToAssign = testObjects
				.toRoleRepresentations(serviceAccountRoleNamesToAssign);
		com.kiwigrid.keycloak.controller.client.ClientResource k8sClientResourceDefinition = testObjects
				.createK8sClientResourceWith(serviceAccountRoleNamesToAssign);

		var realmResource = mock(RealmResource.class);
		var rolesResource = mock(RolesResource.class);
		when(realmResource.roles()).thenReturn(rolesResource);

		var roleMappingResource = mock(RoleMappingResource.class);
		var roleScopeResource = mock(RoleScopeResource.class);
		when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

		when(serviceAccountRoleAssignment.getServiceAccountRoleMappingsFor(realmResource, CLIENT_UUID))
				.thenReturn(roleMappingResource);
		when(serviceAccountRoleAssignment.findRolesToAssignWith(realmResource,
				k8sClientResourceDefinition,
				CLIENT_UUID)).thenReturn(serviceAccountRolesToAssign);
		when(serviceAccountRoleAssignment.findAssignedRolesToRemoveWith(realmResource,
				k8sClientResourceDefinition,
				CLIENT_UUID)).thenReturn(Collections.emptyList());
		when(serviceAccountRoleAssignment.findRequestedRolesToCreateWith(realmResource,
				k8sClientResourceDefinition)).thenReturn(Collections.emptyList());

		ServiceAccountRoleAssignmentSynchronizer synchronizer = new ServiceAccountRoleAssignmentSynchronizer(
				serviceAccountRoleAssignment);

		// Act
		synchronizer.manageServiceAccountRealmRoles(realmResource, k8sClientResourceDefinition, CLIENT_UUID);

		InOrder inOrder = Mockito.inOrder(roleScopeResource, rolesResource);
		inOrder.verify(roleScopeResource, times(0)).remove(anyList());
		inOrder.verify(rolesResource, times(0)).create(any(RoleRepresentation.class));
		inOrder.verify(roleScopeResource).add(serviceAccountRolesToAssign);
		inOrder.verifyNoMoreInteractions();
	}
}

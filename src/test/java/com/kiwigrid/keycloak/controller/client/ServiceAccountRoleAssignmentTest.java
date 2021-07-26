package com.kiwigrid.keycloak.controller.client;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RoleRepresentation;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceAccountRoleAssignmentTest {

	private static final String SOME_CLIENT_UUID = "some-client-uuid";
	private static final ServiceAccountRoleAssignmentTestObjects testObjects = new ServiceAccountRoleAssignmentTestObjects();
	private static final ServiceAccountRoleAssignmentMocks mocks = new ServiceAccountRoleAssignmentMocks();

	@Test
	public void noRoleFoundForRemovalWhenLowerCaseRoleNamesEqualGivenUpperCaseRoleNames() {
		// Arrange
		List<RoleRepresentation> assignedServiceAccountRoleMappings = testObjects.toRoleRepresentations(List
				.of("hero", "criminal"));
		List<RoleRepresentation> serviceAccountRealmRoleMappings = testObjects.toRoleRepresentations(List.of("hero",
				"criminal",
				"citizen"));
		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = mocks.createKeycloakClientResourceMock();
		RealmResource realmResource = mocks.createKeycloakRealmResourceMockWith(keycloakClientResource,
				assignedServiceAccountRoleMappings,
				serviceAccountRealmRoleMappings);

		ClientResource k8sClientResourceDefinition = testObjects.createK8sClientResourceWith(List.of("HERO",
				"CRIMINAL"));

		var serviceAccountRoles = new ServiceAccountRoleAssignment();

		// Act
		List<RoleRepresentation> actualAssignedRolesToRemove = serviceAccountRoles.findAssignedRolesToRemoveWith(
				realmResource,
				k8sClientResourceDefinition,
				SOME_CLIENT_UUID);

		// Assert
		assertThat(actualAssignedRolesToRemove).isEmpty();
	}

	@Test
	public void noRoleFoundForRemovalWhenUpperCaseRoleNamesEqualGivenLowerCaseRoleNames() {
		// Arrange
		List<RoleRepresentation> assignedServiceAccountRoleMappings = testObjects.toRoleRepresentations(List
				.of("HERO", "CRIMINAL"));
		List<RoleRepresentation> serviceAccountRealmRoleMappings = testObjects.toRoleRepresentations(List.of(
				"HERO",
				"CRIMINAL",
				"CITIZEN"));
		org.keycloak.admin.client.resource.ClientResource keycloakClientSourceResource = mocks.createKeycloakClientResourceMock();
		RealmResource realmResource = mocks.createKeycloakRealmResourceMockWith(keycloakClientSourceResource,
				assignedServiceAccountRoleMappings,
				serviceAccountRealmRoleMappings);

		ClientResource k8sClientResourceDefinition = testObjects.createK8sClientResourceWith(List.of("hero",
				"criminal"));

		var serviceAccountRoles = new ServiceAccountRoleAssignment();

		// Act
		List<RoleRepresentation> actualAssignedRolesToRemove = serviceAccountRoles.findAssignedRolesToRemoveWith(
				realmResource,
				k8sClientResourceDefinition,
				SOME_CLIENT_UUID);

		// Assert
		assertThat(actualAssignedRolesToRemove).isEmpty();
	}

	@Test
	public void rolesWithLowerCaseNamesAreFoundForRemovalWhenGivenWithUpperCaseNames() {
		// Arrange
		List<RoleRepresentation> assignedServiceAccountRoleMappings = testObjects.toRoleRepresentations(List
				.of("hero", "criminal"));
		List<RoleRepresentation> serviceAccountRealmRoleMappings = testObjects.toRoleRepresentations(List.of(
				"hero",
				"criminal"));
		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = mocks.createKeycloakClientResourceMock();
		RealmResource keycloakRealmResource = mocks.createKeycloakRealmResourceMockWith(keycloakClientResource,
				assignedServiceAccountRoleMappings,
				serviceAccountRealmRoleMappings);

		ClientResource k8sClientResourceDefinition = testObjects.createK8sClientResourceWith(List.of("HERO"));
		var serviceAccountRoles = new ServiceAccountRoleAssignment();

		var expectedAssignedServiceAccountRoleToRemove = new RoleRepresentation();
		expectedAssignedServiceAccountRoleToRemove.setName("criminal");

		// Act
		List<RoleRepresentation> actualServiceAccountRoleMappingsToRemove = serviceAccountRoles.findAssignedRolesToRemoveWith(
				keycloakRealmResource,
				k8sClientResourceDefinition,
				SOME_CLIENT_UUID);

		// Assert
		assertThat(actualServiceAccountRoleMappingsToRemove)
				.usingRecursiveFieldByFieldElementComparator()
				.containsExactly(expectedAssignedServiceAccountRoleToRemove);
	}

	@Test
	public void rolesWithUpperCaseNamesAreFoundForRemovalWhenGivenWithLowerCaseName() {
		// Arrange
		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = mocks.createKeycloakClientResourceMock();
		List<RoleRepresentation> assignedServiceAccountRoleMappings = testObjects.toRoleRepresentations(List
				.of("HERO", "CRIMINAL"));
		List<RoleRepresentation> serviceAccountRealmRoleMappings = testObjects.toRoleRepresentations(List.of(
				"HERO",
				"CRIMINAL",
				"CITIZEN"));
		RealmResource keycloakRealmResource = mocks.createKeycloakRealmResourceMockWith(keycloakClientResource,
				assignedServiceAccountRoleMappings,
				serviceAccountRealmRoleMappings);

		ClientResource k8sClientResourceDefinition = testObjects.createK8sClientResourceWith(List.of("hero"));
		var expectedServiceAccountRoleRepresentationToRemove = new RoleRepresentation();
		expectedServiceAccountRoleRepresentationToRemove.setName("CRIMINAL");
		var serviceAccountRoles = new ServiceAccountRoleAssignment();

		// Act
		List<RoleRepresentation> actualAssignedServiceAccountRolesToRemove = serviceAccountRoles.findAssignedRolesToRemoveWith(
				keycloakRealmResource,
				k8sClientResourceDefinition,
				SOME_CLIENT_UUID);

		// Assert
		assertThat(actualAssignedServiceAccountRolesToRemove)
				.usingRecursiveFieldByFieldElementComparator()
				.containsExactly(expectedServiceAccountRoleRepresentationToRemove);
	}

	@Test
	public void rolesWithLowerCaseNamesAreFoundToAssignWhenGivenWithUpperCaseName() {
		// Arrange
		List<RoleRepresentation> assignedServiceAccountRoleMappings = testObjects.toRoleRepresentations(List
				.of("citizen"));
		List<RoleRepresentation> serviceAccountRealmRoleMappings = testObjects.toRoleRepresentations(List.of(
				"hero",
				"citizen",
				"criminal"));
		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = mocks.createKeycloakClientResourceMock();
		RealmResource keycloakRealmResource = mocks.createKeycloakRealmResourceMockWith(keycloakClientResource,
				assignedServiceAccountRoleMappings,
				serviceAccountRealmRoleMappings);

		ClientResource k8sClientResourceDefinition = testObjects.createK8sClientResourceWith(List.of("CITIZEN",
				"HERO"));
		var expectedServiceAccountRoleToAssign = new RoleRepresentation();
		expectedServiceAccountRoleToAssign.setName("hero");

		var serviceAccountRoles = new ServiceAccountRoleAssignment();

		// Act
		List<RoleRepresentation> actualServiceAccountRoleMappingsToAssign = serviceAccountRoles.findRolesToAssignWith(
				keycloakRealmResource,
				k8sClientResourceDefinition,
				SOME_CLIENT_UUID);

		// Assert
		assertThat(actualServiceAccountRoleMappingsToAssign)
				.usingRecursiveFieldByFieldElementComparator()
				.containsExactly(expectedServiceAccountRoleToAssign);
	}

	@Test
	public void rolesWithUpperCaseNamesAreFoundToAssignWhenGivenWithLowerCaseNames() {
		// Arrange
		List<RoleRepresentation> assignedServiceAccountRoleMappings = testObjects.toRoleRepresentations(List
				.of("CITIZEN"));
		List<RoleRepresentation> serviceAccountRealmRoleMappings = testObjects.toRoleRepresentations(List.of(
				"HERO",
				"CITIZEN",
				"CRIMINAL"));
		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = mocks.createKeycloakClientResourceMock();
		RealmResource keycloakRealmResource = mocks.createKeycloakRealmResourceMockWith(keycloakClientResource,
				assignedServiceAccountRoleMappings,
				serviceAccountRealmRoleMappings);

		ClientResource k8sClientResourceDefinition = testObjects.createK8sClientResourceWith(List.of("citizen",
				"hero"));
		var expectedServiceAccountRoleToAssign = new RoleRepresentation();
		expectedServiceAccountRoleToAssign.setName("HERO");
		var serviceAccountRoles = new ServiceAccountRoleAssignment();

		// Act
		List<RoleRepresentation> actualServiceAccountRoleMappingsToAssign = serviceAccountRoles.findRolesToAssignWith(
				keycloakRealmResource,
				k8sClientResourceDefinition,
				SOME_CLIENT_UUID);

		// Assert
		assertThat(actualServiceAccountRoleMappingsToAssign)
				.usingRecursiveFieldByFieldElementComparator()
				.containsExactly(expectedServiceAccountRoleToAssign);
	}

	@Test
	public void noRolesFoundToAssignWhenNoRoleExistWithGivenName() {
		List<RoleRepresentation> serviceAccountRealmRolesMappings = testObjects.toRoleRepresentations(List.of(
				"HERO",
				"CITIZEN"));
		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = mocks.createKeycloakClientResourceMock();
		RealmResource keycloakRealmResource = mocks.createKeycloakRealmResourceMockWith(keycloakClientResource,
				Collections.emptyList(),
				serviceAccountRealmRolesMappings);

		ClientResource k8sClientResourceDefinition = testObjects.createK8sClientResourceWith(List.of("CRIMINAL"));

		var serviceAccountRoles = new ServiceAccountRoleAssignment();

		// Act
		List<RoleRepresentation> actualServiceAccountRoleMappingsToAssign = serviceAccountRoles.findRolesToAssignWith(
				keycloakRealmResource,
				k8sClientResourceDefinition,
				SOME_CLIENT_UUID);

		// Assert
		assertThat(actualServiceAccountRoleMappingsToAssign)
				.isEmpty();
	}

	@Test
	public void rolesWithLowerCaseNameAreNotFoundForCreationWhenAlreadyExistentWithUpperCaseNames() {
		// Arrange
		List<RoleRepresentation> serviceAccountRealmRoleMappings = testObjects.toRoleRepresentations(List.of(
				"CITIZEN",
				"HERO"));
		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = mocks.createKeycloakClientResourceMock();
		RealmResource keycloakRealmResource = mocks.createKeycloakRealmResourceMockWith(keycloakClientResource,
				Collections.emptyList(),
				serviceAccountRealmRoleMappings);

		ClientResource k8sClientResourceDefinition = testObjects.createK8sClientResourceWith(List.of("hero",
				"citizen"));

		var serviceAccountRoles = new ServiceAccountRoleAssignment();

		// Act
		List<RoleRepresentation> actualServiceAccountRealmRolesMappingsToCreate = serviceAccountRoles.findRequestedRolesToCreateWith(
				keycloakRealmResource,
				k8sClientResourceDefinition);

		// Assert
		assertThat(actualServiceAccountRealmRolesMappingsToCreate).isEmpty();
	}

	@Test
	public void rolesWithUpperCaseNameAreNotFoundForCreationWhenAlreadyExistentWithLowerCaseNames() {
		List<RoleRepresentation> serviceAccountRealmRoleMappings = testObjects.toRoleRepresentations(List.of(
				"citizen",
				"hero"));

		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = mocks.createKeycloakClientResourceMock();
		RealmResource keycloakRealmResource = mocks.createKeycloakRealmResourceMockWith(keycloakClientResource,
				Collections.emptyList(),
				serviceAccountRealmRoleMappings);

		ClientResource k8sClientResourceDefinition = testObjects.createK8sClientResourceWith(List.of("CITIZEN",
				"HERO"));
		var serviceAccountRoles = new ServiceAccountRoleAssignment();

		// Act
		List<RoleRepresentation> actualServiceAccountRealmRoleMappinsToCreate = serviceAccountRoles.findRequestedRolesToCreateWith(
				keycloakRealmResource,
				k8sClientResourceDefinition);

		// Assert
		assertThat(actualServiceAccountRealmRoleMappinsToCreate).isEmpty();
	}

	@Test
	public void rolesGivenWithNamesAreFoundToBeCreatedWhenMissingInRealm() {
		// Arrange
		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = mocks.createKeycloakClientResourceMock();
		RealmResource keycloakRealmResource = mocks.createKeycloakRealmResourceMockWith(keycloakClientResource,
				Collections.emptyList(),
				Collections.emptyList());

		List<String> serviceAccountRoleNames = List.of("HERO", "citizen", "CriMInaL");
		ClientResource k8sClientResourceDefinition = testObjects.createK8sClientResourceWith(serviceAccountRoleNames);

		var serviceAccountRoles = new ServiceAccountRoleAssignment();
		List<RoleRepresentation> expectedServiceAccountRolesToCreate = serviceAccountRoleNames.stream()
				.map(serviceAccountRoles::createRoleRepresentation)
				.collect(Collectors.toList());
		// Act
		List<RoleRepresentation> actualServiceAccountRealmRolesToCreate = serviceAccountRoles.findRequestedRolesToCreateWith(
				keycloakRealmResource,
				k8sClientResourceDefinition);

		// Assert
		assertThat(actualServiceAccountRealmRolesToCreate)
				.usingRecursiveFieldByFieldElementComparator()
				.containsExactlyElementsOf(expectedServiceAccountRolesToCreate);
	}

	@Test
	public void noRoleIsFoundForCreationWhenGivenRolesAreAssigned() {
		List<String> serviceAccountRealmRoleNames = List.of("HERO", "CITIZEN", "police_president");
		List<RoleRepresentation> serviceAccountRealmRoleMappings = testObjects.toRoleRepresentations(
				serviceAccountRealmRoleNames);

		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = mocks.createKeycloakClientResourceMock();
		RealmResource keycloakRealmResource = mocks.createKeycloakRealmResourceMockWith(keycloakClientResource,
				serviceAccountRealmRoleMappings,
				serviceAccountRealmRoleMappings);

		ClientResource k8sClientResourceDefinition = testObjects.createK8sClientResourceWith(List.of("HEro",
				"citizen",
				"POLICE_president"));
		var serviceAccountRoles = new ServiceAccountRoleAssignment();

		// Act
		List<RoleRepresentation> actualServiceAccountRoleMappingsToCreate = serviceAccountRoles.findRequestedRolesToCreateWith(
				keycloakRealmResource, k8sClientResourceDefinition);

		// Assert
		assertThat(actualServiceAccountRoleMappingsToCreate).isEmpty();
	}
}
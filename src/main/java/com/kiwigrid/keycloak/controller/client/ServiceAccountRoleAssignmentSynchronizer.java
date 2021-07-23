package com.kiwigrid.keycloak.controller.client;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ServiceAccountRoleAssignmentSynchronizer {
	private final Logger LOG = LoggerFactory.getLogger(getClass());
	private final ServiceAccountRoleAssignment serviceAccountRoleAssignment;

	public ServiceAccountRoleAssignmentSynchronizer(ServiceAccountRoleAssignment serviceAccountRoleAssignment) {
		this.serviceAccountRoleAssignment = serviceAccountRoleAssignment;
	}

	public void manageServiceAccountRealmRoles(RealmResource realmResource, ClientResource clientResourceDefinition, String clientUuid) {
		var keycloak = clientResourceDefinition.getSpec().getKeycloak();
		var realm = clientResourceDefinition.getSpec().getRealm();
		var clientId = clientResourceDefinition.getSpec().getClientId();

		RoleMappingResource serviceAccountRoleMappings = serviceAccountRoleAssignment.getServiceAccountRoleMappingsFor(
				realmResource,
				clientUuid);

		List<RoleRepresentation> assignedServiceAccountRolesToRemove = serviceAccountRoleAssignment
				.findAssignedRolesToRemoveWith(
						realmResource,
						clientResourceDefinition,
						clientUuid);

		if (!assignedServiceAccountRolesToRemove.isEmpty()) {
			removeAssignedRolesFromServiceAccount(keycloak,
					realm,
					clientId,
					serviceAccountRoleMappings,
					assignedServiceAccountRolesToRemove);
		}

		List<RoleRepresentation> serviceAccountRealmRolesToCreate = serviceAccountRoleAssignment
				.findRequestedRolesToCreateWith(
						realmResource,
						clientResourceDefinition);

		if (!serviceAccountRealmRolesToCreate.isEmpty()) {
			createNewRealmRoles(realmResource, keycloak, realm, clientId, serviceAccountRealmRolesToCreate);
		}

		List<RoleRepresentation> serviceAccountRolesToAssign = serviceAccountRoleAssignment
				.findRolesToAssignWith(
						realmResource,
						clientResourceDefinition,
						clientUuid);

		if (!serviceAccountRolesToAssign.isEmpty()) {
			assignRequestedRolesToServiceAccount(keycloak,
					realm,
					clientId,
					serviceAccountRoleMappings,
					serviceAccountRolesToAssign);
		}
	}

	private void removeAssignedRolesFromServiceAccount(String keycloak, String realm, String clientId, RoleMappingResource serviceAccountRoleMappings, List<RoleRepresentation> assignedServiceAccountRolesToRemove) {
		serviceAccountRoleMappings.realmLevel().remove(assignedServiceAccountRolesToRemove);
		LOG.info("{}/{}/{}: deleted roles not requested anymore {}",
				keycloak,
				realm,
				clientId,
				assignedServiceAccountRolesToRemove.stream()
						.map(RoleRepresentation::getName)
						.collect(Collectors.toList()));
	}

	private void createNewRealmRoles(RealmResource realmResource, String keycloak, String realm, String clientId, List<RoleRepresentation> serviceAccountRealmRolesToCreate) {
		serviceAccountRealmRolesToCreate.stream()
				.forEach(roleRepresentation -> realmResource.roles().create(roleRepresentation));
		LOG.info("{}/{}/{}: created realm roles {}",
				keycloak,
				realm,
				clientId,
				serviceAccountRealmRolesToCreate.stream()
						.map(RoleRepresentation::getName)
						.collect(Collectors.toList()));
	}

	private void assignRequestedRolesToServiceAccount(String keycloak, String realm, String clientId, RoleMappingResource serviceAccountRoleMappings, List<RoleRepresentation> serviceAccountRolesToAssign) {
		serviceAccountRoleMappings.realmLevel().add(serviceAccountRolesToAssign);
		LOG.info("{}/{}/{}: assigned realm roles {}",
				keycloak,
				realm,
				clientId,
				serviceAccountRolesToAssign.stream().map(RoleRepresentation::getName).collect(Collectors.toList()));
	}
}

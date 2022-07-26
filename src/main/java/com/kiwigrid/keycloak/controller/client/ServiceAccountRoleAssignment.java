package com.kiwigrid.keycloak.controller.client;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.representations.idm.RoleRepresentation;

@Singleton
public class ServiceAccountRoleAssignment {
	public List<RoleRepresentation> findAssignedRolesToRemoveWith(RealmResource realmResource,
			ClientResource clientResource, String clientUuid)
	{
		List<RoleRepresentation> assignedServiceAccountRoles = getAssignedServiceAccountRoles(
				realmResource,
				clientUuid);
		List<String> requestedServiceAccountRoleNames = getRequestedServiceAccountRoleNamesFrom(
				clientResource);

		return assignedServiceAccountRoles
				.stream()
				.filter(roleRepresentation -> !requestedServiceAccountRoleNames
						.stream()
						.anyMatch(roleName -> roleName.equalsIgnoreCase(roleRepresentation.getName())))
				.collect(Collectors.toList());
	}

	private List<String> getRequestedServiceAccountRoleNamesFrom(ClientResource clientResourceDefinition) {
		return clientResourceDefinition
				.getSpec()
				.getServiceAccountRealmRoles();
	}

	private List<RoleRepresentation> getAssignedServiceAccountRoles(RealmResource realmResource, String clientUuid) {
		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = realmResource.clients()
				.get(clientUuid);
		return realmResource.users()
				.get(keycloakClientResource.getServiceAccountUser().getId())
				.roles().getAll().getRealmMappings();
	}

	public List<RoleRepresentation> findRolesToAssignWith(RealmResource realmResource,
			ClientResource clientResourceDefinition, String clientUuid)
	{
		List<RoleRepresentation> assignedServiceAccountRoles = getAssignedServiceAccountRoles(
				realmResource,
				clientUuid);
		List<String> requestedServiceAccountRoleNames = getRequestedServiceAccountRoleNamesFrom(
				clientResourceDefinition);
		List<RoleRepresentation> serviceAccountRealmRoles = realmResource.roles().list();

		var unassignedRequestedRoleNames = requestedServiceAccountRoleNames.stream()
				.filter(roleName -> !assignedServiceAccountRoles.stream()
						.anyMatch(roleRepresentation -> roleName.equalsIgnoreCase(roleRepresentation.getName())))
				.collect(Collectors.toList());

		return serviceAccountRealmRoles.stream()
				.filter(roleRepresentation -> unassignedRequestedRoleNames.stream()
						.anyMatch(roleName -> roleRepresentation.getName().equalsIgnoreCase(roleName)))
				.collect(Collectors.toList());
	}

	public List<RoleRepresentation> findRequestedRolesToCreateWith(RealmResource realmResource,
			ClientResource clientResourceDefinition)
	{
		List<RoleRepresentation> serviceAccountRealmRoleMappings = realmResource.roles().list();

		return getRequestedServiceAccountRoleNamesFrom(clientResourceDefinition)
				.stream()
				.filter(roleName -> !serviceAccountRealmRoleMappings
						.stream()
						.anyMatch(roleRepresentation -> roleRepresentation
								.getName()
								.equalsIgnoreCase(roleName)))
				.map(this::createRoleRepresentation)
				.collect(Collectors.toList());
	}

	RoleRepresentation createRoleRepresentation(String roleName) {
		var roleRepresentation = new RoleRepresentation();
		roleRepresentation.setName(roleName);
		roleRepresentation.setClientRole(false);
		roleRepresentation.setComposite(false);
		return roleRepresentation;
	}

	public RoleMappingResource getServiceAccountRoleMappingsFor(RealmResource realmResource, String clientUuid) {
		return realmResource.users()
				.get(realmResource
						.clients()
						.get(clientUuid)
						.getServiceAccountUser()
						.getId()).roles();
	}
}

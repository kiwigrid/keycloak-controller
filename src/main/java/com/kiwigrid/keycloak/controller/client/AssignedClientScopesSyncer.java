package com.kiwigrid.keycloak.controller.client;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Singleton;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AssignedClientScopesSyncer {
	private final Logger log = LoggerFactory.getLogger(getClass());

	public void manageClientScopes(RealmResource realmResource, String clientUuid, com.kiwigrid.keycloak.controller.client.ClientResource clientResource) {
		var keycloak = clientResource.getSpec().getKeycloak();
		var realm = clientResource.getSpec().getRealm();
		var clientId = clientResource.getSpec().getClientId();

		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = realmResource.clients().get(clientUuid);
		List<String> existingDefaultClientScopeNames = keycloakClientResource.toRepresentation().getDefaultClientScopes();
		List<String> existingOptionalClientScopeNames = keycloakClientResource.toRepresentation()
				.getOptionalClientScopes();

		List<String> requestedDefaultClientScopes = clientResource.getSpec().getDefaultClientScopes().stream()
				.map(String::toLowerCase)
				.collect(Collectors.toList());
		List<String> requestedOptionalClientScopes = clientResource.getSpec().getOptionalClientScopes().stream()
				.map(String::toLowerCase)
				.collect(Collectors.toList());

		// add new
		getClientScopesForName(realmResource, requestedDefaultClientScopes)
				.filter(cs -> !existingDefaultClientScopeNames.contains(cs.getName()))
				.forEach(cs -> {
					keycloakClientResource.addDefaultClientScope(cs.getId());
					log.info("{}/{}/{}: added default client scope {}", keycloak, realm, clientId, cs.getName());
				});
		getClientScopesForName(realmResource, requestedOptionalClientScopes)
				.filter(cs -> !existingOptionalClientScopeNames.contains(cs.getName()))
				.forEach(cs -> {
					keycloakClientResource.addOptionalClientScope(cs.getId());
					log.info("{}/{}/{}: added optional client scope {}", keycloak, realm, clientId, cs.getName());
				});

		// remove obsolete
		keycloakClientResource.getDefaultClientScopes().stream()
				.filter(cs -> !requestedDefaultClientScopes.contains(cs.getName().toLowerCase()))
				.forEach(cs -> {
					keycloakClientResource.removeDefaultClientScope(cs.getId());
					log.info("{}/{}/{}: removed default client scope {}", keycloak, realm, clientId, cs.getName());
				});
		keycloakClientResource.getOptionalClientScopes().stream()
				.filter(cs -> !requestedOptionalClientScopes.contains(cs.getName().toLowerCase()))
				.forEach(cs -> {
					keycloakClientResource.removeOptionalClientScope(cs.getId());
					log.info("{}/{}/{}: removed optional client scope {}", keycloak, realm, clientId, cs.getName());
				});
	}

	private Stream<ClientScopeRepresentation> getClientScopesForName(RealmResource realmResource, List<String> requestedClientScopes) {
		return realmResource.clientScopes()
				.findAll()
				.stream()
				.filter(cs -> requestedClientScopes.contains(cs.getName().toLowerCase()));
	}
}

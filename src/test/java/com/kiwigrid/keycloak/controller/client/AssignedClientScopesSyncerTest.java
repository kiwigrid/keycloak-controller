package com.kiwigrid.keycloak.controller.client;

import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AssignedClientScopesSyncerTest {

	private final AssignedClientScopesSyncer assignedClientScopesSyncer = new AssignedClientScopesSyncer();

	@Test
	public void testNonRequestedClientScopesRemoved() {
		String clientUuid = "clientUuid";
		RealmResource keycloakRealmResource = Mockito.mock(RealmResource.class);
		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = prepareClientResource(
				clientUuid,
				keycloakRealmResource,
				List.of("dcs2", "dcs3"),
				List.of("ocs2", "ocs3"),
				List.of("dcs1", "dcs2", "dcs3", "another-unrelated-dcs", "ocs1", "ocs2", "ocs3", "another-unrelated-ocs"));

		ClientResource kubernetesClientResource = createKubernetesClientResource(
				List.of("dcs1", "dcs2"), List.of("ocs1", "ocs2"));

		assignedClientScopesSyncer.manageClientScopes(keycloakRealmResource, clientUuid, kubernetesClientResource);

		verify(keycloakClientResource).removeDefaultClientScope("dcs3-id");
		verify(keycloakClientResource).removeOptionalClientScope("ocs3-id");
	}

	@Test
	public void testRequestedClientScopesAdded() {
		String clientUuid = "clientUuid";
		RealmResource keycloakRealmResource = Mockito.mock(RealmResource.class);
		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = prepareClientResource(
				clientUuid,
				keycloakRealmResource,
				List.of("dcs2", "dcs3"),
				List.of("ocs2", "ocs3"),
				List.of("dcs1", "dcs2", "dcs3", "another-unrelated-dcs", "ocs1", "ocs2", "ocs3", "another-unrelated-ocs"));

		ClientResource kubernetesClientResource = createKubernetesClientResource(
				List.of("dcs1", "dcs2"), List.of("ocs1", "ocs2"));

		assignedClientScopesSyncer.manageClientScopes(keycloakRealmResource, clientUuid, kubernetesClientResource);

		verify(keycloakClientResource).addDefaultClientScope("dcs1-id");
		verify(keycloakClientResource).addOptionalClientScope("ocs1-id");
	}

	@Test
	public void testNonExistingClientScopeNotAdded() {
		String clientUuid = "clientUuid";
		RealmResource keycloakRealmResource = Mockito.mock(RealmResource.class);
		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = prepareClientResource(
				clientUuid,
				keycloakRealmResource,
				List.of("dcs1", "dcs2", "non-existing-dcs"),
				List.of("ocs1", "ocs2", "non-existing-ocs"),
				List.of("dcs1", "dcs2", "another-unrelated-dcs", "ocs1", "ocs2", "another-unrelated-ocs"));

		ClientResource kubernetesClientResource = createKubernetesClientResource(
				List.of("dcs1", "dcs2"), List.of("ocs1", "ocs2"));

		assignedClientScopesSyncer.manageClientScopes(keycloakRealmResource, clientUuid, kubernetesClientResource);

		verify(keycloakClientResource, times(0)).addDefaultClientScope(anyString());
		verify(keycloakClientResource, times(0)).addOptionalClientScope(anyString());
	}

	@Test
	public void testUnchangedClientScopeNotTouched() {
		String clientUuid = "clientUuid";
		RealmResource keycloakRealmResource = Mockito.mock(RealmResource.class);
		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = prepareClientResource(
				clientUuid,
				keycloakRealmResource,
				List.of("dcs1", "dcs2"),
				List.of("ocs1", "ocs2"),
				List.of("dcs1", "dcs2", "another-unrelated-dcs", "ocs1", "ocs2", "another-unrelated-ocs"));

		ClientResource kubernetesClientResource = createKubernetesClientResource(
				List.of("dcs1", "dcs2"), List.of("ocs1", "ocs2"));

		assignedClientScopesSyncer.manageClientScopes(keycloakRealmResource, clientUuid, kubernetesClientResource);

		verify(keycloakClientResource, times(0)).addDefaultClientScope(anyString());
		verify(keycloakClientResource, times(0)).addOptionalClientScope(anyString());
	}

	@NotNull
	private org.keycloak.admin.client.resource.ClientResource prepareClientResource(String clientUuid, RealmResource realmResource,
		List<String> defaultClientScopes, List<String> optionalClientScopes, List<String> availableClientScopes) {
		ClientRepresentation clientRepresentation = new ClientRepresentation();
		org.keycloak.admin.client.resource.ClientsResource clientsResource = Mockito.mock(org.keycloak.admin.client.resource.ClientsResource.class);
		org.keycloak.admin.client.resource.ClientResource clientResource = Mockito.mock(org.keycloak.admin.client.resource.ClientResource.class);
		org.keycloak.admin.client.resource.ClientScopesResource clientScopesResource = Mockito.mock(org.keycloak.admin.client.resource.ClientScopesResource.class);

		clientRepresentation.setDefaultClientScopes(defaultClientScopes);
		clientRepresentation.setOptionalClientScopes(optionalClientScopes);

		given(clientsResource.get(clientUuid)).willReturn(clientResource);
		given(clientResource.toRepresentation()).willReturn(clientRepresentation);
		given(clientResource.getDefaultClientScopes()).willReturn(defaultClientScopes.stream().map(this::mapToClientRepresentation).collect(Collectors.toList()));
		given(clientResource.getOptionalClientScopes()).willReturn(optionalClientScopes.stream().map(this::mapToClientRepresentation).collect(Collectors.toList()));
		given(realmResource.clients()).willReturn(clientsResource);
		given(realmResource.clientScopes()).willReturn(clientScopesResource);
		given(clientScopesResource.findAll()).willReturn(getClientScopeRepresentations(availableClientScopes));

		return clientResource;
	}

	@NotNull
	private ClientScopeRepresentation mapToClientRepresentation(String cs) {
		ClientScopeRepresentation representation = new ClientScopeRepresentation();
		representation.setName(cs);
		representation.setId(cs + "-id");
		return representation;
	}

	private com.kiwigrid.keycloak.controller.client.ClientResource createKubernetesClientResource(
			List<String> defaultClientScopes, List<String> optionalClientScopes) {
		com.kiwigrid.keycloak.controller.client.ClientResource clientResourceK8s = new com.kiwigrid.keycloak.controller.client.ClientResource();
		clientResourceK8s.setSpec(new com.kiwigrid.keycloak.controller.client.ClientResource.ClientResourceSpec());
		clientResourceK8s.getSpec().setDefaultClientScopes(defaultClientScopes);
		clientResourceK8s.getSpec().setOptionalClientScopes(optionalClientScopes);
		clientResourceK8s.getSpec().setRealm("realm");
		clientResourceK8s.getSpec().setKeycloak("keycloak");
		clientResourceK8s.getSpec().setClientId("clientId");
		return clientResourceK8s;
	}

	private List<ClientScopeRepresentation> getClientScopeRepresentations(List<String> clientScopeNames) {
		return clientScopeNames.stream()
				.map(this::mapToClientRepresentation)
				.collect(Collectors.toList());
	}
}
package com.kiwigrid.keycloak.controller.clientscope;

import com.kiwigrid.keycloak.controller.KubernetesController;
import com.kiwigrid.keycloak.controller.client.ClientResource;
import com.kiwigrid.keycloak.controller.client.ClientType;
import com.kiwigrid.keycloak.controller.keycloak.KeycloakController;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class ClientScopeController extends KubernetesController<ClientScopeResource> {

	final KeycloakController keycloak;

	public ClientScopeController(KeycloakController keycloak, KubernetesClient kubernetes) {
		super(kubernetes, ClientScopeResource.DEFINITION, ClientScopeResource.class, ClientScopeResource.ClientScopeResourceList.class,
				ClientScopeResource.ClientScopeResourceDoneable.class);
		this.keycloak = keycloak;
	}

	@Override
	public void apply(ClientScopeResource clientScopeResource) {

		String keycloak = clientScopeResource.getSpec().getKeycloak();
		String realm = clientScopeResource.getSpec().getRealm();
		String scopeId = clientScopeResource.getSpec().getId();

		// get realm resource

		Optional<RealmResource> optionalRealm = realm(keycloak, realm);
		if (optionalRealm.isEmpty()) {
			log.warn("{}/{}/{}: creating client scope failed because realm was not found", keycloak, realm, scopeId);
			updateStatus(clientScopeResource, "Realm not found");
			return;
		}
		RealmResource realmResource = optionalRealm.get();

		// process client

		try {

			String clientUuid;

			// hand client basics

			Optional<ClientScopeRepresentation> optionalClientScopeRepresentation = realmResource.clientScopes().findAll().stream()
					.filter(clientScopeRepresentation -> clientScopeRepresentation.getId().equals(scopeId))
					.findFirst();


			if (optionalClientScopeRepresentation.isEmpty()) {
				ClientScopeRepresentation clientRepresentation = new ClientScopeRepresentation();
				clientRepresentation.setProtocol("openid-connect");
				clientRepresentation.setId(scopeId);
				map(true, clientScopeResource.getSpec(), clientRepresentation);
				clientUuid = getId(realmResource.clientScopes().create(clientRepresentation));
				log.info("{}/{}/{}: created client scope", keycloak, realm, scopeId);
			} else {
				ClientScopeRepresentation clientRepresentation = optionalClientScopeRepresentation.get();
				clientUuid = clientRepresentation.getId();
				if (map(false, clientScopeResource.getSpec(), clientRepresentation)) {
					realmResource.clientScopes().get(clientUuid).update(clientRepresentation);
					log.info("{}/{}/{}: updated client", keycloak, realm, scopeId);
				}
			}

			manageMapper(realmResource, clientUuid, clientScopeResource);

			updateStatus(clientScopeResource, null);
		} catch (RuntimeException e) {
			String error = e.getClass().getSimpleName() + ": " + e.getMessage();
			if (e instanceof WebApplicationException) {
				Response response = WebApplicationException.class.cast(e).getResponse();
				error = "Keycloak returned " + response.getStatus() + " with: " + response.readEntity(String.class);
			}
			log.error(keycloak + "/" + realm + "/" + scopeId + ": " + error);
			updateStatus(clientScopeResource, error);
		}
	}

	@Override
	public void delete(ClientScopeResource clientResource) {

		String keycloak = clientResource.getSpec().getKeycloak();
		String realm = clientResource.getSpec().getRealm();
		String clientScopeId = clientResource.getSpec().getId();

		// get realm resource

		Optional<RealmResource> optionalRealm = realm(keycloak, realm);
		if (optionalRealm.isEmpty()) {
			log.warn("{}/{}/{}: deleting client scope failed because realm was not found", keycloak, realm, clientScopeId);
			return;
		}
		ClientScopesResource clientResources = optionalRealm.get().clientScopes();

		// delete client

		Optional<ClientScopeRepresentation> clientScopeOptional = clientResources.findAll().stream()
				.filter(clientScopeRepresentation -> clientScopeRepresentation.getId().equals(clientScopeId))
				.findFirst();

		clientScopeOptional.ifPresentOrElse(
				clientScopeRepresentation -> {
					clientResources.get(clientScopeRepresentation.getId()).remove();
					log.info("{}/{}/{}: client deleted", keycloak, realm, clientScopeId);
				},
				() -> log.info("{}/{}/{}: client not found, nothing to delete", keycloak, realm, clientScopeId)
		);

	}

	@Override
	public void retry() {
		customResources.list().getItems().stream()
				.filter(r -> r.getStatus().getError() != null)
				.forEach(this::apply);
	}

	// internal

	void updateStatus(ClientScopeResource clientScopeResource, String error) {

		// skip if nothing changed

		if (clientScopeResource.getStatus().getTimestamp() != null && Objects.equals(clientScopeResource.getStatus().getError(), error)) {
			return;
		}

		// update status

		clientScopeResource.getStatus().setError(error);
		clientScopeResource.getStatus().setTimestamp(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
		customResources.withName(clientScopeResource.getMetadata().getName()).replace(clientScopeResource);
	}

	Optional<RealmResource> realm(String keycloakName, String realmName) {
		return keycloak
				.get(keycloakName)
				.map(k -> k.realm(realmName))
				.filter(realm -> {
					try {
						realm.toRepresentation();
						return true;
					} catch (NotFoundException e) {
						return false;
					}
				});
	}

	boolean map(boolean create, ClientScopeResource.ClientScopeResourceSpec spec, ClientScopeRepresentation client) {
		boolean changed = false;

		if (changed |= changed(create, spec, "name", spec.getName(), client.getName())) {
			client.setName(spec.getName());
		}

		if (changed |= changed(create, spec, "description", spec.getDescription(), client.getDescription())) {
			client.setDescription(spec.getDescription());
		}

		if (changed |= changed(create, spec, "protocol", spec.getProtocol(), client.getProtocol())) {
			client.setProtocol(spec.getProtocol());
		}

		if (changed |= changed(create, spec, "attributes", spec.getAttributes(), client.getAttributes())) {
			client.setAttributes(spec.getAttributes());
		}

		return changed;
	}

	boolean changed(boolean create, ClientScopeResource.ClientScopeResourceSpec spec, String name, Object specValue, Object clientValue) {
		boolean changed = specValue != null && !specValue.equals(clientValue);
		if (changed) {
			if (create) {
				log.debug("{}/{}/{}: set {} to {}",
						spec.getKeycloak(), spec.getRealm(), spec.getId(), name, specValue);
			} else {
				log.info("{}/{}/{}: change {} from {} to {}",
						spec.getKeycloak(), spec.getRealm(), spec.getId(), name, clientValue, specValue);
			}
		}
		return changed;
	}

	void manageMapper(RealmResource realmResource, String clientScopeUuid, ClientScopeResource clientScopeResource) {

		String keycloak = clientScopeResource.getSpec().getKeycloak();
		String realm = clientScopeResource.getSpec().getRealm();
		String clientScopeId = clientScopeResource.getSpec().getId();

		ProtocolMappersResource keycloakResource = realmResource.clientScopes().get(clientScopeUuid).getProtocolMappers();
		List<ProtocolMapperRepresentation> keycloakMappers = keycloakResource.getMappers();
		List<ClientScopeResource.ClientScopeMapper> specMappers = clientScopeResource.getSpec().getMappers();

		// handle requested mappers

		for (ClientScopeResource.ClientScopeMapper specMapper : specMappers) {
			String mapperName = specMapper.getName();
			Optional<ProtocolMapperRepresentation> optional = keycloakMappers.stream().filter(m -> m.getName().equals(mapperName)).findFirst();
			if (optional.isEmpty()) {
				ProtocolMapperRepresentation keycloakMapper = new ProtocolMapperRepresentation();
				keycloakMapper.setName(mapperName);
				keycloakMapper.setProtocol("openid-connect");
				keycloakMapper.setProtocolMapper(specMapper.getProtocolMapper());
				keycloakMapper.setConfig(specMapper.getConfig());
				getId(keycloakResource.createMapper(keycloakMapper));
				log.info("{}/{}/{}: created mapper {}", keycloak, realm, clientScopeId, mapperName);
			} else {
				ProtocolMapperRepresentation keycloakMapper = optional.get();
				if (keycloakMapper.getConfig().equals(specMapper.getConfig())
						&& keycloakMapper.getProtocolMapper().equals(specMapper.getProtocolMapper())) {
					continue;
				}
				keycloakMapper.setProtocolMapper(specMapper.getProtocolMapper());
				keycloakMapper.setConfig(specMapper.getConfig());
				keycloakResource.update(keycloakMapper.getId(), keycloakMapper);
				log.info("{}/{}/{}: updated mapper {}", keycloak, realm, clientScopeId, mapperName);
			}
		}

		// remove obsolete mappers

		Set<String> names = specMappers.stream().map(ClientScopeResource.ClientScopeMapper::getName).collect(Collectors.toSet());
		for (ProtocolMapperRepresentation mapper : keycloakMappers) {
			if (!names.contains(mapper.getName())) {
				keycloakResource.delete(mapper.getId());
				log.info("{}/{}/{}: deleted obsolete mapper {}", keycloak, realm, clientScopeId, mapper.getName());
			}
		}
	}

	String getId(Response response) {
		if (response.getStatus() >= 400) {
			throw new IllegalStateException("Failed to get id from response because status was " + response.getStatus()
					+ " and response: " + response.readEntity(String.class));
		}
		return Stream.of(response.getHeaderString(HttpHeaders.LOCATION).split("/"))
				.filter(p -> p.length() == 36)
				.filter(p -> p.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
				.findAny().get();
	}
}
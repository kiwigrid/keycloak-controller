package com.kiwigrid.keycloak.controller.clientscope;

import com.kiwigrid.keycloak.controller.KubernetesController;
import com.kiwigrid.keycloak.controller.keycloak.KeycloakController;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class ClientScopeController extends KubernetesController<ClientScopeResource> {

	private final KeycloakController keycloak;

	public ClientScopeController(KeycloakController keycloak, KubernetesClient kubernetes) {
		super(kubernetes, ClientScopeResource.class);
		this.keycloak = keycloak;
	}

	@Override
	public void apply(ClientScopeResource clientScopeResource) {

		String keycloak = clientScopeResource.getSpec().getKeycloak();
		String realmName = clientScopeResource.getSpec().getRealm();
		String clientScopeName = clientScopeResource.getSpec().getName();

		Optional<RealmResource> optionalRealm = getRealm(keycloak, realmName);
		if (optionalRealm.isEmpty()) {
			log.warn("{}/{}/{}: creating client scope failed because realm was not found", keycloak, realmName, clientScopeName);
			updateStatus(clientScopeResource, "Realm not found");
			return;
		}
		ClientScopesResource clientScopesResource = optionalRealm.get().clientScopes();
		try {

			Optional<ClientScopeRepresentation> optionalClientScopeRepresentation = getClientScopeFromRealm(clientScopeName, clientScopesResource);

			String clientUuid;
			if (optionalClientScopeRepresentation.isEmpty()) {
				ClientScopeRepresentation clientRepresentation = new ClientScopeRepresentation();
				clientRepresentation.setProtocol("openid-connect");
				clientRepresentation.setName(clientScopeName);
				map(clientScopeResource.getSpec(), clientRepresentation, true);
				clientUuid = getId(clientScopesResource.create(clientRepresentation));
				log.info("{}/{}/{}: created client scope", keycloak, realmName, clientScopeName);
			} else {
				ClientScopeRepresentation clientRepresentation = optionalClientScopeRepresentation.get();
				clientUuid = clientRepresentation.getId();
				if (map(clientScopeResource.getSpec(), clientRepresentation, false)) {
					clientScopesResource.get(clientUuid).update(clientRepresentation);
					log.info("{}/{}/{}: updated client", keycloak, realmName, clientScopeName);
				}
			}

			manageMapper(clientScopesResource, clientUuid, clientScopeResource);
			updateStatus(clientScopeResource, null);
		} catch (RuntimeException e) {
			String error = e.getClass().getSimpleName() + ": " + e.getMessage();
			if (e instanceof WebApplicationException) {
				Response response = WebApplicationException.class.cast(e).getResponse();
				error = "Keycloak returned " + response.getStatus() + " with: " + response.readEntity(String.class);
			}
			log.error(keycloak + "/" + realmName + "/" + clientScopeName + ": " + error);
			updateStatus(clientScopeResource, error);
		}
	}

	void updateStatus(ClientScopeResource clientScopeResource, String error) {
		if (clientScopeResource.getStatus() == null) {
			clientScopeResource.setStatus(new ClientScopeResourceStatus());
		}

		if (isOldErrorStatus(clientScopeResource, error)) {
			return;
		}

		clientScopeResource.getStatus().setError(error);
		clientScopeResource.getStatus().setTimestamp(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());

		kubernetes.resource(clientScopeResource).replace();
	}

	private boolean isOldErrorStatus(ClientScopeResource clientScopeResource, String error) {
		return clientScopeResource.getStatus().getTimestamp() != null && Objects.equals(clientScopeResource.getStatus().getError(), error);
	}

	void manageMapper(ClientScopesResource clientScopesResource, String clientScopeUuid, ClientScopeResource clientScopeResource) {

		String keycloak = clientScopeResource.getSpec().getKeycloak();
		String realm = clientScopeResource.getSpec().getRealm();
		String clientScopeName = clientScopeResource.getSpec().getName();

		ProtocolMappersResource keycloakResource = clientScopesResource.get(clientScopeUuid).getProtocolMappers();
		List<ProtocolMapperRepresentation> keycloakMappers = keycloakResource.getMappers();
		List<ClientScopeMapper> specMappers = clientScopeResource.getSpec().getMappers();

		handleSpecMappers(keycloak, realm, clientScopeName, keycloakResource, keycloakMappers, specMappers);

		removeObsoleteMappers(keycloak, realm, clientScopeName, keycloakResource, keycloakMappers, specMappers);
	}

	private void handleSpecMappers(String keycloak, String realm, String clientScopeName, ProtocolMappersResource keycloakResource, List<ProtocolMapperRepresentation> keycloakMappers, List<ClientScopeMapper> specMappers) {
		for (ClientScopeMapper specMapper : specMappers) {
			String mapperName = specMapper.getName();
			Optional<ProtocolMapperRepresentation> protocolMapperOptional = keycloakMappers.stream().filter(m -> m.getName().equals(mapperName)).findFirst();
			if (protocolMapperOptional.isEmpty()) {
				ProtocolMapperRepresentation keycloakMapper = new ProtocolMapperRepresentation();
				keycloakMapper.setName(mapperName);
				keycloakMapper.setProtocol("openid-connect");
				keycloakMapper.setProtocolMapper(specMapper.getProtocolMapper());
				keycloakMapper.setConfig(specMapper.getConfig());
				getId(keycloakResource.createMapper(keycloakMapper));
				log.info("{}/{}/{}: created mapper {}", keycloak, realm, clientScopeName, mapperName);
			} else {
				ProtocolMapperRepresentation keycloakMapper = protocolMapperOptional.get();
				if (keycloakMapper.getConfig().equals(specMapper.getConfig())
						&& keycloakMapper.getProtocolMapper().equals(specMapper.getProtocolMapper())) {
					continue;
				}
				keycloakMapper.setProtocolMapper(specMapper.getProtocolMapper());
				keycloakMapper.setConfig(specMapper.getConfig());
				keycloakResource.update(keycloakMapper.getId(), keycloakMapper);
				log.info("{}/{}/{}: updated mapper {}", keycloak, realm, clientScopeName, mapperName);
			}
		}
	}

	private void removeObsoleteMappers(String keycloak, String realm, String clientScopeName, ProtocolMappersResource keycloakResource, List<ProtocolMapperRepresentation> keycloakMappers, List<ClientScopeMapper> specMappers) {
		Set<String> names = specMappers.stream().map(ClientScopeMapper::getName).collect(Collectors.toSet());
		for (ProtocolMapperRepresentation mapper : keycloakMappers) {
			if (!names.contains(mapper.getName())) {
				keycloakResource.delete(mapper.getId());
				log.info("{}/{}/{}: deleted obsolete mapper {}", keycloak, realm, clientScopeName, mapper.getName());
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

	@Override
	public void delete(ClientScopeResource clientResource) {
		String keycloak = clientResource.getSpec().getKeycloak();
		String realm = clientResource.getSpec().getRealm();
		String clientScopeName = clientResource.getSpec().getName();

		Optional<RealmResource> optionalRealm = getRealm(keycloak, realm);
		if (optionalRealm.isEmpty()) {
			log.warn("{}/{}/{}: deleting client scope failed because realm was not found", keycloak, realm, clientScopeName);
			return;
		}
		ClientScopesResource clientScopeResources = optionalRealm.get().clientScopes();
		Optional<ClientScopeRepresentation> clientScopeOptional = getClientScopeFromRealm(clientScopeName, clientScopeResources);

		clientScopeOptional.ifPresentOrElse(
				clientScopeRepresentation -> {
					clientScopeResources.get(clientScopeRepresentation.getId()).remove();
					log.info("{}/{}/{}: client deleted", keycloak, realm, clientScopeName);
				},
				() -> log.info("{}/{}/{}: client not found, nothing to delete", keycloak, realm, clientScopeName)
		);

	}

	private Optional<RealmResource> getRealm(String keycloakName, String realmName) {
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

	private Optional<ClientScopeRepresentation> getClientScopeFromRealm(String clientScopeName, ClientScopesResource clientScopesResource) {
		return clientScopesResource.findAll().stream()
				.filter(clientScopeRepresentation -> clientScopeRepresentation.getName().equals(clientScopeName))
				.findFirst();
	}

	@Override
	public void retry() {
		customResources.list().getItems().stream()
				.filter(r -> r.getStatus() != null && r.getStatus().getError() != null)
				.forEach(this::apply);
	}

	boolean map(ClientScopeSpec sourceSpec, ClientScopeRepresentation targetRepresentation, boolean create) {
		boolean changed = false;

		if (changed |= changed(create, sourceSpec, "name", sourceSpec.getName(), targetRepresentation.getName())) {
			targetRepresentation.setName(sourceSpec.getName());
		}

		if (changed |= changed(create, sourceSpec, "description", sourceSpec.getDescription(), targetRepresentation.getDescription())) {
			targetRepresentation.setDescription(sourceSpec.getDescription());
		}

		if (changed |= changed(create, sourceSpec, "protocol", sourceSpec.getProtocol(), targetRepresentation.getProtocol())) {
			targetRepresentation.setProtocol(sourceSpec.getProtocol());
		}

		if (changed |= changed(create, sourceSpec, "attributes", sourceSpec.getAttributes(), targetRepresentation.getAttributes())) {
			targetRepresentation.setAttributes(sourceSpec.getAttributes());
		}

		return changed;
	}

	boolean changed(boolean create, ClientScopeSpec spec, String name, Object specValue, Object clientValue) {
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
}
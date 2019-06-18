package com.kiwigrid.keycloak.client.controller.client;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.kiwigrid.keycloak.client.controller.KubernetesController;
import com.kiwigrid.keycloak.client.controller.client.ClientResource.*;
import com.kiwigrid.keycloak.client.controller.keycloak.KeycloakController;
import io.fabric8.kubernetes.client.KubernetesClient;
import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

@Singleton
public class ClientController extends KubernetesController<ClientResource> {

	final KeycloakController keycloak;

	public ClientController(KeycloakController keycloak, KubernetesClient kubernetes) {
		super(kubernetes, ClientResource.DEFINITION, ClientResource.class, ClientResourceList.class,
				ClientResourceDoneable.class);
		this.keycloak = keycloak;
	}

	@Override
	public void apply(ClientResource clientResource) {

		var keycloak = clientResource.getSpec().getKeycloak();
		var realm = clientResource.getSpec().getRealm();
		var clientId = clientResource.getSpec().getClientId();

		// get realm resource

		var optionalRealm = realm(keycloak, realm);
		if (optionalRealm.isEmpty()) {
			log.warn("{}/{}/{}: creating client failed because realm was not found", keycloak, realm, clientId);
			updateStatus(clientResource, "Realm not found");
			return;
		}
		var realmResource = optionalRealm.get();

		// process client

		try {

			String clientUuid;

			// hand client basics

			var optionalClientRepresentation = realmResource.clients().findByClientId(clientId).stream().findFirst();
			if (optionalClientRepresentation.isEmpty()) {
				var clientRepresentation = new ClientRepresentation();
				clientRepresentation.setProtocol("openid-connect");
				clientRepresentation.setClientId(clientResource.getSpec().getClientId());
				map(true, clientResource.getSpec(), clientRepresentation);
				clientUuid = getId(realmResource.clients().create(clientRepresentation));
				log.info("{}/{}/{}: created client", keycloak, realm, clientId);
			} else {
				var clientRepresentation = optionalClientRepresentation.get();
				clientUuid = clientRepresentation.getId();
				if (map(false, clientResource.getSpec(), clientRepresentation)) {
					realmResource.clients().get(clientUuid).update(clientRepresentation);
					log.info("{}/{}/{}: updated client", keycloak, realm, clientId);
				}
			}

			// handle other resources

			if (clientResource.getSpec().getClientType() == ClientType.CONFIDENTIAL) {
				manageSecret(realmResource, clientUuid, clientResource);
			}
			manageMapper(realmResource, clientUuid, clientResource);
			manageRoles(realmResource, clientUuid, clientResource);

			updateStatus(clientResource, null);
		} catch (RuntimeException e) {
			String error = e.getClass().getSimpleName() + ": " + e.getMessage();
			if (e instanceof WebApplicationException) {
				var response = WebApplicationException.class.cast(e).getResponse();
				error = "Keycloak returned " + response.getStatus() + " with: " + response.readEntity(String.class);
			}
			log.error(keycloak + "/" + realm + "/" + clientId + ": " + error);
			updateStatus(clientResource, error);
		}
	}

	@Override
	public void delete(ClientResource clientResource) {

		var keycloak = clientResource.getSpec().getKeycloak();
		var realm = clientResource.getSpec().getRealm();
		var clientId = clientResource.getSpec().getClientId();

		// get realm resource

		var optionalRealm = realm(keycloak, realm);
		if (optionalRealm.isEmpty()) {
			log.warn("{}/{}/{}: deleting client failed because realm was not found", keycloak, realm, clientId);
			return;
		}
		var clientResources = optionalRealm.get().clients();

		// delete client

		var client = clientResources.findByClientId(clientId).stream().findAny();
		if (client.isPresent()) {
			clientResources.get(client.get().getId()).remove();
			log.info("{}/{}/{}: client deleted", keycloak, realm, clientId);
		} else {
			log.info("{}/{}/{}: client not found, nothing to delete", keycloak, realm, clientId);
		}
	}

	@Override
	public void retry() {
		customResources.list().getItems().stream()
				.filter(r -> r.getStatus().getError() != null)
				.forEach(this::apply);
	}

	// internal

	void updateStatus(ClientResource clientResource, String error) {

		// skip if nothing changed

		if (clientResource.getStatus().getTimestamp() != null && Objects.equals(clientResource.getStatus().getError(), error)) {
			return;
		}

		// update status

		clientResource.getStatus().setError(error);
		clientResource.getStatus().setTimestamp(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
		customResources.withName(clientResource.getMetadata().getName()).replace(clientResource);
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

	boolean map(boolean create, ClientResourceSpec spec, ClientRepresentation client) {
		var changed = false;

		if (changed |= changed(create, spec, "name", spec.getName(), client.getName())) {
			client.setName(spec.getName());
		}

		if (changed |= changed(create, spec, "enabled", spec.getEnabled(), client.isEnabled())) {
			client.setEnabled(spec.getEnabled());
		}

		var publicClient = spec.getClientType() == ClientType.PUBLIC;
		if (changed |= changed(create, spec, "publicClient", publicClient, client.isPublicClient())) {
			client.setPublicClient(publicClient);
		}

		var bearerOnly = spec.getClientType() == ClientType.BEARER;
		if (changed |= changed(create, spec, "bearerOnly", bearerOnly, client.isBearerOnly())) {
			client.setBearerOnly(bearerOnly);
		}

		if (changed |= changed(create, spec, "standardFlowEnabled", spec.getStandardFlowEnabled(),
				client.isStandardFlowEnabled())) {
			client.setStandardFlowEnabled(spec.getStandardFlowEnabled());
		}

		if (changed |= changed(create, spec, "implicitFlowEnabled", spec.getImplicitFlowEnabled(),
				client.isImplicitFlowEnabled())) {
			client.setImplicitFlowEnabled(spec.getImplicitFlowEnabled());
		}

		if (changed |= changed(create, spec, "serviceAccountsEnabled", spec.getServiceAccountsEnabled(),
				client.isServiceAccountsEnabled())) {
			client.setServiceAccountsEnabled(spec.getServiceAccountsEnabled());
		}

		if (changed |= changed(create, spec, "directAccessGrantsEnabled", spec.getDirectAccessGrantsEnabled(),
				client.isDirectAccessGrantsEnabled())) {
			client.setDirectAccessGrantsEnabled(spec.getDirectAccessGrantsEnabled());
		}

		var clientDefaultRoles = client.getDefaultRoles() == null ? List.of() : List.of(client.getDefaultRoles());
		if (changed |= changed(create, spec, "defaultRoles", spec.getDefaultRoles(), clientDefaultRoles)) {
			client.setDefaultRoles(spec.getDefaultRoles().toArray(new String[spec.getDefaultRoles().size()]));
		}

		if (changed |= changed(create, spec, "defaultClientScopes", spec.getDefaultClientScopes(),
				client.getDefaultClientScopes())) {
			client.setDefaultClientScopes(spec.getDefaultClientScopes());
		}

		if (changed |= changed(create, spec, "optionalClientScopes", spec.getOptionalClientScopes(),
				client.getOptionalClientScopes())) {
			client.setOptionalClientScopes(spec.getOptionalClientScopes());
		}

		if (changed |= changed(create, spec, "webOrigins", spec.getWebOrigins(), client.getWebOrigins())) {
			client.setWebOrigins(spec.getWebOrigins());
		}

		return changed;
	}

	boolean changed(boolean create, ClientResourceSpec spec, String name, Object specValue, Object clientValue) {
		boolean changed = specValue != null && !specValue.equals(clientValue);
		if (changed) {
			if (create) {
				log.debug("{}/{}/{}: set {} to {}",
						spec.getKeycloak(), spec.getRealm(), spec.getClientId(), name, specValue);
			} else {
				log.info("{}/{}/{}: change {} from {} to {}",
						spec.getKeycloak(), spec.getRealm(), spec.getClientId(), name, clientValue, specValue);
			}
		}
		return changed;
	}

	void manageSecret(RealmResource realmResource, String clientUuid, ClientResource clientResource) {

		var keycloak = clientResource.getSpec().getKeycloak();
		var realm = clientResource.getSpec().getRealm();
		var clientId = clientResource.getSpec().getClientId();

		var secretNamespace = Optional
				.ofNullable(clientResource.getSpec().getSecretNamespace())
				.orElse(clientResource.getMetadata().getNamespace());
		var secretName = Optional
				.ofNullable(clientResource.getSpec().getSecretName())
				.orElse(clientResource.getSpec().getClientId());
		var secretKey = clientResource.getSpec().getSecretKey();

		// get secret from keycloak / kubernetes

		var keycloakSecretValue = realmResource.clients().get(clientUuid).getSecret().getValue();
		var kubernetesSecretResource = kubernetes.secrets().inNamespace(secretNamespace).withName(secretName);
		var kubernetesSecret = kubernetesSecretResource.get();

		// create secret if not found

		if (kubernetesSecret == null) {
			kubernetesSecretResource.create(kubernetesSecretResource.createNew().withNewMetadata()
					.withNamespace(secretNamespace).withName(secretName).withResourceVersion(null).and()
					.addToData(secretKey, Base64.getEncoder().encodeToString(keycloakSecretValue.getBytes())).done());
			log.info("{}/{}/{}: kubernetes secret {}/{} created",
					keycloak, realm, clientId, secretNamespace, secretName);
			return;
		}

		// get value from secret value from kubernetes

		var kubernetesSecretValueEncoded = kubernetesSecret.getData().get(secretKey);
		if (kubernetesSecretValueEncoded == null) {
			log.warn("{}/{}/{}: kubernetes secret {}/{} has no value for key {}",
					keycloak, realm, clientId, secretNamespace, secretName, secretKey);
			return;
		}
		var kubernetesSecretValue = new String(Base64.getDecoder().decode(kubernetesSecretValueEncoded));
		if (kubernetesSecretValue.equals(keycloakSecretValue)) {
			log.debug("{}/{}/{}: keycloak secret matches kubernetes secret {}/{}",
					keycloak, realm, clientId, secretNamespace, secretName);
			return;
		}

		// update secret in keycloak

		var client = new ClientRepresentation();
		client.setClientId(clientId);
		client.setClientAuthenticatorType("client-secret");
		client.setSecret(kubernetesSecretValue);
		realmResource.clients().get(clientUuid).update(client);
		log.info("{}/{}/{}: updated secret from kubernetes secret {}/{}",
				keycloak, realm, clientId, secretNamespace, secretName);
	}

	void manageMapper(RealmResource realmResource, String clientUuid, ClientResource clientResource) {

		var keycloak = clientResource.getSpec().getKeycloak();
		var realm = clientResource.getSpec().getRealm();
		var clientId = clientResource.getSpec().getClientId();

		var keycloakResource = realmResource.clients().get(clientUuid).getProtocolMappers();
		var keycloakMappers = keycloakResource.getMappers();
		var specMappers = clientResource.getSpec().getMapper();

		// handle requested mappers

		for (var specMapper : specMappers) {
			var mapperName = specMapper.getName();
			var optional = keycloakMappers.stream().filter(m -> m.getName().equals(mapperName)).findFirst();
			if (optional.isEmpty()) {
				var keycloakMapper = new ProtocolMapperRepresentation();
				keycloakMapper.setName(mapperName);
				keycloakMapper.setProtocol("openid-connect");
				keycloakMapper.setProtocolMapper(specMapper.getProtocolMapper());
				keycloakMapper.setConfig(specMapper.getConfig());
				getId(keycloakResource.createMapper(keycloakMapper));
				log.info("{}/{}/{}: created mapper {}", keycloak, realm, clientId, mapperName);
			} else {
				var keycloakMapper = optional.get();
				if (keycloakMapper.getConfig().equals(specMapper.getConfig())
						&& keycloakMapper.getProtocolMapper().equals(specMapper.getProtocolMapper())) {
					continue;
				}
				keycloakMapper.setProtocolMapper(specMapper.getProtocolMapper());
				keycloakMapper.setConfig(specMapper.getConfig());
				keycloakResource.update(keycloakMapper.getId(), keycloakMapper);
				log.info("{}/{}/{}: updated mapper {}", keycloak, realm, clientId, mapperName);
			}
		}

		// remove obsolete mappers

		var names = specMappers.stream().map(ClientMapper::getName).collect(Collectors.toSet());
		for (var mapper : keycloakMappers) {
			if (!names.contains(mapper.getName())) {
				keycloakResource.delete(mapper.getId());
				log.info("{}/{}/{}: deleted obsolete mapper {}", keycloak, realm, clientId, mapper.getName());
			}
		}
	}

	void manageRoles(RealmResource realmResource, String clientUuid, ClientResource clientResource) {

		var keycloak = clientResource.getSpec().getKeycloak();
		var realm = clientResource.getSpec().getRealm();
		var clientId = clientResource.getSpec().getClientId();

		org.keycloak.admin.client.resource.ClientResource keycloakClientResource = realmResource.clients().get(clientUuid);
		var clientRolesResource = keycloakClientResource.roles();
		var clientRoleRepresentations = clientRolesResource.list();
		var specRoles = clientResource.getSpec().getRoles();

		// remove obsolete roles

		var specRoleNames = specRoles.stream().map(ClientRole::getName).collect(Collectors.toSet());
		for (var clientRoleRepresentation : clientRoleRepresentations) {
			if (!specRoleNames.contains(clientRoleRepresentation.getName())) {
				clientRolesResource.deleteRole(clientRoleRepresentation.getName());
				log.info("{}/{}/{}: deleted client role {}",
						keycloak, realm, clientId, clientRoleRepresentation.getName());
			}
		}

		// handle requested roles

		for (var specRole : specRoles) {
			var clientRoleName = specRole.getName();

			var optional = clientRoleRepresentations.stream().filter(r -> r.getName().equals(clientRoleName)).findAny();
			if (optional.isEmpty()) {
				var representation = new RoleRepresentation();
				representation.setName(clientRoleName);
				representation.setClientRole(true);
				representation.setContainerId(clientUuid);
				representation.setComposite(false);
				clientRolesResource.create(representation);
				log.info("{}/{}/{}: created client role {}", keycloak, realm, clientId, clientRoleName);
			}

			var clientRoleRepresentation = optional.isPresent()
					? optional.get()
					: clientRolesResource.get(clientRoleName).toRepresentation();

			for (var realmRoleName : specRole.getRealmRoles()) {
				var composites = realmResource.roles().get(realmRoleName).getClientRoleComposites(clientUuid);
				if (!composites.stream().anyMatch(r -> r.getName().equals(clientRoleName))) {
					realmResource.roles().get(realmRoleName).addComposites(List.of(clientRoleRepresentation));
					log.info("{}/{}/{}: added client role {} to realm role {}",
							keycloak, realm, clientId, clientRoleName, realmRoleName);
				}
			}

			if (clientResource.getSpec().getServiceAccountsEnabled() == Boolean.TRUE) {
				realmResource.users()
						.get(keycloakClientResource.getServiceAccountUser().getId())
						.roles()
						.clientLevel(clientUuid)
						.add(List.of(clientRoleRepresentation));
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
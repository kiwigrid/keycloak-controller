package com.kiwigrid.keycloak.client.controller.keycloak;

import java.util.List;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ServerInfoResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kiwigrid.keycloak.client.controller.exception.ClientConflictException;
import com.kiwigrid.keycloak.client.controller.exception.ClientRegistrationException;
import com.kiwigrid.keycloak.client.controller.exception.RetrieveClientRepresenationException;

import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpStatus;

@Singleton
public class Registrar {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Matches ID of the org.keycloak.protocol.oidc.installatio.KeycloakOIDCClientInstallation
	 * Unfortunately there is no constant provided by keycloak itself
	 */
	public static final String KEYCLOAK_OIDC_PROVIDER_ID = "keycloak-oidc-keycloak-json";

	private final String keycloakUrl;
	private final String adminUser;
	private final String adminPwd;

	public Registrar(RegistrarProperties properties, Environment env) {
		logger.trace("Properties: " + env.getProperties("registrar"));

		this.keycloakUrl = properties.getKeycloakUrl();
		this.adminUser = properties.getKeycloakUser();
		this.adminPwd = properties.getKeycloakPwd();

		logger.trace(keycloakUrl + " " + adminUser + " " + adminPwd);
//		verifyKeycloakConnection();
	}

	public void verifyKeycloakConnection() {
		logger.debug("Verify Keycloak connection...");
		Keycloak keycloak = null;
		try {
			keycloak = getKeycloak();
			ServerInfoResource serverInfoResource = keycloak.serverInfo();
			logger.debug("Keycloak ServerInfo {}", serverInfoResource.getInfo().getSystemInfo().getVersion());
		} catch (Exception e) {
			logger.error("Connection to Keycloak failed", e);
			throw e;
		} finally {
			if (keycloak != null) {
				keycloak.close();
			}
		}
	}

	/**
	 * Register a client in the specified realm at keycloak.
	 *
	 * @param realm The realm where the client will be created.
	 * @param client The client representation.
	 * @return Client adapter configuration
	 * @throws ClientRegistrationException If registration wasn't successful
	 * adapter configuration
	 * @throws ClientConflictException If a client with the same client-id already exists.
	 * @see <a href="https://www.keycloak.org/docs-api/4.8/rest-api/index.html#_clientrepresentation">https://www.keycloak.org/docs-api/4.8/rest-api/index.html#_clientrepresentation</a>
	 */
	public String register(String realm, final ClientRepresentation client)
			throws ClientRegistrationException, ClientConflictException
	{
		final Keycloak keycloak = getKeycloak();
		try (Response response = keycloak.realm(realm).clients().create(client)) {
			Response.StatusType statusInfo = response.getStatusInfo();
			if (HttpStatus.CONFLICT.getCode() == response.getStatus()) {
				throw new ClientConflictException("Client with client-id ["
						+ client.getClientId()
						+ "] already exists.");
			}
			if (HttpStatus.CREATED.getCode() != response.getStatus()) {
				throw new ClientRegistrationException(statusInfo.getStatusCode()
						+ " - "
						+ statusInfo.getReasonPhrase());
			}
		}

		try {
			return retrieveClientAdapterConfiguration(realm, client.getClientId());
		} catch (RetrieveClientRepresenationException e) {
			throw new ClientRegistrationException(e);
		}
	}

	/**
	 * Deletes a client in the specified realm at Keycloak.
	 *
	 * @param realm The realm name
	 * @param clientId clientId of the client (not id) see {@link ClientRepresentation#clientId}
	 * @throws RetrieveClientRepresenationException If client representation could not be found
	 */
	public void deleteClient(String realm, String clientId) throws RetrieveClientRepresenationException {

		ClientRepresentation clientRepresentation = getClientRepresentation(realm, clientId);
		getKeycloak().realm(realm).clients().get(clientRepresentation.getId()).remove();
	}

	private String retrieveClientAdapterConfiguration(String realm, String clientId)
			throws RetrieveClientRepresenationException
	{
		final ClientRepresentation clientRepresentation = getClientRepresentation(realm, clientId);
		String id = clientRepresentation.getId();

		return getKeycloak().realm(realm)
				.clients()
				.get(id)
				.getInstallationProvider(KEYCLOAK_OIDC_PROVIDER_ID);
	}

	private ClientRepresentation getClientRepresentation(String realm, String clientId)
			throws RetrieveClientRepresenationException
	{
		final List<ClientRepresentation> clientRepresentations = getKeycloak().realm(realm)
				.clients()
				.findByClientId(clientId);

		if (clientRepresentations.size() != 1) {
			throw new RetrieveClientRepresenationException("Failed to retrieve client representation for clientId ["
					+ clientId
					+ "]");
		}

		return clientRepresentations.get(0);
	}

	private Keycloak getKeycloak() {
		return KeycloakBuilder.builder()
				.serverUrl(keycloakUrl)
				// User 'admin' exists in master realm, that's why we authenticate there
				.realm("master")
				.clientId("admin-cli")
				.username(adminUser)
				.password(adminPwd)
				.build();
	}
}
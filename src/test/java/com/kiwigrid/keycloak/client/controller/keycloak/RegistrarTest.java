package com.kiwigrid.keycloak.client.controller.keycloak;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.kiwigrid.keycloak.client.controller.exception.ClientConflictException;
import com.kiwigrid.keycloak.client.controller.exception.ClientRegistrationException;
import com.kiwigrid.keycloak.client.controller.exception.RetrieveClientRepresenationException;
import org.hamcrest.CoreMatchers;
import org.junit.*;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.testcontainers.containers.GenericContainer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("WeakerAccess")
public class RegistrarTest {

	public static final String KEYCLOAK_REPO = "jboss/keycloak";
	public static final String KEYCLOAK_TAG = "4.8.3.Final";
	public static final String KEYCLOAK_IMAGE = String.format("%s:%s", KEYCLOAK_REPO, KEYCLOAK_TAG);
	public static final String KEYCLOAK_USER = "admin";
	public static final String KEYCLOAK_PWD = "admin";
	public static final String KEYCLOAK_REALM = "master";
	public static final int KEYCLOAK_PORT = 8080;

	@ClassRule
	public static GenericContainer keycloakContainer = new GenericContainer<>(KEYCLOAK_IMAGE)
			.withEnv("KEYCLOAK_USER", KEYCLOAK_USER)
			.withEnv("KEYCLOAK_PASSWORD", KEYCLOAK_PWD)
			.withExposedPorts(KEYCLOAK_PORT);

	private static RegistrarProperties registrarProperties;
	private static Registrar registrar;

	@BeforeClass
	public static void setup() {
		registrarProperties = new RegistrarProperties();
		registrarProperties.setKeycloakUser(KEYCLOAK_USER);
		registrarProperties.setKeycloakPwd(KEYCLOAK_PWD);
		String authUrl = "http://"
				+ keycloakContainer.getContainerIpAddress()
				+ ":"
				+ keycloakContainer.getFirstMappedPort()
				+ "/auth";
		registrarProperties.setKeycloakUrl(authUrl);

		registrar = new Registrar(registrarProperties);
	}

	@Test
	public void verifyKeycloakConnection() {
		registrar.verifyKeycloakConnection();
	}

	@Test
	public void verifyThatAllConfigurationIsAppliedWhenRegister()
			throws ClientRegistrationException, ClientConflictException
	{
		final List<String> anyUris = Collections.singletonList("*");

		ClientRepresentation client = new ClientRepresentation();
		client.setClientId(UUID.randomUUID().toString());
		client.setWebOrigins(anyUris);
		client.setRedirectUris(anyUris);
		client.setServiceAccountsEnabled(true);

		// Test
		registrar.register(KEYCLOAK_REALM, client);

		// Verify
		Keycloak keycloak = getKeycloak();
		List<ClientRepresentation> clients = keycloak.realm(KEYCLOAK_REALM)
				.clients()
				.findByClientId(client.getClientId());
		assertThat(clients.size(), is(1));
		ClientRepresentation actualClient = clients.get(0);
		assertThat(actualClient.getRedirectUris(), is(anyUris));
		assertThat(actualClient.getWebOrigins(), is(anyUris));
		assertThat(actualClient.isServiceAccountsEnabled(), is(true));
	}

	@Test
	public void registerPublicClient() throws ClientRegistrationException, ClientConflictException {
		ClientRepresentation client = new ClientRepresentation();
		client.setClientId(UUID.randomUUID().toString());
		client.setPublicClient(true);

		// Test
		registrar.register(KEYCLOAK_REALM, client);

		// Verify
		Keycloak keycloak = getKeycloak();
		List<ClientRepresentation> clients = keycloak.realm(KEYCLOAK_REALM)
				.clients()
				.findByClientId(client.getClientId());
		assertThat(clients.size(), is(1));
		ClientRepresentation actualClient = clients.get(0);
		assertThat(actualClient.isPublicClient(), is(true));
	}

	@Test(expected = ClientConflictException.class)
	public void createClientTwiceShouldThrowConflictException() throws ClientRegistrationException, ClientConflictException {
		ClientRepresentation client = new ClientRepresentation();
		client.setClientId(UUID.randomUUID().toString());

		registrar.register(KEYCLOAK_REALM, client);
		// and call again
		registrar.register(KEYCLOAK_REALM, client);
	}

	@Test
	public void deleteClient()
			throws ClientRegistrationException, ClientConflictException, RetrieveClientRepresenationException
	{
		ClientRepresentation client = new ClientRepresentation();
		client.setClientId(UUID.randomUUID().toString());
		client.setPublicClient(true);
		registrar.register(KEYCLOAK_REALM, client);

		// Test
		registrar.deleteClient(KEYCLOAK_REALM, client.getClientId());
		// Verify
		Keycloak keycloak = getKeycloak();
		List<ClientRepresentation> clients = keycloak.realm(KEYCLOAK_REALM)
				.clients()
				.findByClientId(client.getClientId());
		assertThat(clients.size(), is(0));
	}

	@Test(expected = RetrieveClientRepresenationException.class)
	public void deletionOfUnknownClientShouldThrowException() throws RetrieveClientRepresenationException {
		registrar.deleteClient(KEYCLOAK_REALM, "unknown");
	}

	private Keycloak getKeycloak() {
		return KeycloakBuilder.builder()
				.serverUrl(registrarProperties.getKeycloakUrl())
				// User 'admin' exists in master realm, that's why we authenticate there
				.realm(KEYCLOAK_REALM)
				.clientId("admin-cli")
				.username(registrarProperties.getKeycloakUser())
				.password(registrarProperties.getKeycloakPwd())
				.build();
	}
}
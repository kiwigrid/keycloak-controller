package com.kiwigrid.keycloak.controller.keycloak;

import java.net.UnknownHostException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.kiwigrid.keycloak.controller.KubernetesController;
import io.fabric8.kubernetes.client.KubernetesClient;
import javax.inject.Singleton;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import org.apache.http.conn.HttpHostConnectException;
import org.keycloak.admin.client.Keycloak;

@Singleton
public class KeycloakController extends KubernetesController<KeycloakResource> {

	final Map<String, Keycloak> clients = new HashMap<>();

	public KeycloakController(KubernetesClient kubernetes) {
		super(kubernetes, KeycloakResource.class);
	}

	@Override
	public void apply(KeycloakResource resource) {

		var name = resource.getMetadata().getName();
		var status = resource.getStatus();
		if (status == null) {
			status = new KeycloakResourceStatus();
			resource.setStatus(status);
		}

		try {

			var keycloak = connect(resource);
			var version = keycloak.serverInfo().getInfo().getSystemInfo().getVersion();
			clients.put(name, keycloak);
			updateStatus(resource, version, null);
			log.info("Connected to {} in version {}.", name, status.getVersion());

		} catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			delete(resource);
			updateStatus(resource, resource.getStatus().getVersion(), e.getMessage());
			log.warn("Connecting to {} failed: {}", name, status.getError());
		}
	}

	@Override
	public void delete(KeycloakResource resource) {
		var client = clients.remove(resource.getMetadata().getName());
		if (client != null) {
			client.close();
		}
	}

	@Override
	public void retry() {
		customResources.inAnyNamespace().list().getItems().stream()
				.filter(r -> r.getStatus() != null && r.getStatus().getError() != null)
				.forEach(this::apply);
	}

	public Optional<Keycloak> get(String keycloak) {
		if (!clients.containsKey(keycloak)) {
			customResources.inAnyNamespace().list().getItems().stream()
					.filter(r -> r.getMetadata().getName().equals(keycloak))
					.forEach(this::apply);
		}
		return Optional.ofNullable(clients.get(keycloak));
	}

	// internal

	void updateStatus(KeycloakResource resource, String version, String error) {

		if (resource.getStatus() == null) {
			resource.setStatus(new KeycloakResourceStatus());
		}

		// skip if nothing changed

		if (resource.getStatus().getTimestamp() != null
				&& Objects.equals(resource.getStatus().getError(), error)
				&& Objects.equals(resource.getStatus().getVersion(), version)) {
			return;
		}

		// update status (version only if present)

		resource.getStatus().setError(error);
		resource.getStatus().setTimestamp(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
		resource.getStatus().setVersion(version);

		kubernetes.resource(resource).replace();
	}

	Keycloak connect(KeycloakResource resource) {

		// construct keycloak client

		var keycloak = Keycloak.getInstance(
				resource.getSpec().getUrl(),
				resource.getSpec().getRealm(),
				resource.getSpec().getUsername(),
				getPassword(resource),
				resource.getSpec().getClientId());

		// test connection to keycloak

		try {
			keycloak.serverInfo().getInfo().getSystemInfo().getVersion();
		} catch (ProcessingException e) {
			var error = "Unable to connect: " + e.getMessage();
			if (e.getCause() instanceof HttpHostConnectException || e.getCause() instanceof UnknownHostException) {
				error = e.getCause().getMessage();
			}
			if (e.getCause() instanceof WebApplicationException) {
				var response = WebApplicationException.class.cast(e.getCause()).getResponse();
				error = "Keycloak returned " + response.getStatus() + " with: " + response.readEntity(String.class);
			}
			throw new IllegalStateException(error);
		}

		return keycloak;
	}

	String getPassword(KeycloakResource resource) {

		// get names

		var secretNamespace = resource.getSpec().getPasswordSecretNamespace();
		var secretName = resource.getSpec().getPasswordSecretName();
		var secretKey = resource.getSpec().getPasswordSecretKey();

		// get secret

		var secret = kubernetes.secrets().inNamespace(secretNamespace).withName(secretName).get();
		if (secret == null) {
			throw new IllegalStateException("Secret " + secretNamespace + "/" + secretName + " not found.");
		}
		if (!secret.getData().containsKey(secretKey)) {
			throw new IllegalStateException(
					"Secret " + secretNamespace + "/" + secretName + " has no key " + secretKey + ".");
		}

		// parse secret

		return new String(Base64.getDecoder().decode(secret.getData().get(secretKey)));
	}
}
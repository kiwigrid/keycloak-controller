package com.kiwigrid.keycloak.client.controller.k8s;

import java.io.IOException;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiwigrid.keycloak.client.controller.RegistrarHelper;
import com.kiwigrid.keycloak.client.controller.exception.ClientConflictException;
import com.kiwigrid.keycloak.client.controller.exception.ClientRegistrationException;
import com.kiwigrid.keycloak.client.controller.exception.RetrieveClientRepresenationException;
import com.kiwigrid.keycloak.client.controller.k8s.crd.KcrDoneable;
import com.kiwigrid.keycloak.client.controller.k8s.crd.KcrSpec;
import com.kiwigrid.keycloak.client.controller.k8s.crd.KeycloakClientRegistration;
import com.kiwigrid.keycloak.client.controller.k8s.crd.KeycloakClientRegistrationList;
import com.kiwigrid.keycloak.client.controller.keycloak.Registrar;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.micronaut.core.util.StringUtils;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.json.simple.JSONValue;
import org.keycloak.representations.idm.ClientRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

@Singleton
public class CrdWatcher {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Inject
	private Registrar registrar;

	public void doit() throws IOException {

		Yaml yaml = new Yaml();
		Object crdYaml = yaml.load(getClass().getResourceAsStream("/keycloakClientCreationCRD.yaml"));
		ObjectMapper objectMapper = new ObjectMapper();
		KeycloakClientRegistrationCRD crd = objectMapper.readValue(JSONValue.toJSONString(
				crdYaml), KeycloakClientRegistrationCRD.class);

		try (final KubernetesClient client = new DefaultKubernetesClient()) {
			NonNamespaceOperation<KeycloakClientRegistration, KeycloakClientRegistrationList, KcrDoneable, Resource<KeycloakClientRegistration, KcrDoneable>> dummyClient = client
					.customResources(
							crd,
							KeycloakClientRegistration.class,
							KeycloakClientRegistrationList.class,
							KcrDoneable.class);

			String apiVersion = String.format("%s/%s",
					KeycloakClientRegistrationCRD.GROUP,
					KeycloakClientRegistrationCRD.VERSION);
			KubernetesDeserializer.registerCustomKind(apiVersion,
					KeycloakClientRegistration.class.getSimpleName(),
					KeycloakClientRegistration.class);

			dummyClient.watch(new Watcher<>() {
				@Override
				public void eventReceived(Action action, KeycloakClientRegistration resource) {
					logger.info("Event [{}] received [{}]", action.name(), resource.getMetadata().getName());
					switch (action) {
					case ADDED:
						processAddedEvent(resource);
						break;
					case MODIFIED:
						processModifyEvent(resource);
						break;
					case DELETED:
						processDeletedEvent(resource);
						break;
					case ERROR:
					default:
						logger.warn("Error {}");
					}
				}

				@Override
				public void onClose(KubernetesClientException cause) {
					logger.error("Watcher close due to [{}]", cause.getMessage());
				}
			});
		}
	}

	private void processAddedEvent(KeycloakClientRegistration kcr) {
		final KcrSpec kcrSpec = kcr.getSpec();
		final ClientRepresentation clientRepresentation = RegistrarHelper.transformKeycloakClientRegistrationSpec(
				kcrSpec);

		try {
			String clientConfig = registrar.register(kcrSpec.getRealm(), clientRepresentation);
			logger.info("Client [{}] in realm [{}] created", kcrSpec.getClientId(), kcrSpec.getRealm());
			if ("confidential".equals(kcrSpec.getAccessType())) {
				JsonNode jsonNode = new ObjectMapper().readTree(clientConfig);
				String clientSecret = jsonNode.get("credentials").get("secret").textValue();
				storeClientSecret(kcr, clientSecret);
			}
		} catch (ClientRegistrationException | ClientConflictException e) {
			logger.error("Failed to create client [{}] in realm [{}]. Cause: {}",
					kcrSpec.getClientId(),
					kcrSpec.getClientId(),
					e.getMessage());
		} catch (IOException e) {
			logger.error("Failed to read client secret from Keycloak ClientConfiguration", e);
		}
	}

	private void processModifyEvent(KeycloakClientRegistration resource) {

	}

	private void processDeletedEvent(KeycloakClientRegistration resource) {
		KcrSpec kcrSpec = resource.getSpec();
		try {
			registrar.deleteClient(kcrSpec.getRealm(), kcrSpec.getClientId());
			deleteSecret(resource);
			logger.info("Client [{}] in realm [{}] deleted", kcrSpec.getClientId(), kcrSpec.getRealm());
		} catch (RetrieveClientRepresenationException e) {
			logger.error("Failed to delete client [{}] in realm [{}]. Cause: {}",
					kcrSpec.getClientId(),
					kcrSpec.getClientId(),
					e.getMessage());
		}
	}

	private void storeClientSecret(KeycloakClientRegistration resource, String clientSecret) {
		try (final KubernetesClient client = new DefaultKubernetesClient()) {
			String name = resource.getMetadata().getName() + "-secret";
			String namespace = StringUtils.isEmpty(resource.getMetadata().getNamespace()) ?
					"default" :
					resource.getMetadata().getNamespace();
			String encodedClientSecret = Base64.getEncoder().encodeToString(clientSecret.getBytes());
			Secret secret = new SecretBuilder()
					.withNewMetadata().withName(name).endMetadata()
					.addToData("clientSecret", encodedClientSecret)
					.build();
			client.secrets().inNamespace(namespace).create(secret);
			logger.info("Secret {} created", name);
		}
	}

	private void deleteSecret(KeycloakClientRegistration resource) {
		try (final KubernetesClient client = new DefaultKubernetesClient()) {
			String name = resource.getMetadata().getName() + "-secret";
			String namespace = StringUtils.isEmpty(resource.getMetadata().getNamespace()) ?
					"default" :
					resource.getMetadata().getNamespace();
			Secret secret = new SecretBuilder()
					.withNewMetadata().withName(name).endMetadata()
					.build();
			client.secrets().inNamespace(namespace).delete(secret);
		}
	}
}

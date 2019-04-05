package com.kiwigrid.keycloak.client.controller;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiwigrid.keycloak.client.controller.k8s.CrdWatcher;
import com.kiwigrid.keycloak.client.controller.k8s.KeycloakClientRegistrationCRD;
import com.kiwigrid.keycloak.client.controller.keycloak.Registrar;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

public class CrdWatcherTest {

	@Rule
	public KubernetesServer server = new KubernetesServer(true, true);

	private KubernetesClient client;

	private static CrdWatcher crdWatcher;

	@BeforeClass
	public static void setupServer() {
		ApplicationContext app = Micronaut.run(Application.class);
		app.createBean(Registrar.class);
		crdWatcher = app.createBean(CrdWatcher.class);
	}

	@Before
	public void setup() throws IOException {
		client = server.getClient();
		Object crdYaml = new Yaml().load(getClass().getResourceAsStream("/keycloakClientCreationCRD.yaml"));
		ObjectMapper objectMapper = new ObjectMapper();
		KeycloakClientRegistrationCRD crd = objectMapper.readValue(JSONValue.toJSONString(
				crdYaml), KeycloakClientRegistrationCRD.class);
		client.customResourceDefinitions().create(crd);
	}

	@Test
	public void testIt() throws IOException {
		crdWatcher.doit();
	}
}

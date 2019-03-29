package com.kiwigrid.keycloak.client.controller;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiwigrid.keycloak.client.controller.exception.FailedToCreateApiClientException;
import com.kiwigrid.keycloak.client.controller.k8s.K8sHelper;
import com.kiwigrid.keycloak.client.controller.keycloak.Registrar;
import com.kiwigrid.keycloak.client.controller.keycloak.RegistrarProperties;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.ApiextensionsV1beta1Api;
import io.kubernetes.client.models.V1beta1CustomResourceDefinition;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.discovery.event.ServiceStartedEvent;
import io.micronaut.http.HttpStatus;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

@Singleton
public class Starter implements ApplicationEventListener<ServiceStartedEvent> {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Inject
	private K8sHelper k8sHelper;

	@Inject
	private Registrar registrar;

	@Inject
	private RegistrarProperties properties;

	@Override
	public void onApplicationEvent(ServiceStartedEvent event) {

//		try {
//			registrar = new Registrar(properties);
//		} catch (Exception e) {
//			logger.error("Keycloak failed", e);
//			System.exit(1);
//		}

		try {
			createCrd();
		} catch (FailedToCreateApiClientException | Exception e) {
			logger.error("Failed to create CRD", e);
			System.exit(1);
		}
	}

	private void createCrd() throws ApiException, FailedToCreateApiClientException, IOException {
		logger.debug("Creating CRD...");
		Yaml yaml = new Yaml();
		Object crdYaml = yaml.load(getClass().getResourceAsStream("/keycloakClientCreationCRD.yaml"));

		ApiextensionsV1beta1Api api = new ApiextensionsV1beta1Api();
		api.setApiClient(k8sHelper.getConfiguredApiClient());

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			V1beta1CustomResourceDefinition crd = objectMapper.readValue(JSONValue.toJSONString(
					crdYaml), V1beta1CustomResourceDefinition.class);
			api.createCustomResourceDefinition(crd, null, null, null);
			logger.debug("CRD created.");
		} catch (ApiException ae) {
			if (HttpStatus.CONFLICT.getCode() == ae.getCode()) {
				logger.debug("CRD already exists");
			} else {
				throw ae;
			}
		}
	}
}

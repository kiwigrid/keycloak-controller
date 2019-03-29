package com.kiwigrid.keycloak.client.controller.k8s;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import com.google.gson.reflect.TypeToken;
import com.kiwigrid.keycloak.client.controller.exception.FailedToCreateApiClientException;
import com.squareup.okhttp.Call;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;
import javax.inject.Singleton;

@Singleton
public class ConfigMapWatcher {

	private K8sHelper k8sHelper;

	public void watch() throws ApiException, FailedToCreateApiClientException, IOException {

		try (Watch<KeycloakClientRegistration> watch = getConfigMapWatch()) {
			for (Watch.Response<KeycloakClientRegistration> item : watch) {
				final KeycloakClientRegistration configMap = item.object;

//				V1ObjectMeta metadata = configMap.getMetadata();
//				String appName = metadata.getLabels().getOrDefault(RegistrationHelper.HELM_APP_LABEL, "unknown");
//				logger.info("ConfigMap {} for self registration of app {} {}", metadata.getName(), appName, item.type);

//				try {
//					processConfigMapAndPerformRelatedAction(configMap, item.type);
//				} catch (Exception e) {
//					logger.warn("Failed to register.", e);
//				}
			}
		}
	}

	private Watch<KeycloakClientRegistration> getConfigMapWatch() throws FailedToCreateApiClientException, ApiException {
		final ApiClient apiClient = k8sHelper.getConfiguredApiClient();
		final Call configMapCall = getConfigMapCall();
		final Type returnType = new TypeToken<Watch.Response<KeycloakClientRegistration>>() {
		}.getType();

		return Watch.createWatch(
				apiClient,
				configMapCall,
				returnType);
	}

	private Call getConfigMapCall() throws ApiException {
		CoreV1Api api = new CoreV1Api();
		// TODO Improve querying by using Label selector
		// As a drawback the Watch might run into Timeout exception.
		// As a result the whole querying couldn't relay on Watch and has to be implemented in a different way.
		String labelSelector = null;
		return api.listConfigMapForAllNamespacesCall(null,
				null,
				null,
				labelSelector,
				null,
				null,
				null,
				null,
				Boolean.TRUE,
				null,
				null);
	}
}

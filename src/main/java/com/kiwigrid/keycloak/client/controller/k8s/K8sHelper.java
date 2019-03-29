package com.kiwigrid.keycloak.client.controller.k8s;

import java.util.concurrent.TimeUnit;

import com.kiwigrid.keycloak.client.controller.exception.FailedToCreateApiClientException;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.authenticators.GCPAuthenticator;
import javax.inject.Singleton;

@Singleton
public class K8sHelper {

	public ApiClient getConfiguredApiClient() throws FailedToCreateApiClientException {
		final ApiClient apiClient;
		try {
			apiClient = Config.defaultClient();
		} catch (Exception e) {
			throw new FailedToCreateApiClientException(e.getMessage());
		}
		apiClient.getHttpClient().setReadTimeout(60, TimeUnit.SECONDS);
		Configuration.setDefaultApiClient(apiClient);

		return apiClient;
	}
}

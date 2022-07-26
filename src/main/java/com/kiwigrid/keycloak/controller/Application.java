package com.kiwigrid.keycloak.controller;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;
import javax.inject.Singleton;

@Factory
public class Application {

	public static void main(String[] args) {
		Micronaut.run(Application.class);
	}

	@Singleton
	@Bean(preDestroy = "close")
	KubernetesClient kubernetesClient() {
		return new KubernetesClientBuilder().build();
	}
}
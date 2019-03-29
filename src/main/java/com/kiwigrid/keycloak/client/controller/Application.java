package com.kiwigrid.keycloak.client.controller;

import java.io.IOException;

import com.kiwigrid.keycloak.client.controller.k8s.CrdWatcher;
import com.kiwigrid.keycloak.client.controller.keycloak.Registrar;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;

public class Application {

	public static void main(String[] args) throws IOException {
		ApplicationContext app = Micronaut.run(Application.class);
		CrdWatcher crdWatcher = app.createBean(CrdWatcher.class);
		Registrar registrar = app.createBean(Registrar.class);
		registrar.verifyKeycloakConnection();
		crdWatcher.doit();
	}
}
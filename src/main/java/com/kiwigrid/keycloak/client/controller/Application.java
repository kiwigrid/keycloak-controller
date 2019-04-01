package com.kiwigrid.keycloak.client.controller;

import com.kiwigrid.keycloak.client.controller.keycloak.Registrar;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;

public class Application {

	public static void main(String[] args) {
		ApplicationContext app = Micronaut.run(Application.class);
		Registrar r = app.createBean(Registrar.class);
		r.verifyKeycloakConnection();
	}
}
package com.kiwigrid.keycloak.client.controller.exception;

/**
 * Indicates that client registration at keycloak failed.
 *
 * @author Axel Koehler
 */
public class ClientRegistrationException extends Exception {

	public ClientRegistrationException(String s) {
		super(s);
	}

	public ClientRegistrationException(Exception e) {
		super(e);
	}
}

package com.kiwigrid.keycloak.client.controller.exception;

/**
 * Indicates that a client registration could not be done due an already existing client with the same client-id.
 *
 * @author Axel Koehler
 */
public class ClientConflictException extends Exception {
	public ClientConflictException(String s) {
		super(s);
	}
}
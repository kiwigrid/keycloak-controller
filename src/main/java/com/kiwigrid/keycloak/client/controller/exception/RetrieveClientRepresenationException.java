package com.kiwigrid.keycloak.client.controller.exception;

/**
 * Indicates that the {@link org.keycloak.representations.idm.ClientRepresentation} could not be determined.<br>
 * Either there was no or more than one clients for the requested clientId.
 *
 * @author Axel Koehler
 */
public class RetrieveClientRepresenationException extends Exception {

	public RetrieveClientRepresenationException(String s) {
		super(s);
	}
}
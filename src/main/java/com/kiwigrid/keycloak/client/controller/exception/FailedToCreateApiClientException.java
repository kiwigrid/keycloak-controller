package com.kiwigrid.keycloak.client.controller.exception;

/**
 * Indicates that the {@link io.kubernetes.client.ApiClient} creation failed.
 */
public class FailedToCreateApiClientException extends Throwable {
	public FailedToCreateApiClientException(String s) {
		super(s);
	}
}
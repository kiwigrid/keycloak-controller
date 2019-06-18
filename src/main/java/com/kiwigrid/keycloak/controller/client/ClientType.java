package com.kiwigrid.keycloak.controller.client;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ClientType {

	PUBLIC,
	CONFIDENTIAL,
	BEARER;

	@JsonValue
	public String getValue() {
		return name().toLowerCase();
	}
}
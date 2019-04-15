package com.kiwigrid.keycloak.client.controller;

import java.util.Collections;
import java.util.UUID;

import com.kiwigrid.keycloak.client.controller.k8s.crd.KcrSpec;
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RegistrarHelperTest {

	@Test
	public void verifyCommonAttributeTransformatio() {
		KcrSpec kcrSpec = new KcrSpec();
		kcrSpec.setClientId(UUID.randomUUID().toString());
		kcrSpec.setRedirectUris(Collections.singletonList("https://redirect/*"));
		kcrSpec.setWebOrigins(Collections.singletonList("https://origin/*"));
		kcrSpec.setAccessType("default");

		// Test
		ClientRepresentation actualClientRepresenation = RegistrarHelper.transformKeycloakClientRegistrationSpec(kcrSpec);

		// Verify
		assertThat(actualClientRepresenation.getClientId(), is(kcrSpec.getClientId()));
		assertThat(actualClientRepresenation.getWebOrigins(), is(kcrSpec.getWebOrigins()));
		assertThat(actualClientRepresenation.getRedirectUris(), is(kcrSpec.getRedirectUris()));
	}

	@Test
	public void verifyConfidentialClientTransformation() {
		KcrSpec kcrSpec = new KcrSpec();
		kcrSpec.setAccessType("confidential");
		kcrSpec.setServiceAccountsEnabled(true);

		// Test
		ClientRepresentation actualClientRepresenation = RegistrarHelper.transformKeycloakClientRegistrationSpec(kcrSpec);

		// Verify
		assertThat(actualClientRepresenation.isPublicClient(), is(false));
		assertThat(actualClientRepresenation.isBearerOnly(), is(false));
		assertThat(actualClientRepresenation.isServiceAccountsEnabled(), is(true));
	}

	@Test
	public void verifyPublicClientTransformation() {
		KcrSpec kcrSpec = new KcrSpec();
		kcrSpec.setAccessType("public");

		// Test
		ClientRepresentation actualClientRepresenation = RegistrarHelper.transformKeycloakClientRegistrationSpec(kcrSpec);

		// Verify
		assertThat(actualClientRepresenation.isPublicClient(), is(true));
		assertThat(actualClientRepresenation.isBearerOnly(), is(false));
	}

	@Test
	public void verifyBearerOnlyClientTransformation() {
		KcrSpec kcrSpec = new KcrSpec();
		kcrSpec.setAccessType("bearer-only");

		// Test
		ClientRepresentation actualClientRepresenation = RegistrarHelper.transformKeycloakClientRegistrationSpec(kcrSpec);

		// Verify
		assertThat(actualClientRepresenation.isPublicClient(), is(false));
		assertThat(actualClientRepresenation.isBearerOnly(), is(true));
	}
}
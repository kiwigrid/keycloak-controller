package com.kiwigrid.keycloak.client.controller.k8s.crd;

import java.util.List;

import com.kiwigrid.keycloak.client.controller.k8s.KeycloakClientRegistrationCRD;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionSpec;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.NoArgsConstructor
public class KcrSpec extends CustomResourceDefinitionSpec {

	private String realm;
	private String accessType;
	private String clientId;
	private List<String> redirectUris;
	private List<String> webOrigins;
	private boolean serviceAccountsEnabled;

	@Override
	public String getGroup() {
		return KeycloakClientRegistrationCRD.GROUP;
	}

	@Override
	public String getVersion() {
		return KeycloakClientRegistrationCRD.VERSION;
	}
}

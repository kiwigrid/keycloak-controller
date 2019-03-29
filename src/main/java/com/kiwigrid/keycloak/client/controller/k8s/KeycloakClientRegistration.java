package com.kiwigrid.keycloak.client.controller.k8s;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import io.kubernetes.client.models.*;

@lombok.Getter
@lombok.Setter
@lombok.ToString
public class KeycloakClientRegistration {

	public static class KeycloakClientRegistrationSpec {
		@SerializedName("clientId")
		public String clientId = null;

		@SerializedName("publicClient")
		public String publicClient = null;

		@SerializedName("redirectUris")
		public List<String> redirectUris = null;

		@SerializedName("webOrigins")
		public  List<String> webOrigins = null;
	}

	@SerializedName("metadata")
	public V1ObjectMeta metadata = null;

	@SerializedName("kind")
	private String kind = "KeycloakClientRegistration";

	@SerializedName("apiVersion")
	private String apiVersion = "k8s.kiwigrid.com/v1beta1";

	@SerializedName("spec")
	public KeycloakClientRegistrationSpec spec;
}

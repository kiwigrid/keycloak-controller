package com.kiwigrid.keycloak.client.controller.keycloak;

import io.micronaut.context.annotation.ConfigurationProperties;
import javax.validation.constraints.NotBlank;

@lombok.Getter
@lombok.Setter
@ConfigurationProperties("registrar")
public class RegistrarProperties {

	@NotBlank
	private String keycloakUrl;
	@NotBlank
	private String keycloakUser;
	@NotBlank
	private String keycloakPwd;

}

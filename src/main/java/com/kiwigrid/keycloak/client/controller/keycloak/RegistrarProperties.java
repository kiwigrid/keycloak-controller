package com.kiwigrid.keycloak.client.controller.keycloak;

import io.micronaut.context.annotation.ConfigurationProperties;
import javax.validation.constraints.NotNull;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@ConfigurationProperties("registrar")
public class RegistrarProperties {

	@NotNull
	private String keycloakUrl;
	@NotNull
	private String keycloakUser;
	@NotNull
	private String keycloakPwd;

}

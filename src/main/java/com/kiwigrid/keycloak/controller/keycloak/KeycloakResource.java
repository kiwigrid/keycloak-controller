package com.kiwigrid.keycloak.controller.keycloak;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Singular;
import io.fabric8.kubernetes.model.annotation.Version;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode(callSuper = false)
@Group("k8s.kiwigrid.com")
@Kind("Keycloak")
@Version("v1beta1")
@ShortNames("kc")
@Singular("keycloak")
@Plural("keycloaks")
public class KeycloakResource extends CustomResource<KeycloakSpec, KeycloakResourceStatus> {

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}
}
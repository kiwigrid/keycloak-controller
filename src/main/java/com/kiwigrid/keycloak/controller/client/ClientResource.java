package com.kiwigrid.keycloak.controller.client;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import io.fabric8.kubernetes.api.model.Namespaced;
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
@Version("v1beta1")
@Kind("KeycloakClient")
@Singular("keycloakclient")
@Plural("keycloakclients")
@ShortNames("kcc")
public class ClientResource extends CustomResource<ClientSpec, ClientResourceStatus> implements Namespaced {

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}
}
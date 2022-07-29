package com.kiwigrid.keycloak.controller.realm;

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
@Version("v1beta1")
@Kind("KeycloakRealm")
@Singular("keycloakrealm")
@Plural("keycloakrealms")
@ShortNames("kcr")
public class RealmResource extends CustomResource<RealmSpec, RealmResourceStatus> {

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}
}
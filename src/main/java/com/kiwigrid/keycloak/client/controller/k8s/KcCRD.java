package com.kiwigrid.keycloak.client.controller.k8s;

import java.util.Collections;

import io.kubernetes.client.models.*;

public class KcCRD extends V1beta1CustomResourceDefinition {

	public KcCRD() {
		V1ObjectMeta metaData = new V1ObjectMeta();
		metaData.setName("keycloakclientregistrations.k8s.kiwigrid.com");

		V1beta1CustomResourceDefinitionNames names = new V1beta1CustomResourceDefinitionNames();
		names.setSingular("keycloakclientregistration");
		names.setPlural("keycloakclientregistrations");
		names.setKind("KeycloakClientRegistration");
		names.setShortNames(Collections.singletonList("kcr"));

		V1beta1CustomResourceValidation validation = new V1beta1CustomResourceValidation();
		V1beta1JSONSchemaProps schema = new V1beta1JSONSchemaProps();
		validation.setOpenAPIV3Schema(schema);

		V1beta1CustomResourceDefinitionSpec spec = new V1beta1CustomResourceDefinitionSpec();
		spec.setGroup("k8s.kiwigrid.com");
		spec.setVersion("v1beta1");
		spec.setScope("Cluster");
		spec.setNames(names);
		spec.setValidation(validation);
		setSpec(spec);
		setMetadata(metaData);
	}
}

package com.kiwigrid.keycloak.controller.realm;

import java.util.ArrayList;
import java.util.List;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode(callSuper = false)
public class RealmSpec {
    private String keycloak;
	private String realm;

	private List<String> roles = new ArrayList<>();
}
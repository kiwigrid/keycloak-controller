# keycloak-client-controller

[![Build Status](https://travis-ci.com/kiwigrid/keycloak-client-controller.svg?branch=master)](https://travis-ci.com/kiwigrid/keycloak-client-controller)

This controller manage Keycloak clients and realms over Kubernetes resources and creates a Kubernetes secret with 
the `clientSecret` for clients of type `confidential`.

Within the cluster, multiple Keycloak instances can be referenced. This become useful in a multi-tenant environment where different services
has to be registered at different Keycloak instances.
 
## Setup

Before deploying the controller, create the [CustomResourceDefinition](https://kubernetes.io/docs/tasks/access-kubernetes-api/custom-resources/custom-resource-definitions/):

```bash
kubectl apply -f src/main/k8s/
```

## Examples

See sub-dir `examples` for more sophisticated samples.

### Keycloak

```yaml
apiVersion: k8s.kiwigrid.com/v1beta1
kind: Keycloak
metadata:
  name: keycloak-instance-example
spec:
  url: https://keycloak.example.com/auth
  realm: master
  clientId: admin-cli
  username: admin
  passwordSecretName: keycloak-http
```

### Realm

```yaml
apiVersion: k8s.kiwigrid.com/v1beta1
kind: KeycloakRealm
metadata:
  name: realm-example
spec:
  keycloak: keycloak-instance-example
  realm: my-realm
  roles:
  - service
  - admin
  - operations
```

### Client

```yaml
apiVersion: k8s.kiwigrid.com/v1beta1
kind: KeycloakClient
metadata:
  name: client-example
spec:
  keycloak: keycloak-instance-example
  realm: my-realm
  clientId: client-example
  clientType: public
  directAccessGrantsEnabled: true
  standardFlowEnabled: false
  implicitFlowEnabled: false
  mapper:
  - name: example-service-audience
    protocolMapper: oidc-audience-mapper
    config:
      claim.name: audience
      access.token.claim: "true"
      included.client.audience: my-service
```
# keycloak-client-controller

[![Build Status](https://travis-ci.com/kiwigrid/keycloak-client-controller.svg?branch=master)](https://travis-ci.com/kiwigrid/keycloak-client-controller)

This controller manage Keycloak clients over Kubernetes resources and creates a Kubernetes secret with 
the `clientSecret` for clients of type `confidential`.
 
The secret will be named like the the `KeycloakClientRegistration` resource with suffix `-secret` (e.g. `kcr-sample-secret` from _Examples_ section)

## Setup

Before deploying the controller, create the [CustomResourceDefinition](https://kubernetes.io/docs/tasks/access-kubernetes-api/custom-resources/custom-resource-definitions/)

```bash
kubectl apply -f keycloakClientCreationCRD.yaml
```

```yaml
apiVersion: apiextensions.k8s.io/v1beta1
kind: CustomResourceDefinition
metadata:
  name: keycloakclientregistrations.k8s.kiwigrid.com
  labels:
    app: keycloak-client-controller
spec:
  group: k8s.kiwigrid.com
  version: v1beta1
  scope: Namespaced
  names:
    plural: keycloakclientregistrations
    singular: keycloakclientregistration
    kind: KeycloakClientRegistration
    shortNames:
      - kcr
  validation:
    openAPIV3Schema:
      properties:
        spec:
          properties:
            realm:
              type: string
            clientId:
              type: string
            accessType:
              type: string
              enum:
                - public
                - confidential
                - bearer-only
            redirectUris:
              items:
                type: string
              type: array
            webOrigins:
              items:
                type: string
              type: array
            serviceAccountsEnabled:
              type: boolean
          required:
            - realm
            - clientId
            - accessType
            - redirectUris
            - webOrigins
          type: object
        status:
          type: object
```

## Examples

```yaml
apiVersion: k8s.kiwigrid.com/v1beta1
kind: KeycloakClientRegistration
metadata:
  name: kcr-sample
spec:
  realm: master
  clientId: client-example
  accessType: confidential
  serviceAccountsEnabled: true
  redirectUris:
    - "*"
  webOrigins:
    - "*"
```
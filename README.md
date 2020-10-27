# keycloak-controller

![deploy](https://github.com/kiwigrid/keycloak-controller/workflows/deploy/badge.svg)

This controller manage Keycloak clients and realms over Kubernetes resources and creates a Kubernetes secret with 
the `clientSecret` for clients of type `confidential`.

Within the cluster, multiple Keycloak instances can be referenced.
This become useful in a multi-tenant environment where different services
has to be registered at different Keycloak instances.

By default, the controller watches only for events in its namespace.
To enable watching in all namespaces set environment variable `CONTROLLER_NAMESPACED=false`.
 
## Setup

Before deploying the controller, create the [CustomResourceDefinition](https://kubernetes.io/docs/tasks/access-kubernetes-api/custom-resources/custom-resource-definitions/):

```bash
kubectl apply -f src/main/k8s/
```
The controller can then be deployed using [the corresponding helm chart](https://github.com/kiwigrid/helm-charts/tree/master/charts/keycloak-controller).

The Docker container can be found here: https://hub.docker.com/r/kiwigrid/keycloak-controller

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

# Development

To test the controller using the same process as Github Actions from a blank container, install `act`:

```
brew install act
```

And then trigger the pull request action:

```
act pull_request -P ubuntu-latest=nektos/act-environments-ubuntu:18.04
```

## Machine Setup

To run Keycloak Controller locally some of the same scripts that power the Github Actions can be used, but you'll want to provision your machine locally instead, as you most likely don't want to delete all your installs and builds for every single change, or change your local environment in a forceful manner - such as installing versions of a tool that conflicts with another local tool you are using.

The tools you'll need to make sure are installed are `kubectl`, `helm`, `kind`, `java`, and `maven`.

Please look at their official documentation to find how to install each.

Once they are installed you can run the various ci scripts:

Here is an example of running the full pipeline, parallelized where possible - of course you could run them ad-hoc in any order that makes sense:

###### Setup

Build `.jar` and run a Kubernetes cluster in Docker:

```sh
bash .github/local.maven.sh &
bash .github/local.kind.sh &
wait
```

Build docker image using .jar from previous step, and get Helm ready:

```sh
bash .github/ci.docker-build.sh &
bash .github/ci.helm.sh &
wait
```

Install Keycloak and Keycloak Controller configured to use the image produced and uploaded to Kind in the last step:

```sh
bash .github/ci.keycloak.sh "9.0.1" & \
bash .github/ci.keycloak-controller.sh "0.6.1" & \
wait
```

###### Run Examples

```
bash .github/ci.example.sh &&
bash .github/ci.verify.sh
```

###### Make changes and see them running in Kubernetes

```
bash .github/local.maven.sh &&
bash .github/ci.docker-build.sh &&
kubectl rollout restart deployment -n keycloak keycloak-controller && 
kubectl rollout status deployment -n keycloak keycloak-controller
```

###### Teardown

```
kind delete clusters chart-testing
```

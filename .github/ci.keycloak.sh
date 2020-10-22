#!/bin/bash
#
# ci for keycloak-controller
#

set -o errexit

KEYCLOAK_CHART_VERSION="${1}"
NAMESPACE="keycloak"

echo -e "\n##### create keycloak namespace #####\n"
kubectl create namespace $NAMESPACE

echo -e "\n##### install keycloak #####\n"
kubectl create secret generic keycloak-auth \
    --namespace $NAMESPACE \
    --from-literal=username=keycloak \
    --from-literal=password=keycloak

helm upgrade -i keycloak codecentric/keycloak \
    --wait \
    --namespace $NAMESPACE \
    --version "${KEYCLOAK_CHART_VERSION}" \
    --values .github/keycloak-values.yaml
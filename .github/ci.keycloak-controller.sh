#!/bin/bash
#
# ci for keycloak-controller
#

set -o errexit

KEYCLOAK_CONTROLLER_CHART_VERSION="${1}"
NAMESPACE="keycloak"

echo -e "\n##### install keycloak-controller crds #####\n"
while IFS= read -r CRD; do
    kubectl apply -f "${CRD}"
done < <(find src/main/k8s -type f)

echo -e "\n##### test controller crds #####\n"
kubectl -n "$NAMESPACE" wait --for condition=established --timeout=15s crd/keycloakclients.k8s.kiwigrid.com
kubectl -n "$NAMESPACE" wait --for condition=established --timeout=15s crd/keycloakclientscopes.k8s.kiwigrid.com
kubectl -n "$NAMESPACE" wait --for condition=established --timeout=15s crd/keycloakrealms.k8s.kiwigrid.com
kubectl -n "$NAMESPACE" wait --for condition=established --timeout=15s crd/keycloaks.k8s.kiwigrid.com

echo -e "\n##### install keycloak-controller #####\n"
helm upgrade -i keycloak-controller kiwigrid/keycloak-controller \
    --wait \
    --namespace keycloak \
    --version "$KEYCLOAK_CONTROLLER_CHART_VERSION" \
    --set image.repository=keycloak-controller \
    --set image.tag=local
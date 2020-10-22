#!/bin/bash
#
# ci for keycloak-controller
#

set -o errexit

NAMESPACE="keycloak"

echo -e "\n##### install keycloak-controller examples #####\n"
while IFS= read -r KEYCLOAK_EXAMPLE; do
    kubectl -n "$NAMESPACE" apply -f "${KEYCLOAK_EXAMPLE}"
done < <(find examples -type f)

echo -e "\n##### show keycloak-controller examples #####\n"
kubectl -n "$NAMESPACE" get keycloaks.k8s.kiwigrid.com
echo ""
kubectl -n "$NAMESPACE" get keycloakrealms.k8s.kiwigrid.com
echo ""
kubectl -n "$NAMESPACE" get keycloakclients.k8s.kiwigrid.com
echo ""
kubectl -n "$NAMESPACE" get keycloakclientscopes.k8s.kiwigrid.com
echo ""
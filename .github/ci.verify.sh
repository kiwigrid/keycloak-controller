#!/bin/bash

set -o errexit

NAMESPACE="keycloak"

echo -e "\n##### check for errors in keycloak-controller logs #####\n"
sleep 60
if kubectl -n "${NAMESPACE}" logs -l app.kubernetes.io/name=keycloak-controller | grep -q ERROR; then
    echo "errors found in logs :("
    kubectl -n "${NAMESPACE}" logs -l app.kubernetes.io/name=keycloak-controller
    exit 1
else
    echo "no errors found in logs :)"
    kubectl -n "${NAMESPACE}" logs -l app.kubernetes.io/name=keycloak-controller
fi
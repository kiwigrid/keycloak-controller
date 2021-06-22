#!/bin/bash

set -o errexit

HELM_VERSION="${1}"

echo -e "\n##### install helm #####\n"
curl --silent --show-error --fail --location --output get_helm.sh https://raw.githubusercontent.com/kubernetes/helm/main/scripts/get
chmod 700 get_helm.sh
./get_helm.sh --version "${HELM_VERSION}"

helm version
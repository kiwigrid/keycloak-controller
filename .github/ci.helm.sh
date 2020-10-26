#!/bin/bash
#
# ci for keycloak-controller
#

set -o errexit

echo -e "\n##### add helm repos #####\n"
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add codecentric https://codecentric.github.io/helm-charts
helm repo add kiwigrid https://kiwigrid.github.io

echo -e "\n##### update helm repos #####\n"
helm repo update

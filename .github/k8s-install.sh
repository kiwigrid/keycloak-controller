#!/bin/bash
#
# ci for keycloak-controller
#

set -o errexit

KEYCLOAK_CHART_VERSION="${1}"
KEYCLOAK_CONTROLLER_CHART_VERSION="${2}"
K8S_VERSION="${3}"
NAMESPACE="keycloak"

echo -e "\n##### show versions #####\n"
echo "helm version:                       ${HELM_VERSION}"
echo "kubernetes version:                 ${K8S_VERSION}"
echo "keycloak chart version:             ${KEYCLOAK_CHART_VERSION}"
echo "keycloak-controller chart version:  ${KEYCLOAK_CONTROLLER_CHART_VERSION}"
echo ""

echo -e "\n##### install kubectl #####\n"
curl --silent --show-error --fail --location --output kubectl "https://storage.googleapis.com/kubernetes-release/release/${K8S_VERSION}/bin/linux/amd64/kubectl"
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin/kubectl

echo -e "\n##### install helm #####\n"
curl --silent --show-error --fail --location --output get_helm.sh https://raw.githubusercontent.com/kubernetes/helm/master/scripts/get
chmod 700 get_helm.sh
./get_helm.sh --version "${HELM_VERSION}"

echo -e "\n##### add helm repos #####\n"
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add codecentric https://codecentric.github.io/helm-charts
helm repo add kiwigrid https://kiwigrid.github.io

echo -e "\n##### update helm repos #####\n"
helm repo update

echo -e "\n##### create keycloak namespace #####\n"
kubectl create namespace "${NAMESPACE}"

echo -e "\n##### install keycloak #####\n"
helm upgrade -i keycloak codecentric/keycloak --wait --namespace "${NAMESPACE}" --version "${KEYCLOAK_CHART_VERSION}"

echo -e "\n##### install keycloak-controller #####\n"
helm upgrade -i keycloak-controller kiwigrid/keycloak-controller --wait --namespace "${NAMESPACE}" --version "${KEYCLOAK_CONTROLLER_CHART_VERSION}" --set image.repository=keycloak-controller --set image.tag=ci-snapshot

echo -e "\n##### install keycloak-controller crds #####\n"
while IFS= read -r CRD; do
    kubectl apply -f "${CRD}"
done < <(find src/main/k8s -type f)

echo -e "\n##### test controller crds #####\n"
kubectl -n "${NAMESPACE}" wait --for condition=established --timeout=15s crd/keycloakclients.k8s.kiwigrid.com
kubectl -n "${NAMESPACE}" wait --for condition=established --timeout=15s crd/keycloakclientscopes.k8s.kiwigrid.com
kubectl -n "${NAMESPACE}" wait --for condition=established --timeout=15s crd/keycloakrealms.k8s.kiwigrid.com
kubectl -n "${NAMESPACE}" wait --for condition=established --timeout=15s crd/keycloaks.k8s.kiwigrid.com

echo -e "\n##### install keycloak-controller examples #####\n"
while IFS= read -r KEYCLOAK_EXAMPLE; do
    kubectl -n "${NAMESPACE}" apply -f "${KEYCLOAK_EXAMPLE}"
done < <(find examples -type f)

echo -e "\n##### show keycloak-controller examples #####\n"
kubectl -n "${NAMESPACE}" get keycloakclients.k8s.kiwigrid.com
echo ""
kubectl -n "${NAMESPACE}" get keycloakclientscopes.k8s.kiwigrid.com
echo ""
kubectl -n "${NAMESPACE}" get keycloakrealms.k8s.kiwigrid.com
echo ""
kubectl -n "${NAMESPACE}" get keycloaks.k8s.kiwigrid.com
echo ""

echo -e "\n##### check for errors in keycloak-controller logs #####\n"
kubectl -n "${NAMESPACE}" logs -l app.kubernetes.io/name=keycloak-controller &
sleep 150
if kubectl -n "${NAMESPACE}" logs -l app.kubernetes.io/name=keycloak-controller | grep -q ERROR; then
    echo "errors found in logs"
    exit 1
else
    echo "no errors found in logs"
fi

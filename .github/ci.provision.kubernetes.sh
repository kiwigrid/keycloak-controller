#!/bin/bash

set -o errexit

K8S_VERSION="${1}"

echo -e "\n##### install kubectl #####\n"
curl --silent --show-error --fail --location --output kubectl "https://storage.googleapis.com/kubernetes-release/release/${K8S_VERSION}/bin/linux/amd64/kubectl"
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin/kubectl
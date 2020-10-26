#!/bin/bash
#
# ci for keycloak-controller
#

set -o errexit

kind create cluster --config .github/kind-config.yaml --name chart-testing
#!/bin/bash
#
# build docker image and push it to kind nodes

set -o errexit

DOCKER_TAG="keycloak-controller:local"

echo -e "\n##### create docker image with tag $DOCKER_TAG #####\n"
docker build -t "$DOCKER_TAG" ./target

echo -e "\n##### push docker image to kind nodes #####\n"
kind --name chart-testing load docker-image "$DOCKER_TAG"

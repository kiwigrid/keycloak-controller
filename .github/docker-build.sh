#!/bin/bash
#
# build docker image and push it to kind nodes

set -o errexit

DOCKER_TAG="keycloak-controller:ci-snapshot"

echo -e "\n##### install mvn & java #####\n"
sudo apt-get update
sudo apt-get install -y maven openjdk-11-jdk

echo -e "\n##### build keycloak-controller #####\n"
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
mvn package --quiet

echo -e "\n##### create docker image with tag ${DOCKER_TAG} #####\n"
docker build -t "${DOCKER_TAG}" ./target

echo -e "\n##### push docker image to kind nodes #####\n"
kind --name chart-testing load docker-image "${DOCKER_TAG}"

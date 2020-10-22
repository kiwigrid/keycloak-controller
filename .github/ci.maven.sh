#!/bin/bash
#
# ci for keycloak-controller
#

set -o errexit

export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

mvn clean && mvn package
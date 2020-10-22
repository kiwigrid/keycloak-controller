#!/bin/bash
#
# ci for keycloak-controller
#

set -o errexit

mvn clean && mvn package
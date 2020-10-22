#!/bin/bash

set -o errexit

echo -e "\n##### install mvn & java #####\n"
sudo apt-get update && sudo apt-get install -y maven openjdk-11-jdk
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

mvn --version
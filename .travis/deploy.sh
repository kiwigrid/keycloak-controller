#!/bin/bash

set -e

docker build -t $IMG:latest ./target
echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin
docker push $IMG:latest

if ! [ -z "$1"  ]; then
    docker tag $IMG:latest $IMG:$1
    docker push $IMG:$1
fi


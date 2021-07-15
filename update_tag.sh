#! /bin/bash
if [ -n "$1" ]
then
  TAG=$1
else
  TAG=$(git rev-parse --short=7 HEAD)
fi
# login to the OpenShift cluster before launching this script

# Tags the imagestream
oc tag quay.io/quarkus/registry-app:$TAG quarkus-registry-app:production

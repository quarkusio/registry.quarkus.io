#! /bin/bash
 : ${1:?"Must specify a tag. Ex: latest"}

# login to the OpenShift cluster before launching this script

# Tags the imagestream
oc tag quay.io/quarkus/registry-app:$1 quarkus-registry-app:production

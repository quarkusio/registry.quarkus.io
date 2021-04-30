#! /bin/bash

# login to the OpenShift cluster before launching this script

# Delete the image stream to prevent manifest errors
oc delete is ubi-quarkus-native-binary-s2i

mvn clean package -Dquarkus.kubernetes.deploy=true -Dquarkus.native.container-build=true -Dnative

# add kubernetes.io/tls-acme: 'true' to the route to renew the SSL certificate automatically

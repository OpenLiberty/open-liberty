#!/bin/bash

#Needs to be provided by user
USER_NAME=kyleaure

#Version of docker image.  Increment if doing a new release
VERSION=1.0.full.ssl

#Name of the final image
IMAGE_NAME=oracle-21.3.0-faststart

#Docker image signiture in form username/image:version
SIGNATURE=$USER_NAME/$IMAGE_NAME:$VERSION

echo "Attempting to build and push $SIGNATURE"

#Ensure user is logged in
docker login || (echo "Unable to login to DockerHub" && exit 1)

#This script assumes it is in the same directory as the Dockerfile
docker build --no-cache -t $SIGNATURE .

#Extract wallet
SECURITY_DIR=../../servers/com.ibm.ws.jdbc.fat.oracle.ssl/security/
STORE_DIR=../../servers/com.ibm.ws.jdbc.fat.oracle.ssl/store/
CONTAINER="TMP-CONTAINER"

mkdir -p $SECURITY_DIR
mkdir -p $STORE_DIR

docker create --name $CONTAINER $SIGNATURE
docker cp $CONTAINER:/client/oracle/wallet/ewallet.p12 $SECURITY_DIR
docker cp $CONTAINER:/client/oracle/wallet/cwallet.sso $SECURITY_DIR
docker cp $CONTAINER:/client/oracle/store/client-keystore.jks $STORE_DIR
docker cp $CONTAINER:/client/oracle/store/client-truststore.jks $STORE_DIR
docker rm $CONTAINER

#Make security files readable
chmod -R 755 $SECURITY_DIR
chmod -R 755 $STORE_DIR

#Push image to DockerHub
docker push "$SIGNATURE"

#Add a comment to the Dockerfile and script
sed -i '' -e '/.*Currently tagged in DockerHub as.*/d' *Dockerfile
cat << EOF >> *Dockerfile

# Currently tagged in DockerHub as: $SIGNATURE
EOF

#!/bin/bash

#Needs to be provided by user
USER_NAME=kyleaure

#Version of docker image.  Increment if doing a new release
VERSION=2.0

#Name of the final image
IMAGE_NAME=db2-ssl

#Docker image signiture in form username/image:version
SIGNITURE=$USER_NAME/$IMAGE_NAME:$VERSION

echo "Attempting to build and push $SIGNITURE"

#Ensure user is logged in
docker login || (echo "Unable to login to DockerHub" && exit 1)

#This script assumes it is in the same directory as the Dockerfile
docker build -t $SIGNITURE .

echo "-------------------------- Extract keystore --------------------------"
SECURITY_DIR=../../servers/com.ibm.ws.jdbc.fat.db2/security/
CONTAINER="TMP-CONTAINER"

rm -rf $SECURITY_DIR
mkdir -p $SECURITY_DIR

docker create --name $CONTAINER $SIGNITURE
docker cp $CONTAINER:/certs/db2-keystore.p12 $SECURITY_DIR
docker rm $CONTAINER

keytool -list -v -keystore $SECURITY_DIR/db2-keystore.p12 -storepass db2test -storetype PKCS12

#Push image to DockerHub
docker push "$SIGNITURE"

#Add a comment to the Dockerfile and script
sed -i '' -e '/.*Currently tagged in DockerHub as.*/d' *Dockerfile
cat << EOF >> *Dockerfile
# Currently tagged in DockerHub as: $SIGNITURE
EOF
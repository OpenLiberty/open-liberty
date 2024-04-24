#!/bin/bash

#Needs to be provided by user
USER_NAME=zachhein

#Version of docker image.  Increment if doing a new release
VERSION=0.5

#Name of the final image
IMAGE_NAME=ldap-server

#Docker image signiture in form username/image:version
SIGNATURE=$USER_NAME/$IMAGE_NAME:$VERSION

echo "Attempting to build and push $SIGNATURE"

#Ensure user is logged in
docker login || (echo "Unable to login to DockerHub" && exit 1)

#This script assumes it is in the same directory as the Dockerfile
docker build --no-cache -t $SIGNATURE .

#Push image to DockerHub
docker push "$SIGNATURE"

#Add a comment to the Dockerfile and script
sed -i '' -e '/.*Currently tagged in DockerHub as.*/d' *Dockerfile
cat << EOF >> *Dockerfile

# Currently tagged in DockerHub as: $SIGNATURE
EOF

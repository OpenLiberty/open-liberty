#!/bin/bash

#Needs to be provided by user
USER_NAME=kyleaure

#Version of docker image.  Increment if doing a new release
VERSION=1.0

#Name of the final image
IMAGE_NAME=couchdb-ssl

#Docker image signiture in form username/image:version
SIGNITURE=$USER_NAME/$IMAGE_NAME:$VERSION

echo "Attempting to build and push $SIGNITURE"

#Ensure user is logged in
docker login || (echo "Unable to login to DockerHub" && exit 1)

#This script assumes it is in the same directory as the Dockerfile
docker build --no-cache -t $SIGNITURE .

#Push image to DockerHub
docker push "$SIGNITURE"

#Add a comment to the Dockerfile and script
sed -i '' -e '/.*Currently tagged in DockerHub as.*/d' *Dockerfile
cat << EOF >> *Dockerfile
# Currently tagged in DockerHub as: $SIGNITURE
EOF
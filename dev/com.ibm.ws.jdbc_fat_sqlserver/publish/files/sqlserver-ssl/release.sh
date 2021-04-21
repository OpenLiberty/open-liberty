#!/bin/bash
#Sample script to make it easier to push custom images to dockerhub

#TODO needs to be provided by user
USER_NAME=kyleaure

#TODO need to provide version of docker image.  Increment if doing a new release
VERSION=2019-CU10-ubuntu-16.04

#TODO need to provide name of the final image
IMAGE_NAME=sqlserver-ssl

#Docker image signiture in form username/image:version
SIGNITURE=$USER_NAME/$IMAGE_NAME:$VERSION

echo "Attempting to build and push $SIGNITURE"

#Ensure user is logged in
docker login || (echo "Unable to login to DockerHub" && exit 1)

#This script assumes it is in the same directory as the Dockerfile
docker build -t "$SIGNITURE" .

#Push image to DockerHub
docker push "$SIGNITURE"

#Add a comment to the Dockerfile and script
sed -i '' -e '/.*Currently tagged in DockerHub as.*/d' *Dockerfile
cat << EOF >> *Dockerfile

# Currently tagged in DockerHub as: $SIGNITURE
EOF

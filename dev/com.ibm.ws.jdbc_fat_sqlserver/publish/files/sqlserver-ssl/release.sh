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

#Extract keystore
CONTAINER="tmp-container"
SECURITY_DIR=../../servers/com.ibm.ws.jdbc.fat.sqlserver.ssl/security/
PASSWORD="WalletPasswd123"

rm -rf $SECURITY_DIR && mkdir -p $SECURITY_DIR

docker create --name $CONTAINER $SIGNITURE
docker cp $CONTAINER:/etc/ssl/certs/mssql.pem $SECURITY_DIR
docker cp $CONTAINER:/etc/ssl/mssql.key $SECURITY_DIR
docker rm $CONTAINER

keytool -importcert -file $SECURITY_DIR/mssql.pem -alias server -keystore $SECURITY_DIR/truststore.p12 -storepass  $PASSWORD -storetype PKCS12
keytool -list -v -keystore $SECURITY_DIR/truststore.p12 -storepass $PASSWORD
rm $SECURITY_DIR/mssql.pem $SECURITY_DIR/mssql.key

#Push image to DockerHub
docker push "$SIGNITURE"

#Add a comment to the Dockerfile and script
sed -i '' -e '/.*Currently tagged in DockerHub as.*/d' *Dockerfile
cat << EOF >> *Dockerfile

# Currently tagged in DockerHub as: $SIGNITURE
EOF

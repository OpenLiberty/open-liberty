#!/bin/bash

#Needs to be provided by user
USER_NAME=kyleaure

#Version of docker image.  Increment if doing a new release
VERSION=3.0

#Name of the final image
IMAGE_NAME=db2-ssl

#Docker image signature in form username/image:version
SIGNATURE=$USER_NAME/$IMAGE_NAME:$VERSION

echo "Attempting to build and push $SIGNATURE"

#Ensure user is logged in
docker login || (echo "Unable to login to DockerHub" && exit 1)

#This script assumes it is in the same directory as the Dockerfile
docker build -t $SIGNATURE .

SECURITY_DIR=../../servers/com.ibm.ws.jdbc.fat.db2/security/
CONTAINER="TMP-CONTAINER"

rm -rf $SECURITY_DIR
mkdir -p $SECURITY_DIR

echo "-------------------------- Extract keystore --------------------------"
docker create --name $CONTAINER $SIGNATURE
docker cp $CONTAINER:/certs/db2-keystore.p12 $SECURITY_DIR
docker rm $CONTAINER

keytool -list -v -keystore $SECURITY_DIR/db2-keystore.p12 -storepass db2test -storetype PKCS12

echo "-------------------------- Extract server cert ------------------------"
docker create --name $CONTAINER $SIGNATURE
docker cp $CONTAINER:/certs/server.arm $SECURITY_DIR/server.crt
docker rm $CONTAINER

cat $SECURITY_DIR/server.crt

echo "-------------------------- Generate wrong cert ------------------------"
### Same certificate used by our PostgreSQL SSL tests (which is invalid for DB2) ###
touch $SECURITY_DIR/wrong.crt
cat <<EOF >> $SECURITY_DIR/wrong.crt
-----BEGIN CERTIFICATE-----
MIIC+zCCAeOgAwIBAgIJALOwwUU1kHLrMA0GCSqGSIb3DQEBCwUAMBQxEjAQBgNV
BAMMCWxvY2FsaG9zdDAeFw0xOTA1MjExNTAzNDdaFw0xOTA2MjAxNTAzNDdaMBQx
EjAQBgNVBAMMCWxvY2FsaG9zdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC
ggEBAKfa9xN63c12+tG69cj8oOY10ksB0IAoDjUhNXGU/IEDJKsS0yVHag9OBBO4
LF0m1fiUkdvIpveLaLVvyOSxQ9C3aRkGyz8YoSxe6wHOnZx3bB62C/Juz+FReWnh
0QX0op6fbChIP99mEYyL6vIjUnH1dMHsrS2nOpFyIRwtuLyJqJGOaq2aNleVsS0t
dCSb8MszbZ+ARvC/GklER9kZdTvdTTeJprzl2UfkeCFgla48vc0yiuM5eLAy+h6M
P8mUC+UJwX/zoHhDBjyjlUNApy6de4QcHWapIQVFlfGO8oOWZs3e+3nGwLLpT7a1
7ulTqHu4i7DZ0osWgdqt9bv08qUCAwEAAaNQME4wHQYDVR0OBBYEFDdafw0OVsi5
xSgntDwILWhFvcABMB8GA1UdIwQYMBaAFDdafw0OVsi5xSgntDwILWhFvcABMAwG
A1UdEwQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAARud5FsEtdC8E1mCphgtzsc
eIvNLEwNN3xqvFQlm/JwjlzZBnp0zdsKhMh7FME+W6fiIExjsd18Uk8MTb8XRyEQ
cKLbc7XFD3YDux3NELexp1LWMIpBdWXORiy0K625Y/qs1K26BYmYFz1eWXmEj4vY
FnQ8cURQYeVOTM2pFCG8GjCVPk4IdB8aiElEbNiI3jaOH777Vk5VymLxi9tqfCu/
xDJmkxIcC3C3PPIBn8mEPpqoyxgI8gVWP85Zdyyb0ZoEYFUcvv5QknR1Om/julGM
HAW8+C7+TzU+tVxEUze6b+3UFVhTm6YeDMR9Ifbaix2EruVz9wZt9xOOXLf3W5c=
-----END CERTIFICATE-----
EOF

cat $SECURITY_DIR/wrong.crt

#Push image to DockerHub
docker push "$SIGNATURE"

#Add a comment to the Dockerfile and script
sed -i '' -e '/.*Currently tagged in DockerHub as.*/d' *Dockerfile
cat <<EOF >> *Dockerfile
# Currently tagged in DockerHub as: $SIGNATURE
EOF
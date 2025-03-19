#!/bin/sh

# This is an enablement script we want to run during image creation

source ${SETUPDIR?}/include/db2_constants

#Directories
CERTS=/certs/
GSKIT_BINARY=${DB2DIR?}/gskit
GSKIT_LIBRARY=${DB2DIR?}/lib64/gskit_db2
JAVA_HOME=${DB2DIR?}/java/jdk64/

#Key database
KEY_DB=$CERTS/server.kdb
KEY_STORE=$CERTS/db2-keystore.p12
STASH=$CERTS/server.sth
CERT=$CERTS/server.arm
LABEL="mylabel"
SSLPASSWORD="db2test"
DN="CN=testcompany"

#Path variables
export PATH=$PATH:$GSKIT_BINARY/bin:${DB2DIR?}/bin:$JAVA_HOME/bin
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$GSKIT_LIBRARY
ldconfig

#Directories
mkdir -p $CERTS

echo "------------------------------------- create keys ------------------------"

gsk8capicmd_64 -keydb -create  -db $KEY_DB -pw $SSLPASSWORD -stash
gsk8capicmd_64 -cert  -create  -db $KEY_DB -pw $SSLPASSWORD -label $LABEL -dn $DN -expire 36500 -size 2048 -sig_alg SHA256WithRSA
gsk8capicmd_64 -cert  -extract -db $KEY_DB -pw $SSLPASSWORD -label $LABEL -target $CERT -format ascii -fips
gsk8capicmd_64 -cert  -details -db $KEY_DB -pw $SSLPASSWORD -label $LABEL

echo "-------------------------- create keystore via keytool ---------------------"
keytool -noprompt -import -trustcacerts -alias $SSLPASSWORD -file $CERT -keystore $KEY_STORE  -storepass $SSLPASSWORD -storetype PKCS12
keytool -list -v -keystore $KEY_STORE -storepass $SSLPASSWORD -storetype PKCS12

echo "-------------------------- /certs/ directory contents ---------------------"
ls -la $CERTS

#!/bin/sh

### Setup server certificates
CONF_DIR=/var/lib/postgresql
DN="/CN=localhost/OU=postgres"
PASSWORD=postgres

echo "(*) Create server certificate signing request"
openssl req -new -text -passout pass:$PASSWORD -subj $DN -out $CONF_DIR/server.req -keyout $CONF_DIR/privkey.pem 2>/dev/null && echo "Successful" || echo "Failure"

echo "(*) Create server signing key"
openssl rsa -in $CONF_DIR/privkey.pem -passin pass:$PASSWORD -out $CONF_DIR/server.key

echo "(*) Create server certificate" 
openssl req -x509 -days 3650 -in $CONF_DIR/server.req -text -key $CONF_DIR/server.key -out $CONF_DIR/server.crt

echo "(*) Change owner of key and cert to postgres user"
chown postgres $CONF_DIR/server.key && chmod 600 $CONF_DIR/server.key
chown postgres $CONF_DIR/server.crt && chmod 600 $CONF_DIR/server.crt

### Setup server certificates
TEMP_DIR=/tmp
DN="/CN=localhost/OU=defaultServer/O=ibm/C=us"
PASSWORD=liberty

echo "(*) Create client certificate signing request"
openssl req -new -text -passout pass:$PASSWORD -subj $DN -out $TEMP_DIR/client.req -keyout $TEMP_DIR/privkey.pem 2>/dev/null && echo "Successful" || echo "Failure"

echo "(*) Create client signing key"
openssl rsa -in $TEMP_DIR/privkey.pem -passin pass:$PASSWORD -out $TEMP_DIR/client.key

echo "(*) Create client certificate" 
openssl req -x509 -days 3650 -in $TEMP_DIR/client.req -text -key $TEMP_DIR/client.key -out $TEMP_DIR/client.crt

echo "(*) Create client keystore with trusted server certificate"
openssl pkcs12 -export -in $TEMP_DIR/client.crt          -inkey $TEMP_DIR/client.key -name user \
					   -out $TEMP_DIR/clientKeystore.p12 -passout pass:$PASSWORD

echo "(*) Debug PKCS12 contents"
openssl pkcs12 -passin pass:$PASSWORD -in $TEMP_DIR/clientKeystore.p12 -passout pass:$PASSWORD -info

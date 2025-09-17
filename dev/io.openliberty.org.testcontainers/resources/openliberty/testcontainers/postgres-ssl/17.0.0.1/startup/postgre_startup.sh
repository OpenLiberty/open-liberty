#!/bin/sh

### Setup client certificates
TEMP_DIR=/tmp

echo "(*) Create client certificate signing request"
openssl req -x509 -config $TEMP_DIR/client.conf -days 3650 -nodes -keyout $TEMP_DIR/client.key -out $TEMP_DIR/client.crt 2>/dev/null && echo "Successful" || echo "Failure"

echo "(*) Debug client certificate files"
cat $TEMP_DIR/client.key
cat $TEMP_DIR/client.crt

### Setup server certificates
CONF_DIR=/var/lib/postgresql

echo "(*) Create server certificate signing request"
openssl req -x509 -config $TEMP_DIR/server.conf -days 3650 -nodes -keyout $CONF_DIR/server.key -out $CONF_DIR/server.crt 2>/dev/null && echo "Successful" || echo "Failure"

echo "(*) Debug server certificate files"
cat $CONF_DIR/server.key
cat $CONF_DIR/server.crt

echo "(*) Change owner of key and cert to postgres user"
chown postgres $CONF_DIR/server.key && chmod 600 $CONF_DIR/server.key
chown postgres $CONF_DIR/server.crt && chmod 600 $CONF_DIR/server.crt

### Setup keystore
PASSWORD=liberty

echo "(*) Create client keystore with trusted server certificate"
openssl pkcs12 -export -in $TEMP_DIR/client.crt          -inkey $TEMP_DIR/client.key -name user \
					   -out $TEMP_DIR/clientKeystore.p12 -passout pass:$PASSWORD

echo "(*) Debug PKCS12 contents"
openssl pkcs12 -passin pass:$PASSWORD -in $TEMP_DIR/clientKeystore.p12 -passout pass:$PASSWORD -info

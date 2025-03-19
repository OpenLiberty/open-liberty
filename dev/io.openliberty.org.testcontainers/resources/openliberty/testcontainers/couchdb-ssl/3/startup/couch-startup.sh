#!/bin/sh

### Setup server certificates
CONF_DIR=/etc/couchdb/cert
DN="/CN=localhost/OU=couchdb"
PASSWORD=liberty

mkdir -p $CONF_DIR

echo "(*) Create server certificate signing request"
openssl req -new -text -passout pass:$PASSWORD -subj $DN -out $CONF_DIR/server.req -keyout $CONF_DIR/privkey.pem 2>/dev/null && echo "Successful" || echo "Failure"

echo "(*) Create server signing key"
openssl rsa -in $CONF_DIR/privkey.pem -passin pass:$PASSWORD -out $CONF_DIR/server.key

echo "(*) Create server certificate" 
openssl req -x509 -days 3650 -in $CONF_DIR/server.req -text -key $CONF_DIR/server.key -out $CONF_DIR/server.crt

echo "(*) Change read access of certificate files"
chmod 644 $CONF_DIR/server.crt && ls -la $CONF_DIR/server.crt
chmod 644 $CONF_DIR/server.key && ls -la $CONF_DIR/server.key

#!/bin/sh

### Setup server certificates
CONF_DIR=/etc/couchdb/cert

mkdir -p $CONF_DIR

echo "(*) Create server certificate signing request"
openssl req -x509 -config /tmp/csr.conf -days 3650 -nodes -keyout $CONF_DIR/server.key -out $CONF_DIR/server.crt 2>/dev/null && echo "Successful" || echo "Failure"

echo "(*) Debug certificate files"
cat $CONF_DIR/server.key
cat $CONF_DIR/server.crt

echo "(*) Change read access of certificate files"
chmod 644 $CONF_DIR/server.crt && ls -la $CONF_DIR/server.crt
chmod 644 $CONF_DIR/server.key && ls -la $CONF_DIR/server.key

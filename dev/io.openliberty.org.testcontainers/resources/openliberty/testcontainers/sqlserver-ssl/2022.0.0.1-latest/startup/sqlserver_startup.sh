#!/bin/sh

### Setup server certificates
CONF_DIR=/usr/ssl

mkdir -p $CONF_DIR/certs

echo "(*) Create server certificate signing request"
openssl req -x509 -config /tmp/csr.conf -days 3650 -nodes -keyout $CONF_DIR/server.key -out $CONF_DIR/certs/server.crt 2>/dev/null && echo "Successful" || echo "Failure"

echo "(*) Debug certificate files"
cat $CONF_DIR/server.key
cat $CONF_DIR/certs/server.crt

echo "(*) Change owner of certificate files to mssql"
chown mssql $CONF_DIR/certs/server.crt && ls -la $CONF_DIR/certs/server.crt
chown mssql $CONF_DIR/server.key && ls -la $CONF_DIR/server.key

# Note client does not have a key/cert, exchange is one way server -> client
PASSWORD=WalletPasswd123

echo "(*) Create truststore for client"
openssl pkcs12 -export -nokeys \
               -in $CONF_DIR/certs/server.crt \
               -out /tmp/truststore.p12 -passout pass:$PASSWORD

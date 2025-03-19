#!/bin/sh

### Setup server certificates
CONF_DIR=/usr/ssl
DN="/CN=localhost/OU=sqlserver"
PASSWORD=WalletPasswd123

mkdir -p $CONF_DIR/certs

echo "(*) Create server certificate signing request"
openssl req -new -text -passout pass:$PASSWORD -subj $DN -out $CONF_DIR/server.req -keyout $CONF_DIR/privkey.pem 2>/dev/null && echo "Successful" || echo "Failure"

echo "(*) Create server signing key"
openssl rsa -in $CONF_DIR/privkey.pem -passin pass:$PASSWORD -out $CONF_DIR/server.key

echo "(*) Create server certificate" 
openssl req -x509 -days 3650 -in $CONF_DIR/server.req -text -key $CONF_DIR/server.key -out $CONF_DIR/certs/server.crt

echo "(*) Change owner of certificate files to mssql"
chown mssql $CONF_DIR/certs/server.crt && ls -la $CONF_DIR/certs/server.crt
chown mssql $CONF_DIR/server.key && ls -la $CONF_DIR/server.key

# Note client does not have a key/cert, exchange is one way server -> client
echo "(*) Create truststore for client"
openssl pkcs12 -export -nokeys \
               -in $CONF_DIR/certs/server.crt \
               -out /tmp/truststore.p12 -passout pass:$PASSWORD

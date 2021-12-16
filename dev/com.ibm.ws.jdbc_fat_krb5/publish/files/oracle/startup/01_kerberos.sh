#!/bin/sh

echo "Username:"
whoami

echo "Initialize oracle user(s)"
echo password | okinit -old -f ORACLEUSR

echo "List principles in key table: "
oklist -k -t /etc/krb5.keytab

echo "Make credential cache accessible"
chmod 777 /tmp/krb5cc_
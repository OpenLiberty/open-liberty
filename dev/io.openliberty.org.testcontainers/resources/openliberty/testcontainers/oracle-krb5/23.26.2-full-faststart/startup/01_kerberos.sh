#!/bin/sh

echo "Username:"
whoami

echo "Initialize oracle user(s)"
echo "password" | okinit ORACLEUSR

echo "List principles in key table: "
oklist -k -t /etc/krb5.keytab

echo "Make credential cache accessible"
chmod 777 /tmp/krb5cc_

echo "List credential cache"
oklist -e -f -a

# To manually test a local connection from on the Oracle Free container, you can do:
# docker exec -it <oracle-container-id> /bin/bash
# 
# -- Login as os user (oracle)
# sqlplus /@FREE 
#
# -- Login as application user (ORACLEUSR)
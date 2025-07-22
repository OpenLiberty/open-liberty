#!/bin/sh

echo "Username:"
whoami

echo "Adding static keytab entries for postgres/postgresql@EXAMPLE.COM and pguser@EXAMPLE.COM using password ${KRB5_PASS}"
printf 'add_entry -password -p postgres/postgresql@EXAMPLE.COM -k 1 -e aes256-cts\n'"${KRB5_PASS}"'\nwkt /etc/krb5.keytab' | ktutil
printf 'add_entry -password -p pguser@EXAMPLE.COM -k 1 -e aes256-cts\n'"${KRB5_PASS}"'\nwkt /etc/krb5.keytab' | ktutil

echo "Adding dynamic keytab entry for postgres/${EXTERNAL_HOSTNAME}@EXAMPLE.COM using password ${KRB5_PASS}"
printf 'add_entry -password -p postgres/'"${EXTERNAL_HOSTNAME}"'@EXAMPLE.COM -k 0 -e aes256-cts\n'"${KRB5_PASS}"'\nwkt /etc/krb5.keytab' | ktutil

echo "Initialize user(s)"
kinit -k -t /etc/krb5.keytab postgres/${EXTERNAL_HOSTNAME}@EXAMPLE.COM
kinit -k -t /etc/krb5.keytab pguser@EXAMPLE.COM

echo "List principles in key table: "
klist -k -t /etc/krb5.keytab

echo "List credential cache: "
klist -e -f -a 

echo "Creating kerberos role"
psql --user=nonkrbuser --dbname=pg -c 'CREATE ROLE "pguser@EXAMPLE.COM" SUPERUSER LOGIN'

# To manually test a local connection from on the PostgreSQL container, you can do:
# docker exec -it --user postgres <pg-container-id> /bin/bash
# > psql -U "pguser@EXAMPLE.COM" -h postgresql pg

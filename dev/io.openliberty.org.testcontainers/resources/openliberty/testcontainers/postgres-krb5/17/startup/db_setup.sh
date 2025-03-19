#!/bin/sh

echo "Username:"
whoami

echo "Adding dynamic keytab entry for postgres/${EXTERNAL_HOSTNAME}@EXAMPLE.COM"
printf 'add_entry -password -p postgres/'"${EXTERNAL_HOSTNAME}"'@EXAMPLE.COM -k 0 -e aes256-cts\npassword\nwkt /etc/krb5.keytab' | ktutil

echo "Initialize user(s)"
kinit -k -t /etc/krb5.keytab postgres/${EXTERNAL_HOSTNAME}@EXAMPLE.COM
kinit -k -t /etc/krb5.keytab pguser@EXAMPLE.COM

echo "List principles in key table: "
klist -k -t /etc/krb5.keytab

echo "Creating kerberos role"
psql --user=nonkrbuser --dbname=pg -c 'CREATE ROLE "pguser@EXAMPLE.COM" SUPERUSER LOGIN'

# To manually test a local connection from on the PostgreSQL container, you can do:
# exec -it <pg-container-id> bash
# su postgres
# psql -U "pguser@EXAMPLE.COM" -h postgresql pg

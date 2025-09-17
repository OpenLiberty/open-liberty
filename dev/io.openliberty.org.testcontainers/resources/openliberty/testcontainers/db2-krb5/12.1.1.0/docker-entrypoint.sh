#!/bin/sh

if [ -z ${KRB5_REALM} ]; then
    echo "No KRB5_REALM Provided. Exiting ..."
    exit 1
fi

if [ -z ${KRB5_KDC} ]; then
    echo "No KRB5_KDC Provided. Exiting ..."
    exit 1
fi

if [ -z ${KRB5_ADMINSERVER} ]; then
    echo "KRB5_ADMINSERVER provided. Using ${KRB5_KDC} in place."
    KRB5_ADMINSERVER=${KRB5_KDC}
fi

if [ -z ${DB2_KRB5_PRINCIPAL} ]; then
    echo "No DB2_KRB5_PRINCIPAL Provided. Exiting ..."
    exit 1
fi

echo "(*) Creating Krb5 Client Configuration"

cat <<EOT > /etc/krb5.conf
[libdefaults]
 dns_lookup_realm = false
 ticket_lifetime = 24h
 renew_lifetime = 7d
 forwardable = true
 rdns = false
 default_realm = ${KRB5_REALM}
 
[realms]
 ${KRB5_REALM} = {
    kdc = ${KRB5_KDC}:99
    admin_server = ${KRB5_ADMINSERVER}
 }
EOT

chmod 777 /etc/krb5.conf
cat /etc/krb5.conf

## NOTE: The kerberos server has to have been created and started before these files are created

echo "(*) Creating server keytab file at /etc/krb5.keytab"
printf 'add_entry -password -p db2srvc@EXAMPLE.COM -k 1 -e aes256-cts\npassword\nwrite_kt /etc/krb5.keytab' | ktutil
printf 'add_entry -password -p db2inst1@EXAMPLE.COM -k 1 -e aes256-cts\npassword\nwrite_kt /etc/krb5.keytab' | ktutil
printf 'read_kt /etc/krb5.keytab\nlist\nq' | ktutil
chmod 777 /etc/krb5.keytab

echo "(*) Creating client keytab file at /tmp/krb5.keytab"
printf 'add_entry -password -p dbuser@EXAMPLE.COM -k 1 -e aes256-cts\npassword\nwrite_kt /tmp/krb5.keytab' | ktutil
printf 'read_kt /tmp/krb5.keytab\nlist\nq' | ktutil
chmod 777 /tmp/krb5.keytab

echo "(*) Running setup_db2_instance.sh"
/bin/sh -c /var/db2_setup/lib/setup_db2_instance.sh
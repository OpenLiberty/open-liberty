#!/bin/sh

# This is a setup script we want to run inside the database container after databse setup
# but before database start.  This script creates the kerb5.conf file

if [ -z ${KRB5_REALM} ]; then
    echo "No KRB5_REALM Provided. Exiting ..."
    exit 1
fi

if [ -z ${KRB5_KDC} ]; then
    echo "No KRB5_KDC Provided. Exting ..."
    exit 1
fi

if [ -z ${KRB5_ADMINSERVER} ]; then
    echo "KRB5_ADMINSERVER provided. Using ${KRB5_KDC} in place."
    KRB5_ADMINSERVER=${KRB5_KDC}
fi

echo "Creating Krb5 Client Configuration"

cat <<EOT > /etc/krb5.conf
[libdefaults]
    dns_lookup_realm = false
    ticket_lifetime = 24h
    renew_lifetime = 7d
    forwardable = true
    rdns = false
    ccache_type = 4
    allow_weak_crypto = true
    ignore_acceptor_hostname = true
    kdc_timesync = 0
    default_realm = ${KRB5_REALM}
 
[realms]
    ${KRB5_REALM} = {
        kdc = ${KRB5_KDC}:99
        admin_server = ${KRB5_ADMINSERVER}
    }

[domain_realm]
        .example.com = EXAMPLE.COM
        example.com = EXAMPLE.COM
EOT

echo "Make configuration and key table accessible"
# Not ideal security paractice but only used for testing
# In production these files should be restricted to user "oracle"
chmod 777 /etc/krb5.conf
chmod 777 /etc/krb5.keytab
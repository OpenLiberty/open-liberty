#!/bin/sh

if [ -z ${KRB5_REALM} ]; then
    echo "No KRB5_REALM Provided. Using default, EXAMPLE.COM"
	KRB5_REALM="EXAMPLE.COM"
fi

if [ -z ${KRB5_KDC} ]; then
    echo "No KRB5_KDC Provided. Using default, kerberos"
	KRB5_KDC="kerberosKDC"
fi

if [ -z ${KRB5_ADMINSERVER} ]; then
    echo "No KRB5_ADMINSERVER provided. Using ${KRB5_KDC} in place."
    KRB5_ADMINSERVER=${KRB5_KDC}
fi

if [ -z ${EXTERNAL_HOSTNAME} ]; then
    echo "No EXTERNAL_HOSTNAME provided. Using localhost in place."
    EXTERNAL_HOSTNAME="localhost"
fi

if [ -z ${KRB5_PWD} ]; then
    echo "No Password for kdb provided ... Using \"pwd\""
    KRB5_PWD="pwd"
    echo "Using Password ${KRB5_PWD}"
fi

echo "Creating Krb5 Client Configuration"

cat <<EOT > /etc/krb5.conf
[libdefaults]
 dns_lookup_realm = false
 ticket_lifetime = 24h
 renew_lifetime = 7d
 forwardable = true
 rdns = false
 default_realm = ${KRB5_REALM}
 default_tkt_enctypes = aes256-cts-hmac-sha1-96
 default_tgs_enctypes = aes256-cts-hmac-sha1-96
 
 [realms]
 ${KRB5_REALM} = {
    kdc = ${KRB5_KDC}:88
    admin_server = ${KRB5_ADMINSERVER}
 }
EOT

cat /etc/krb5.conf

rm -rf /var/lib/krb5kdc/principal*

if [ ! -f "/var/lib/krb5kdc/principal" ]; then

    echo "No Krb5 Database Found. Creating One with provided information"

    echo "Creating KDC Configuration"

cat <<EOT > /var/lib/krb5kdc/kdc.conf
[kdcdefaults]
    kdc_listen = 88
    kdc_tcp_listen = 88
    
[realms]
    ${KRB5_REALM} = {
        kadmin_port = 749
        max_life = 12h 0m 0s
        max_renewable_life = 7d 0h 0m 0s
        master_key_type = aes256-cts
        supported_enctypes = aes256-cts:normal
        default_principal_flags = +preauth
    }
    
[logging]
    kdc = FILE:/var/log/krb5kdc.log
    admin_server = FILE:/var/log/kadmin.log
    default = FILE:/var/log/krb5lib.log
EOT

cat /var/lib/krb5kdc/kdc.conf

echo "Creating Default Policy - Admin Access to */admin"
echo "*/admin@${KRB5_REALM} *" > /var/lib/krb5kdc/kadm5.acl
echo "*/service@${KRB5_REALM} aci" >> /var/lib/krb5kdc/kadm5.acl

echo "Creating Temp pass file"
cat <<EOT > /etc/KRB5_PWD
${KRB5_PWD}
${KRB5_PWD}
EOT

    echo "Creating krb5util database"
    kdb5_util create -r ${KRB5_REALM} < /etc/KRB5_PWD
    rm /etc/KRB5_PWD

    echo "Creating Admin Account"
    kadmin.local -q "addprinc -pw ${KRB5_PWD} admin/admin@${KRB5_REALM}"
	
	echo "Creating ldap/$EXTERNAL_HOSTNAME Account"
    kadmin.local -q "addprinc -pw ${KRB5_PWD} ldap/${EXTERNAL_HOSTNAME}@${KRB5_REALM}"
    
    echo "Creating principal for ldap user1"
    kadmin.local -q "addprinc -pw ${KRB5_PWD} user1@${KRB5_REALM}"

    echo "Creating principal for ldap user17"
    kadmin.local -q "addprinc -pw max_secret user17@${KRB5_REALM}"
    
    echo "KERB SETUP COMPLETE"

fi


/usr/bin/supervisord -c /etc/supervisord.conf
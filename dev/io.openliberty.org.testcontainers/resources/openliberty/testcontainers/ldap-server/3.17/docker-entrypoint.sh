#!/bin/sh
if [ -z ${DOMAIN} ]; then
    echo "No DOMAIN Provided. Using default: DOMAIN=\"dc=example,dc=com\""
	echo "Example: -e DOMAIN=\"dc=example,dc=com\""
	DOMAIN="dc=example,dc=com"
fi

if [ -z ${KDC_HOSTNAME} ]; then
    echo "No KDC_HOSTNAME Provided. Using default: kerberos"
	echo "Example: -e KDC_HOSTNAME=\"kerberosKDC\""
	KDC_HOSTNAME="kerberosKDC"
fi

if [ -z ${EXTERNAL_HOSTNAME} ]; then
    echo "No EXTERNAL_HOSTNAME provided. Using localhost in place."
    EXTERNAL_HOSTNAME=localhost
fi

echo "KDC_HOSTNAME=$KDC_HOSTNAME"
echo "EXTERNAL_HOSTNAME=$EXTERNAL_HOSTNAME"

echo "/etc/hosts:"
cat /etc/hosts

echo "Configuring openLDAP Server"
echo "delete old files..."
rm -rf /etc/openldap/slapd.d
rm -rf /var/lib/openldap/openldap-data/*
echo "Creating slapd.d dir"
ls /etc/openldap/
install -m 755 -o ldap -g ldap -d /etc/openldap/slapd.d
echo "All OpenLDAP server config files are located in /etc/openldap/slapd.d"
ls /etc/openldap/

echo "setting ldap hostname in ldap.conf: ${EXTERNAL_HOSTNAME}"
sed -i~ -e "s/LDAP_HOSTNAME_NOT_SET_IN_LDAP_CONF/${EXTERNAL_HOSTNAME}/" /etc/openldap/ldap.conf


echo "Importing slapd configuration..."
slapadd -n 0 -F /etc/openldap/slapd.d -l /etc/openldap/slapd.ldif
 
#TODO: remove if able
echo "Importing kerberos schema"
slapadd -n 0 -l /etc/kerberos.openldap.ldif
 
echo "Adding ldif data from: /etc/full_example_com.ldif"
slapadd -n 1 -F /etc/openldap/slapd.d -l /etc/full_example_com.ldif 
chown -R ldap:ldap /etc/openldap/slapd.d/*
 
echo "Configuring slapd service..."
install -m 755 -o ldap -g ldap -d /var/lib/openldap/run
 
chown -R ldap:ldap /var/lib/openldap/openldap-data/

 
 ####### KERBEROS KDC CONFIG #######
 
echo "Creating Krb5 Client Configuration"
cat <<EOT > /etc/krb5.conf
[libdefaults]
 dns_lookup_realm = false
 ticket_lifetime = 24h
 renew_lifetime = 7d
 forwardable = true
 rdns = false
 default_realm = EXAMPLE.COM
 default_tkt_enctypes = aes256-cts-hmac-sha1-96
 default_tgs_enctypes = aes256-cts-hmac-sha1-96
 
[realms]
 EXAMPLE.COM = {
    kdc = ${KDC_HOSTNAME}:88
    admin_server = ${KDC_HOSTNAME}
 }
 
[domain_realm]
 .${DOCKERHOST_DOMAIN} = EXAMPLE.COM
 ${DOCKERHOST_DOMAIN} = EXAMPLE.COM
 .example.com = EXAMPLE.COM
 example.com = EXAMPLE.COM
EOT

cat /etc/krb5.conf

echo "Starting ldap as root so kerberos can create its database"
slapd -h "ldap:/// ldapi:///" -u root -g root -d -1 >> "/etc/ldapstdout.log" 2>> "/etc/ldapstderr.log" &
sleep 4

chmod 777 /etc/krb5.keytab
chmod 777 /etc/krb5.conf

echo "Adding static keytab entry for user17@EXAMPLE.COM"
printf 'add_entry -password -p user17@EXAMPLE.COM -k 1 -e aes256-cts\nmax_secret\nwkt /etc/krb5.keytab' | ktutil

echo "Adding dynamic keytab entry for ldap/${EXTERNAL_HOSTNAME}@EXAMPLE.COM"
printf 'add_entry -password -p ldap/'"${EXTERNAL_HOSTNAME}"'@EXAMPLE.COM -k 1 -e aes256-cts\npwd\nwkt /etc/krb5.keytab' | ktutil

echo "Initialize(kinit) ldap SPN: ldap/$EXTERNAL_HOSTNAME@EXAMPLE.COM"
kinit -k -t /etc/krb5.keytab ldap/${EXTERNAL_HOSTNAME}@EXAMPLE.COM
echo "Initialize(kinit) user17@EXAMPLE.COM"
kinit -k -t /etc/krb5.keytab user17@EXAMPLE.COM

echo "List principles in key table: "
klist -k -t /etc/krb5.keytab

chmod 777 /etc/krb5.keytab

echo "klist"
klist

cp /tmp/krb5cc_0 /etc/user17.cc

echo "klist /etc/user17.cc"
klist /etc/user17.cc

cp /etc/krb5.keytab /etc/user17.keytab

echo "klist -k /etc/user17.keytab "
klist -k -t /etc/user17.keytab

chmod 777 /etc/krb5.keytab


echo "ldapsearch ${EXTERNAL_HOSTNAME}"
ldapsearch -LLL -Y GSSAPI -H ldap://${EXTERNAL_HOSTNAME} -s "base" -b "dc=example,dc=com"

echo "docker entry ldapsearch tests COMPLETE ---" >> "/etc/ldapstderr.log"

LDAP_PID=$(ps | grep slapd | awk 'NR==1 {print $1}')
echo "ldap pid: $LDAP_PID"=

echo "stop the ldap so it can be restarted using supervisord"
kill $LDAP_PID
echo "LDAP SERVER SETUP COMPLETE"

#Start the slapd(openLdap) process as service using supervisord
#supervisord will start the openLdap with this command:  slapd -h "ldap:/// ldapi:///" -u ldap -g ldap
/usr/bin/supervisord -c /etc/supervisord.conf

# test ldap with: ldapsearch -x -H ldap:/// -b "dc=example,dc=com" -D "cn=admin,dc=example,dc=com" -w "admin" (does not work without -H)
# test user17 with: ldapsearch -x -H ldap:/// -b "dc=example,dc=com" -D "uid=user17,dc=example,dc=com" -w "max_secret"
# check GSSAPI is supported: ldapsearch -x -H ldap:/// -s "base" -b "" -D "cn=admin,dc=example,dc=com" -w "admin" supportedSASLMechanisms
# use GSSAPI authentication: ldapsearch -LLL -Y GSSAPI -H ldap://localhost -s "base" -b "dc=example,dc=com"
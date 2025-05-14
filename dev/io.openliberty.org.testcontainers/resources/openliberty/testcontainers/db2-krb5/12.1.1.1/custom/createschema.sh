#!/bin/sh

echo "(*) Verify user"
whoami

echo "(*) Verify hostname"
hostname

# Execute as database user
sudo -i -u db2inst1 bash << EOF

echo "(*) Configure DB2 database manager to use Kerberos"
${DB2_ADMIN}/bin/db2 UPDATE DATABASE MANAGER CONFIGURATION USING CLNT_KRB_PLUGIN IBMkrb5 IMMEDIATE
${DB2_ADMIN}/bin/db2 UPDATE DATABASE MANAGER CONFIGURATION USING AUTHENTICATION KERBEROS IMMEDIATE

echo "(*) Log database manager configuration"
${DB2_ADMIN}/bin/db2 GET DATABASE MANAGER CONFIGURATION

echo "(*) Initialize kerberos user"
kinit -k -t /etc/krb5.keytab db2inst1

echo "(*) Debug keytab file"
klist -k -t /etc/krb5.keytab

echo "(*) Debug credential cache"
klist -e -f -a

# ${DB2_ADMIN}/adm/db2start
# ${DB2_ADMIN}/bin/db2 db2stop
# ${DB2_ADMIN}/bin/db2 db2start

# ${DB2_ADMIN}/bin/db2 CREATE DATABASE TESTDB

echo "SETUP SCRIPT COMPLETE"

EOF

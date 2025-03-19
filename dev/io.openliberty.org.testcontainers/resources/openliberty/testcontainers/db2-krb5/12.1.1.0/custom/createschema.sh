#!/bin/sh

source ${SETUPDIR?}/include/db2_constants
source ${SETUPDIR?}/include/db2_common_functions

echo "(*) Verify hostname"
sudo hostname db2
hostname

sudo -i -u db2inst1 bash << EOF

echo "(*) Configure DB2 database manager to use Kerberos"
db2 UPDATE DATABASE MANAGER CONFIGURATION USING CLNT_KRB_PLUGIN IBMkrb5
db2 UPDATE DATABASE MANAGER CONFIGURATION USING AUTHENTICATION KERBEROS

echo "(*) Log database manager configuration"
db2 GET DATABASE MANAGER CONFIGURATION

echo "(*) Initialize principal ${DB2INSTANCE?}"
touch /tmp/kinit.log && export KRB5_TRACE=/tmp/kinit.log
kinit -k -t /etc/krb5.keytab -c FILE:/tmp/krb5cc_1000 ${DB2INSTANCE?}
cat /tmp/kinit.log

echo "(*) Debug credential cache at /tmp/krb5cc_1000"
ls -la /tmp/krb5cc_1000
klist /tmp/krb5cc_1000

echo "(*) Debug keytab file at /etc/krb5.keytab"
ls -la /etc/krb5.keytab
klist -k /etc/krb5.keytab

echo "(*) Start database manager"
db2start

echo "(*) Restart DB2"
db2 db2stop
db2 db2start

EOF

echo "SETUP SCRIPT COMPLETE"

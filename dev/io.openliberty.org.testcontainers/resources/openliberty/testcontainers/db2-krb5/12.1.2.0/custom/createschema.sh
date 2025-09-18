#!/bin/sh

echo "(*) Verify user is root"
whoami

echo "(*) Change external hostname to db2"
sudo hostname db2
hostname

echo "(*) Create new user dbuser"
useradd -s /bin/bash dbuser

echo "(*) Persist env variables to user profile"
USERPROFILE=/database/config/db2inst1/sqllib/userprofile
echo "export DB2_KRB5_PRINCIPAL=${DB2_KRB5_PRINCIPAL}" >> $USERPROFILE
cat $USERPROFILE

# Execute as database user
sudo -i -u db2inst1 bash << EOF

echo "(*) Configure DB2 database manager to use Kerberos"
db2 update dbm cfg using CLNT_KRB_PLUGIN IBMkrb5
db2 update dbm cfg using AUTHENTICATION KERBEROS

echo "(*) Debug keytab file"
klist -k -t /etc/krb5.keytab

echo "(*) Initialize kerberos user db2inst1/db2"
kinit -k -t /etc/krb5.keytab db2inst1/db2

echo "(*) Debug credential cache"
klist -e -f -a

echo "(*) Grant connect to dbuser"
db2 connect to TESTDB
db2 grant createtab, implicit_schema, connect on database to user dbuser
db2 grant use of tablespace USERSPACE1 to user dbuser
db2 terminate

echo "(*) Restart database"

$DB2_ADMIN/adm/db2start
db2 db2stop
db2 db2start

echo "(*) Log database manager configuration"
db2 get database manager configuration | grep -e KRB -e KERBEROS

echo "SETUP SCRIPT COMPLETE"

EOF

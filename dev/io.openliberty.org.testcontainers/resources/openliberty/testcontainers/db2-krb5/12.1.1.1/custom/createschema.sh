#!/bin/sh

echo "(*) Verify user is root"
whoami

echo "(*) Change external hostname to db2"
sudo hostname db2
hostname

# Execute as database user
sudo -i -u db2inst1 bash << EOF

echo "(*) Debug keytab file"
klist -k -t /etc/krb5.keytab

echo "(*) Initialize kerberos user db2inst1/db2"
kinit -k -t /etc/krb5.keytab db2inst1/db2

echo "(*) Debug credential cache"
klist -e -f -a

echo "(*) Configure DB2 database manager to use Kerberos"
db2 update dbm cfg using CLNT_KRB_PLUGIN IBMkrb5
db2 update dbm cfg using AUTHENTICATION KERBEROS

echo "(*) Restart database"
## Uncomment to debug the restart process
## db2trc on -f /tmp/db2restart.out

db2 db2stop
db2 db2start

## db2trc off
## db2trc fmt /tmp/db2restart.out /tmp/db2restart.txt

echo "(*) Log database manager configuration"
db2 get database manager configuration | grep -e KRB -e KERBEROS

echo "SETUP SCRIPT COMPLETE"

EOF

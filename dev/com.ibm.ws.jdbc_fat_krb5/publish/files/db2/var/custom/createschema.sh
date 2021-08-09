chmod 777 /etc/krb5.keytab
chmod 777 /opt/ibm/db2/V11.5/adm/db2start
hostname db2
sudo -i -u db2inst1 bash << EOF
export DB2_KRB5_PRINCIPAL=db2srvc@EXAMPLE.COM
/opt/ibm/db2/V11.5/bin/db2 UPDATE DATABASE MANAGER CONFIGURATION USING CLNT_KRB_PLUGIN IBMkrb5
/opt/ibm/db2/V11.5/bin/db2 UPDATE DATABASE MANAGER CONFIGURATION USING AUTHENTICATION KERBEROS

kinit -k -t /etc/krb5.keytab db2inst1
/opt/ibm/db2/V11.5/adm/db2start
/opt/ibm/db2/V11.5/bin/db2 db2stop
/opt/ibm/db2/V11.5/bin/db2 db2start

/opt/ibm/db2/V11.5/bin/db2 CREATE DATABASE TESTDB

echo "SETUP SCRIPT COMPLETE"
EOF
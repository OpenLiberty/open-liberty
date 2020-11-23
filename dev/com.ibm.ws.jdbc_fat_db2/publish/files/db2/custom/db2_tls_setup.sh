#!/bin/bash

echo "BEGIN SSH SETUP"
export PATH=/opt/ibm/db2/V11.5/bin/:$PATH

su - db2inst1 -c 'db2 update dbm cfg using DIAGLEVEL 2'
su - db2inst1 -c 'db2 update dbm cfg using SSL_SVR_KEYDB /certs/server.kdb'
su - db2inst1 -c 'db2 update dbm cfg using SSL_SVR_STASH /certs/server.sth'
su - db2inst1 -c 'db2 update dbm cfg using SSL_SVR_LABEL mylabel'
su - db2inst1 -c 'db2 update dbm cfg using ssl_svcename 50001'
su - db2inst1 -c 'db2set -i db2inst1 DB2COMM=SSL,TCPIP'
su - db2inst1 -c 'db2stop'
su - db2inst1 -c 'db2start'
  
echo "DB2 SSH SETUP DONE"
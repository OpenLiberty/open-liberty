#!/bin/bash

echo "BEGIN SSH SETUP"

echo "----------------------- update db2 configuration  ------------------------"

su - db2inst1 -c 'db2 update dbm cfg using DIAGLEVEL 2'
su - db2inst1 -c 'db2 update dbm cfg using SSL_SVR_KEYDB /certs/server.kdb'
su - db2inst1 -c 'db2 update dbm cfg using SSL_SVR_STASH /certs/server.sth'
su - db2inst1 -c 'db2 update dbm cfg using SSL_SVR_LABEL mylabel'
su - db2inst1 -c 'db2 update dbm cfg using SSL_VERSIONS TLSV12'
su - db2inst1 -c 'db2 update dbm cfg using ssl_svcename 50001'
su - db2inst1 -c 'db2set -i db2inst1 DB2COMM=SSL,TCPIP'

echo "-------------------------- restart db2 ----------------------------------"

su - db2inst1 -c 'db2stop'
su - db2inst1 -c 'db2start'

echo "-------------------------- show config ----------------------------------"

su - db2inst1 -c 'db2 get database manager configuration' | grep SSL

echo "DB2 SSH SETUP DONE"
#!/bin/sh

# This is a startup script we want to run inside the database container after database has been started.

if [ -z ${ORACLE_HOME} ]; then
    echo "No ORACLE_HOME variable.  Exiting..."
    exit 1
fi

echo "Creating server wallet and cert"
mkdir -p /u01/app/oracle/wallet
orapki wallet create -wallet "/u01/app/oracle/wallet" -pwd WalletPasswd123 -auto_login_local
orapki wallet add -wallet "/u01/app/oracle/wallet" -pwd WalletPasswd123 -dn "CN=localhost" -keysize 1024 -self_signed -validity 36500
orapki wallet display -wallet "/u01/app/oracle/wallet" -pwd WalletPasswd123
orapki wallet export -wallet "/u01/app/oracle/wallet" -pwd WalletPasswd123 -dn "CN=localhost" -cert /tmp/oracle-server-certificate.crt
cat /tmp/oracle-server-certificate.crt

echo "Create client wallet and cert"
mkdir -p /client/oracle/wallet
orapki wallet create -wallet "/client/oracle/wallet" -pwd WalletPasswd123 -auto_login_local
orapki wallet add -wallet "/client/oracle/wallet" -pwd WalletPasswd123 -dn "CN=localhost" -keysize 1024 -self_signed -validity 36500
orapki wallet display -wallet "/client/oracle/wallet" -pwd WalletPasswd123
orapki wallet export -wallet "/client/oracle/wallet" -pwd WalletPasswd123 -dn "CN=localhost" -cert /tmp/oracle-client-certificate.crt
cat /tmp/oracle-client-certificate.crt

echo "Exchange certs"
orapki wallet add -wallet "/client/oracle/wallet" -pwd WalletPasswd123 -trusted_cert -cert /tmp/oracle-server-certificate.crt
orapki wallet display -wallet "/client/oracle/wallet" -pwd WalletPasswd123
orapki wallet add -wallet "/u01/app/oracle/wallet" -pwd WalletPasswd123 -trusted_cert -cert /tmp/oracle-client-certificate.crt
orapki wallet display -wallet "/u01/app/oracle/wallet" -pwd WalletPasswd123
echo "Done exchanging certs"


# Step 6A: Configure Kerberos on the Client and on the Database Server
SQLNETORA="$ORACLE_HOME/network/admin/sqlnet.ora"
touch $SQLNETORA
# tried (BEQ, TCPS)
# tried (TCPS,NTS) and it failed to boot
echo "SQLNET.AUTHENTICATION_SERVICES=(BEQ,TCPS)" >> $SQLNETORA
echo "SSL_CLIENT_AUTHENTICATION=FALSE" >> $SQLNETORA
echo "SSL_VERSION=UNDETERMINED" >> $SQLNETORA
echo "WALLET_LOCATION=(SOURCE=(METHOD=FILE)(METHOD_DATA=(DIRECTORY=/u01/app/oracle/wallet)))" >> $SQLNETORA
echo "SQLNET.INBOUND_CONNECT_TIMEOUT=120" >> $SQLNETORA
echo "SSL_CIPHER_SUITES = (SSL_RSA_WITH_AES_256_CBC_SHA, SSL_RSA_WITH_3DES_EDE_CBC_SHA, SSL_DH_anon_WITH_3DES_EDE_CBC_SHA, SSL_DH_anon_WITH_RC4_128_MD5, SSL_DH_anon_WITH_DES_CBC_SHA)" >> $SQLNETORA
echo "UPDATED SQLNET.ORA"

cat > $ORACLE_HOME/network/admin/listener.ora << END_OF_LISTENER
# listener.ora Network Configuration File:

         SID_LIST_LISTENER =
           (SID_LIST =
             (SID_DESC =
               (SID_NAME = PLSExtProc)
               (ORACLE_HOME = /opt/oracle/product/18c/dbhomeXE)
               (PROGRAM = extproc)
             )
           )

         LISTENER =
           (DESCRIPTION_LIST =
             (DESCRIPTION =
               (ADDRESS = (PROTOCOL = IPC)(KEY = EXTPROC_FOR_XE))
               (ADDRESS = (PROTOCOL = TCP)(HOST = 0.0.0.0)(PORT = 1521))
               (ADDRESS = (PROTOCOL = TCPS)(HOST = 0.0.0.0)(PORT = 2484))
             )
           )
           
         DEFAULT_SERVICE_LISTENER = (XE)
         INBOUND_CONNECT_TIMEOUT_LISTENER=110
         SSL_CLIENT_AUTHENTICATION=FALSE
END_OF_LISTENER

# For some reason this oracle image does not allow user 'oracle' to run the oracle process *sigh*
chmod 6751 $ORACLE_HOME/bin/oracle

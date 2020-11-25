#!/bin/sh

# This is a startup script we want to run inside the database container after database has been started.

if [ -z ${ORACLE_HOME} ]; then
    echo "No ORACLE_HOME variable.  Exiting..."
    exit 1
fi

# Step 6A: Configure Kerberos on the Client and on the Database Server
SQLNETORA="$ORACLE_HOME/network/admin/sqlnet.ora"
touch $SQLNETORA
# Tell oracle to use kerberos for authentication
echo "SQLNET.AUTHENTICATION_SERVICES=(BEQ, TCPS)" >> $SQLNETORA
echo "SSL_CLIENT_AUTHENTICATION=FALSE" >> $SQLNETORA
echo "SSL_VERSION=UNDETERMINED" >> $SQLNETORA
#echo "WALLET_LOCATION=(SOURCE=(METHOD=FILE)(METHOD_DATA=(DIRECTORY=/server/wallet/path)))" >> $SQLNETORA

# Increase inbround connection timeout in-case response from kerberos is slow
echo "SQLNET.INBOUND_CONNECT_TIMEOUT=120" >> $SQLNETORA
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
             )
           )
           
         LISTENER = (ADDRESS_LIST=
           (ADDRESS=(PROTOCOL=tcps)(HOST=0.0.0.0)(PORT=2484))
         )

         DEFAULT_SERVICE_LISTENER = (XE)
         INBOUND_CONNECT_TIMEOUT_LISTENER=110
         SSL_CLIENT_AUTHENTICATION=FALSE
END_OF_LISTENER
#echo "WALLET_LOCATION=(SOURCE=(METHOD=FILE)(METHOD_DATA=(DIRECTORY=/server/wallet/path)))" >> $LISTENERORA

# For some reason this oracle image does not allow user 'oracle' to run the oracle process *sigh*
chmod 6751 $ORACLE_HOME/bin/oracle

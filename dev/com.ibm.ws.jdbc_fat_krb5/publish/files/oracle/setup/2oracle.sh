#!/bin/sh

# This is a startup script we want to run inside the database container after database has been started.
# This script will do all the oracle configuration steps needed to enable Kerberos Authentication

if [ -z ${ORACLE_HOME} ]; then
    echo "No ORACLE_HOME variable.  Exiting..."
    exit 1
fi

if [ -z ${ORACLE_ADMIN} ]; then
    echo "No ORACLE_ADMIN variable.  Exiting..."
    exit 1
fi

# Step 6A: Configure Kerberos on the Client and on the Database Server
SQLNETORA="$ORACLE_ADMIN/sqlnet.ora"
touch $SQLNETORA
    # Set authentication service we want to use (Kerberos v.5)
    echo "Setup sqlnet.ora authentication servies"
    # Tell oracle to use kerberos for authentication
    echo "SQLNET.AUTHENTICATION_SERVICES=(BEQ, KERBEROS5)" >> $SQLNETORA
    # Use the service name oracle (kerberos service name NOT ORACLE SERVICE NAME)
    echo "SQLNET.AUTHENTICATION_KERBEROS5_SERVICE=XE" >> $SQLNETORA
    echo "UPDATED SQLNET.ORA"

# Step 6B: Set the Initialization Parameters
INITORA="$ORACLE_HOME/dbs/init.ora"
touch $INITORA
    echo "Setup init.ora username parameters"
    # No longer need to use OPS$ infront of externally authenticated usernames
    echo 'OS_AUTHENT_PREFIX=""' >> $INITORA
    echo "UPDATED INIT.ORA"

#Step 6C: Set sqlnet.ora Parameters (Optional)
# These are kerberos specific parameters that oracle says are not required
# but, if we are running into trouble adding these in may help
    echo "Setup sqlnet.ora kerberos settings"
    echo "SQLNET.KERBEROS5_CC_NAME=/tmp/krb5cc_" >> $SQLNETORA
    echo "SQLNET.KERBEROS5_CLOCKSKEW=5000" >> $SQLNETORA
    echo "SQLNET.KERBEROS5_CONF=/etc/krb5.conf" >> $SQLNETORA
    echo "SQLNET.KERBEROS5_KEYTAB=/etc/krb5.keytab" >> $SQLNETORA
    #echo "SQLNET.KERBEROS5_REALMS=/krb5/krb.realms" >> $SQLNETORA
    echo "SQLNET.KERBEROS5_CONF_MIT=TRUE" >> $SQLNETORA
    #Increase inbround connection timeout in-case response from kerberos is slow
    echo "SQLNET.INBOUND_CONNECT_TIMEOUT=120" >> $SQLNETORA
    echo "UPDATED SQLNET.ORA"

LISTENERORA="$ORACLE_ADMIN/listener.ora"
touch $LISTENERORA
    echo "INBOUND_CONNECT_TIMEOUT_LISTENER=110" >> $LISTENERORA

# For some reason this oracle image does not allow user 'oracle' to run the oracle process *sigh*
chmod 6751 $ORACLE_HOME/bin/oracle
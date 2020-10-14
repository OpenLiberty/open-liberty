/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.spi;

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LazyEnlistableConnectionManager;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.SharingViolationException;
import javax.security.auth.Subject;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import com.ibm.ws.ejbcontainer.fat.rar.core.AdapterUtil;
import com.ibm.ws.ejbcontainer.fat.rar.core.ConnectionEventSender;
import com.ibm.ws.ejbcontainer.fat.rar.core.Reassociateable;
import com.ibm.ws.ejbcontainer.fat.rar.jdbc.JdbcConnection;

/**
 * javax.resource.spi.ManagedConnection impl class. <p>
 *
 * Implementing ConnectionEventSender can make ManagedConnectionImpl object
 * directly sends events to listeners. This is only used to test how
 * application server (J2C, Transaction) deals with the valid or invalid
 * combination of event sequences. In all invalid case, an appropriate
 * exception is expected. We cannot use process***Event() method since
 * they are also changing the object's states.<p>
 */
public class ManagedConnectionImpl implements ManagedConnection, ConnectionEventSender {
    private final static String CLASSNAME = ManagedConnectionImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Physical connection object. */
    protected Connection sqlConn; // LIDB2110-69

    /** Physical pooledConnection object, may be an XAConnection object too. */
    protected PooledConnection poolConn; // LIDB2110-69

    /** List of currently open inUse and Free handles on this MC. */
    protected ArrayList handlesInUse;

    /** Event listeners. */
    private ConnectionEventListener[] ivEventListeners;

    /** Number of the event listeners. */
    private int numListeners;

    /** Managed connection factory object. */
    ManagedConnectionFactoryImpl mcf;

    /** Local transaction object. */
    private LocalTransaction localTran;

    /** XA resource object. */
    protected XAResource xares; // LIDB2110-69

    /** Connection request info object. */
    private ConnectionRequestInfoImpl cri;

    /** Subject object. */
    private Subject subject;

    /** LogWriter */
    private final PrintWriter logWriter;

    /** Tracks the current transaction state for this MC. */
    StateManager stateMgr;

    /** Indicates if the Connection supports two phase commit. */
    protected boolean is2Phase; // LIDB2110-69

    boolean isLazyEnlisted;

    /** Indicates whether we have detected a fatal Connection error on this MC. */
    private boolean connectionErrorDetected;

    /** Indicates whether we are currently cleaning up handles. */
    protected boolean cleaningUpHandles;

    // Connection attributes
    private int isolevel, defaultIsolevel;
    private Boolean readOnly, defaultReadOnly;
    private Map typeMap, defaultTypeMap;
    private String catalog, defaultCatalog;
    private boolean autoCommit, defaultAutoCommit, currentAutoCommit;

    private final Class currClass = ManagedConnectionImpl.class;

    /**
     * Constructor
     */
    public ManagedConnectionImpl(ManagedConnectionFactoryImpl mcf, PooledConnection pconn, Connection conn, Subject sub,
                                 ConnectionRequestInfoImpl cxRequestInfo) throws ResourceException {
        svLogger.entering(CLASSNAME, "<init>", new Object[] { mcf, conn, cxRequestInfo, sub, cxRequestInfo });
        this.sqlConn = conn;
        this.poolConn = pconn;
        this.mcf = mcf;
        this.cri = cxRequestInfo;
        this.subject = sub;
        is2Phase = poolConn instanceof XAConnection;

        if (subject != null)
            subject = copySubject();

        handlesInUse = new ArrayList(13);

        // LIDB???? - Change ConnectionEventListener
        ivEventListeners = new ConnectionEventListener[13];
        numListeners = 0;

        logWriter = mcf.getLogWriter();
        stateMgr = new StateManager();
        readOnly = cri.ivReadOnly;

        try {
            defaultAutoCommit = autoCommit = sqlConn.getAutoCommit();

            // d156456, set connection properties.
            setTypeMap(defaultTypeMap = cri.getTypeMap());
            setCatalog(defaultCatalog = cri.getCatalog());
            setTransactionIsolation(defaultIsolevel = cri.getIsolationLevel());
            defaultReadOnly = cri.isReadOnly();
            if (defaultReadOnly != null)
                setReadOnly(defaultReadOnly.booleanValue());

            svLogger.info("autoCommit = "
                          + autoCommit
                          + "\t TypeMap = "
                          + typeMap
                          + "\t Catalog = "
                          + catalog
                          + "\t Isolation = "
                          + AdapterUtil.getIsolationLevelString(isolevel)
                          + "\t isReadOnly = "
                          + readOnly);
            svLogger.info("default autoCommit = "
                          + defaultAutoCommit
                          + "\t default TypeMap = "
                          + defaultTypeMap
                          + "\t default Catalog = "
                          + defaultCatalog
                          + "\t default Isolation = "
                          + AdapterUtil.getIsolationLevelString(defaultIsolevel)
                          + "\t default isReadOnly = "
                          + defaultReadOnly);
            // d155456 end.
        } catch (SQLException sqle) {
            svLogger.info("Cannot get connection attributes");
            throw new ResourceException(sqle.getMessage());
        }
        svLogger.exiting(CLASSNAME, "<init>", this);
    }

    /**
     * @see javax.resource.spi.ManagedConnection#getConnection(Subject, ConnectionRequestInfo)
     */
    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        svLogger.entering(CLASSNAME, "getConnection", new Object[] { this, cxRequestInfo });

        // @alvinso.1
        if (AdapterUtil.getSharingViolation()) {
            svLogger.exiting(CLASSNAME, "getConnection - throwing SharingViolationException for testing purpose.", getTransactionStateAsString());
            throw new SharingViolationException("Tesing of SharingViolationException.");
        }

        // if you aren't in a valid state when doing getConnection, you can't get a connection
        // from this MC
        int transactionState = stateMgr.transtate;
        if ((transactionState != StateManager.NO_TRANSACTION_ACTIVE)
            && (transactionState != StateManager.GLOBAL_TRANSACTION_ACTIVE)
            && (transactionState != StateManager.LOCAL_TRANSACTION_ACTIVE)) {

            svLogger.exiting(CLASSNAME, "getConnection - bad transaction state, throwing exception", getTransactionStateAsString());
            String message = "Operation 'getConnection' is not permitted for transaction state: " + getTransactionStateAsString();
            throw new ResourceException(message);
        }

        // We Only support JDBC handle here
        Object handle = new JdbcConnection(this, sqlConn);
        // Only synchronize operations on handlesInUse. [d128891]
        synchronized (this) {
            handlesInUse.add(handle);
        }

        svLogger.exiting(CLASSNAME, "getConnection", handle);
        return handle;
    }

    /**
     * @see javax.resource.spi.ManagedConnection#destroy()
     */
    @Override
    public void destroy() throws ResourceException {
        svLogger.entering(CLASSNAME, "destroy", this);

        // Added by gburli - Begin: 06/08/04
        // Adding the MC to RA for verification in the testcase
        // All the invalid MCs will be destroyed here
        // Do this only if we want to test ValidatingMCF test cases.
        if (AdapterUtil.getInvalidConnFlag() != AdapterUtil.DEFAULT_EMPTY_MC_SET) {
            try {
                svLogger.info("Destroy method called, adding MC to set: " + this);
                //Add set to RA
                AdapterUtil.addMCToSet(this);
            } catch (Exception ex) {
                svLogger.info("MC can't be added to Set. Throws exception");
                throw new ResourceException(ex); //junk
            }
        }
        // Added by gburli - Ends

        // We are creating this exception here and just saving the exceptions as they happen.  We want to make
        // sure that if there is an exception, we keep processing the closes.  So, only throw the first exception
        // encountered.
        // Don't map exceptions and fire ConnectionError event from destroy since the
        // ManagedConnection is already being destroyed. [d128891]
        ResourceException dsae = null;

        // Cleanup should be done before destroy. We should continue processing even if there
        // is an error.
        try {
            // LIDB1181.28.1 - We can't use the normal cleanup here because it dissociates
            // handles and we don't want that.  Instead we do it in pieces...
            cleanupTransactions();
            cleanupStates();
        } catch (ResourceException re) {
            svLogger.info("DSTRY_ERROR_EX: " + re); //d115327
            if (dsae == null)
                dsae = re; //138040
        }

        // d118681.1- Always clean out the jdbc connection list since they can't be reused beyond
        //  the sharing boundary
        // Synchronization is not needed here since the ConnectionManager should not be
        // calling destroy concurrently with other operations on the same ManagedConnection.
        // [d128891]
        ResourceException closeX = closeHandles();
        if (dsae == null)
            dsae = closeX;

        try {
            if (sqlConn != null) {
                sqlConn.close();
            }
        } catch (SQLException sqle) {
            throw new ResourceException(sqle.getMessage());
        }
        try {
            if (poolConn != null) {
                poolConn.close();
            }
        } catch (SQLException sqle) {
            throw new ResourceException(sqle.getMessage());
        }

        handlesInUse = null;
        mcf = null;
        ivEventListeners = null;
        localTran = null;
        xares = null;
        cri = null;
        subject = null;
        sqlConn = null;
        poolConn = null;

        // Lastly, if there was a DataStoreAdapterException to throw from above, throw it here
        if (dsae != null) {
            svLogger.exiting(CLASSNAME, "destroy - throwing exception caught during destroy() processing");
            throw dsae;
        }

        svLogger.exiting(CLASSNAME, "destroy");
    }

    /**
     * @see javax.resource.spi.ManagedConnection#cleanup()
     */
    @Override
    public final void cleanup() throws ResourceException {
        svLogger.entering(CLASSNAME, "cleanup", this);

        // LIDB???? - invalidate all the handles.
        ResourceException firstX = handlesInUse.size() < 1 ? null : closeHandles();

        if (!connectionErrorDetected)
            firstX = dissociateHandles();

        try {
            cleanupTransactions();
        } catch (ResourceException resX) {
            if (firstX == null)
                firstX = resX;
        }

        // At this point, all of the most important stuff has been cleaned up.  Handles are
        // dissociated and transactions are rolled back.  Now we may throw any exceptions
        // which previously occurred.

        if (firstX != null) {
            svLogger.exiting(CLASSNAME, "cleanup", firstX);
            throw firstX;
        }

        // Cleanup ManagedConnection and Connection states in a separate method. [d128891]
        cleanupStates();
        svLogger.exiting(CLASSNAME, "cleanup");
    }

    /**
     * Used by the container to change the association of an application-level
     * connection handle with a ManagedConneciton instance. The container should
     * find the right ManagedConnection instance and call the associateConnection
     * method.
     * <p>
     * The resource adapter is required to implement the associateConnection method.
     * The method implementation for a ManagedConnection should dissociate the
     * connection handle (passed as a parameter) from its currently associated
     * ManagedConnection and associate the new connection handle with itself. In addition
     * the state of the old ManagedConnection needs to be copied into the new ManagedConnection
     * in case the association occurs between methods during a transaction.
     *
     * @see javax.resource.spi.ManagedConnection#associateConnection(Object)
     *
     * @param Object connection - Application-level connection handle
     * @exception ResourceException - Possible causes for this exception are:
     *                1) method called with an invalid handle type - WSJdbcConnection and WSRdbConnectionImpl
     *                are the only valid types.
     *                2) The connection is not in a valid state for reassociation.
     *                3) A fatal connection error was detected during reassoctiation.
     */
    @Override
    public void associateConnection(Object connection) throws ResourceException {
        svLogger.entering(CLASSNAME, "associateConnection", new Object[] { this, connection });
        Reassociateable connHandle;

        try {
            connHandle = (Reassociateable) connection;
        } catch (ClassCastException castX) {
            svLogger.info("Unable to cast Connection handle to Reassociateable");
            svLogger.exiting(CLASSNAME, "associateConnection - failed casting connection, throwing exception");
            throw new ResourceException("Called \"associateConnection\" with a connection type that " + "is not recognized " + connection.getClass().getName());
        }

        // JDBC Handles do not support reassociation during a transaction because of the
        // inability to reassociate "child" handles. (Statement, ResultSet, ...)
        // Therefore, we do not reassociate the handle, but instead "reserve" it for token
        // reassociation back to the same MC.  This relies on the guarantee we will always
        // be reassociated back to the original MC for use during the same transaction.

        // If smart handle is not supported, should we close all the child handles
        // when a JDBC handle is reassociated?

        try {
            ManagedConnectionImpl oldMC = (ManagedConnectionImpl) connHandle.getManagedConnection();
            int tranState = oldMC == null ? StateManager.NO_TRANSACTION_ACTIVE : oldMC.stateMgr.transtate;
            svLogger.info("Old ManagedConnection transaction state: " + oldMC == null ? null : oldMC.getTransactionStateAsString());
            svLogger.info("New ManagedConnection transaction state: " + this.getTransactionStateAsString());

            if ((tranState == StateManager.GLOBAL_TRANSACTION_ACTIVE || tranState == StateManager.LOCAL_TRANSACTION_ACTIVE)) {
                svLogger.info("Reassociation requested within a transaction; " + "handle reassociation will be ignored.");

                if (isLazyAssociatable()) {
                    // If smart handle is supported,
                    // A transaction is active; ignore the reassociation, but mark the handle as
                    // reserved for its current ManagedConnection.

                    if (!connHandle.isReserved())
                        connHandle.reserve();

                    // Drop the handle from its current ManagedConnection. We may not be able to
                    // drop handle's reference to the ManagedConnection, but we can at least drop
                    // the ManagedConnection's reference to the handle.
                    oldMC.dissociateHandle(connHandle);
                } else {
                    svLogger.info("Reassociation is called in a transaction, child wrappers are closed.");

                    // Dissociate the handle from its previous MC.
                    connHandle.dissociate();

                    // Close the child wrappers since they are no valid after the MC is dissociated.
                    ((JdbcConnection) connHandle).closeChildWrappers();

                    // Reassoicate the connection handle with this MC.
                    connHandle.reassociate(this, sqlConn);
                }
            } else {
                // or no transaction is active, so a full reassociation is allowed.
                // If the handle is still ACTIVE, dissociate it from its previous MC.
                if (connHandle.getState() == Reassociateable.ACTIVE)
                    connHandle.dissociate();

                // LIDB1181.28.1 - The connection handle must be supplied with both the new
                // ManagedConnection and underlying JDBC Connection.  The handle will handle
                // making the state of the new underlying Connection consistent with the
                // previously held one.
                connHandle.reassociate(this, sqlConn);
            }

            this.handlesInUse.add(connection);
            svLogger.exiting(CLASSNAME, "associateConnection");
        } catch (ResourceException resX) {
            svLogger.exiting(CLASSNAME, "associateConnection", "Exception");
            throw resX;
        }
    }

    /**
     * This method is invoked by the connection handle during dissociation to signal the
     * ManagedConnection to remove all references to the handle. If the ManagedConnection
     * is not associated with the specified handle, this method is a no-op and a warning
     * message is traced.
     *
     * @param the connection handle.
     */
    public synchronized void dissociateHandle(Object connHandle) {
        // Access to handlesInUse needs to be synchronized. [d128891]
        if (!handlesInUse.remove(connHandle))
            svLogger.info("Unable to dissociate Connection handle with current ManagedConnection "
                          + "because it is not currently associated with the ManagedConnection: " + new Object[] { connHandle, this });
    }

    /**
     * Adds a connection event listener to the ManagedConnection instance.
     * <p>
     * The registered ConnectionEventListener instances are notified of connection
     * close and error events, also of local transaction related events on the
     * Managed Connection.
     *
     * @param listener - a new ConnectionEventListener to be registered
     *
     * @throws NullPointerException if you try to add a null listener.
     */

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        svLogger.entering(CLASSNAME, "addConnectionEventListener", new Object[] { this, listener });

        if (listener == null)
            throw new NullPointerException("Cannot add null ConnectionEventListener.");

        // check if the array is already full
        if (numListeners >= ivEventListeners.length) {
            // there is not enough room for the listener in the array
            // create a new, bigger array
            ConnectionEventListener[] tempArray = ivEventListeners;
            ivEventListeners = new ConnectionEventListener[numListeners + 3];
            System.arraycopy(tempArray, 0, ivEventListeners, 0, tempArray.length);
            // point out in the trace that we had to do this - consider code changes if there
            // are new CELs to handle (change KNOWN_NUMBER_OF_CELS, new events?, ...)
            svLogger.info("received more ConnectionEventListeners than expected, increased array size to " + ivEventListeners.length);
        }

        // add listener to the array, increment listener counter
        ivEventListeners[numListeners++] = listener;
    }

    /**
     * Removes an already registered connection event listener from the
     * ManagedConnection instance.
     *
     * @param listener - already registered connection event listener to be removed
     */

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        svLogger.entering(CLASSNAME, "removeConnectionEventListener", listener);

        if (listener == null) {
            NullPointerException nullX = new NullPointerException("Cannot remove null ConnectionEventListener.");
            svLogger.exiting(CLASSNAME, "removeConnectionEventListener", nullX);
            throw nullX; // d128891
        }

        // loop through the listeners
        for (int i = 0; i < numListeners; i++)
            // look for matching listener
            if (listener == ivEventListeners[i]) {
                // remove the matching listener, but don't leave a gap in the array -- the order of
                // the listeners in the array doesn't matter, so move the last listener to fill the
                // gap left by the remove, if necessary
                ivEventListeners[i] = ivEventListeners[--numListeners];
                ivEventListeners[numListeners] = null;
                svLogger.exiting(CLASSNAME, "removeConnectionEventListener");
                return;
            }

        svLogger.exiting(CLASSNAME, "removeConnectionEventListener", "Listener not found for remove.");
    }

    /**
     * Returns a javax.transaction.xa.XAresource instance. An application server
     * enlists this XAResource instance with the Transaction Manager if the
     * ManagedConnection instance is being used in a JTA transaction that is
     * being coordinated by the Transaction Manager.
     *
     * @return a XAResource - if the dataSource specified for this ManagedConnection
     *         is of type XADataSource, then an XAResource from the physical connection is returned
     *         wrappered in our WSRdbXaResourceImpl. If the dataSource is of type ConnectionPoolDataSource,
     *         then our wrapper WSRdbOnePhaseXaResourceImpl is returned as the connection will not be
     *         capable of returning an XAResource as it is not two phase capable.
     *
     * @exception ResourceException - Possible causes for this exception are:
     *                1) failed to get an XAResource from the XAConnection object.
     */

    @Override
    public XAResource getXAResource() throws ResourceException {
        svLogger.entering(CLASSNAME, "getXAResource", this);

        if (xares != null) {
            svLogger.info("Returning existing XAResource: " + xares);
        } else if (is2Phase) {
            try {
                XAResource xa = ((XAConnection) poolConn).getXAResource();
                xares = new XAResourceImpl(xa, this);
            } catch (SQLException se) {
                svLogger.exiting(CLASSNAME, "getXAResource - failed trying to create XAResource, throwing exception");
                throw new ResourceException(se.getMessage());
            }
        } else {
            xares = new OnePhaseXAResourceImpl(sqlConn, this);
        }

        svLogger.exiting(CLASSNAME, "getXAResource");
        return xares;
    }

    /**
     * @see javax.resource.spi.ManagedConnection#getLocalTransaction()
     */
    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        svLogger.entering(CLASSNAME, "getLocalTransaction", this);
        if (localTran == null)
            localTran = new LocalTransactionImpl(this, sqlConn);

        svLogger.exiting(CLASSNAME, "getLocalTransaction", localTran);
        return localTran;
    }

    /**
     * @see javax.resource.spi.ManagedConnection#getMetaData()
     */
    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return null;
    }

    /**
     * @see javax.resource.spi.ManagedConnection#setLogWriter(PrintWriter)
     */
    @Override
    public void setLogWriter(PrintWriter arg0) throws ResourceException {
    }

    /**
     * @see javax.resource.spi.ManagedConnection#getLogWriter()
     */
    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return null;
    }

    /**
     * Get subject
     */
    public Subject getSubject() {
        return subject;
    }

    /**
     * Get ConnectionRequestInfo
     */
    public ConnectionRequestInfoImpl getCRI() {
        return cri;
    }

    /**
     * Clean up any outstanding transactions. This method is called from cleanup and destroy.
     *
     * @throws ResourceException if an error occurs cleaning up transactions.
     */
    private void cleanupTransactions() throws ResourceException {
        // Send connection error event if in transtates: Global transaction, Local Transaction,
        // or Trans ending because cleanup should never be called from these states.  If it is,
        // we rollback and throw the exception

        switch (stateMgr.transtate) {
            case StateManager.GLOBAL_TRANSACTION_ACTIVE: {
                try {
                    ((XAResourceImpl) xares).end();
                } catch (XAException xae) {
                }

                try {
                    ((XAResourceImpl) xares).rollback();
                } catch (XAException xae) {
                    svLogger.info("Failed to end or rollback XAResource during cleanup from failure state. Continuing with cleanup.");
                    throw new ResourceException(xae.getMessage());
                }

                svLogger.info("Cleanup should not be invoked while ManagedConnection is still in a transaction. Continuing with cleanup.");
                String message = "Cannot call 'cleanup' on a ManagedConnection while it is still in a " + "transaction.";
                throw new ResourceException(message);
            }
            case StateManager.LOCAL_TRANSACTION_ACTIVE:
            case StateManager.TRANSACTION_ENDING: {
                // If no work was done during the transaction, the autoCommit value may still
                // be on.  In this case, just no-op, since some drivers like ConnectJDBC 3.1
                // don't allow commit/rollback when autoCommit is on.  [d145849]

                try {
                    if (!autoCommit) {
                        sqlConn.rollback();
                    }
                } catch (SQLException se) {
                    throw new ResourceException(se.getMessage());
                }

                svLogger.info("Cleanup should not be invoked while ManagedConnection is still in a transaction. Continuing with cleanup.");
                String message = "Cannot call 'cleanup' on a ManagedConnection while it is still in a " + "transaction.";
                throw new ResourceException(message);
            }
        }
    }

    /**
     * Resets ManagedConnection and underlying Connection states. This method is used by both
     * cleanup and destroy. [d128891]
     *
     * @throws ResourceException if an error occurs resetting the states.
     */
    private void cleanupStates() throws ResourceException {
        stateMgr.transtate = StateManager.NO_TRANSACTION_ACTIVE;

        try {
            sqlConn.clearWarnings();
            setAutoCommit(defaultAutoCommit);
            catalog = defaultCatalog;
            typeMap = defaultTypeMap;
            readOnly = defaultReadOnly;
            isolevel = defaultIsolevel;
        } catch (SQLException ex) {
            throw new ResourceException("Exception thrown when cleaning up states");
        }

        isLazyEnlisted = false;
    }

    /**
     * Dissociates all handles from this ManagedConnection. Processing continues when errors
     * occur. All errors are logged, and the first error is saved to be returned when
     * processing completes.
     *
     * @return the first error to occur, or null if none.
     */
    private synchronized ResourceException dissociateHandles() {
        ResourceException firstX = null;
        Reassociateable conn = null;

        // Indicate that we are cleaning up handles, so we know not to send events for
        // operations done in the cleanup. [d138049]
        cleaningUpHandles = true;

        for (int i = handlesInUse.size() - 1; i >= 0; i--) {
            try {
                // d118681.1
                // The handle dissociate method will signal us back to remove our
                // references to the handles.
                conn = (Reassociateable) handlesInUse.get(i);
                conn.dissociate();
            } catch (ResourceException dissociationX) {
                // No FFDC code needed. Maybe. Depends on the ErrorCode.
                // Cleanup is allowed while handles are in use. Dissociate is not. So if
                // we get this error trying to dissociate, just close the handle instead.
                // [d133149.1]
                if (dissociationX.getErrorCode().equals("HANDLE_IN_USE"))
                    try {
                        svLogger.info("Unable to dissociate handle because it is doing work in the database.  Closing it instead.");
                        // This is a JDBC specific error, so we can cast to WSJdbcConnection.
                        ((JdbcConnection) conn).close();
                        dissociationX = null;
                    } catch (SQLException closeX) {
                        // No FFDC code needed here because we do it below.
                        dissociationX = new ResourceException(closeX.getMessage());
                    }

                if (dissociationX != null) {
                    svLogger.info("Error dissociating handle. Continuing... " + conn);
                    if (firstX == null)
                        firstX = dissociationX;
                }
            }
        }

        cleaningUpHandles = false;
        return firstX;
    }

    public final ConnectionEventListener getLastActionEventListener() {
        ConnectionEventListener cel = null;
        int celLength = this.ivEventListeners.length;
        svLogger.info("KDK: Number of ActionEventListeners found: " + celLength);
        celLength--;
        if (celLength >= 0) {
            while (cel == null && celLength >= 0) {
                cel = ivEventListeners[celLength--];
            }
            svLogger.info("KDK: Found non-null ActionEventListener at list position: " + celLength + 1);
        } else {
            svLogger.info("KDK: Error!  No ActionEventListener found!");
        }
        return cel;
    }

    /**
     * Returns the current transaction state for this ManagedConnection as a string. This
     * method is used for printing messages and trace statements.
     * <p>
     * Possible Transaction state strings are:
     * <ul>
     * <li>TRANSACTION_FAIL
     * <li>GLOBAL_TRANSACTION_ACTIVE
     * <li>LOCAL_TRANSACTION_ACTIVE
     * <li>TRANSACTION_ENDING
     * <li>NO_TRANSACTION_ACTIVE
     *
     * @return String - indicating the transaction state from StateManager
     */
    public final String getTransactionStateAsString() {
        return stateMgr.getStateAsString();
    }

    private Subject copySubject() throws ResourceException {
        //--------------d115459 wrap the .getPrivateCredentials() in a doPrivileged()
        //              d116816 merge the two consecutive doPriv calls into one.
        // this code is straight from the javadoc for AccessController
        // using the example that returns a object.
        try {
            return (Subject) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                @Override
                public Object run() throws Exception {
                    // privileged code goes here, for example:
                    // can only access outer class final variables from here
                    // so had to make readOnly, principals, pubCredentials & subF final.
                    return new Subject(subject.isReadOnly(), subject.getPrincipals(), subject.getPublicCredentials(), subject.getPrivateCredentials());
                }
            });
        } catch (PrivilegedActionException pae) {
            throw (ResourceException) pae.getException();
        }
    }

    /**
     * Closes all handles associated with this ManagedConnection. Processing continues even
     * if close fails on a handle. All errors are logged, and the first error is saved to be
     * returned when processing completes.
     *
     * @return the first error to occur closing a handle, or null if none.
     */
    private ResourceException closeHandles() {
        ResourceException firstX = null;
        Object conn = null;

        // Indicate that we are cleaning up handles, so we know not to send events for
        // operations done in the cleanup. [d138049]
        cleaningUpHandles = true;

        // There are valid cases in our testing where the 'handles in use' list is null..and in these cases, we can't
        // try to use the list or else we'll get a null pointer.
        // The full details regarding these scenarios are documented in the 'sendConnectionClosedEvent()' method...look there
        // for the rest of the story.
        if (handlesInUse != null) {
            for (int i = handlesInUse.size() - 1; i >= 0; i--) {
                conn = handlesInUse.remove(i);

                try {
                    ((JdbcConnection) conn).close();
                } catch (SQLException closeX) {
                    svLogger.info("Error closing handle. Continuing... " + conn);
                    ResourceException resX = new ResourceException(closeX.getMessage());

                    if (firstX == null) {
                        firstX = resX;
                    }
                }
            }
        } else {
            svLogger.info("We are NOT attempting to close any Handles because the 'handles in use' list is null....");
        }

        cleaningUpHandles = false;
        return firstX;
    }

    /**
     * @return a ConnectionRequestInfo based on the currently requested values.
     */
    public final ConnectionRequestInfoImpl createConnectionRequestInfo() {
        return new ConnectionRequestInfoImpl(cri.ivUserName, cri.ivPassword, isolevel, catalog, readOnly, typeMap);
    }

    /**
     * Returns the ManagedConnectionFactory which created this ManagedConnection
     *
     * @return a ManagedConnectionFactory.
     */
    public final ManagedConnectionFactoryImpl getManagedConnectionFactory() {
        return mcf;
    }

    /**
     * Returns the current transaction state of this managed connection.
     *
     * @return The transaction state.
     */
    public final int getTransactionState() {
        return stateMgr.transtate;
    }

    /**
     * Test whether the managed connection is enlisted in global transaction or not.<p>
     *
     * @return true if the ManagedConnection is enlisted in a global transaction, otherwise
     *         false.
     */
    public final boolean inGlobalTransaction() {
        // If we have a one-phase resource being used in a global transaction, then the
        // ManagedConnection state will be local transaction active.  So we need to check the
        // interaction pending event status, which, when deferred enlistment is used, signals
        // whether a global transaction is active.
        int state = stateMgr.transtate;

        return state == StateManager.GLOBAL_TRANSACTION_ACTIVE || (isLazyEnlisted && state == StateManager.LOCAL_TRANSACTION_ACTIVE);
    }

    /**
     * Get whether transaction registration enlistment is dynamic or not. <p>
     *
     * @return true, if dynamic; false, if static.
     */
    public boolean isLazyEnlistable() {
        return false;
    }

    /**
     * Get whether implicit handle reactivation (smart handle) is supported or not.
     *
     * @return true if implicit handle reactivation (smart handle) is supported,
     *         otherwise not.
     */
    public boolean isLazyAssociatable() {
        return false;
    }

    //-------------------------------------------------------------------------
    // Process Event methods
    //-------------------------------------------------------------------------

    /**
     * Process request for a CONNECTION_CLOSED event.
     *
     * @param handle the Connection handle requesting to fire the event.
     *
     * @throws ResourceException if an error occurs processing the request.
     */
    public void processConnectionClosedEvent(Object handle) throws ResourceException {
        // JdbcConnection does not synchronize on the ManagedConnection when sending the
        // CONNECTION CLOSED event, so this method (or parts of it) must be synchronized.
        // All operations on the handlesInUse list must be synchronized together.  Also,
        // synchronization is needed for the event listener loop.

        // A connection handle was closed - must notify the connection manager
        // of the close on the handle.  JDBC connection handles
        // which are closed are not allowed to be reused because there is no
        // guarantee that the user will not try to reuse an already closed JDBC handle.

        // Only send the event if the application is requesting the close. [d138049]
        if (cleaningUpHandles)
            return;

        // LIDB1181.28.1 (found while testing this feature) A ConnectionError situation may
        // trigger a ManagedConnection close before the handle sends the ConnectionClosed
        // event.  When the event is sent the ManagedConnection is already closed.  If so,
        // just do a no-op here.
        if (handlesInUse == null) {
            svLogger.info("ManagedConnection already closed");
            return;
        }

        // Create the event only if needed, and outside of the synchronized block. [d133293]
        AdapterConnectionEvent event = new AdapterConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED, null, handle);

        svLogger.info("Firing CONNECTION CLOSED event for: " + handle);
        synchronized (this) {
            handlesInUse.remove(handle);

            // This is checked only for the case where a JDBC connection is obtained and used
            // in a manner that does not start a transaction of any sort.  For example, obtaining
            // a connection, calling setAutoCommit, and closing the connection does not start a
            // transaction.  As a transaction was never started, there is no associated cleanup that
            // would normally happen.  Because of this, the connection doesn't get the autoCommit reset
            // back to its default value.  If it isn't reset, the next time JDBC gets a connection
            // the autocommit is still at the value set by the first setAutoCommit which did not start
            // a transaction.
            // Also note this must happen after the handle is removed from the InUse list.  Otherwise,
            // the list would not be zero length.  It can only be reset if there are no other handles
            // using the MC currently.
            if (stateMgr.transtate == StateManager.NO_TRANSACTION_ACTIVE && handlesInUse.isEmpty())
                try {
                    svLogger.info("set AutoCommit to true when connection is closed.");
                    setAutoCommit(true);
                } catch (SQLException sqle) {
                    throw new ResourceException(sqle.getMessage());
                }
        }

        // d116090
        // loop through the listeners
        // Not synchronized because of contract that listeners will only be changed on
        // ManagedConnection create/destroy. [d135971.2]
        for (int i = 0; i < numListeners; i++) {
            // send Connection Closed event to the current listener
            ivEventListeners[i].connectionClosed(event);
        }
    }

    /**
     * Process request for a CONNECTION_ERROR_OCCURRED event.
     *
     * @param event the Connection handle requesting to send the event.
     * @param ex the exception which indicates the connection error, or null if no exception.
     */
    public void processConnectionErrorOccurredEvent(Object handle, Exception ex) {
        // Method is not synchronized because of the contract that add/remove event
        // listeners will only be used on ManagedConnection create/destroy, when the
        // ManagedConnection is not used by any other threads. [d135971.2]

        // Some object using the physical jdbc connection has received a SQLException that
        // when translated to a ResourceException is determined to be a connection event error.
        // The SQLException is mapped to a StaleConnectionException in JdbcConnection.
        // SCE's will (almost) always be connection errors.

        // Track whether a fatal Connection error was detected, since this will determine the
        // behavior of the cleanup method. [d134425]  Technically, the Connection Manager is
        // required to be able to handle duplicate events, but since we already have a flag
        // for the occasion, we'll be nice and skip the unnecessary event when convenient.

        if (connectionErrorDetected) {
            svLogger.info("CONNECTION_ERROR_OCCURRED event already fired for connection");
            return;
        }

        connectionErrorDetected = true;

        // Close all active handles for this ManagedConnection, since we cannot rely on the
        // ConnectionManager to request cleanup/destroy immediately.  The ConnectionManager is
        // required to wait until the transaction has ended. [d138049]
        synchronized (this) {
            closeHandles();
        }

        // Create a Connection Error Event with the given SQLException.
        AdapterConnectionEvent event = new AdapterConnectionEvent(this, ConnectionEvent.CONNECTION_ERROR_OCCURRED, ex, handle);
        svLogger.info("Firing CONNECTION_ERROR_OCCURRED event for handle: " + handle);

        // d116090
        // loop through the listeners
        for (int i = 0; i < numListeners; i++) {
            // send Connection Error Occurred event to the current listener
            ivEventListeners[i].connectionErrorOccurred(event);
        }
    }

    /**
     * lazily enlist a connection object with a XA transaction
     */
    public void lazyEnlist(LazyEnlistableConnectionManager lazyEnlistableConnectionManager) throws ResourceException {
        {
            svLogger.entering(CLASSNAME, "lazyEnlist", this);
        }

        // Signal the ConnectionManager directly to lazily enlist.
        // ['if/else' block added under LIDB2110.16]
        if (isLazyEnlisted)
        // Already enlisted; don't need to do anything.
        {
            svLogger.exiting(CLASSNAME, "lazyEnlist", new Object[] { Boolean.FALSE, "ManagedConnection is already enlisted in a transaction.", this });
        } else {
            lazyEnlistableConnectionManager.lazyEnlist(this);

            // Indicate we lazily enlisted in the current transaction, if so.
            isLazyEnlisted = stateMgr.transtate != StateManager.NO_TRANSACTION_ACTIVE;
            svLogger.exiting(CLASSNAME, "lazyEnlist", isLazyEnlisted ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    /**
     * Process request for a LOCAL_TRANSACTION_STARTED event.
     *
     * @param handle the Connection handle requesting the event.
     *
     * @throws ResourceException if an error occurs starting the local transaction, or if the
     *             state is not valid.
     */
    public void processLocalTransactionStartedEvent(Object handle) throws ResourceException {
        svLogger.entering(CLASSNAME, "processLocalTransactionStartedEvent", handle);

        // An application level local transaction has been requested started
        // The isValid method returns an exception if it is not valid.  This allows the
        // WSStateManager to create a more detailed message than this class could.
        ResourceException re = stateMgr.isValid(StateManager.LT_BEGIN);

        if (re == null) {
            // Already validated the state so just set it. [d139351.19]
            stateMgr.transtate = StateManager.LOCAL_TRANSACTION_ACTIVE;
        } else {
            throw re;
        }

        AdapterConnectionEvent event = new AdapterConnectionEvent(this, ConnectionEvent.LOCAL_TRANSACTION_STARTED, null, handle);
        svLogger.info("Firing LOCAL TRANSACTION STARTED event for: " + handle);

        // Notification of the eventListeners must happen after the state change because if the statechange
        // is illegal, we need to throw an exception.  If this exception occurs, we do not want to
        // notify the cm of the tx started because we are not allowing it to start.
        // d116090
        // loop through the listeners
        for (int i = 0; i < numListeners; i++) {
            // send Local Transaction Started event to the current listener
            ivEventListeners[i].localTransactionStarted(event);
        }

        svLogger.exiting(CLASSNAME, "processLocalTransactionStartedEvent");
    }

    /**
     * Process request for a LOCAL_TRANSACTION_COMMITTED event.
     *
     * @param handle the Connection handle requesting to send an event.
     *
     * @throws ResourceException if an error occurs committing the transaction or the state is
     *             not valid.
     */
    public void processLocalTransactionCommittedEvent(Object handle) throws ResourceException {
        svLogger.entering(CLASSNAME, "processLocalTransactionCommittedEvent", handle);

        // A application level local transaction has been committed.
        ResourceException re = stateMgr.isValid(StateManager.LT_COMMIT);
        if (re == null) {
            try {
                svLogger.info("Connection is committed");
                sqlConn.commit();
            } catch (SQLException sqle) {
                throw new ResourceException(sqle.getMessage());
            }

            stateMgr.transtate = StateManager.NO_TRANSACTION_ACTIVE;
        } else {
            throw re;
        }

        AdapterConnectionEvent event = new AdapterConnectionEvent(this, ConnectionEvent.LOCAL_TRANSACTION_COMMITTED, null, handle);
        svLogger.info("Firing LOCAL TRANSACTION COMMITTED event for: " + handle);

        // loop through the listeners
        for (int i = 0; i < numListeners; i++) {
            // send Local Transaction Committed event to the current listener
            ivEventListeners[i].localTransactionCommitted(event);
        }

        // reset flag so event will be sent if we end up in a Global Transaction
        isLazyEnlisted = false;

        svLogger.exiting(CLASSNAME, "processLocalTransactionCommittedEvent");
    }

    /**
     * Process request for a LOCAL_TRANSACTION_ROLLEDBACK event.
     *
     * @param handle the Connection handle requesting to send an event.
     *
     * @throws ResourceException if an error occurs rolling back the transaction or the state
     *             is not valid.
     */
    public void processLocalTransactionRolledbackEvent(Object handle) throws ResourceException {
        // A CCILocalTransaction has been rolledback
        ResourceException re = stateMgr.isValid(StateManager.LT_ROLLBACK);
        if (re == null) {
            try {
                svLogger.info("Connection is rollbacked");
                sqlConn.rollback();
            } catch (SQLException sqle) {
                throw new ResourceException(sqle.getMessage());
            }

            stateMgr.transtate = StateManager.NO_TRANSACTION_ACTIVE;
        } else {
            throw re;
        }

        AdapterConnectionEvent event = new AdapterConnectionEvent(this, ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK, null, handle);
        svLogger.info("Firing LOCAL TRANSACTION ROLLEDBACK event for: " + handle);

        // loop through the listeners
        for (int i = 0; i < numListeners; i++) {
            // send Local Transaction Rolledback event to the current listener
            ivEventListeners[i].localTransactionRolledback(event);
        }

        // reset flag so event will be sent if we end up in a Global Transaction
        isLazyEnlisted = false;
    }

    //***************************************************************
    // Accessor methods for Connection Attributes.
    //***************************************************************
    public boolean getAutoCommit() {
        svLogger.info("AutoCommit is " + autoCommit);
        return autoCommit;
    }

    public void setAutoCommit(boolean ac) throws SQLException {
        if (autoCommit == ac)
            return;

        svLogger.info("Set AutoCommit to " + ac);
        sqlConn.setAutoCommit(ac);
        autoCommit = ac;
    }

    public int getTransactionIsolation() {
        return isolevel;
    }

    public void setTransactionIsolation(int level) throws SQLException {
        if (isolevel == level)
            return;

        svLogger.info("Set transaction isolation level to " + level);
        sqlConn.setTransactionIsolation(level);
        isolevel = level;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) throws SQLException {
        if (catalog == null && this.catalog == null)
            return;

        if (catalog != null && this.catalog != null) {
            if (catalog.equals(this.catalog))
                return;
        }

        svLogger.info("Set catalog to " + catalog);
        sqlConn.setCatalog(catalog);
        this.catalog = catalog;
    }

    public Map getTypeMap() {
        return typeMap;
    }

    public void setTypeMap(Map map) throws SQLException {
        typeMap = map;
    }

    public boolean isReadOnly() {
        return readOnly.booleanValue();
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        if (this.readOnly != null && readOnly == this.readOnly.booleanValue())
            return;

        svLogger.info("Set readOnly to " + readOnly);
        sqlConn.setReadOnly(readOnly);
        this.readOnly = new Boolean(readOnly);
    }

    //-------------------------------------------------------------------------
    // Send Event methods
    //-------------------------------------------------------------------------

    /**
     * Send request for a CONNECTION_CLOSED event. The handle might
     * be already closed.
     *
     * @param handle the Connection handle requesting to fire the event.
     *
     * @throws ResourceException if an error occurs processing the request.
     */
    @Override
    public void sendConnectionClosedEvent(Object handle) throws ResourceException {
        // Create the event only if needed, and outside of the synchronized block. [d133293]
        AdapterConnectionEvent event = new AdapterConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED, null, handle);

        svLogger.info("Sending CONNECTION CLOSED event for: " + handle);

        synchronized (this) {
            //There are some valid cases in our test bucket where the 'handle in use' list will be null, and in these instances,
            //we must no-op the .remove(), or else we'll get a NullPointer
            //
            //The 'handles in use' list gets nulled out when the Server calls the .destroy() method on this object, and
            //that will happen when the 'connection error event' gets fired.
            //
            //We have at least two instances in the testing where we fire a 'connection error event', and then follow that up with
            //a second action that will cause us to use the 'handles in use' list...but of course at this point its been nulled out,
            //so we have to skip that action or we'll get the NulPointer.
            if (handlesInUse != null) {
                handlesInUse.remove(handle);
            } else {
                svLogger.info("The 'handles in use' list is null...NOT attempting to remove the Handle from it.");
            }
        }

        for (int i = 0; i < numListeners; i++) {
            ivEventListeners[i].connectionClosed(event);
        }
    }

    /**
     * Send request for a CONNECTION_ERROR_OCCURRED event.
     *
     * @param event the Connection handle requesting to send the event.
     * @param ex the exception which indicates the connection error, or null if no exception.
     */
    @Override
    public void sendConnectionErrorOccurredEvent(Object handle, Exception ex) {
        connectionErrorDetected = true;

        synchronized (this) {
            closeHandles();
        }

        AdapterConnectionEvent event = new AdapterConnectionEvent(this, ConnectionEvent.CONNECTION_ERROR_OCCURRED, ex, handle);

        svLogger.info("Sending CONNECTION_ERROR_OCCURRED event for handle: " + handle);
        for (int i = 0; i < numListeners; i++) {
            ivEventListeners[i].connectionErrorOccurred(event);
        }
    }

    /**
     * Send request for an INTERACTION_PENDING event. This is the websphere
     * specific event to deal with deferred enlistment.
     *
     * @param handle the Connection handle requesting to send the event.
     *
     * @throws ResourceException if an error occurs signalling the interaction pending.
     */
    @Override
    public void sendInteractionPendingEvent(Object handle) throws ResourceException {
        // LIDB??? -- FIX ME -- Need modification. InteractionPending event doesn't
        // exist in J2C 1.5 any more.

        svLogger.info("Not sending INTERACTION PENDING event for: " + handle);
    }

    /**
     * Send request for a LOCAL_TRANSACTION_STARTED event.
     *
     * @param handle the Connection handle requesting the event.
     *
     * @throws ResourceException if an error occurs starting the local transaction, or if the
     *             state is not valid.
     */
    @Override
    public void sendLocalTransactionStartedEvent(Object handle) throws ResourceException {
        svLogger.entering(CLASSNAME, "sendLocalTransactionStartedEvent", handle);

        // Already validated the state so just set it. [d139351.19]
        stateMgr.transtate = StateManager.LOCAL_TRANSACTION_ACTIVE;
        AdapterConnectionEvent event = new AdapterConnectionEvent(this, ConnectionEvent.LOCAL_TRANSACTION_STARTED, null, handle);

        svLogger.info("Sending LOCAL TRANSACTION STARTED event for: " + handle);
        for (int i = 0; i < numListeners; i++) {
            ivEventListeners[i].localTransactionStarted(event);
        }

        svLogger.exiting(CLASSNAME, "sendLocalTransactionStartedEvent");
    }

    /**
     * Send request for a LOCAL_TRANSACTION_COMMITTED event.
     *
     * @param handle the Connection handle requesting to send an event.
     *
     * @throws ResourceException if an error occurs committing the transaction or the state is
     *             not valid.
     */
    @Override
    public void sendLocalTransactionCommittedEvent(Object handle) throws ResourceException {
        svLogger.entering(CLASSNAME, "sendLocalTransactionCommittedEvent", handle);

        stateMgr.transtate = StateManager.NO_TRANSACTION_ACTIVE;
        AdapterConnectionEvent event = new AdapterConnectionEvent(this, ConnectionEvent.LOCAL_TRANSACTION_COMMITTED, null, handle);

        svLogger.info("Sending LOCAL TRANSACTION COMMITTED event for: " + handle);
        for (int i = 0; i < numListeners; i++) {
            ivEventListeners[i].localTransactionCommitted(event);
        }

        svLogger.exiting(CLASSNAME, "processLocalTransactionCommittedEvent");
    }

    /**
     * Send request for a LOCAL_TRANSACTION_ROLLEDBACK event.
     *
     * @param handle the Connection handle requesting to send an event.
     *
     * @throws ResourceException if an error occurs rolling back the transaction or the state
     *             is not valid.
     */
    @Override
    public void sendLocalTransactionRolledbackEvent(Object handle) throws ResourceException {
        stateMgr.transtate = StateManager.NO_TRANSACTION_ACTIVE;
        AdapterConnectionEvent event = new AdapterConnectionEvent(this, ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK, null, handle);

        svLogger.info("Sending LOCAL TRANSACTION ROLLEDBACK event for: " + handle);
        for (int i = 0; i < numListeners; i++) {
            ivEventListeners[i].localTransactionRolledback(event);
        }
    }

    /**
     * @return the number of handles associated with this ManagedConnection.
     */
    public final int getHandleCount() {
        return handlesInUse.size();
    }

    /**
     * Set the cursor holdability value to the request value
     *
     * @param holdability the cursor holdability
     */
    public final void setHoldability(int holdability) throws SQLException {
        svLogger.info("Set Holdability to " + holdability);
        sqlConn.setHoldability(holdability);
    }

    /**
     * Get the cursor holdability value
     *
     * @return the cursor holdability value
     */
    public final int getHoldability() throws SQLException {
        return sqlConn.getHoldability();
    }

    /**
     * <p>Creates an unnamed savepoint in the current transaction and returns the new Savepoint object
     * that represents it. <p>
     *
     * @return the new Savepoint object
     *
     * @exception SQLException If a database access error occurs or this Connection object is currently
     *                in auto-commit mode.
     */
    public Savepoint setSavepoint() throws SQLException {
        return sqlConn.setSavepoint();
    }

    /**
     * <p>Creates a savepoint with the given name in the current transaction and returns the new
     * Savepoint object that represents it. </p>
     *
     * @param name a String containing the name of the savepoint
     *
     * @return the new Savepoint object
     *
     * @exception SQLException f a database access error occurs or this Connection object is
     *                currently in auto-commit mode
     */
    public Savepoint setSavepoint(String name) throws SQLException {
        return sqlConn.setSavepoint(name);
    }

    /**
     * <p>Undoes all changes made after the given Savepoint object was set. This method should be used
     * only when auto-commit has been disabled. </p>
     *
     * @param savepoint the Savepoint object to roll back to
     *
     * @exception SQLException If a database access error occurs, the Savepoint object is no longer
     *                valid, or this Connection object is currently in auto-commit mode
     */
    public void rollback(Savepoint savepoint) throws SQLException {
        sqlConn.rollback(savepoint);
    }

    /**
     * <p>Removes the given Savepoint object from the current transaction. Any reference to the savepoint
     * after it have been removed will cause an SQLException to be thrown. </p>
     *
     * @param savepoint the Savepoint object to be removed
     *
     * @exception SQLException If a database access error occurs or the given Savepoint object is not
     *                a valid savepoint in the current transaction
     */
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        sqlConn.releaseSavepoint(savepoint);
    }
}
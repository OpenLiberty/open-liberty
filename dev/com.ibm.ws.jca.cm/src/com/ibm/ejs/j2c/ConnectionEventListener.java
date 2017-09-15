/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

/*
 * Class name   : ConnectionEventListener
 *
 * Scope        : EJB server
 *
 * Object model : 1 per ManagedConnection
 *
 * An instance of the ConnectionEventListener class is created during initialisation of the
 * MCWrapper class.
 */

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ManagedConnection;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.j2c.TranWrapper;

public final class ConnectionEventListener implements javax.resource.spi.ConnectionEventListener {
    private MCWrapper mcWrapper = null;

    private static final TraceComponent tc = Tr.register(ConnectionEventListener.class, J2CConstants.traceSpec, J2CConstants.messageFile);

    /**
     * Default constructor provided so that subclasses need not override (implement).
     */
    ConnectionEventListener() {}

    /**
     * ctor
     *
     * @param mcWrapper
     */
    protected ConnectionEventListener(MCWrapper mcWrapper) {
        this.mcWrapper = mcWrapper;
    }

    /**
     * This method is called by a resource adapter when the application calls close on a
     * Connection.
     *
     * @param ConnectionEvent
     *
     */
    @Override
    public void connectionClosed(ConnectionEvent event) {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "connectionClosed");
        }

        if (event.getId() == ConnectionEvent.CONNECTION_CLOSED) {
            if (!mcWrapper.isParkedWrapper()) {

                if (isTracingEnabled && tc.isDebugEnabled()) {
                    //Tr.debug(this, tc, "Closing handle for ManagedConnection@" + Integer.toHexString(mc.hashCode()) + " from pool " + mcWrapper.gConfigProps.pmiName + " from mcWrapper " + mcWrapper.toString() + " from mc " + mc);
                    // Omit handle info.
                    //Tr.debug(this, tc, "***Connection Close Request*** Handle Name: " + event.getConnectionHandle().toString() + "  Connection Pool: " + mcWrapper.getPoolManager().toString() + "  Details: : " + mcWrapper.toString() );
                    //          I only removed the connection handle toString, since this is the one getting the null pointer exception.
                    //          The trace component code will check for null first before calling toString on the handle.
                    //          Any new debug should not use the .toString() to prevent null pointer exceptions in trace.
                    //          Changed trace string to only dump pool manager name not entire pool.
                    Tr.debug(this, tc, "***Connection Close Request*** Handle Name: " + event.getConnectionHandle() + "  Connection Pool: "
                                       + mcWrapper.getPoolManager().getGConfigProps().getXpathId() + "  Details: : " + mcWrapper);

                }

                ConnectionManager cm = mcWrapper.getConnectionManagerWithoutStateCheck();

                if (cm != null && cm.handleToThreadMap != null) {
                    cm.handleToThreadMap.clear();
                }
                if (cm != null && cm.handleToCMDMap != null) {
                    cm.handleToCMDMap.clear();
                }

                if (!(mcWrapper.gConfigProps.isSmartHandleSupport() && (cm != null && cm.shareable()))) {

                    Object conHandle = event.getConnectionHandle();
                    if (null == conHandle) {
                        Tr.warning(tc, "CONNECTION_CLOSED_NULL_HANDLE_J2CA0148", event);
                    } else {
                        mcWrapper.removeFromHandleList(conHandle);
                        // TODO - need to implement - Notify the CHM to stop tracking the handle because it has been closed.
                    }
                }

                /*
                 * Decrement the number of open connections for Managed Connection and see if
                 * all the connections are now closed. If they are all closed and the MC is not
                 * associated with a transaction, then return it to the pool. If all the handles
                 * are closed and the MC is associated with a transaction, then do nothing.
                 * The MC will be released back to the pool at the end of the transaction when
                 * afterCompletion is called on one of the transactional wrapper objects.
                 */
                mcWrapper.decrementHandleCount();

                if (mcWrapper.getHandleCount() == 0) {

                    /*
                     * If we are processing a NoTransaction resource, then we are essentially done with
                     * the "transaction" when all of the connection handles are closed. We have to
                     * perform this extra cleanup because of the dual purpose of the involvedInTransaction
                     * method on the MCWrapper.
                     */

                    if (mcWrapper.getTranWrapperId() == MCWrapper.NOTXWRAPPER) {
                        mcWrapper.transactionComplete();
                    }

                    // Deleted calling mcWrapper.transactionComplete() for RRS Local Tran

                    /*
                     * If the ManagedConnection is not associated with a
                     * transaction any more, return it to the pool.
                     */

                    if (!mcWrapper.involvedInTransaction()) {

                        /*
                         * The ManagedConnection is not associated with a transactional
                         * context so return it to the pool. We need to check if the MC was
                         * shareable or not. If it was shareable, then we need to extract the
                         * coordinator from the MCWrapper and send it into releaseToPoolManager.
                         * If it was unshareable, then we just pass null into the
                         * releaseToPoolManager method and it releases it back to the pool.
                         */

                        try {
                            mcWrapper.releaseToPoolManager();
                        } catch (Exception ex) {
                            // Nothing to do here. PoolManager has already logged it.
                            // Since we are in cleanup mode, we will not surface a Runtime exception to the ResourceAdapter
                            FFDCFilter.processException(ex, "com.ibm.ejs.j2c.ConnectionEventListener.connectionClosed", "197", this);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc,
                                         "connectionClosed: Closing connection in pool " + mcWrapper.gConfigProps.getXpathId()
                                                   + " caught exception, but will continue processing: ",
                                         ex);
                            }
                        }
                    }
                }
            }
        } else {
            // Connection Event passed in doesn't match the method called.
            // This should never happen unless there is an error in the ResourceAdapter.
            processBadEvent("connectionClosed", ConnectionEvent.CONNECTION_CLOSED, event);
        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "connectionClosed");
        }

        return;

    }

    /**
     * This method is called by a resource adapter when a connection error occurs.
     * This is also called internally by this class when other event handling methods fail
     * and require cleanup.
     *
     * @param ConnectionEvent
     *
     */
    @Override
    public void connectionErrorOccurred(ConnectionEvent event) {

        int eventID = event.getId();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            StringBuilder entry = new StringBuilder(event.getClass().getSimpleName()).append('{');
            entry.append("id=").append(event.getId()).append(", ");
            entry.append("source=").append(event.getSource());
            entry.append('}');

            if (event.getException() == null)
                Tr.entry(this, tc, "connectionErrorOccurred", entry.toString());
            else
                Tr.entry(this, tc, "connectionErrorOccurred", entry.toString(), event.getException());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            // (ASSERT: event is not null)
            StringBuffer tsb = new StringBuffer();
            Object connHandle = event.getConnectionHandle();
            tsb.append("***Connection Error Request*** Handle Name: " + connHandle);
            if (mcWrapper != null) {
                Object poolMgr = mcWrapper.getPoolManager();
                tsb.append(", Connection Pool: " + poolMgr + ", Details: " + mcWrapper);
            } else {
                tsb.append(", Details: null");
            }
            Tr.debug(this, tc, tsb.toString());
        }

        switch (eventID) {

            case ConnectionEvent.CONNECTION_ERROR_OCCURRED: {

                Exception tempEx = event.getException();

                // Initialize tempString so the msg makes sense for the case where the event has NO associated Exception
                String tempString = "";
                if (tempEx != null) {

                    // If there is an associated Exception, generate tempString from that
                    tempString = J2CUtilityClass.generateExceptionString(tempEx);
                    Tr.audit(tc, "RA_CONNECTION_ERROR_J2CA0056", tempString, mcWrapper.gConfigProps.cfName);

                }

                else {
                    Tr.audit(tc, "NO_RA_EXCEPTION_J2CA0216", mcWrapper.gConfigProps.cfName);
                }

                // NOTE: Moving all functional code for this to the MCWrapper as it is
                //  closer to all the data/objects needed to perform this cleanup.
                mcWrapper.connectionErrorOccurred(event);

                break;
            }

            case com.ibm.websphere.j2c.ConnectionEvent.SINGLE_CONNECTION_ERROR_OCCURRED: {

                /*
                 * 51 is the id selected for this event.
                 *
                 * If a resource adapter uses this Id, the connection may be
                 * unconditionally cleaned up and destroyed. We are assuming the resource
                 * adapter knows this connection can not be recovered.
                 *
                 * Existing transactions may delay destroying the connection.
                 *
                 * The connectionErrorOccurred method will process this request,
                 *
                 * Only this connection will be destroyed.
                 */

                Exception tempEx = event.getException();

                // Initialize tempString so the msg makes sense for the case where the event has NO associated Exception
                String tempString = "";
                if (tempEx != null) {

                    // If there is an associated Exception, generate tempString from that
                    tempString = J2CUtilityClass.generateExceptionString(tempEx);
                    Tr.audit(tc, "RA_CONNECTION_ERROR_J2CA0056", tempString, mcWrapper.gConfigProps.cfName);
                }

                else {
                    Tr.audit(tc, "NO_RA_EXCEPTION_J2CA0216", mcWrapper.gConfigProps.cfName);
                }

                // NOTE: Moving all functional code for this to the MCWrapper as it is
                // closer to all the data/objects needed to perform this cleanup.
                mcWrapper.connectionErrorOccurred(event);

                break;
            }

            case com.ibm.websphere.j2c.ConnectionEvent.CONNECTION_ERROR_OCCURRED_NO_EVENT: {

                // NOTE: Moving all functional code for this to the MCWrapper as it is
                //  closer to all the data/objects needed to perform this cleanup.
                mcWrapper.connectionErrorOccurred(event);

                break;
            }

            default: {

                // Connection Event passed in doesn't match the method called.
                // This should never happen unless there is an error in the ResourceAdapter.
                processBadEvent("connectionErrorOccurred", ConnectionEvent.CONNECTION_ERROR_OCCURRED, event);

            }

        } // end switch

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "connectionErrorOccurred");
        }

        return;

    }

    /**
     * This method is called by a resource adapter when a CCI local transation commit is called
     * by the application on a connection. If the MC is associated with a UOW,
     * delist its corresponding transaction wrapper.
     *
     * @param ConnectionEvent
     */

    @Override
    public void localTransactionCommitted(ConnectionEvent event) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "localTransactionCommitted");
        }

        if (event.getId() == ConnectionEvent.LOCAL_TRANSACTION_COMMITTED) {

            if (mcWrapper.involvedInTransaction()) {

                /*
                 * The ManagedConnection is associated with a transaction.
                 * Delist the ManagedConnection from the transaction and
                 * postpone release of the connection until transaction finishes.
                 */

                TranWrapper wrapper = null;
                try {
                    wrapper = mcWrapper.getCurrentTranWrapper();
                    wrapper.delist();
                } catch (ResourceException e) {
                    // Can't delist, something went wrong.
                    //  Destroy the connection(s) so it can't cause any future problems.
                    FFDCFilter.processException(e, "com.ibm.ejs.j2c.ConnectionEventListener.localTransactionCommitted", "316", "this");
                    // add datasource name to message
                    Tr.error(tc, "DELIST_FAILED_J2CA0073", "localTransactionCommitted", e, mcWrapper.gConfigProps.cfName);
                    // Moved event.getSource() inside of catch block for performance reasons
                    ManagedConnection mc = null;
                    try {
                        mc = (ManagedConnection) event.getSource();
                    } catch (ClassCastException cce) {
                        Tr.error(tc, "GET_SOURCE_CLASS_CAST_EXCP_J2CA0098", cce);
                        throw new IllegalStateException("ClassCastException occurred attempting to cast event.getSource to ManagedConnection");
                    }
                    ConnectionEvent errorEvent = new ConnectionEvent(mc, ConnectionEvent.CONNECTION_ERROR_OCCURRED);
                    this.connectionErrorOccurred(errorEvent);
                    RuntimeException rte = new IllegalStateException(e.getMessage());
                    throw rte;
                }
            } else {
                // if we are not involved in a transaction, then we are likely running with NO transaction
                //  context on the thread.  This case currently needs to be supported because
                //  servlets can spin their own threads which would not have context.
                // Note: it is very rare that users do this.  All other occurances are
                //  considered to be an error.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(this, tc, "localTransactionCommitted", "no transaction context, return without delisting");
                }
                return;
            }
        } else {
            // Connection Event passed in doesn't match the method called.
            // This should never happen unless there is an error in the ResourceAdapter.
            processBadEvent("localTransactionCommitted", ConnectionEvent.LOCAL_TRANSACTION_COMMITTED, event);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "localTransactionCommitted");
        }
        return;

    }

    /**
     * This method is called by a resource adapter when a CCI local transation rollback is called
     * by the application on a connection. If the MC is associated with at UOW,
     * delist its coresponding transaction wrapper.
     *
     * @param ConnectionEvent
     */

    @Override
    public void localTransactionRolledback(ConnectionEvent event) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "localTransactionRolledback");
        }

        if (event.getId() == ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK) {

            if (mcWrapper.involvedInTransaction()) {

                /*
                 * The ManagedConnection is associated with a transaction.
                 * Delist the ManagedConnection from the transaction and
                 * postpone release of the connection until transaction finishes.
                 */

                try {
                    mcWrapper.getCurrentTranWrapper().delist();
                } catch (ResourceException e) {
                    // Can't delist, something went wrong.
                    //  Destroy the connection(s) so it can't cause any future problems.
                    FFDCFilter.processException(e, "com.ibm.ejs.j2c.ConnectionEventListener.localTransactionRolledback", "393", this);
                    // add datasource name to message
                    Tr.error(tc, "DELIST_FAILED_J2CA0073", "localTransactionRolledback", e, mcWrapper.gConfigProps.cfName);
                    // Moved event.getSource() inside of catch block for performance reasons
                    ManagedConnection mc = null;
                    try {
                        mc = (ManagedConnection) event.getSource();
                    } catch (ClassCastException cce) {
                        Tr.error(tc, "GET_SOURCE_CLASS_CAST_EXCP_J2CA0098", cce);
                        throw new IllegalStateException("ClassCastException occurred attempting to cast event.getSource to ManagedConnection");
                    }
                    ConnectionEvent errorEvent = new ConnectionEvent(mc, ConnectionEvent.CONNECTION_ERROR_OCCURRED);
                    this.connectionErrorOccurred(errorEvent);
                    RuntimeException rte = new IllegalStateException(e.getMessage());
                    throw rte;
                }
            } else {
                // if we are not involved in a transaction, then we are likely running with NO transaction
                //  context on the thread.  This case currently needs to be supported because
                //  servlets can spin their own threads which would not have context.
                // Note: it is very rare that users do this.  All other occurances are
                //  considered to be an error.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(this, tc, "localTransactionRolledback", "no transaction context, return without delisting");
                }
                return;
            }
        } else {
            // Connection Event passed in doesn't match the method called.
            // This should never happen unless there is an error in the ResourceAdapter.
            processBadEvent("localTransactionRolledback", ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK, event);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "localTransactionRolledback");
        }
        return;
    }

    /**
     * This method is called by a resource adapter when a CCI local transation begin is called
     * by the application on a connection
     *
     * @param ConnectionEvent
     */

    @Override
    public void localTransactionStarted(ConnectionEvent event) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "localTransactionStarted");
        }

        if (event.getId() == ConnectionEvent.LOCAL_TRANSACTION_STARTED) {

            //
            // The LocalTransactionStarted event for local transaction connections is analogous to the
            //  InteractionPending event for xa transaction connections.  JMS's usage of ManagedSessions
            //  is very dependent on the ability to use these two events for properly enlisting their
            //  unshared connections.  When a ManagedSession is first created, it is done under an LTC.
            //  When that LTC ends, some partial cleanup is done on the MCWrapper, but it is not put
            //  back into the pool because the unshared connection handle is still active.  One of the
            //  items that gets cleaned up is the coordinator within the MCWrapper.  This makes sense
            //  since the LTC has ended.  But, when it is determined by JMS that it's time to get
            //  enlisted in the current transaction, a null coordinator causes all kinds of problems
            //  (reference the string of defects that have attempted to correct this situation).  The
            //  solution determined seems to be the right one.  In both localTransactionStarted
            //  and interactionPending methods, we need to check if the MCWrapper coordinator is null and
            //  if it is null, then go out to the TM and get it updated.
            //
            // Note that this special null coordinator processing is due to the SmartHandle support
            //  utilized by JMS and RRA.  Without smart handles, the coordinator would have gotten
            //  re-initialized during the handle re-association logic.
            //
            // In addition, we have determined that the check for involvedInTransaction() is not necessary
            //  (and causes problems for the scenario described just previous).  Since the getCurrentTranWrapper()
            //  already checks for a valid state setting before returning, we can just rely on that method to
            //  do the proper checking instead of calling the involvedInTransaction() method.
            //
            UOWCoordinator uowCoordinator = mcWrapper.getUOWCoordinator();

            if (uowCoordinator == null) {
                uowCoordinator = mcWrapper.updateUOWCoordinator();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "uowCoord was null, updating it to current coordinator");
                }
            }

            // if the coordinator is still null, then we are running with NO transaction
            //  context on the thread.  This case currently needs to be supported because
            //  servlets can spin their own threads which would not have context.
            // Note: it is very rare that users do this.  All other occurances are
            //  considered to be an error.
            if (uowCoordinator == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(this, tc, "localTransactionStarted", "no transaction context, return without enlisting");
                }
                return;
            }

            /*
             * We know we are in a transaction but we need to verify that it is not a global transaction
             * before continuing.
             */
            if (tc.isDebugEnabled() && uowCoordinator.isGlobal()) {
                IllegalStateException ise = new IllegalStateException("Illegal attempt to start a local transaction within a global (user) transaction");
                Tr.debug(this, tc, "ILLEGAL_USE_OF_LOCAL_TRANSACTION_J2CA0295", ise);
            }

            /*
             * The ManagedConnection should be associated with a transaction. And, if it's not,
             * the getCurrentTranWrapper() method will detect the situation and throw an
             * exception.
             *
             * enlist() the ManagedConnection from the transaction.
             */

            try {
                mcWrapper.getCurrentTranWrapper().enlist();
            } catch (ResourceException e) {
                /*
                 * // Can't enlist, something went wrong.
                 * // Destroy the connection(s) so it can't cause any future problems.
                 * try {
                 * mcWrapper.markStale();
                 * mcWrapper.releaseToPoolManager();
                 * }
                 * catch (Exception ex) {
                 * // Nothing to do here. PoolManager has already logged it.
                 * // Since we are in cleanup mode, we will not surface a Runtime exception to the ResourceAdapter
                 * FFDCFilter.processException(ex, "com.ibm.ejs.j2c.ConnectionEventListener.localTransactionStarted", "473", this);
                 * if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                 * Tr.debug(this, tc, "localTransactionStarted: Error when trying to enlist " + mcWrapper.getPoolManager().getPmiName() +
                 * " caught exception, but will continue processing: ", ex);
                 * }
                 * }
                 */

                FFDCFilter.processException(e, "com.ibm.ejs.j2c.ConnectionEventListener.localTransactionStarted", "481", this);
                // add datasource name to message
                Tr.error(tc, "ENLIST_FAILED_J2CA0074", "localTransactionStarted", e, mcWrapper.gConfigProps.cfName);
                // Moved event.getSource() inside of catch block for performance reasons
                ManagedConnection mc = null;
                try {
                    mc = (ManagedConnection) event.getSource();
                } catch (ClassCastException cce) {
                    Tr.error(tc, "GET_SOURCE_CLASS_CAST_EXCP_J2CA0098", cce);
                    throw new IllegalStateException("ClassCastException occurred attempting to cast event.getSource to ManagedConnection");
                }
                ConnectionEvent errorEvent = new ConnectionEvent(mc, ConnectionEvent.CONNECTION_ERROR_OCCURRED);
                this.connectionErrorOccurred(errorEvent);

                RuntimeException rte = new IllegalStateException(e.getMessage());
                throw rte;
            }
        } else {
            // Connection Event passed in doesn't match the method called.
            // This should never happen unless there is an error in the ResourceAdapter.
            processBadEvent("localTransactionStarted", ConnectionEvent.LOCAL_TRANSACTION_STARTED, event);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "localTransactionStarted");
        }
        return;
    }

    private void processBadEvent(String methodName, int methodEventId, ConnectionEvent eventIn) {

        // Connection Event passed in doesn't match the method called.
        // This should never happen unless there is an error in the ResourceAdapter.
        String eventIdIn = Integer.toString(eventIn.getId());
        String xpathId = mcWrapper.gConfigProps.getXpathId();
        String cfName = mcWrapper.gConfigProps.cfName;

        Tr.error(tc, "UNEXPECTED_CONNECTION_EVENT_J2CA0034", methodEventId, eventIdIn, cfName);

        // Retrieve the corresponding ManagedConnection.
        ManagedConnection mc = null;
        try {
            mc = (ManagedConnection) eventIn.getSource();
        } catch (ClassCastException cce) {
            Tr.error(tc, "GET_SOURCE_CLASS_CAST_EXCP_J2CA0098", cce);
            RuntimeException rte = new IllegalStateException("processBadEvent: ClassCastException occurred attempting to cast event.getSource to ManagedConnection");
            FFDCFilter.processException(cce, "com.ibm.ejs.j2c.ConnectionEventListener.processBadEvent", "809", this);
            throw rte;
        }

        // Force the cleanup of the MC.
        ConnectionEvent errorEvent = new ConnectionEvent(mc, ConnectionEvent.CONNECTION_ERROR_OCCURRED);
        this.connectionErrorOccurred(errorEvent);

        // Throw a runtime exception
        RuntimeException rte = new IllegalStateException("Method " + methodName
                                                         + " detected an unexpected ConnectionEvent of " + eventIdIn
                                                         + " for DataSource/ConnectionFactory " + xpathId);
        FFDCFilter.processException(rte, "com.ibm.ejs.j2c.ConnectionEventListener.processBadEvent", "709", this);
        throw rte;
    }

    /**
     * @return Returns the mcWrapper.
     */
    public MCWrapper getMcWrapper() {
        return mcWrapper;
    }
}
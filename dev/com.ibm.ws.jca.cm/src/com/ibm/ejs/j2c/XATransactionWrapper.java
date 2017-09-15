/*******************************************************************************
 * Copyright (c) 1997, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.util.Properties;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.transaction.Synchronization;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.tx.jta.AbortableXAResource;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.j2c.TranWrapper;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;


/**
 * <P>XATransactionWrapper is a wrapper for the resource adapter's SPI XAResource object. It is
 * enlisted with the transaction manager and supports all the XAResource verbs as a two
 * phase capable XAResource.
 * 
 * <P>Scope : EJB server
 * 
 * <P>Object model : 1 instance per ManagedConnection (if required)
 * 
 */

public class XATransactionWrapper implements XAResource, Synchronization, TranWrapper { 

    private final MCWrapper mcWrapper;
    private XAResource xaResource;
    private boolean enlisted = false;
    private boolean hasRollbackOccured = false; 
    private static final TraceComponent tc =
                    Tr.register(XATransactionWrapper.class,
                                J2CConstants.traceSpec,
                                J2CConstants.messageFile); 


    protected XATransactionWrapper(MCWrapper mcWrapper) {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled()) { 
            Tr.entry(this, tc, "<init>");
        }

        this.mcWrapper = mcWrapper;

        if (isTracingEnabled && tc.isEntryEnabled()) { 
            Tr.exit(this, tc, "<init>");
        }

    }

    protected void initialize() throws ResourceException {

        if (xaResource == null) {

            try {
                xaResource = mcWrapper.getManagedConnection().getXAResource();
            } catch (ResourceException e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(
                                                            e,
                                                            "com.ibm.ejs.j2c.XATransactionWrapper.initialize",
                                                            "149",
                                                            this);
                Tr.error(tc, "FAILED_TO_OBTAIN_XAResource_J2CA0078", e, mcWrapper.gConfigProps.cfName);
                throw e;
            } catch (Exception e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(
                                                            e,
                                                            "com.ibm.ejs.j2c.XATransactionWrapper.initialize",
                                                            "154",
                                                            this);
                String pmiName = null;
                if (mcWrapper != null) {
                    pmiName = mcWrapper.gConfigProps.cfName;
                }
                Tr.error(tc, "FAILED_TO_OBTAIN_XAResource_J2CA0078", new Object[] { e, pmiName });
                ResourceException re = new ResourceException("initialize: caught Exception");
                re.initCause(e); 
                throw re;
            }

        }

    }

    /**
     * Reinitializes its own state such that it may be placed in the PoolManagers
     * pool for reuse. The XAResource will be retained.
     */
    public void cleanup() {
        enlisted = false; 
    }

    /**
     * Reinitializes its own state such that it may be reused.
     * The XAResource will be released.
     */
    public void releaseResources() {
        xaResource = null;
    }

    /**
     * Register the current object with the Synchronization manager for the current transaction
     * 
     * @throws ResourceException.
     */

    @Override
    public boolean addSync() throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled()) { 
            Tr.entry(this, tc, "addSync");
        }

        UOWCoordinator uowCoord = mcWrapper.getUOWCoordinator(); 

        if (uowCoord == null) {
            java.lang.IllegalStateException e = new java.lang.IllegalStateException("addSync: illegal state exception. uowCoord is null");
            Object[] parms = new Object[] { "addSync", e };
            Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "addSync", e);
            throw e;
        }

        try {
            if (mcWrapper.isConnectionSynchronizationProvider()) { 
                throw new NotSupportedException();
            }

            /*
             * This code will remain here just in case we run into this
             * case. We should not be isEnlistmentDisabled in this code.
             * 
             * Log a message and return, if isEnlistmentDisabled is true.
             */
            if (mcWrapper.isEnlistmentDisabled()) { 

                if (isTracingEnabled && tc.isDebugEnabled()) { 
                    Tr.debug(this, tc, "Managed connection isEnlistmentDisabled is true.");
                    Tr.debug(this, tc, "Returning without registering.");
                }

                if (isTracingEnabled && tc.isEntryEnabled()) { 
                    Tr.exit(this, tc, "addSync", false);
                }

                return false;

            }

            /*
             * Use our XATransactionWrapper
             */
            EmbeddableWebSphereTransactionManager tranMgr = mcWrapper.pm.connectorSvc.transactionManager;
            tranMgr.registerSynchronization(uowCoord, this); 

        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.addSync",
                                                        "237",
                                                        this);
            Object[] parms = new Object[] { "addSync", e, "ResourceException" };
            Tr.error(tc, "REGISTER_WITH_SYNCHRONIZATION_EXCP_J2CA0026", parms); 
            ResourceException re = new ResourceException("addSync: caught Exception");
            re.initCause(e); 
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "addSync", e);
            throw re;
        }

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.exit(this, tc, "addSync", true); 
        return true;
    }

    /**
     * Part of javax.transaction.Synchronization interface,
     * This method is called by the transaction manager after the transaction is committed or rolled back.
     * 
     * @param status The status of the transaction completion.
     */

    @Override
    public void afterCompletion(int status) {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled()) { 
            Tr.entry(this, tc, "afterCompletion");
        }

        this.hasRollbackOccured = false;

        if (isTracingEnabled && tc.isDebugEnabled()) { 

            if (mcWrapper.getHandleCount() != 0) {
                if (isTracingEnabled && tc.isDebugEnabled()) { 
                  Tr.debug(this, tc, "Information:  handle not closed at end of UOW for resource " + mcWrapper.gConfigProps.cfName);
                }
            }

        }

        if (mcWrapper.isMCAborted()) {
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "afterCompletion", "aborted");
            return;
        }

        /*
         * Mark the transaction complete in the wrapper
         */

        mcWrapper.transactionComplete();


        boolean shareable = mcWrapper.getConnectionManager().shareable();


        // If the connection is sharable, or it is non-sharable and the handleCount is
        // zero, then release it back to the poolManger, otherwise simply reset the
        // transaction related variables.
        //
        // Note: sharable connections are released at the end of transactions regardless of
        //  outstanding handles because we don't supports handled being associated to an active
        //  managedConnection outside of a sharing boundary.
        //
        if ((shareable) || (!shareable && mcWrapper.getHandleCount() == 0) || mcWrapper.isStale()) { 

            if (isTracingEnabled && tc.isDebugEnabled()) { 
                Tr.debug(this, tc,
                         "Releasing the connection to the pool. shareable = " + shareable + "  handleCount = " + mcWrapper.getHandleCount() + "  isStale = " + mcWrapper.isStale()); 
            }

            try {
                mcWrapper.releaseToPoolManager(); 
            } catch (Exception e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(
                                                            e,
                                                            "com.ibm.ejs.j2c.XATransactionWrapper.afterCompletion",
                                                            "291",
                                                            this);
                if (isTracingEnabled && tc.isDebugEnabled()) { 
                    Tr.debug(this, tc, "afterCompletionCode for resource " + mcWrapper.gConfigProps.cfName + ":  caught Exception", e);
                }

            }

        }
        else {
            /*
             * The cleanup of the coordinator and enlisted flag
             */
            enlisted = false;
            mcWrapper.setUOWCoordinator(null); 
        }

        if (isTracingEnabled && tc.isEntryEnabled()) { 
            Tr.exit(this, tc, "afterCompletion");
        }

    }

    /**
     * Part of javax.transaction.Synchronization interface,
     * This method is called by the transaction manager prior to the start of the transaction completion process.
     */

    @Override
    public void beforeCompletion() {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled()) { 
            Tr.entry(this, tc, "beforeCompletion");
            Tr.exit(this, tc, "beforeCompletion");
        }

    }

    /**
     * Excute a commit statement
     * 
     * @param xid A global transaction identifier
     * @param onePhase If true, the resource manager should use a one-phase commit protocol to commit the work done on behalf of xid.
     * 
     * @throws XAException An error has occurred. Possible XAExceptions are XA_HEURHAZ, XA_HEURCOM, XA_HEURRB, XA_HEURMIX, XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or
     *             XAER_PROTO.
     */

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "commit");
        }

        try {
            xaResource.commit(xid, onePhase);
        } catch (XAException e) {
            processXAException(e); 
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.commit",
                                                        "378",
                                                        this);
            if (!mcWrapper.isStale()) {
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_ID_EXCP_J2CA0027", "commit", xid, e, mcWrapper.gConfigProps.cfName);
            }
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", e);
            throw e;
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.commit",
                                                        "384",
                                                        this);
            if (!mcWrapper.shouldBeDestroyed()) { 
                mcWrapper.markTransactionError(); 
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_ID_EXCP_J2CA0027", "commit", xid, e, mcWrapper.gConfigProps.cfName);
            }
            XAException xae = new XAException(XAException.XAER_RMFAIL); 
            xae.initCause(e); 
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", e);
            throw xae; 
        }

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.exit(this, tc, "commit");
    }

    /**
     * Ends the work performed on behalf of a transaction branch. The resource manager disassociates the XA resource from the transaction branch specified and let the transaction
     * be completed.
     * 
     * <P>If TMSUSPEND is specified in flags, the transaction branch is temporarily suspended in incomplete state. The transaction context is in suspened state and must be resumed
     * via start with TMRESUME specified.
     * 
     * <P>If TMFAIL is specified, the portion of work has failed. The resource manager may mark the transaction as rollback-only
     * 
     * <P>If TMSUCCESS is specified, the portion of work has completed successfully.
     * 
     * @param xid A global transaction identifier that is the same as what was used previously in the start method.
     * @param flags One of TMSUCCESS, TMFAIL, or TMSUSPEND.
     * 
     * @throws XAException An error has occurred. Possible XAException values are XAER_RMERR, XAER_RMFAILED, XAER_NOTA, XAER_INVAL, XAER_PROTO, or XA_RB*.
     */

    @Override
    public void end(Xid xid, int flags) throws XAException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.entry(this, tc, "end"); 

        try {
            xaResource.end(xid, flags);
        } catch (XAException e) {
            processXAException(e); 
            if (flags == XAResource.TMFAIL && ((e.errorCode >= XAException.XA_RBBASE) && (e.errorCode <= XAException.XA_RBEND))) {
               // okay error codes for the rollback processing.   
            } else {
                com.ibm.ws.ffdc.FFDCFilter.processException(
                                                            e,
                                                            "com.ibm.ejs.j2c.XATransactionWrapper.end",
                                                            "417",
                                                            this);
                if (!mcWrapper.isStale()) {
                    Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_ID_EXCP_J2CA0027", "end", xid, e, mcWrapper.gConfigProps.cfName);
                }
                if (isTracingEnabled && tc.isEntryEnabled())
                    Tr.exit(this, tc, "end", e);
                throw e;
            }
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.end",
                                                        "423",
                                                        this);
            if (!mcWrapper.shouldBeDestroyed()) { 
                mcWrapper.markTransactionError(); 
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_ID_EXCP_J2CA0027", "end", xid, e, mcWrapper.gConfigProps.cfName);
            }
            XAException xae = new XAException(XAException.XAER_RMFAIL); 
            xae.initCause(e); 
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "end", xae);
            throw xae; 
        }

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.exit(this, tc, "end"); 
    } 

    /**
     * Enlist an XA resource - Overloaded to provide for XA Recovery
     * 
     */

    @Override
    public void enlist() throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.entry(this, tc, "enlist", mcWrapper.getUOWCoordinator()); 

        if (this.hasRollbackOccured) { 
            ResourceException x = new ResourceException("Attempt to continue working after transaction rolledback !");
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(tc, "enlist", "It is NOT valid to continue working under a transaction that has already rolledback");
            throw x;
        }
        if (enlisted == true) {

            if (isTracingEnabled && tc.isEntryEnabled()) { 
                Tr.exit(this, tc, "enlist", "already enlisted");
            }

            return; // Already enlisted, no-op.

        }

        try {
            int branchCoupling = mcWrapper.getCm().getResourceRefInfo().getBranchCoupling(); 
            int startFlag = XAResource.TMNOFLAGS; 
            if (branchCoupling != ResourceRefInfo.BRANCH_COUPLING_UNSET) 
            {
                startFlag = mcWrapper.getCm().supportsBranchCoupling(branchCoupling, mcWrapper.get_managedConnectionFactory()); 
                if (startFlag == -1)
                    throw new ResourceException("Branch coupling attribute not implemented for this resource"); 
            }
            int recoveryToken = mcWrapper.getRecoveryToken(); 
            EmbeddableWebSphereTransactionManager tranMgr = mcWrapper.pm.connectorSvc.transactionManager;
            if (tranMgr.enlist(mcWrapper.getUOWCoordinator(), this, recoveryToken, startFlag) != true) { 
                Tr.error(tc, "BAD_RETURN_VALUE_FROM_ENLIST_J2CA0087", this, mcWrapper.gConfigProps.cfName); 
                ResourceException x = new ResourceException("Error on enlist"); 
                if (isTracingEnabled && tc.isEntryEnabled())
                    Tr.exit(this, tc, "enlist", x);
                throw x;
            }
        } catch (ResourceException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.enlist",
                                                        "535",
                                                        this);

            Tr.error(tc, "ENLIST_RESOURCE_EXCP_J2CA0030", "enlist", e, "ResourceException", mcWrapper.gConfigProps.cfName); 

            mcWrapper.markTransactionError(); 
            try {
                EmbeddableWebSphereTransactionManager tranMgr = mcWrapper.pm.connectorSvc.transactionManager;
                tranMgr.getTransaction().setRollbackOnly();
            } catch (Exception ex) {

                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ejs.j2c.XATransactionWrapper.enlist",
                                                            "630", this);
                if (isTracingEnabled && tc.isEventEnabled()) { 
                    Tr.event(this, tc, "Caught Exception while trying to mark transaction RollbackOnly - Exception:" + ex);
                }

            }

            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "enlist", e);
            throw e;
        } catch (Exception e) {

            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.enlist",
                                                        "546",
                                                        this);

            String pmiName = null;
            if (mcWrapper != null) {
                pmiName = mcWrapper.gConfigProps.cfName;
            }
            Object[] parms = new Object[] { "enlist", e, "ResourceException", pmiName };
            Tr.error(tc, "ENLIST_RESOURCE_EXCP_J2CA0030", parms); 

            mcWrapper.markTransactionError(); 
            try {
                EmbeddableWebSphereTransactionManager tranMgr = mcWrapper.pm.connectorSvc.transactionManager;
                tranMgr.getTransaction().setRollbackOnly();
            } catch (Exception ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ejs.j2c.XATransactionWrapper.enlist",
                                                            "663", this);
                if (isTracingEnabled && tc.isEventEnabled()) { 
                    Tr.event(this, tc, "Caught Exception while trying to mark transaction RollbackOnly - Exception:" + ex);
                }

            }

            ResourceException re = new ResourceException("enlist: caught Exception");
            re.initCause(e); 
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "enlist", e);
            throw re;

        }

        enlisted = true;

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.exit(this, tc, "enlist");
    }

    /**
     * Delist an XA Resource.
     * 
     * @param flag
     */

    @Override
    public void delist() throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.entry(this, tc, "delist", mcWrapper.getUOWCoordinator()); 

        enlisted = false;

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.exit(this, tc, "delist"); 

    }

    /**
     * Tell the resource manager to forget about a heuristically completed transaction branch.
     * 
     * @param xid A global transaction identifier
     * 
     * @throws XAException An error has occurred. Possible exception values are XAER_RMERR, XAER_RMFAIL.
     */

    @Override
    public void forget(Xid xid) throws XAException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.entry(this, tc, "forget"); 

        try {
            xaResource.forget(xid);
        } catch (XAException e) {
            processXAException(e); 
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.forget",
                                                        "561",
                                                        this);
            if (!mcWrapper.isStale()) {
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_ID_EXCP_J2CA0027", "forget", xid, e, mcWrapper.gConfigProps.cfName);
            }
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "forget", e);
            throw e;
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.forget",
                                                        "567",
                                                        this);
            if (!mcWrapper.shouldBeDestroyed()) { 
                mcWrapper.markTransactionError(); 
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_ID_EXCP_J2CA0027", "forget", xid, e, mcWrapper.gConfigProps.cfName);
            }
            XAException xae = new XAException(XAException.XAER_RMFAIL); 
            xae.initCause(e); 
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "forget", e);
            throw xae; 
        }

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.exit(this, tc, "forget");
    }

    /**
     * Obtain the current transaction timeout value set for this XAResource instance. If XAResource.setTransactionTimeout was not use prior to invoking this method, the return
     * value is the default timeout set for the resource manager; otherwise, the value used in the previous setTransactionTimeout call is returned.
     * 
     * @return the transaction timeout value in seconds
     * 
     * @throws XAException An error has occurred. Possible exception values are XAER_RMERR, XAER_RMFAIL.
     */

    @Override
    public int getTransactionTimeout() throws XAException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.entry(this, tc, "getTransactionTimeout"); 

        int rc = -1;
        try {
            rc = xaResource.getTransactionTimeout();
        } catch (XAException e) {
            processXAException(e); 
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.getTransactionTimeout",
                                                        "611",
                                                        this);
            if (!mcWrapper.isStale()) {
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_EXCP_J2CA0028", "getTransactionTimeout", e, mcWrapper.gConfigProps.cfName);
            }
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "getTransactionTimeout", e);
            throw e;
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.getTransactionTimeout",
                                                        "618",
                                                        this);
            if (!mcWrapper.shouldBeDestroyed()) { 
                mcWrapper.markTransactionError(); 
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_EXCP_J2CA0028", "getTransactionTimeout", e, mcWrapper.gConfigProps.cfName);
            }
            XAException xae = new XAException(XAException.XAER_RMFAIL); 
            xae.initCause(e); 
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "getTransactionTimeout", e);
            throw xae; 
        }
        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.exit(this, tc, "getTransactionTimeout", rc); 
        return rc;

    }
    
    /**
     * This method is called to determine if the resource manager instance represented by the target object is the same as the resouce manager instance represented by the parameter
     * xares.
     * 
     * @param xares - An XAResource object whose resource manager instance is to be compared with the resource manager instance of the target object.
     * 
     * @return true if it's the same RM instance; otherwise false.
     * 
     * @throws XAException - An error has occurred. Possible exception values are XAER_RMERR, XAER_RMFAIL.
     */

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.entry(this, tc, "isSameRM"); 

        boolean rc = false;
        try {
            rc = xaResource.isSameRM(xares);
        } catch (XAException e) {

            processXAException(e); 
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.isSameRM",
                                                        "648",
                                                        this);
            if (!mcWrapper.isStale()) {
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_EXCP_J2CA0028", "isSameRM", e, mcWrapper.gConfigProps.cfName);
            }
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "isSameRM", e);
            throw e;

        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.isSameRM",
                                                        "655",
                                                        this);
            if (!mcWrapper.shouldBeDestroyed()) { 
                mcWrapper.markTransactionError(); 
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_EXCP_J2CA0028", "isSameRM", e, mcWrapper.gConfigProps.cfName);
            }
            XAException xae = new XAException(XAException.XAER_RMFAIL); 
            xae.initCause(e); 
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "isSameRM", e);
            throw xae; 
        }

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.exit(this, tc, "isSameRM", rc);
        return rc;

    }

    /**
     * Ask the resource manager to prepare for a transaction commit of the transaction specified in xid.
     * 
     * @param xid A global transaction identifier
     * 
     * @return A value indicating the resource manager's vote on the outcome of the transaction. The possible values are: XA_RDONLY or XA_OK. If the resource manager wants to roll
     *         back the transaction, it should do so by raising an appropriate XAException in the prepare method.
     * 
     * @throws XAException - An error has occurred. Possible exception values are: XA_RB*, XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     */

    @Override
    public int prepare(Xid xid) throws XAException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.entry(this, tc, "prepare"); 

        int rc = -1;
        try {
            rc = xaResource.prepare(xid);
        } catch (XAException e) {
            processXAException(e); 
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.prepare",
                                                        "686",
                                                        this);
            if (!mcWrapper.isStale()) {
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_ID_EXCP_J2CA0027", "prepare", xid, e, mcWrapper.gConfigProps.cfName);
            }
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepare", e);
            throw e;
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.prepare",
                                                        "692",
                                                        this);
            if (!mcWrapper.shouldBeDestroyed()) { 
                mcWrapper.markTransactionError(); 
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_ID_EXCP_J2CA0027", "prepare", xid, e, mcWrapper.gConfigProps.cfName);
            }
            XAException xae = new XAException(XAException.XAER_RMFAIL); 
            xae.initCause(e); 
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepare", e);
            throw xae; 
        }

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.exit(this, tc, "prepare", rc);
        return rc;

    }

    /**
     * Obtain a list of prepared transaction branches from a resource manager. The transaction manager calls this method during recovery to obtain the list of transaction branches
     * that are currently in prepared or heuristically completed states.
     * 
     * @param flag - One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS. TMNOFLAGS must be used when no other flags are set in flags.
     * 
     * @return The resource manager returns zero or more XIDs for the transaction branches that are currently in a prepared or heuristically completed state. If an error occurs
     *         during the operation, the resource manager should throw the appropriate XAException.
     * 
     * @throws XAException - An error has occurred. Possible values are XAER_RMERR, XAER_RMFAIL, XAER_INVAL, and XAER_PROTO.
     */

    @Override
    public Xid[] recover(int flag) throws XAException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.entry(this, tc, "recover"); 

        Xid[] rc;
        try {
            rc = xaResource.recover(flag);
        } catch (XAException e) {
            processXAException(e); 
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.recover",
                                                        "722",
                                                        this);
            if (!mcWrapper.isStale()) {
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_EXCP_J2CA0028", "recover", e, mcWrapper.gConfigProps.cfName);
            }
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "recover", e);
            throw e;
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.recover",
                                                        "728",
                                                        this);
            if (!mcWrapper.shouldBeDestroyed()) { 
                mcWrapper.markTransactionError(); 
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_EXCP_J2CA0028", "recover", e, mcWrapper.gConfigProps.cfName);
            }
            XAException xae = new XAException(XAException.XAER_RMFAIL); 
            xae.initCause(e); 
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "recover", e);
            throw xae; 
        }
        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.exit(this, tc, "recover", rc);
        return rc;

    }

    /**
     * Inform the resource manager to roll back work done on behalf of a transaction branch
     * 
     * @param xid - A global transaction identifier
     * 
     * @throws XAException - An error has occurred
     */

    @Override
    public void rollback(Xid xid) throws XAException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.entry(this, tc, "rollback"); 

        this.hasRollbackOccured = true; 

        try {
            xaResource.rollback(xid);
        } catch (XAException e) {
            processXAException(e); 
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.rollback",
                                                        "755",
                                                        this);
            if (!mcWrapper.isStale()) {
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_ID_EXCP_J2CA0027", "rollback", xid, e, mcWrapper.gConfigProps.cfName);
            }
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "rollback", e);
            throw e;
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.rollback",
                                                        "761",
                                                        this);
            if (!mcWrapper.shouldBeDestroyed()) { 
                mcWrapper.markTransactionError(); 
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_ID_EXCP_J2CA0027", "rollback", xid, e, mcWrapper.gConfigProps.cfName);
            }
            XAException x = new XAException("Exception:" + e.toString());
            x.initCause(e);
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "rollback", x);
            throw x;
        }

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.exit(this, tc, "rollback");
    }

    /**
     * Set the current transaction timeout value for this XAResource instance. Once set, this timeout value is effective until setTransactionTimeout is invoked again with a
     * different value. To reset the timeout value to the default value used by the resource manager, set the value to zero. If the timeout operation is performed successfully, the
     * method returns true; otherwise false. If a resource manager does not support transaction timeout value to be set explicitly, this method returns false.
     * 
     * @param seconds - transaction timeout value in seconds.
     * 
     * @return true if transaction timeout value is set successfully; otherwise false.
     * 
     * @throws XAException - An error has occurred. Possible exception values are XAER_RMERR, XAER_RMFAIL, or XAER_INVAL.
     */

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.entry(this, tc, "setTransactionTimeout"); 

        boolean rc = false;
        try {
            rc = xaResource.setTransactionTimeout(seconds);
        } catch (XAException e) {
            processXAException(e); 
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.setTransactionTimeout",
                                                        "790",
                                                        this);
            if (!mcWrapper.isStale()) {
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_EXCP_J2CA0028", "setTransactionTimeout", e, mcWrapper.gConfigProps.cfName);
            }
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "setTransactionTimeout", e);
            throw e;
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.setTransactionTimeout",
                                                        "796",
                                                        this);
            if (!mcWrapper.shouldBeDestroyed()) { 
                mcWrapper.markTransactionError(); 
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_EXCP_J2CA0028", "setTransactionTimeout", e, mcWrapper.gConfigProps.cfName);
            }
            XAException x = new XAException("Exception:" + e.toString());
            x.initCause(e);
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "setTransactionTimeout", e);
            throw x;
        }

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.exit(this, tc, "setTransactionTimeout", rc);
        return rc;

    }

    /**
     * Start work on behalf of a transaction branch specified in xid If TMJOIN is specified, the start is for joining a transaction previously seen by the resource manager. If
     * TMRESUME is specified, the start is to resume a suspended transaction specified in the parameter xid. If neither TMJOIN nor TMRESUME is specified and the transaction
     * specified by xid has previously been seen by the resource manager, the resource manager throws the XAException exception with XAER_DUPID error code.
     * 
     * @param xid A global transaction identifier to be associated with the resource
     * @param flags One of TMNOFLAGS, TMJOIN, or TMRESUME
     * 
     * @throws XAException - An error has occurred. Possible exceptions are XA_RB*, XAER_RMERR, XAER_RMFAIL, XAER_DUPID, XAER_OUTSIDE, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     */

    @Override
    public void start(Xid xid, int flags) throws XAException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.entry(this, tc, "start"); 

        try {
            xaResource.start(xid, flags);
        } catch (XAException e) {
            processXAException(e); 
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.start",
                                                        "824",
                                                        this);
            if (!mcWrapper.isStale()) {
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_ID_EXCP_J2CA0027", "start", xid, e, mcWrapper.gConfigProps.cfName);
            }
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "start", e);
            throw e;
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.XATransactionWrapper.start",
                                                        "830",
                                                        this);
            if (!mcWrapper.shouldBeDestroyed()) { 
                mcWrapper.markTransactionError(); 
                Tr.error(tc, "XA_RESOURCE_ADAPTER_OPERATION_ID_EXCP_J2CA0027", "start", xid, e, mcWrapper.gConfigProps.cfName);
            }
            XAException xae = new XAException(XAException.XAER_RMFAIL); 
            xae.initCause(e); 
            if (isTracingEnabled && tc.isEntryEnabled())
                Tr.exit(this, tc, "start", e);
            throw xae; 
        }

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.exit(this, tc, "start");
    } 

    /**
     * Method checks for failing error code coming back form an XAResource and marks the MCWrapper
     * as stale so that it doesn't get returned to the freepool.
     */

    public void processXAException(XAException xae) {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled(); 

        if ((xae.errorCode == XAException.XAER_RMERR) || (xae.errorCode == XAException.XAER_RMFAIL)) {

            if (isTracingEnabled && tc.isDebugEnabled()) { 
                Tr.debug(this, tc, "processXAException: detecting bad XAException error code. Marking MCWrapper stale. ");
            }

            mcWrapper.markTransactionError(); 

        }
        if (xae.errorCode != 0) {

            if (isTracingEnabled && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "processXAException: Non-zero return code from XAResource. Return code is:  ",
                         getXAExceptionCodeString(xae.errorCode));
            }

        }

    }

    public static String getXAExceptionCodeString(int code) {

        switch (code) {
            case XAException.XA_RBTRANSIENT:
                return "XA_RBTRANSIENT";
            case XAException.XA_RBROLLBACK:
                return "XA_RBROLLBACK";
            case XAException.XA_HEURCOM:
                return "XA_HEURCOM";
            case XAException.XA_HEURHAZ:
                return "XA_HEURHAZ";
            case XAException.XA_HEURMIX:
                return "XA_HEURMIX";
            case XAException.XA_HEURRB:
                return "XA_HEURRB";
            case XAException.XA_NOMIGRATE:
                return "XA_NOMIGRATE";
            case XAException.XA_RBCOMMFAIL:
                return "XA_RBCOMMFAIL";
            case XAException.XA_RBDEADLOCK:
                return "XA_RBDEADLOCK";
            case XAException.XA_RBINTEGRITY:
                return "XA_RBINTEGRITY";
            case XAException.XA_RBOTHER:
                return "XA_RBOTHER";
            case XAException.XA_RBPROTO:
                return "XA_RBPROTO";
            case XAException.XA_RBTIMEOUT:
                return "XA_RBTIMEOUT";
            case XAException.XA_RDONLY:
                return "XA_RDONLY";
            case XAException.XA_RETRY:
                return "XA_RETRY";
            case XAException.XAER_ASYNC:
                return "XAER_ASYNC";
            case XAException.XAER_DUPID:
                return "XAER_DUPID";
            case XAException.XAER_INVAL:
                return "XAER_INVAL";
            case XAException.XAER_NOTA:
                return "XAER_NOTA";
            case XAException.XAER_OUTSIDE:
                return "XAER_OUTSIDE";
            case XAException.XAER_PROTO:
                return "XAER_PROTO";
            case XAException.XAER_RMERR:
                return "XAER_RMERR";
            case XAException.XAER_RMFAIL:
                return "XAER_RMFAIL";

        }
        return "UNKNOWN XA EXCEPTION CODE: " + code;
    }

    @Override
    public String toString() {

        StringBuffer buf = new StringBuffer(256);

        buf.append("XATransactionWrapper@ ");
        buf.append(Integer.toHexString(this.hashCode()));

        buf.append("  XAResource: ");
        buf.append(xaResource);

        buf.append("  enlisted: ");
        buf.append(enlisted);

        buf.append("Has Tran Rolled Back = "); 
        buf.append(this.hasRollbackOccured); 

        buf.append("  mcWrapper.hashCode()");
        buf.append(mcWrapper.hashCode());

        return buf.toString();
    }


    /**
     * 
     * @return Returns the mcWrapper.
     */
    public MCWrapper getMcWrapper() {
        return mcWrapper;
    }

    /**
     * Indicates whether this TranWrapper implementation instance is RRS transactional
     * 
     * @exception ResourceException
     */
    @Override
    public boolean isRRSTransactional() {
        return false;
    }

}
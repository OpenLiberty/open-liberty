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

/*
 * Class name   : RRSGlobalTransactionWrapper
 *
 * Scope        : EJB server
 *
 * Object model : 1 instance per ManagedConnection (if required)
 *
 * The RRSGlobalTransactionWrapper is a wrapper for managing global
 * transactions across resource adapters that are configured to support
 * transaction coordination using RRS.
 */
import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnection;
import javax.transaction.Synchronization;

import com.ibm.tx.jta.impl.RegisteredSyncs;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.j2c.TranWrapper;
import com.ibm.ws.jca.adapter.WSManagedConnection;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;

/**
 * 
 * <P> The RRSGlobalTransactionWrapper is a wrapper for managing global
 * transactions across resource adapters that are configured to support
 * transaction coordination using RRS.
 * 
 * <P>Scope : EJB server
 * 
 * <P>Object model : 1 instance per ManagedConnection (if required)
 * 
 */
public class RRSGlobalTransactionWrapper implements Synchronization, TranWrapper { 
    private final MCWrapper mcWrapper;
    private boolean enlisted = false;

    private static final TraceComponent tc =
                    Tr.register(RRSGlobalTransactionWrapper.class,
                                J2CConstants.traceSpec,
                                J2CConstants.messageFile); 

    /**
     * Constructor
     * 
     * @param mcWrapper
     */

    protected RRSGlobalTransactionWrapper(MCWrapper mcWrapper) throws ResourceException { 

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "<init>");
        }

        this.mcWrapper = mcWrapper;

        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "<init>");
        }

    }

    /**
     * Performs any special initialization that the
     * RRSGlobalTransactionWrapper may need to do before it can be used.
     * 
     * @throws ResourceException (not really, but other wrappers do, so caller
     *             will be catching ResourceException)
     */
    protected void initialize() throws ResourceException {
        // Nothing to initialize.
    }

    /**
     * Cleans up the RRSGlobalTransactionWrapper so it can be placed
     * with the MCWrapper in the PoolManager's pool for reuse.
     */
    public void cleanup() {
        enlisted = false; 
    }

    /**
     * Reinitializes state of RRSGlobalTransactionWrapper so it may be
     * reused
     */
    public void releaseResources() {
        // Nothing to release.
    }

    /**
     * Register the RRSGlobalTransactionWrapper as a sync object with the
     * Transaction Manager for the current transaction.
     * 
     * @throws ResourceException.
     * @return boolean
     */

    @Override
    public boolean addSync() throws ResourceException {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "addSync");
        }

        UOWCoordinator uowCoord = mcWrapper.getUOWCoordinator();

        if (uowCoord == null) {
            IllegalStateException e = new IllegalStateException("addSync: illegal state exception. uowCoord is null");
            Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", "addSync", e);
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "addSync", e);
            throw e;
        }

        try {
            // added a second synchronization
            //
            // RRS Transactions don't follow the XA model, and therefore don't receive callbacks for
            // end, prepare, and commit/rollback.  Defect  added State Management for RRS
            // controlled transactions, and because the state on the adapter is, for XA transactions,
            // reset during the commit callback, we need to reset the adapter state as close as
            // possible after the commit time.  Therefore, we need to register as a priority sync
            // for the purpose of resetting the adapter state.  We need to also register as a normal
            // sync, however, because we have additional afterCompletion code that returns the
            // managed connection to the free pool.  This code must be executed AFTER DB2 gets its
            // afterCompletion callback.  DB2 is also registered as a priority sync, and since we
            // can't guarantee the order of the afterCompletion callbacks if two syncs are registered
            // as priority, we need to register as a regular sync to execute this part of the code.
            EmbeddableWebSphereTransactionManager tranMgr = mcWrapper.pm.connectorSvc.transactionManager;
            tranMgr.registerSynchronization(uowCoord, this); 
            final ManagedConnection mc = mcWrapper.getManagedConnection();

            // Registering a synchronization object with priority SYNC_TIER_RRS (3) allows the
            // synchronization to be called last.
            if (mc instanceof WSManagedConnection)
            {
                tranMgr.registerSynchronization( 
                uowCoord,
                                                new Synchronization()
                                                {
                                                    @Override
                                                    public void beforeCompletion() 
                                                    {
                                                    } 

                                                    @Override
                                                    public void afterCompletion(int status) 
                                                    {
                                                        ((WSManagedConnection) mc).afterCompletionRRS();
                                                    }
                                                },
                                                RegisteredSyncs.SYNC_TIER_RRS
               );
            }
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.RRSGlobalTransactionWrapper.addSync",
                                                        "238",
                                                        this);
            Tr.error(tc, "REGISTER_WITH_SYNCHRONIZATION_EXCP_J2CA0026", "addSync", e, "ResourceException");
            ResourceException re = new ResourceException("addSync: caught Exception");
            re.initCause(e); 
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "addSync", e);
            throw re;
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "addSync", true);
        }
        return true;
    }

    /**
     * Part of javax.transaction.Synchronization interface,
     * This method is called by the Transaction Manager prior to the
     * start of the transaction completion process.
     */

    @Override
    public void beforeCompletion() {
        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "beforeCompletion");
            Tr.exit(this, tc, "beforeCompletion");
        }
    }

    /**
     * Part of javax.transaction.Synchronization interface,
     * This method is called by the transaction manager after the
     * transaction is committed or rolled back.
     * 
     * @param status The status of the transaction completion.
     */

    @Override
    public void afterCompletion(int status) {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "afterCompletion");
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Using transaction wrapper@" + Integer.toHexString(this.hashCode()));
        }

        /*
         * Mark the transaction complete in the wrapper
         */

        // Do NOT catch runtime exceptions here.  Let them flow out
        // of the component.

        mcWrapper.transactionComplete();

        ManagedConnection mc = mcWrapper.getManagedConnection(); 
        if (mc instanceof WSManagedConnection)
            ((WSManagedConnection) mc).afterCompletionRRS();

        // Check if any connection handles are still in use      
        if (tc.isDebugEnabled()) {
            if (mcWrapper.getHandleCount() != 0) {
                // Issue warning that Connections have not been closed by
                // the end of the current UOW scope and will be closed
                // by the CM.
                //            Tr.warning(this, tc,"HANDLE_NOT_CLOSED_J2CA0055");
                Tr.debug(this, tc, "Information:  handle not closed at end of UOW for resource " + mcWrapper.gConfigProps.cfName);
            }
        }

        // Do NOT catch the runtime exception which getConnectionManager
        // might throw. If this is thrown it is an internal bug and needs
        // to be fixed. Allow the rte to flow up to the container.
        boolean shareable = mcWrapper.getConnectionManager().shareable();

        //
        // If the connection is shareable, or it is non-shareable and the
        // handleCount is zero, then release it back to the poolManger,
        // otherwise simply reset the transaction related variables.
        //
        // Note: shareable connections are released at the end of
        // transactions regardless of outstanding handles because we don't
        // support handled being associated to an active
        //  managedConnection outside of a sharing boundary.
        if ((shareable) || (!shareable && mcWrapper.getHandleCount() == 0)) {

            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "afterCompletionCode: Releasing the connection to the pool. Shareable = " + shareable);
            }

            try {
                mcWrapper.releaseToPoolManager();
            } catch (Exception e) {
                // Do not rethrow the exception here since the client has
                // successfully completed the transaction.
                // Log the error and go on.

                com.ibm.ws.ffdc.FFDCFilter.processException(
                                                            e,
                                                            "com.ibm.ejs.j2c.RRSGlobalTransactionWrapper.afterCompletion",
                                                            "292",
                                                            this);
                if (tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "afterCompletionCode for resource " + mcWrapper.gConfigProps.cfName, e);
                }
            }

        } // end ((shareable) || (!shareable && mcWrapper.getHandleCount() == 0))
        else {
            /*
             * The cleanup of the coordinator and enlisted flag should
             * only be done here for non sharable connections. In other cases,
             * the coord is set to null in mcWrapper.releaseToPoolManager().
             * We only need to worry about this here if the handleCount is
             * greater than 0 (in which case the cleanup will be done
             * by the poolManager). Same thing for the tranFailed flag in
             * the MCWrapper.
             */
            enlisted = false;
            //      mcWrapper.setTranFailed(false);
            mcWrapper.setUOWCoordinator(null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "afterCompletion"); 
        }

    }

    /**
     * Enlist
     * 
     * @throws ResourceException (not really, but other wrappers do, so caller
     *             will be catching ResourceException)
     * 
     *             NOTE: In the case of RRSTransactional support, we will simply
     *             set the enlist indicator on. However, the real enlistment
     *             on the transaction is actually done by the backend
     *             Resource Manager. Thus, enlistment is really deferred
     *             until the backend Resource Manager receives the context
     *             from the connector at which time it registers an interest
     *             in the transaction using RRS services.
     * 
     */

    @Override
    public void enlist() throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "enlist");
        }

        if (!enlisted) { 
            try {
                int branchCoupling = mcWrapper.getCm().getResourceRefInfo().getBranchCoupling();
                int startFlag = javax.transaction.xa.XAResource.TMNOFLAGS;

                if (branchCoupling != ResourceRefInfo.BRANCH_COUPLING_UNSET) {
                    startFlag = mcWrapper.getCm().supportsBranchCoupling(branchCoupling, mcWrapper.get_managedConnectionFactory()); 
                    if (startFlag == -1)
                        throw new ResourceException("Branch coupling attribute not implemented for this resource");
                }

                mcWrapper.enlistRRSXAResource(mcWrapper.getRecoveryToken(), startFlag);
            } catch (ResourceException re) {
                throw re;
            } catch (Throwable t) {
                throw new ResourceException(t);
            }
        }

        ManagedConnection mc = mcWrapper.getManagedConnection(); 
        if (mc instanceof WSManagedConnection)
            ((WSManagedConnection) mc).enlistRRS();

        // Indicate that the  ManagedConnection that this transaction
        // wrapper is associated with is "dirty" (i.e, the managed
        // connection is involved in the transaction.
        enlisted = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "enlist", enlisted);
        }

    }

    /**
     * Delist
     * 
     * @throws ResourceException (not really, but other wrappers do, so caller
     *             will be catching ResourceException)
     */

    @Override
    public void delist() throws ResourceException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "delist");
        }

        enlisted = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "delist");
        }
    }

    /*
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();

        buf.append("RRSGlobalTransactionWrapper");
        buf.append(Integer.toHexString(this.hashCode()));
        buf.append("  enlisted");
        buf.append(enlisted);

        return buf.toString();
    }

    /**
     * Indicates whether this TranWrapper implementation instance is RRS transactional
     * 
     * @exception ResourceException
     */
    @Override
    public boolean isRRSTransactional() {
        return true;
    }

} 
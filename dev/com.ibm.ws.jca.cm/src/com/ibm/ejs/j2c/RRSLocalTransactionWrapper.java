/*******************************************************************************
 * Copyright (c) 2001, 2015 IBM Corporation and others.
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
 * Class name   : RRSLocalTransactionWrapper
 *
 * Scope        : EJB server
 *
 * Object model : 1 instance per ManagedConnection (if required)
 *
 * The RRSLocalTransactionWrapper is a wrapper for managing RRS local
 * transactions across resource adapters that are configured to support
 * transaction coordination using RRS.
 */
import javax.resource.ResourceException;
import javax.transaction.Synchronization;
import javax.transaction.xa.XAResource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.j2c.TranWrapper;

/**
 * 
 * <P> The RRSLocalTransactionWrapper is a wrapper for managing RRS
 * local transactions across resource adapters that are configured to
 * support transaction coordination using RRS.
 * 
 * <P>Scope : EJB server
 * 
 * <P>Object model : 1 instance per ManagedConnection (if required)
 * 
 */
public class RRSLocalTransactionWrapper implements Synchronization, TranWrapper { 
    private String RRSWrapperObject_hexString = null;
    private final MCWrapper mcWrapper;
    private XAResource rrsXAResource = null; 
    private boolean enlisted = false; 
    private boolean registeredForSync = false; 

    private static final TraceComponent tc = Tr.register(RRSLocalTransactionWrapper.class, J2CConstants.traceSpec, J2CConstants.messageFile); 

    /**
     * Constructor
     * 
     * @param mcWrapper
     */
    protected RRSLocalTransactionWrapper(MCWrapper mcWrapper) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) { 
            Tr.entry(this, tc, "<init>");
        }

        this.mcWrapper = mcWrapper;
        RRSWrapperObject_hexString = Integer.toHexString(this.hashCode());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) { 
            Tr.exit(this, tc, "<init> Id " + RRSWrapperObject_hexString);
        }

    }

    /**
     * Performs any special initialization that the
     * RRSLocalTransactionWrapper may need to do before it can be used.
     * 
     * @throws ResourceException
     */
    protected void initialize() throws ResourceException {
        // Nothing to initialize.
    }

    /**
     * Cleans up the RRSLocalTransactionWrapper so it can be placed
     * with the MCWrapper in the PoolManager's pool for reuse.
     */
    public void cleanup() {
        enlisted = false; 
    }

    /**
     * Reinitializes state of RRSLocalTransactionWrapper so it may be
     * reused
     */
    public void releaseResources() {
        // Nothing to release.
    }

    /**
     * Register the RRSLocalTransactionWrapper as a sync object with the
     * Transaction Manager for the current transaction.
     * 
     * @throws ResourceException.
     * @return boolean
     */

    @Override
    public boolean addSync() throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) { 
            Tr.entry(this, tc, "addSync");
        }

        UOWCoordinator uowCoord = mcWrapper.getUOWCoordinator();

        if (uowCoord == null) {
            // No transaction context.  Should never happen.
            // removed null check on mcWrapper since it is redundant from the above code (mcWrapper.getUOWCoordinator())
            String pmiName = mcWrapper.gConfigProps.cfName;

            Object[] parms = new Object[] { "addSync", pmiName };
            Tr.error(tc, "NO_VALID_TRANSACTION_CONTEXT_J2CA0040", parms); 
            throw new ResourceException("INTERNAL ERROR: No valid transaction context present");

        }

        try {

            //  No need to register for synchronization for unshared/LTC/res-control=Application.
            //  Instead of cleanup being done via after completion it will either happen at close or
            //  commit/rollback which ever happens last.  This will allow the connection to be returned
            //  to the pool prior to the end of the LTC scope allowing better utilization of the connection.
            boolean shareable = mcWrapper.getConnectionManager().shareable();
            if (!shareable && !J2CUtilityClass.isContainerAtBoundary(mcWrapper.pm.connectorSvc.transactionManager)) {
                //Register an instance of a no-transaction wrapper
                // so the TM will create an RRS NativeLocalTranasction.  The wrapper
                // provides "empty" TransactioWrapper and Synchronization behaviors,
                // but indicates to the TM that the LTC requires RRS support.  
                ((LocalTransactionCoordinator) uowCoord).enlistSynchronization(new RRSNoTransactionWrapper());

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) { 
                    Tr.exit(this, tc, "addSync", "returning without registering.");
                }
                return false;
            } else { // Register a synchronization with the Transaction Manager
                ((LocalTransactionCoordinator) uowCoord).enlistSynchronization(this);
                mcWrapper.markRRSLocalTransactionWrapperInUse();
                registeredForSync = true;
            }
        } catch (ResourceException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.RRSLocalTransactionWrapper.addSync", "594", this);
            Object[] parms = new Object[] { "addSync", e, "Exception" };
            Tr.error(tc, "REGISTER_WITH_SYNCHRONIZATION_EXCP_J2CA0026", parms);
            throw e;
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.RRSLocalTransactionWrapper.addSync", "605", this);
            Object[] parms = new Object[] { "addSync", e, "Exception" };
            Tr.error(tc, "REGISTER_WITH_SYNCHRONIZATION_EXCP_J2CA0026", parms);
            ResourceException re = new ResourceException("addSync: caught Exception");
            re.initCause(e);
            throw re;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) { 
            Tr.exit(this, tc, "addSync", registeredForSync);
        }

        return registeredForSync;

    }
    
    /**
     * Part of javax.transaction.Synchronization interface,
     * This method is called by the Transaction Manager prior to the
     * start of the transaction completion process.
     */

    @Override
    public void beforeCompletion() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) { 
            Tr.entry(this, tc, "beforeCompletion");
            Tr.exit(this, tc, "beforeCompletion");
        }
    }

    /**
     * Part of javax.transaction.Synchronization interface,
     * This method is called by the transaction manager after the
     * transaction is committed or rolled back.
     * 
     * 
     * @param status The status of the transaction completion.
     */

    @Override
    public void afterCompletion(int status) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) { 
            Tr.entry(this, tc, "afterCompletion", status);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
            Tr.debug(this, tc, "Using transaction wrapper@" + Integer.toHexString(this.hashCode()));
        }


        // When afterCompletionCode is called we need to reset the registeredForSync flag.
        registeredForSync = false;

        /*
         * Mark the transaction complete in the wrapper
         */

        // Do NOT catch runtime exceptions here.  Let them flow out
        // of the component.
        mcWrapper.transactionComplete();

        // Check if any connection handles are still in use
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
            if (mcWrapper.getHandleCount() != 0) {
                // Issue warning that Connections have not been closed by
                // the end of the current UOW scope and will be closed
                // by the CM.
                //            Tr.warning(this, tc,"HANDLE_NOT_CLOSED_J2CA0055");
                // removed null check on mcWrapper since it is redundant from the above code (mcWrapper.getUOWCoordinator())
                String pmiName = mcWrapper.gConfigProps.cfName;
                Tr.debug(this, tc, "Information:  handle not closed at end of UOW for resource " + pmiName);
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
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
                Tr.debug(this, tc, "afterCompletionCode: Releasing the connection to the pool. Shareable = " + shareable);
            }

            try {
                mcWrapper.releaseToPoolManager();
            } catch (Exception e) {
                // Do not rethrow the exception here since the client has
                // successfully completed the transaction.
                // Log the error and go on.

                // need to change the "291" number below
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.RRSLocalTransactionWrapper.afterCompletion", "291", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
                    String pmiName = mcWrapper.gConfigProps.cfName;
                    Tr.debug(this, tc, "afterCompletionCode for resource " + pmiName + ":  caught Exception", e);
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
            //     mcWrapper.setTranFailed(false);
            mcWrapper.setUOWCoordinator(null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "afterCompletion");

    }

    /**
     * Enlist
     * 
     * NOTE: In the case of RRSTransactional support, we will simply
     * set the enlist indicator on. However, the real enlistment
     * on the transaction is actually done by the backend
     * Resource Manager. Thus, enlistment is really deferred
     * until the backend Resource Manager receives the context
     * from the connector at which time it registers an interest
     * in the transaction using RRS services.
     * 
     */

    @Override
    public void enlist() throws ResourceException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) { 
            Tr.entry(this, tc, "enlist");
        }

        // Enlist a one phase resource representing RRS.
        if (!enlisted) { 
            int dummyInt = -1;
            try {
                rrsXAResource = mcWrapper.enlistRRSXAResource(dummyInt, dummyInt);
            } catch (Throwable t) {
                throw new ResourceException(t);
            }
        }
        // Indicate that the  ManagedConnection that this transaction
        // wrapper is associated with is "dirty" (i.e, the managed
        // connection is involved in the transaction.
        enlisted = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) { 
            Tr.exit(this, tc, "enlist");
        }
    }

    /**
     * Delist
     */

    @Override
    public void delist() throws ResourceException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) { 
            Tr.entry(this, tc, "delist");
        }

        if (enlisted) { 
            try {
                mcWrapper.delistRRSXAResource(rrsXAResource);
                rrsXAResource = null;
            } catch (Throwable t) {
                throw new ResourceException(t);
            }
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

        buf.append("RRSLocalTransactionWrapper@");
        buf.append(RRSWrapperObject_hexString);
        buf.append("  enlisted:");
        buf.append(enlisted);
        buf.append("  registeredForSync");
        buf.append(registeredForSync);

        return buf.toString();
    }

    @Override
    public boolean isRRSTransactional() {
        return true;
    }

}
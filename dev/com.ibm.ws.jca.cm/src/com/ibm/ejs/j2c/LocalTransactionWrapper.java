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

import javax.resource.ResourceException;
import javax.resource.spi.LocalTransaction;
import javax.transaction.Synchronization;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.tx.jta.OnePhaseXAResource;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.j2c.TranWrapper;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.uow.embeddable.SynchronizationRegistryUOWScope;

class LocalTransactionWrapper implements OnePhaseXAResource, Synchronization, TranWrapper {

    private final MCWrapper mcWrapper;
    private LocalTransaction localTransaction;
    private XAResource rrsXAResource = null;
    private boolean enlisted = false;
    //private Xid xid = null;
    private boolean registeredForSync = false;
    private boolean hasRollbackOccured = false;
    private static final TraceComponent tc = Tr.register(LocalTransactionWrapper.class,
                                                         J2CConstants.traceSpec,
                                                         J2CConstants.messageFile);

    private String _hexString = "";
    private boolean _rrsTransactional = false;

    /*
     * Constructor
     */

    protected LocalTransactionWrapper(MCWrapper mcWrapper) {
        this.mcWrapper = mcWrapper;

        _hexString = Integer.toHexString(this.hashCode());
    }

    protected void initialize() throws ResourceException {

        if (localTransaction == null) {

            try {
                localTransaction = mcWrapper.getManagedConnection().getLocalTransaction();
            } catch (ResourceException e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(
                                                            e,
                                                            "com.ibm.ejs.j2c.LocalTransactionWrapper.initialize",
                                                            "100",
                                                            this);
                Tr.error(tc, "FAILED_TO_OBTAIN_LOCALTRAN_J2CA0077", e, mcWrapper.gConfigProps.cfName);
                throw e;
            } catch (Exception e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(
                                                            e,
                                                            "com.ibm.ejs.j2c.LocalTransactionWrapper.initialize",
                                                            "105",
                                                            this);
                String pmiName = null;
                if (mcWrapper != null) {
                    pmiName = mcWrapper.gConfigProps.cfName;
                }
                Tr.error(tc, "FAILED_TO_OBTAIN_LOCALTRAN_J2CA0077", e, pmiName);
                ResourceException re = new ResourceException("initialize: caught Exception");
                re.initCause(e);
                throw re;
            }

        } // end if localTransaction == null

    } // end initialize

    // OnePhaseXAResource methods

    // getResourceName is used by the TM to provide additional debug information.
    //  The most meaningful string to return is the xpath unique identifier
    @Override
    public String getResourceName() {

        String nameString = null;
        nameString = mcWrapper.gConfigProps.getXpathId();

        return nameString;

    }

    // XAResource methods

    /*
     * This method is called by the transaction manager to commit the current transaction. The
     * resource adapter is called to commit and then end of transaction processing is performed
     * for the ManagedConnection
     */

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "commit");
        }

        /*
         *
         * if (this.xid != null && !xid.equals(this.xid)) {
         * final XAException e = new XAException(XAException.XAER_NOTA);
         * // What does this error really mean. Need to resolve
         * if (tc.isEventEnabled())
         * Tr.event(this, tc, "Error: Connection is already involved in different transaction.", e);
         * throw e;
         * }
         */

        if (!onePhase) {
            String xPath = null;
            if (mcWrapper != null) {
                xPath = mcWrapper.gConfigProps.getXpathId();
            }
            Tr.error(tc, "XA_OP_NOT_SUPPORTED_J2CA0016", "commit", xid, xPath);
            XAException x = new XAException(XAException.XAER_PROTO);
            if (tc.isEntryEnabled()) {
                Tr.exit(this, tc, "commit", x);
            }
            throw x;
        }

        boolean exceptionCaught = false;
        try {
            localTransaction.commit();
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.LocalTransactionWrapper.commit",
                                                        "164",
                                                        this);
            if (!mcWrapper.shouldBeDestroyed()) {
                mcWrapper.markTransactionError(); // Can't trust the connection since we don't know why it failed.
                Tr.error(tc, "XA_END_EXCP_J2CA0024", "commit", xid, e, "XAException", mcWrapper.gConfigProps.getXpathId());
            }
            exceptionCaught = true;
            XAException xae = new XAException(XAException.XA_HEURHAZ);
            xae.initCause(e);
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", xae);
            throw xae;
        } finally {
            //this.xid = null;
            try {
                if (registeredForSync == false) {
                    // Since we didn't register for synchronization for unshared/LTC/res-control=Application.
                    //  Instead of cleanup being done via after completion it will either happen at close or
                    //  commit/rollback which ever happens last.  This will allow the connection to be returned
                    //  to the pool prior to the end of the LTC scope allowing better utilization of the connection.
                    if (tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "commit: calling afterCompletionCode() for cleanup");
                    }
                    afterCompletion(0);
                }
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "commit: caught exception in finally block: ", e);
                }
                if (exceptionCaught == false) {
                    XAException xae = new XAException(XAException.XAER_RMFAIL);
                    xae.initCause(e);
                    if (tc.isEntryEnabled())
                        Tr.exit(this, tc, "commit", xae);
                    throw xae;
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "commit");
    } // end commit

    /*
     * This method is called by the transaction manager to roll back the current transaction. The
     * resource adapter is called to rollback and then end of transaction processing is performed
     * for the ManagedConnection
     */

    @Override
    public void rollback(Xid xid) throws XAException {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "rollback");
        }

        /*
         *
         * if (this.xid != null && !xid.equals(this.xid)) {
         * final XAException e = new XAException(XAException.XAER_NOTA);
         * // What does this error really mean. Need to resolve
         * if (tc.isEventEnabled())
         * Tr.event(this, tc, "Error: Connection is already involved in different transaction.", e);
         * throw e;
         * }
         */
        this.hasRollbackOccured = true;
        boolean exceptionCaught = false;
        try {
            localTransaction.rollback();
        } catch (Exception e) {
            if (!mcWrapper.isMCAborted())
                com.ibm.ws.ffdc.FFDCFilter.processException(e, getClass().getName(), "198", this);
            if (!mcWrapper.shouldBeDestroyed()) {
                mcWrapper.markTransactionError(); // Can't trust the connection since we don't know why it failed.
                Tr.error(tc, "XA_END_EXCP_J2CA0024", "rollback", xid, e, "XAException", mcWrapper.gConfigProps.getXpathId());
            }
            exceptionCaught = true;
            XAException xae = new XAException(XAException.XAER_RMFAIL);
            xae.initCause(e);
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "rollback", e);
            throw xae;
        } finally {
            //this.xid = null;
            try {
                if (registeredForSync == false) {
                    // Since we didn't register for synchronization for unshared/LTC/res-control=Application.
                    //  Instead of cleanup being done via after completion it will either happen at close or
                    //  commit/rollback which ever happens last.  This will allow the connection to be returned
                    //  to the pool prior to the end of the LTC scope allowing better utilization of the connection.
                    if (tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "rollback: calling afterCompletionCode() for cleanup");
                    }
                    afterCompletion(0);
                }
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "rollback: caught exception in finally block: ", e);
                }
                if (exceptionCaught == false) {
                    XAException xae = new XAException(XAException.XAER_RMFAIL);
                    xae.initCause(e);
                    throw xae;
                }
            }
        } // end rollback
        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "rollback");
    }

    @Override
    public void delist() throws ResourceException {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "delist");
        }

        try {
            UOWCoordinator uowCoord = mcWrapper.getUOWCoordinator();

            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "delisting LocalTranWrapper :" + Integer.toHexString(this.hashCode()) + " with coordinator :" + uowCoord);
            }

            if (uowCoord != null && !uowCoord.isGlobal()) {
                // LTC Scope
                // Only delist if resolutionControl is Application.  Transaction Manager will handle it
                //  if resolutionControl is  ContainerAtBoundary.
                if (!((LocalTransactionCoordinator) uowCoord).isContainerResolved()) {

                    if (tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "calling delistFromCleanup(OnePhaseXAResource)"
                                           + Integer.toHexString(this.hashCode()));
                    }

                    ((LocalTransactionCoordinator) uowCoord).delistFromCleanup(this);

                    if (_rrsTransactional && enlisted) {
                        mcWrapper.delistRRSXAResource(rrsXAResource);
                        rrsXAResource = null;
                    }

                    enlisted = false;

                    try {
                        if (registeredForSync == false) {
                            // Since we didn't register for synchronization for unshared/LTC/res-control=Application.
                            //  Instead of cleanup being done via afterCompletion it will either happen at close or
                            //  commit/rollback which ever happens last.  This will allow the connection to be returned
                            //  to the pool prior to the end of the LTC scope allowing better utilization of the connection.
                            // The afterCompletionCode call is being called from here, knowing that the Handle Count
                            //  could not possible be zero, thus the afterCompletionCode will simply reset the varios
                            //  variables asssocitated with the transaction.  Later when close is called it will be
                            //  returned to the pool manager.
                            if (tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "delist: calling afterCompletionCode() for cleanup");
                            }
                            afterCompletion(0);
                        }
                    } catch (Exception e) {
                        if (tc.isEntryEnabled())
                            Tr.exit(this, tc, "delist", e);
                        throw e;
                    }
                }
            }
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.LocalTransactionWrapper.delist",
                                                        "280",
                                                        this);
            String pmiName = null;
            if (mcWrapper != null) {
                pmiName = mcWrapper.gConfigProps.cfName;
            }
            Tr.error(tc, "DELIST_RESOURCE_EXCP_J2CA0031", "delist", e, "Exception", pmiName);
            ResourceException re = new ResourceException("delist: caught Exception");
            re.initCause(e);
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "delist", e);
            throw re;
        }

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "delist");
    } // end delist

    @Override
    public void end(Xid xid, int flags) throws XAException {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "end", xid);
        }

        /*
         *
         * if (!xid.equals(this.xid)) {
         * final XAException e = new XAException(XAException.XAER_NOTA);
         * if (tc.isEventEnabled())
         * Tr.event(this, tc, "Mismatched Xid", e);
         * throw e;
         * }
         */

        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "end");
        }

    }

    // Note: post TD we need to look at whether or not we want to continue to
    // have the wrapper enlist itself.  It will need a lot more of the data the CM has,
    // so it my be better to let the CM control enlistment.
    @Override
    public void enlist() throws ResourceException {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "enlist", mcWrapper.getUOWCoordinator());
        }

        if (this.hasRollbackOccured) {
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "enlist", "It is NOT valid to continue working under a transaction that has already rolledback");
            throw new ResourceException("Attempt to continue working after transaction rolledback !");
        }
        if (enlisted == true) {

            if (tc.isEntryEnabled()) {
                Tr.exit(this, tc, "enlist", "already enlisted");
            }
            return; // Already enlisted, no-op.

        } // end enlisted == true

        UOWCoordinator uowCoord = mcWrapper.getUOWCoordinator();
        if (uowCoord != null) {
            if (uowCoord.isGlobal()) {
                // Global Transaction
                EmbeddableWebSphereTransactionManager tranMgr = mcWrapper.pm.connectorSvc.transactionManager;
                try {
                    if (_rrsTransactional) {
                        int branchCoupling = mcWrapper.getCm().getResourceRefInfo().getBranchCoupling();
                        int startFlag = XAResource.TMNOFLAGS;
                        if (branchCoupling != ResourceRefInfo.BRANCH_COUPLING_UNSET) {
                            startFlag = mcWrapper.getCm().supportsBranchCoupling(branchCoupling, mcWrapper.get_managedConnectionFactory());
                            if (startFlag == -1)
                                throw new ResourceException("Branch coupling attribute not implemented for this resource");
                        }

                        mcWrapper.enlistRRSXAResource(mcWrapper.getRecoveryToken(), startFlag);
                    }

                    // Use the following enlist call since this is a One Phase resource.
                    boolean rc = false;
                    rc = tranMgr.enlistOnePhase(uowCoord, this);
                    if (rc != true) {
                        Tr.error(tc, "BAD_RETURN_VALUE_FROM_ENLIST_J2CA0087", this, mcWrapper.gConfigProps.cfName);
                        ResourceException x = new ResourceException("Error on enlistOnePhase");
                        if (tc.isEntryEnabled())
                            Tr.exit(this, tc, "enlist", x);
                        throw x;
                    }
                    mcWrapper.markLocalTransactionWrapperInUse();
                    enlisted = true;
                } catch (ResourceException e) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(
                                                                e,
                                                                "com.ibm.ejs.j2c.LocalTransactionWrapper.enlist",
                                                                "344",
                                                                this);
                    Tr.error(tc, "ENLIST_RESOURCE_EXCP_J2CA0030", "enlist", e, "Exception", mcWrapper.gConfigProps.cfName);

                    // Error recovery from enlist has been moved here.  The transaction team has
                    //  asked us to setRollbackOnly() for all the failed enlist() calls.  Doing
                    //  this causes them (for release 5.0. They may change it next release) to
                    //  flow the rollback sequence to all the other resources in the tran.  This
                    //  connection failed to enlist and so won't be called to roll back.  This
                    //  connection is, however registered for afterCompletion calls which they will
                    //  also drive.  Its important that we mark this connection stale prior to
                    //  calling setRollbackOnly() because our afterCompletion code will return it
                    //  to the freePool if we don't.  We want the connection destroyed since it
                    //  is potentially corrupt.
                    mcWrapper.markTransactionError();
                    try {
                        tranMgr.getTransaction().setRollbackOnly();
                    } catch (Exception ex) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ejs.j2c.LocalTransactionWrapper.enlist",
                                                                    "445", this);
                        if (tc.isEventEnabled()) {
                            Tr.event(this, tc,
                                     "Caught Exception while trying to mark transaction RollbackOnly - Exception:"
                                               + ex);
                        }
                    }

                    if (tc.isEntryEnabled())
                        Tr.exit(this, tc, "enlist", e);
                    throw e;
                } catch (Exception e) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(
                                                                e,
                                                                "com.ibm.ejs.j2c.LocalTransactionWrapper.enlist",
                                                                "459",
                                                                this,
                                                                new Object[] { mcWrapper }); //LI3162-5
                    Tr.error(tc, "ENLIST_RESOURCE_EXCP_J2CA0030", "enlist", e, "Exception", mcWrapper.gConfigProps.cfName);

                    // Error recovery from enlist has been moved here.  The transaction team has
                    //  asked us to setRollbackOnly() for all the failed enlist() calls.  Doing
                    //  this causes them (for release 5.0. They may change it next release) to
                    //  flow the rollback sequence to all the other resources in the tran.  This
                    //  connection failed to enlist and so won't be called to roll back.  This
                    //  connection is, however registered for afterCompletion calls which they will
                    //  also drive.  Its important that we mark this connection stale prior to
                    //  calling setRollbackOnly() because our afterCompletion code will return it
                    //  to the freePool if we don't.  We want the connection destroyed since it
                    //  is potentially corrupt.
                    mcWrapper.markTransactionError();
                    try {
                        tranMgr.getTransaction().setRollbackOnly();
                    } catch (Exception ex) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ejs.j2c.LocalTransactionWrapper.enlist",
                                                                    "477", this);
                        if (tc.isEventEnabled()) {
                            Tr.event(this, tc,
                                     "Caught Exception while trying to mark transaction RollbackOnly - Exception:"
                                               + ex);
                        }
                    }

                    ResourceException re = new ResourceException("enlist: caught Exception");
                    re.initCause(e);
                    if (tc.isEntryEnabled())
                        Tr.exit(this, tc, "enlist", re);
                    throw re;
                }
                if (tc.isEntryEnabled()) {
                    Tr.exit(this, tc, "enlist");
                }
                return;
            } // end Global Transaction
            else {
                // Local Transaction
                try {
                    if (_rrsTransactional) {
                        int dummyInt = -1;
                        rrsXAResource = mcWrapper.enlistRRSXAResource(dummyInt, dummyInt);
                    }

                    // If the resolution is CONTAINER_AT_BOUNDARY then do a straight enlist.
                    // The transaction service will manage begin/end of this transaction.
                    if (((LocalTransactionCoordinator) uowCoord).isContainerResolved()) {
                        ((LocalTransactionCoordinator) uowCoord).enlist(this);
                    } else {
                        // If resolution is NOT CONTAINER_AT_BOUNDARY, we assume it is APPLICATION,
                        //  (which is the only other choice and also the default) then do an
                        //  enistForCleanup.  Here the application is expected to commit/rollback
                        //  the local transaction.  If it does, we will get an event in our
                        //  connectionEventListener and it will drive delist() on this object,
                        //  which in turn will call delistFromCleanup. If the user doesn't
                        //  commit/rollback the transaction service will clean things up.
                        ((LocalTransactionCoordinator) uowCoord).enlistForCleanup(this);
                    }
                    mcWrapper.markLocalTransactionWrapperInUse();
                    enlisted = true;
                } catch (Exception e) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(
                                                                e,
                                                                "com.ibm.ejs.j2c.LocalTransactionWrapper.enlist",
                                                                "405",
                                                                this);
                    mcWrapper.markTransactionError();
                    mcWrapper.releaseToPoolManager();
                    Tr.error(tc, "ENLIST_RESOURCE_EXCP_J2CA0030", "enlist", e, "Exception", mcWrapper.gConfigProps.cfName);
                    ResourceException re = new ResourceException("enlist: caught Exception");
                    re.initCause(e);
                    if (tc.isEntryEnabled())
                        Tr.exit(this, tc, "enlist", e);
                    throw re;
                }
                if (tc.isEntryEnabled()) {
                    Tr.exit(this, tc, "enlist", "Local Tran");
                }
                return;
            } // end Local Transaction
        } else {
            // No transaction context.  Should never happen.
            Tr.error(tc, "NO_VALID_TRANSACTION_CONTEXT_J2CA0040", "enlist", null, mcWrapper.gConfigProps.cfName);
            ResourceException x = new ResourceException("INTERNAL ERROR: No valid transaction context present");
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "enlist", x);
            throw x;
        }

    }

    @Override
    public void forget(Xid xid) throws XAException {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "forget");
        }

        String xPath = null;
        if (mcWrapper != null) {
            xPath = mcWrapper.gConfigProps.getXpathId();
        }
        Tr.error(tc, "XA_OP_NOT_SUPPORTED_J2CA0016", "forget", xid, xPath);

        //this.xid = null;

        /*
         *
         * if (!xid.equals(this.xid)) {
         * final XAException e = new XAException(XAException.XAER_NOTA);
         * if (tc.isEventEnabled())
         * Tr.event(this, tc, "Mismatched Xid", e);
         * throw e;
         * }
         */

        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "forget");
        }

        throw new XAException(XAException.XAER_PROTO);

    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "isSameRM");
        }

        String pmiName = null;
        if (mcWrapper != null) {
            pmiName = mcWrapper.gConfigProps.cfName;
        }
        Tr.error(tc, "XA_OPERATION_NOT_SUPPORTED_J2CA0023", "isSameRM", pmiName);

        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "isSameRM");
        }

        return false;

    }

    @Override
    public int prepare(Xid xid) throws XAException {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "prepare");
        }

        if (!mcWrapper.isStale()) {
            Tr.error(tc, "XA_OPERATION_NOT_SUPPORTED_J2CA0023", "prepare", mcWrapper.gConfigProps.cfName);
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "prepare");
        }

        throw new XAException(XAException.XAER_PROTO);

    }

    @Override
    public Xid[] recover(int flags) throws XAException {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "recover");
        }

        String pmiName = null;
        if (mcWrapper != null) {
            pmiName = mcWrapper.gConfigProps.cfName;
        }
        Tr.error(tc, "XA_OPERATION_NOT_SUPPORTED_J2CA0023", "recover", pmiName);

        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "recover");
        }

        throw new XAException(XAException.XAER_PROTO);

    }

    @Override
    public void start(Xid xid, int flag) throws XAException {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "start");
        }

        /*
         *
         * if (this.xid != null) {
         * final XAException e = new XAException(XAException.XAER_PROTO);
         * if (tc.isEventEnabled())
         * Tr.event(this, tc, "start: Already associated with tx", e);
         * throw e;
         * }
         *
         * this.xid = xid;
         */

        try {
            localTransaction.begin();
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.LocalTransactionWrapper.start",
                                                        "464",
                                                        this);
            if (!mcWrapper.shouldBeDestroyed()) {
                mcWrapper.markTransactionError(); // Can't trust the connection since we don't know why it failed.
                Tr.error(tc, "XA_END_EXCP_J2CA0024", "start", "begin", e, "XAException", mcWrapper.gConfigProps.getXpathId());
            }
            XAException xae = new XAException(XAException.XAER_RMFAIL);
            xae.initCause(e);
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "start", e);
            throw xae;
        }
        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "start");
    }

    @Override
    public int getTransactionTimeout() throws XAException {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getTransactionTimeout");
        }

        String pmiName = null;
        if (mcWrapper != null) {
            pmiName = mcWrapper.gConfigProps.cfName;
        }
        Tr.error(tc, "XA_OPERATION_NOT_SUPPORTED_J2CA0023", "getTransactonTimeout", pmiName);

        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "getTransactionTimeout");
        }

        return -1;

    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "setTransactionTimeout");
        }

        String pmiName = null;
        if (mcWrapper != null) {
            pmiName = mcWrapper.gConfigProps.cfName;
        }
        Tr.error(tc, "XA_OPERATION_NOT_SUPPORTED_J2CA0023", "setTransactionTimeout", pmiName);

        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "setTransactionTimeout");
        }

        return false;

    }

    /**
     * Reinitializes its own state such that it may be placed in the PoolManagers
     * pool for reuse.
     */
    public void cleanup() {
        enlisted = false;
    }

    /**
     * Reinitializes its own state such that it may be reused.
     * Releases LocalTransaction object.
     */
    public void releaseResources() {
        localTransaction = null;
    }

    /*
     * Register the current object with the Synchronisation manager for the current transaction
     */

    @Override
    public boolean addSync() throws ResourceException {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "addSync");
        }

        try {

            UOWCoordinator uowCoord = mcWrapper.getUOWCoordinator();
            if (uowCoord != null) {
                if (uowCoord.isGlobal()) {
                    if (mcWrapper.isConnectionSynchronizationProvider()) {
                        throw new UnsupportedOperationException("com.ibm.ws.Transaction.SynchronizationProvider");
                        /*
                         * Use the Synchronization object from the managed connection to
                         * register.
                         */
                        //Synchronization s = ((SynchronizationProvider)mcWrapper.getManagedConnection()).getSynchronization();
                        //LocationSpecificFunction.instance.getTransactionManager().registerSynchronization(uowCoord, s, EmbeddableWebSphereTransactionManager.SYNC_TIER_INNER);
                    }
                    /*
                     * This code will remain here just in case we run into this
                     * case. We should not be isEnlistmentDisabled in this code.
                     *
                     * Log a message and return, if isEnlistmentDisabled is true.
                     */
                    if (mcWrapper.isEnlistmentDisabled()) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "Managed connection isEnlistmentDisabled is true.");
                            Tr.debug(this, tc, "Returning without registering.");
                        }
                        if (tc.isEntryEnabled()) {
                            Tr.exit(this, tc, "addSync", false);
                        }
                        return false;
                    }
                    /*
                     * Use our XATransactionWrapper
                     */
                    // Global Transaction
                    EmbeddableWebSphereTransactionManager tranMgr = mcWrapper.pm.connectorSvc.transactionManager;
                    tranMgr.registerSynchronization(uowCoord, this);
                    mcWrapper.markLocalTransactionWrapperInUse();
                    registeredForSync = true;
                } else {
                    // Local Transaction
                    // No need to register for synchronization for unshared/LTC/res-control=Application.
                    //  Instead of cleanup being done via after completion it will either happen at close or
                    //  commit/rollback which ever happens last.  This will allow the connection to be returned
                    //  to the pool prior to the end of the LTC scope allowing better utilization of the connection.
                    boolean shareable = mcWrapper.getConnectionManager().shareable();
                    if (!shareable && !J2CUtilityClass.isContainerAtBoundary(mcWrapper.pm.connectorSvc.transactionManager)) {
                        // Register an instance of an RRS no-transaction wrapper
                        // so the TM will create an RRS NativeLocalTranasction.  The wrapper
                        // provides "empty" TransactioWrapper and Synchronization behaviors,
                        // but indicates to the TM that the LTC requires RRS support.

                        //if (tc.isEntryEnabled()) {
                        //  Tr.exit(this, tc, "addSync: returning without registering.");
                        //}
                        //return false;
                        if (this.isRRSTransactional()) {
                            ((SynchronizationRegistryUOWScope) uowCoord).registerInterposedSynchronization(
                                                                                                           new RRSNoTransactionWrapper());
                        } else {
                            if (tc.isEntryEnabled()) {
                                Tr.exit(this, tc, "addSync", "returning without registering");
                            }
                            return false;
                        }
                    } else {

                        if (mcWrapper.isConnectionSynchronizationProvider()) {
                            throw new UnsupportedOperationException("com.ibm.ws.Transaction.SynchronizationProvider");
                            /*
                             * Use the Synchronization object from the managed connection to
                             * register.
                             */
                            //Synchronization s = ((SynchronizationProvider)mcWrapper.getManagedConnection()).getSynchronization();
                            //((SynchronizationRegistryUOWScope) uowCoord).registerInterposedSynchronization(s);
                            //registeredForSync = false;
                        } else {

                            /*
                             * In order
                             * to take advantage of the new level of code in which MCWrapper has the isEnlistmentDisabled()
                             */
                            if (mcWrapper.isEnlistmentDisabled()) {
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(this, tc, "Managed connection isEnlistmentDisabled is true.");
                                    Tr.debug(this, tc, "Returning without registering.");
                                }
                                if (tc.isEntryEnabled()) {
                                    Tr.exit(this, tc, "addSync", false);
                                }
                                return false;
                            }
                            ((SynchronizationRegistryUOWScope) uowCoord).registerInterposedSynchronization(this);
                            mcWrapper.markLocalTransactionWrapperInUse();
                            registeredForSync = true;
                        }

                    }
                }
            } else {
                // No transaction context.  Should never happen.
                Tr.error(tc, "NO_VALID_TRANSACTION_CONTEXT_J2CA0040", "addSync", null, mcWrapper.gConfigProps.cfName);
                throw new ResourceException("INTERNAL ERROR: No valid transaction context present");
            }

        } catch (ResourceException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.LocalTransactionWrapper.addSync",
                                                        "594",
                                                        this);
            Tr.error(tc, "REGISTER_WITH_SYNCHRONIZATION_EXCP_J2CA0026", "addSync", e, "Exception");
            throw e;
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.LocalTransactionWrapper.addSync",
                                                        "605",
                                                        this);
            Tr.error(tc, "REGISTER_WITH_SYNCHRONIZATION_EXCP_J2CA0026", "addSync", e, "Exception");
            ResourceException re = new ResourceException("addSync: caught Exception");
            re.initCause(e);
            throw re;
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "addSync", registeredForSync);
        }

        return registeredForSync;

    }

    /**
     * Part of javax.transaction.Synchronization interface,
     * This method is called by the transaction manager after the transaction is committed or rolled back.
     *
     * @param status The status of the transaction completion.
     */

    @Override
    public void afterCompletion(int status) {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "afterCompletion");
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(
                     tc,
                     "Using transaction wrapper@" + Integer.toHexString(this.hashCode()));
        }

        // When afterCompletionCode is called we need to reset the registeredForSync flag.
        registeredForSync = false;
        this.hasRollbackOccured = false;
        /*
         * Mark the transaction complete in the wrapper
         */
        // Do NOT catch runtime exections here.  Let them flow out of the component.
        mcWrapper.transactionComplete();

        if (tc.isDebugEnabled()) {
            if (mcWrapper.getHandleCount() != 0) {
                // Issue warning that Connections have not been closed by the end of the current UOW
                //  scope and will be closed by the CM.
                //            Tr.warning(this, tc,"HANDLE_NOT_CLOSED_J2CA0055");
                Tr.debug(this, tc, "Information:  handle not closed at end of UOW. Connection from pool " + mcWrapper.gConfigProps.getXpathId());
            }
        }

        // Do NOT catch the runtime exception which getConnectionManager might throw.
        // If this is thrown it is an internal bug and needs to be fixed.
        // Allow the rte to flow up to the container.
        boolean shareable = mcWrapper.getConnectionManager().shareable();

        //  - Simplified the following if/else construct taking advantage of the
        //             new releaseToPoolManger call on mcWrapper.
        //
        // If the connection is shareable, or it is non-shareable and the handleCount is
        // zero, then release it back to the poolManger, otherwise simply reset the
        // transaction related variables.
        //
        // Note: shareable connections are released at the end of transactions regardless of
        //  outstanding handles because we don't supports handled being associated to an active
        //  managedConnection outside of a sharing boundary.
        //
        //  if the MCWrapper is stale we will release the connection to the pool.
        //
        if ((shareable) || (!shareable && mcWrapper.getHandleCount() == 0) || mcWrapper.isStale()) {

            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc,
                         "Releasing the connection to the pool. shareable = " + shareable + "  handleCount = " + mcWrapper.getHandleCount() + "  isStale = " + mcWrapper.isStale());
            }

            try {
                mcWrapper.releaseToPoolManager();
            } catch (Exception e) {
                // No need to rethrow this exception since nothing can be done about it,
                //  and the application has successfully finished its use of the connection.
                com.ibm.ws.ffdc.FFDCFilter.processException(
                                                            e,
                                                            "com.ibm.ejs.j2c.LocalTransactionWrapper.afterCompletion",
                                                            "711",
                                                            this);
                if (tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "afterCompletionCode for datasource " + mcWrapper.gConfigProps.cfName + ":  caught Exception", e);
                }
            }

        } // end ((shareable) || (!shareable && mcWrapper.getHandleCount() == 0))
        else {
            /*
             * The cleanup of the coordinator enlisted flag should only be done for non sharable connections, in
             * other cases, the coord is set to null in mcWrapper.releaseToPoolManager(). We only need to worry about this
             * if the handleCount is greater than 0 (in which case the cleanup will be done by the poolManager). Same
             * thing for the tranFailed flag in the MCWrapper.
             */
            mcWrapper.setUOWCoordinator(null);
            // Reset the serialReuseCount since it is only valid for the duration of
            // the LTC. Note that the release path above will also reset it via mcWrapper.cleanup().
            enlisted = false;
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "afterCompletion");
        }
    }

    /**
     * Part of javax.transaction.Synchronization interface,
     * This method is called by the transaction manager prior to the start of the transaction completion process.
     */

    @Override
    public void beforeCompletion() {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "beforeCompletion");
            Tr.exit(this, tc, "beforeCompletion");
        }

    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();

        buf.append("LocalTransactionWrapper@:");
        buf.append(_hexString);

        buf.append("  localTransaction:");
        buf.append(localTransaction);

        buf.append("  enlisted:");
        buf.append(enlisted);

        buf.append("Has Tran Rolled Back = ");
        buf.append(this.hasRollbackOccured);

        buf.append("  registeredForSync");
        buf.append(registeredForSync);

        buf.append("mcWrapper.hashcode()");
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
        return _rrsTransactional;
    }

    protected void setRRSTransactional(boolean rrsTransactional) {
        this._rrsTransactional = rrsTransactional;
    }

    public boolean isEnlisted() {
        return enlisted;
    }

    public boolean isRegisteredForSync() {
        return registeredForSync;
    }
}
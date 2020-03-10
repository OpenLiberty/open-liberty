/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.j2c;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.TransactionSupport;
import javax.resource.spi.TransactionSupport.TransactionSupportLevel;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.ejs.ras.RasHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.j2c.SecurityHelper;
import com.ibm.ws.j2c.TranWrapper;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.jca.adapter.PurgePolicy;
import com.ibm.ws.jca.adapter.WSManagedConnectionFactory;
import com.ibm.ws.jca.cm.AbstractConnectionFactoryService;
import com.ibm.ws.jca.cm.AppDefinedResource;
import com.ibm.ws.jca.cm.handle.HandleList;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.security.jca.AuthDataService;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.tx.rrs.RRSXAResourceFactory;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.resource.ResourceFactory;

/**
 * An instance of the ConnectionManager class is created by the
 * ConnectionFactoryBuilder for a deployed resource adapter when the first
 * JNDI lookup call is performed for that resource adapter with a particular
 * set of res-xxx settings. Resource adapter ConnectionFactory instances
 * delegate getConnection method calls from EJB's as an allocateConnection
 * method call to the associated ConnectionManager instance.
 *
 * Scope : Admin Server
 *
 * Object model : Multiple CM instances will be associated with each
 * ManagedConnectionFactory (MCF).
 * This is because information the CM needs about the user, which comes
 * from the resource-ref, is only available at the time the user does a lookup
 * on the Connection Factory. Therefore a place is needed to store that unique
 * information. Thus for each variation in the combination of these unique
 * attributes there is another instace of a CM to hold the data.
 *
 * See {@link com.ibm.ejs.j2c.CMConfigDataImpl#getCFDetailsKey}
 */
public final class ConnectionManager implements com.ibm.ws.j2c.ConnectionManager, com.ibm.ws.jca.adapter.WSConnectionManager, java.io.Serializable {

    private static final long serialVersionUID = -3078170792213348926L;

    /**
     * The ResourceAdapterDD and ConnectorPoolProperties are per deployed resource adapter
     * and hence per ManagedConnectionFactory (MCF).
     */
    private static final TraceComponent tc = Tr.register(ConnectionManager.class, J2CConstants.traceSpec, J2CConstants.messageFile);
    private static final TraceComponent ConnLeakLogic = Tr.register(ConnectionManager.class, "ConnLeakLogic", J2CConstants.messageFile);

    private final AbstractConnectionFactoryService connectionFactorySvc;

    private static final AtomicLong numberOfCMinstancesEverCreated = new AtomicLong(0);

    private String cfDetailsKey = "NameNotSet";
    protected CMConfigData cmConfig = null;
    protected HashMap<String, Integer> qmidcmConfigMap = null;

    private boolean shareable = false;

    private int recoveryToken;

    private final transient SecurityHelper securityHelper;
    private final boolean isJDBC;
    private transient int commitPriority = 0;
    private boolean localTranSupportSet = false;

    /**
     * The following is a variable which will tell us whether or not
     * the RelationalResourceAdapter we are working with (if we are)
     * is configured to run OnePhase commit even though the RRA's RAR
     * file always indicates twoPhase support.
     *
     * <br><br> A value of "0" indicates not initialized.
     * <br><br> A value of "1" indicates it supports 1PC.
     * <br><br> A value of "2" indicates it supports 2PC.
     */

    protected transient PrivilegedExceptionAction<Boolean> _getADP = null;
    protected boolean containerManagedAuth = false;
    protected HashMap<Object, String> handleToThreadMap = null;
    protected HashMap<Object, ComponentMetaData> handleToCMDMap = null;

    protected transient PoolManager _pm = null;
    protected J2CGlobalConfigProperties gConfigProps = null;

    /*
     * Indicates if the configured MCF is RRSTransactional. If so,
     * transactions will be processed using z/OS Resource Recovery
     * Services (RRS) instead of the transaction support indicated
     * by the connector's Deployment descriptor.
     *
     * This property is applicable to WAS z/OS only.
     */
    private final boolean rrsTransactional;

    /**
     * @param cfSvc connection factory service
     * @param mcfXProps MCFExtendedProperties
     * @param pm pool manager supplied by the lightweight server. Otherwise null.
     * @param jxri J2CXAResourceInfo
     *
     * @pre jxri != null
     */
    ConnectionManager(AbstractConnectionFactoryService cfSvc,
                      PoolManager pm,
                      J2CGlobalConfigProperties gconfigProps,
                      CommonXAResourceInfo jxri) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "<init>");
        }
        this.connectionFactorySvc = cfSvc;
        this._pm = pm;
        this.gConfigProps = gconfigProps;
        this.cmConfig = jxri.getCmConfig();
        cfDetailsKey = cmConfig.getCFDetailsKey();

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "cfDetailsKey = " + cfDetailsKey + "   for PmiName = " + gConfigProps.cfName);
        }

        shareable = cmConfig.getSharingScope() == ResourceRef.SHARING_SCOPE_SHAREABLE;

        isJDBC = cfSvc.getClass().getName().startsWith("com.ibm.ws.jdbc.");

        int[] zosInfo = cfSvc.getThreadIdentitySecurityAndRRSSupport(null); // TODO supply identifier on XA recovery path?
        rrsTransactional = zosInfo[2] > 0;

        // Indicates if the configured MCF requires an z/OS ACEE to be placed
        // on the thread (TCB) when "current thread identity" is used for
        // getConnection() processing.
        boolean threadSecurity = zosInfo[1] > 0;

        // Indicates if the configured MCF allows "current thread identity" to be used for getConnection() processing.
        int threadIdentitySupport = zosInfo[0];

        // If thread identity is enabled, a ThreadIdentitySecurityHelper; otherwise a DefaultSecurityHelper.
        if (ThreadIdentityManager.isThreadIdentityEnabled() && threadIdentitySupport != AbstractConnectionFactoryService.THREAD_IDENTITY_NOT_ALLOWED)
            securityHelper = new ThreadIdentitySecurityHelper(threadIdentitySupport, threadSecurity);
        else
            securityHelper = new DefaultSecurityHelper();

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(this, tc, " globalConfigProps " + gConfigProps);
            Tr.debug(this, tc, " jxri      " + jxri);
            Tr.debug(this, tc, " securityHelper " + securityHelper);
        }

        containerManagedAuth = (cmConfig.getAuth() == J2CConstants.AUTHENTICATION_CONTAINER);

        commitPriority = cmConfig.getCommitPriority();

        // Create and register a J2C XAResource only if the
        // transaction support is not RRSTransactional.

        // Get the TransactionManager and if not null, register
        // the XAResource. If the TransactionManager is null then
        // the ConnectionManager constructor must have been
        // invoked during JNDI Lookup processing under an
        // environment where the Transaction Manager may not exist
        // (for example, under a client dumping the name space in zOS).
        // In this case, skip attempting to register the XAResource.

        EmbeddableWebSphereTransactionManager tm = pm.connectorSvc.transactionManager;
        if (tm != null) {
            if (!rrsTransactional) {
                recoveryToken = registerXAResourceInfo(tm, jxri, commitPriority, null);
            } else {
                if (!TransactionSupportLevel.NoTransaction.equals(gconfigProps.transactionSupport)) {
                    RRSXAResourceFactory xaFactory = (RRSXAResourceFactory) _pm.connectorSvc.rrsXAResFactorySvcRef.getService();

                    // Make sure that the bundle is active.
                    if (xaFactory == null) {
                        throw new IllegalStateException("Native service for RRS transactional support is not active or available. Resource registration is rejected.");
                    }

                    UOWCurrent currentUOW = (UOWCurrent) tm;
                    UOWCoordinator uowCoord = currentUOW.getUOWCoord();
                    Xid xid = (uowCoord != null) ? uowCoord.getXid() : null;

                    // Create a filter for the transaction manager to be able to find the native
                    // transaction factory in the service registry during recovery.
                    String filter = FilterUtils.createPropertyFilter("native.xa.factory", (xaFactory.getClass().getCanonicalName()));

                    // NOTE: At this point in time, the transaction manager does not support logging
                    // XAResourceInfo type objects; However, they do allow generic serializable objects such as a String
                    // to be logged and retrieved during recovery. So, a String is what is currently passed as resource info to
                    // the registerResourceInfo call.
                    Serializable xaResInfo = xaFactory.getXAResourceInfo(xid);
                    recoveryToken = tm.registerResourceInfo(filter, xaResInfo, commitPriority);
                }
            }
        } else if (isTraceOn && tc.isEventEnabled()) {
            Tr.event(this, tc, "<constructor>, TransactionManager is null");
        }

        //  end !rrsTransactional

        if (isTraceOn && tc.isDebugEnabled()) {
            numberOfCMinstancesEverCreated.incrementAndGet();
            Tr.debug(this, tc, "This brings the total no. of CM instances to " + numberOfCMinstancesEverCreated.get());
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "<init>", this.toString());
        }
    }

    /**
     * This method is called only by the relational resource adapter to obtain a CMConfigData object
     * which encapsulates the res-xxx settings from the resource reference
     *
     * @return The CMConfigData object containing settings for res-xxx from the resource reference
     * @deprecated use WSConnectionManager.getResourceRefInfo instead
     */
    // TODO: delete this method if we ever fork the ConnectionFactoryDetailsImpl class
    @Deprecated
    @Override
    public CMConfigData getCMConfigData() {
        return cmConfig;
    }

    /**
     * This method is called by a resource adapter ConnectionFactory to obtain a Connection each
     * time the application calls getConnection() on the resource adapter ConnectionFactory.
     *
     * @param factory The managed connection factory for this connection.
     * @param requestInfo The connection specific request info, i.e. userID, Password.
     *
     * @return The newly allocated connection (returned as type object per JCA spec).
     */
    @Override
    public Object allocateConnection(ManagedConnectionFactory factory, ConnectionRequestInfo requestInfo) throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "allocateConnection");
        }

        if (_pm == null) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "This should not happen!  pm was null for cf name " + cmConfig.getCfKey());
            }
            String formattedMessage = CommonFunction.getNLSMessage("POOL_MANAGER_NOT_FOUND_J2CA0695",
                                                                   new Object[] { cmConfig.getJNDIName() == null ? cmConfig.getCfKey() : cmConfig.getJNDIName() });
            java.lang.IllegalStateException re = new java.lang.IllegalStateException(formattedMessage);
            Tr.error(tc, "FAILED_MANAGED_CONNECTION_J2CA0020", re);
            throw re;
        }

        UOWCurrent uowCurrent = (UOWCurrent) _pm.connectorSvc.transactionManager;
        UOWCoordinator uowCoord = uowCurrent == null ? null : uowCurrent.getUOWCoord();

        Subject subj = getFinalSubject(requestInfo, factory, this);

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "This CM is " + this.toString());
            Tr.debug(this, tc, "Input MCF is     " + factory);
        }

        Object rVal = null;

        Object credTokenObj = null;
        MCWrapper mcWrapper = null;
        try { // Begin processing to get connection

            // Perform any security setup that may be needed
            // before we proceed to get a connection.

            credTokenObj = securityHelper.beforeGettingConnection(subj, requestInfo);
            mcWrapper = allocateMCWrapper(factory, requestInfo, subj, uowCoord);

            involveMCInTran(mcWrapper, uowCoord, this);

            /*
             * Get the a Connection from the ManagedConnection to return to our caller.
             */

            int poolState = mcWrapper.getPoolState();
            try {
                mcWrapper.setPoolState(50);

                rVal = mcWrapper.getConnection(subj, requestInfo);
                mcWrapper.setPoolState(poolState);
                if (mcWrapper.do_not_reuse_mcw) {
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Connection error occurred for this mcw " + mcWrapper + ", mcw will not be reuse");
                    }
                    mcWrapper.markStale();
                    ResourceException e = new ResourceException("Resource adatepr called connection error event during getConnection " +
                                                                "processing and did not throw a resource exception.  The reason for " +
                                                                "this falue may have been logged during the connection error event " +
                                                                "logging.");
                    throw e;
                }
            } catch (ResourceException e) {
                mcWrapper.setPoolState(poolState);
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.ConnectionManager.allocateConnection", "344", this);
                Tr.error(tc, "FAILED_CONNECTION_J2CA0021", new Object[] { e, _pm.gConfigProps.cfName });

                /*
                 * If the Resource Adapter throws a ResourceException and we are not in a
                 * transaction and there are no other connections open on the ManagedConnection,
                 * return the ManagedConnection to the pool before
                 * rethrowing the exception. The ManagedConnection is probably OK - the exception
                 * may only be a logon failure or similar so the MC shouldn't be 'lost'.
                 *
                 * If we are in a transaction, just throw the exception. Assume we will cleanup during
                 * the aftercompletion call on the tran wrapper.
                 *
                 * for resource adapter that support NOTXWRAPPER, if an error occurs, we can move them from the inuse
                 * pool, to the free pool, assuming the managed connection is good.
                 * This managed connection can be in a transaction (uowCoord != null) but it still should be ok to release it to the free pool
                 *
                 * if resource failover is enabled and we are in current mode 102, since we are now using the alternate
                 * resource adapter, instead of considering this connection ok, lets remove it. When the pool fails back we
                 * do not what to risk having pooled bad connections.
                 */
                if (isTraceOn && tc.isDebugEnabled()) {

                    if (uowCoord != null) {
                        Tr.debug(this, tc, "getConnection failed for using uow is " + uowCoord +
                                           " tran wrapper id is " + mcWrapper.getTranWrapperId() +
                                           " handle count is " + mcWrapper.getHandleCount());
                    } else {
                        Tr.debug(this, tc, "getConnection failed for using uow is null" +
                                           " tran wrapper id is " + mcWrapper.getTranWrapperId() +
                                           " handle count is " + mcWrapper.getHandleCount());

                    }

                }
                if (mcWrapper.do_not_reuse_mcw) {
                    if (mcWrapper.getTranWrapperId() == MCWrapper.LOCALTXWRAPPER) {
                        if (!mcWrapper.getLocalTransactionWrapper().isEnlisted() && !mcWrapper.getLocalTransactionWrapper().isRegisteredForSync()) {
                            // We are not enlisted or registered, we need to remove this connection since after completion will not be called.
                            // Null the uowCoord and the following code will remove this connection from the pool.
                            if (isTraceOn && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "This connection is not registered for sync or enlisted in a transaction");
                            }
                            uowCoord = null;
                            if (mcWrapper.getHandleCount() == 1) {
                                /*
                                 * Since the resource adater call connection error event, they should not have returned
                                 * a connection handle. Since we are not enlisted or registered, we can decrement the
                                 * handle count and let the following code cleanup and destory this managed connection.
                                 */
                                if (isTraceOn && tc.isDebugEnabled()) {
                                    Tr.debug(this, tc, "Decrementing the handle count for clean up and destroying the managed connection.");
                                }
                                mcWrapper.decrementHandleCount();
                            }
                        }
                    }
                }

                if ((uowCoord == null || (mcWrapper.getTranWrapperId() == MCWrapper.NOTXWRAPPER)) && mcWrapper.getHandleCount() == 0) {

                    try {
                        if (isTraceOn && tc.isDebugEnabled()) {
                            Tr.debug(this, tc,
                                     "Connection error occurred during getConnection to resource adapter.  The managed connection should be good, moving it to free pool.");
                        }
                        mcWrapper.releaseToPoolManager();
                    } catch (Exception exp) { // don't rethrow, already on exception path
                        com.ibm.ws.ffdc.FFDCFilter.processException(exp, "com.ibm.ejs.j2c.ConnectionManager.allocateConnection", "364", this);
                        Tr.error(tc, "FAILED_CONNECTION_RELEASE_J2CA0022", new Object[] { exp, _pm.gConfigProps.cfName });
                    }
                }
                if (isTraceOn && tc.isEntryEnabled()) {
                    Tr.exit(this, tc, "allocateConnection");
                }
                throw e;
            } catch (java.lang.Exception e) {
                mcWrapper.setPoolState(poolState);
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.ConnectionManager.allocateConnection", "372", this);
                Tr.error(tc, "FAILED_CONNECTION_J2CA0021", new Object[] { e, _pm.gConfigProps.cfName });

                /*
                 * If the Resource Adapter throws an Exception and we are not in a
                 * transaction and there are no other connections open on the ManagedConnection,
                 * return the ManagedConnection to the pool before
                 * rethrowing the exception. The ManagedConnection is probably OK - the exception
                 * may only be a logon failure or similar so the MC shouldn't be 'lost'.
                 *
                 * If we are in a transaction, just throw the exception. Assume we will cleanup during
                 * the aftercompletion call on the tran wrapper.
                 *
                 * - for resource adapter that support NOTXWRAPPER, if an error occurs, we can move them from the inuse
                 * pool, to the free pool, assuming the managed connection is good.
                 * This managed connection can be in a transaction (uowCoord != null) but it still should be ok to release it to the free pool
                 */

                if (isTraceOn && tc.isDebugEnabled()) {

                    if (uowCoord != null) {
                        Tr.debug(this, tc, "getConnection failed for using uow is " + uowCoord +
                                           " tran wrapper id is " + mcWrapper.getTranWrapperId() +
                                           " handle count is " + mcWrapper.getHandleCount());
                    } else {
                        Tr.debug(this, tc, "getConnection failed for using uow is null" +
                                           " tran wrapper id is " + mcWrapper.getTranWrapperId() +
                                           " handle count is " + mcWrapper.getHandleCount());
                    }

                }

                if ((uowCoord == null || (mcWrapper.getTranWrapperId() == MCWrapper.NOTXWRAPPER)) && mcWrapper.getHandleCount() == 0) {

                    try {
                        if (isTraceOn && tc.isDebugEnabled()) {
                            Tr.debug(this, tc,
                                     "Connection error occurred during getConnection to resource adapter.  The managed connection should be good, moving it to free pool.");
                        }
                        mcWrapper.releaseToPoolManager();
                    } catch (Exception exp) { // don't rethrow, already on exception path
                        com.ibm.ws.ffdc.FFDCFilter.processException(exp, "com.ibm.ejs.j2c.ConnectionManager.allocateConnection", "392", this);
                        Tr.error(tc, "FAILED_CONNECTION_RELEASE_J2CA0022", new Object[] { exp, _pm.gConfigProps.cfName });

                    }

                }

                ResourceException re = new ResourceException("allocateConnection: caught Exception");
                re.initCause(e);
                throw re;

            }

            if (_pm != null && (!(_pm.gConfigProps.isSmartHandleSupport() && shareable))) {
                HandleList hl = null;

                //  store the handle list in the MCWrapper
                mcWrapper.addToHandleList(rVal, hl);

            }

            boolean connLeakOrmaxNumThreads = ((isTraceOn && ConnLeakLogic.isDebugEnabled()) || (_pm != null && _pm.maxNumberOfMCsAllowableInThread > 0));
            boolean usingTLS = ((isTraceOn && tc.isDebugEnabled()) && (_pm != null && _pm.maxCapacity > 0));
            if (connLeakOrmaxNumThreads || usingTLS) {
                // add thread information to mcWrapper
                Thread myThread = Thread.currentThread();
                mcWrapper.setThreadID(RasHelper.getThreadId());
                mcWrapper.setThreadName(myThread.getName());
                if (connLeakOrmaxNumThreads) {
                    // add current time and stack information
                    if (mcWrapper.getLastAllocationTime() == 0)
                        mcWrapper.setLastAllocationTime(mcWrapper.getHoldTimeStart());
                    else
                        mcWrapper.setLastAllocationTime(System.currentTimeMillis());
                    if (mcWrapper.getInitialRequestStackTrace() == null) {
                        Throwable t = new Throwable();
                        mcWrapper.setInitialRequestStackTrace(t);
                    }
                }
            }

        } // end try block
          // NOTE: Only a "finally" clause is implemented.
          //       No catch is done, because prior processing
          //       is already covered by catch clauses in
          //       either Connection Manager or other services
          //       which will record the error and then
          //       percolate up to the finally clause. The
          //       "finally" processing MUST be done to ensure
          //       any thread identity pushed to the OS
          //       thread is removed and the original identity
          //       is restored.
        finally {
            if (credTokenObj != null) {
                securityHelper.afterGettingConnection(subj, requestInfo, credTokenObj);
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "allocateConnection", rVal==null?" connection handle is null":Integer.toHexString(rVal.hashCode()));
        }

        return rVal;

    }

    /**
     * This method is called internally by the CM obtain a ManagedConnection wrapped in a MCWrapper.
     * Used by: allocateConnection, reAssociate, and associateConnection.
     *
     * @param requestInfo The connection specific request info, i.e. userID, Password.
     * @param subj The subject for this request. Can be null.
     * @param uowCoord The current UOWCoordinator (transaction) for this request.
     *
     * @return A MCWrapper appropriate for the Current UOW, and enlisted
     *         as appropriate with the UOW.
     */

    private MCWrapper allocateMCWrapper(ManagedConnectionFactory managedConnectionFactory,
                                        ConnectionRequestInfo requestInfo,
                                        Subject subj,
                                        UOWCoordinator uowCoord) throws ResourceException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "allocateMCWrapper");
        }

        MCWrapper mcWrapper = null;

        /*
         * Allocate ManagedConnection using PoolManager, passing the Tx as the affinityId
         */

        //  try {
        boolean enforceSerialReuse = false;
        if (shareable && uowCoord != null) { // true sharing detection
            // In 5.0, we're no longer supporting true connection sharing within an LTC.  This was
            // done to be consistent with the JCA spec (connection sharing requires a global
            // transaction) and to prevent "surprises" by sharing when it is not expected.  So,
            // if the returned MC has active handles and we're within an LTC, we're going to
            // force the creation of a new shareable MC.

            // Post 5.0, when (if) the JCA spec is updated, we will throw an exception. The
            // exception will be thrown within the SharedPool code.
            if (!uowCoord.isGlobal()) {
                // We are in a LTC Scope
                boolean container_at_boundary = ((LocalTransactionCoordinator) uowCoord).isContainerResolved();
                if (!container_at_boundary) {
                    // The LTC is resolved by the application
                    enforceSerialReuse = true;
                }
            } // end not isGlobal
        } // end if (shareable...)

        // Now that we are using the real null value for the uowCoord, we don't need separate
        //  reserve() invocations for the different "types" of coordinators.  Just call it with the
        //  appropriate parameters and let the reserve() do the right thing.  The reserve() method
        //  requires both a non-null coordinator and the shareable flag set to true to allocate
        //  a connection from the shared pool.
        try {
            mcWrapper = (com.ibm.ejs.j2c.MCWrapper) _pm.reserve(// factory
                                                                managedConnectionFactory,
                                                                subj,
                                                                requestInfo,
                                                                uowCoord,
                                                                shareable,
                                                                enforceSerialReuse,
                                                                cmConfig.getCommitPriority(),
                                                                cmConfig.getBranchCoupling());

        } catch (ResourceException r) {

            if (isTraceOn && tc.isEntryEnabled()) {
                Tr.exit(this, tc, "allocateMCWrapper");
            }
            throw r;
        }

        if (mcWrapper == null) {
            Tr.error(tc, "NULL_MANAGED_CONNECTION_J2CA0015", _pm.gConfigProps.cfName);
            throw new ResourceException("PoolManager returned null ManagedConnection");
        } else {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Using MCWrapper@" + Integer.toHexString(mcWrapper.hashCode()));
            }
        }

        //  this code needed to be moved here after the check managed connection instanceof

        // Normally a transaction of some sort is required to use the ConnectionManager
        // However, there are several scenarios to consider:
        //
        // 1. No transaction and dynamic enlistment is not supported.
        //    This is the case that we want to at least put out a debug
        //    message that this is a potential problem that the designer
        //    should look into.
        // 2. No transaction and dynamic enlistment is supported.
        //    This is basically a promise that a transaction will be involved
        //    later when it is actually needed, so not having a transaction
        //    is not a problem at this time.
        if (uowCoord == null) {

            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Transaction context does NOT exist");
            }

            if (!_pm.gConfigProps.isDynamicEnlistmentSupported()) {

                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Not marked for dynamic transaction enlistment");
                }

                // Only log warnings or throw exception for LocalTransaction, XATransaction, and RRSTransactional RAs
                if (_pm.gConfigProps.transactionSupport != TransactionSupportLevel.NoTransaction) {
                    // deleted check for RRSTransactional support
                    // If the property, logMissingTranContext is not set to false in the j2c.properties file
                    //    (which is the default)
                    //   this is NOT JMS accessing a J2EE resource (such as a JDBC datasource) via
                    //   an EJB at the older 1.1 level
                    // then we want to issue a warning that the transaction context is missing.
                    // So basically, we want to issue a warning except for this earlier case where
                    // not having a transaction context was allowed.
                    if (_pm.gConfigProps.logMissingTranContext) {
                        Tr.warning(tc, "MISSING_TRANSACTION_CONTEXT_J2CA0075", "allocateMCWrapper");
                    }
                    // Remove check for whether connector runtime supports no transaction mode
                }
            }
        } // end uowCoord == null
          // end of moved code for defect

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "allocateMCWrapper");
        }

        return mcWrapper;

    }

    /**
     * This method is called internally by the CM obtain a ManagedConnection wrapped in a MCWrapper.
     * Used by: allocateConnection, reAssociate, and associateConnection.
     *
     * @param mcWrapper The managed connection wrapper to involve.
     */

    private void involveMCInTran(MCWrapper mcWrapper, UOWCoordinator uowCoord, com.ibm.ejs.j2c.ConnectionManager inUseCM) throws ResourceException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "involveMCInTran", inUseCM.toString());
        }

        if (!mcWrapper.involvedInTransaction()) { // this whole first part of the if statement is new.

            // Not involved in transaction, need to get involved in transaction.
            // Set a Reference to this CM instance into the McWrapper
            mcWrapper.setConnectionManager(inUseCM);

            /*
             * If isEnlistmentDisabled is true, log a message and return.
             */
            if (mcWrapper.isEnlistmentDisabled()) {

                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Managed connection isEnlistmentDisabled is true.");
                    Tr.debug(this, tc, "Returning without calling method initializeForUOW.");
                }
                if (isTraceOn && tc.isEntryEnabled()) {
                    Tr.exit(this, tc, "involveMCInTran");
                }

                return;

            }
            mcWrapper.setUOWCoordinator(uowCoord);

            // Do the transactional setup if required.
            initializeForUOW(mcWrapper, false);

        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "involveMCInTran");
        }

    }

    /**
     * This method will setup required objects needed for participating in the current UOW
     * and enlist or register them with the appropriate UOW services.
     *
     * @param mcWrapper The Managed Connection wrapper associated with this request.
     * @param originIsDeferred Deferred enlistment flag.
     */

    public void initializeForUOW(MCWrapper mcWrapper, boolean originIsDeferred) throws ResourceException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "initializeForUOW");
        }

        boolean registeredForSync = false;
        boolean enlisted = false;

        UOWCoordinator uowCoord = mcWrapper.getUOWCoordinator();

        // See allocateMCWrapper for a discussion on the checks being done here
        if (uowCoord == null) {

            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Transaction context does NOT exist");
            }

            if (!mcWrapper.gConfigProps.isDynamicEnlistmentSupported()) {

                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Not marked for dynamic transaction enlistment");
                }

                // Only log warnings or throw exception for LocalTransaction and XATransaction RAs...
                if ((mcWrapper.gConfigProps.transactionSupport == TransactionSupport.TransactionSupportLevel.LocalTransaction) ||
                    (mcWrapper.gConfigProps.transactionSupport == TransactionSupport.TransactionSupportLevel.XATransaction)) { // 144070 WS14620.01

                    // deleted check for RRSTransactional support
                    if (mcWrapper.gConfigProps.logMissingTranContext) {
                        Tr.warning(tc, "MISSING_TRANSACTION_CONTEXT_J2CA0075", "initializeForUOW");
                    }
                    // Remove check for whether connector runtime supports no transaction mode

                }

            }

        } else {

            // If there is an UOWCoordinator context then consider this ManagedConnection
            // for association with this transaction

            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Transaction context exists");
            }

            /*
             * If this ManagedConnection is not already associated then perform the
             * enlistment and create necessary associations. No synchronisation is
             * necessary here because we are in a global transaction context and the
             * ManagedConnection was reserved under this context and the context is
             * thread specific so no ManagedConnection can be being manipulated by
             * two threads at the saem time.
             */

            TranWrapper wrapper = null;

            // Note:  in future this logic for determining which kind of tran wrapper to create
            // Can be reorganized (see truth table)
            if (mcWrapper.isEnlistmentDisabled()) {

                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Creating NoTransactionWrapper, since this a a non-transactional datasource");
                }

                wrapper = mcWrapper.getNoTransactionWrapper();

            } else { // transactional datasource

                // When the adapter is RRSTransactional, if
                // possible, perform standard J2EE local tran
                // processing under the RRS Local tran. If not,
                // use an RRSLocalTranactionWrapper to handle
                // transaction processing.
                // To determine if standard J2EE local tran
                // processing can be performed, check if the
                // level of J2EE transaction support defined
                // by the adapter is one of the following:
                // TransactionSupport.TransactionSupportLevel.LocalTransaction
                // TransactionSupport.TransactionSupportLevel.XATransaction
                // If not, then perform RRSLocalTransaction
                // processing. On the other hand, if it is
                // one of these and is not the CICS ECI
                // resource adapter, perform J2EE Local
                // transaction processing.
                // In the case of the CICS ECI resource adapter,
                // even though it indicates that it
                // SupportsLocalTransactions, WAS z/OS cannot
                // currently use the CICS resource adapter SPI
                // Local Transaction Support when the adapter
                // is running as an RRSTransactional adapter.
                // This is because CICS attempts to establish a
                // private transaction context with RRS and
                // this fails because an authorized transaction
                // context (created by WAS) already exists.
                // To allow for the possibility that CICS could
                // change it's support in the future, a check
                // will be made to see if the CICS adapter
                // supports CCI Local Transaction processing.
                // When CICS does change it's SPI J2EE Local
                // transaction support, it has agreed that they
                // will also support CCI Local Transaction
                // support at that time. Thus, if the adapter
                // supports CCI Local Transaction processing,
                // then it will be assumed that the SPI
                // Local Transaction processing will now also
                // work OK under WAS z/OS.

                switch (mcWrapper.gConfigProps.transactionSupport) {

                    // resource adapter supports neither resource manager nor JTA transactions
                    case NoTransaction:

                        if (!rrsTransactional) { // No RRS-coordinated transaction support
                            if (isTraceOn && tc.isDebugEnabled()) {
                                if (uowCoord.isGlobal()) {
                                    Tr.debug(this, tc, "Creating NoTransactionWrapper for use in Global Transaction. RA supports No Transaction.");
                                } else {
                                    Tr.debug(this, tc, "Creating NoTransactionWrapper for use in Local Transaction. RA supports No Transaction.");
                                }
                            }

                            wrapper = mcWrapper.getNoTransactionWrapper();
                        } else { // RRS-coordinated transaction support
                            if (uowCoord.isGlobal()) { // global transaction scope
                                if (isTraceOn && tc.isDebugEnabled()) {
                                    Tr.debug(this, tc, "Creating RRSGlobalTransactionWrapper for use in Global Transaction. RA supports RRS Coordinated Transactions.");
                                }
                                wrapper = mcWrapper.getRRSGlobalTransactionWrapper();
                            } else { // local transaction scope
                                if (!localTranSupportSet) {
                                    mcWrapper.gConfigProps.setLocalTranSupport(false); // not CICS ECI resource adapter
                                    localTranSupportSet = true;
                                }
                                if (mcWrapper.gConfigProps.cciLocalTranSupported) { // CICS ECI resource adapter
                                    if (isTraceOn && tc.isDebugEnabled()) {
                                        Tr.debug(this, tc, "Creating LocalTransactionWrapper for use in Local Transaction under RRSTransactional adapter.");
                                    }
                                    wrapper = mcWrapper.getLocalTransactionWrapper(rrsTransactional);
                                } else { // not CICS ECI resource adapter
                                    if (isTraceOn && tc.isDebugEnabled()) {
                                        Tr.debug(this, tc, "Creating RRSLocalTransactionWrapper for use in Local Transaction under RRSTransactional adapter.");
                                    }
                                    wrapper = mcWrapper.getRRSLocalTransactionWrapper();
                                }
                            } // end local scope
                        } // end RRS-coordinated

                        break;

                    // resource adapter supports resource manager local transactios
                    case LocalTransaction:

                        if (!rrsTransactional) { // No RRS-coordinated transaction support
                            if (isTraceOn && tc.isDebugEnabled()) {
                                if (uowCoord.isGlobal()) { // global transaction scope
                                    Tr.debug(this, tc, "Creating LocalTransactionWrapper for use in Global Transaction. RA supports Local Transaction.");
                                } else { // local transaction scope
                                    Tr.debug(this, tc, "Creating LocalTransactionWrapper for use in Local Transaction. RA supports Local Transaction.");
                                }
                            } // end if (TraceComponent.isAnyTracingEnabled()  && tc.isDebugEnabled())
                            wrapper = mcWrapper.getLocalTransactionWrapper();
                        } else { // RRS-coordinated transaction support
                            if (uowCoord.isGlobal()) { // global transaction scope
                                if (isTraceOn && tc.isDebugEnabled()) {
                                    Tr.debug(this, tc, "Creating RRSGlobalTransactionWrapper for use in Global Transaction. RA supports RRS Coordinated Transactions.");
                                }
                                wrapper = mcWrapper.getRRSGlobalTransactionWrapper();
                            } else { // local transaction scope
                                if (!localTranSupportSet) {
                                    String mcfClass = mcWrapper.get_managedConnectionFactory().getClass().getName();
                                    if (mcfClass.equals("com.ibm.connector2.cics.ECIManagedConnectionFactory")) {
                                        mcWrapper.gConfigProps.setLocalTranSupport(raSupportsCCILocalTran(mcWrapper.get_managedConnectionFactory()));
                                        localTranSupportSet = true;

                                    }
                                }

                                if (mcWrapper.gConfigProps.cciLocalTranSupported) { // CICS ECI resource adapter
                                    if (isTraceOn && tc.isDebugEnabled()) {
                                        Tr.debug(this, tc, "Creating LocalTransactionWrapper for use in Local Transaction under RRSTransactional adapter.");
                                    }
                                    wrapper = mcWrapper.getLocalTransactionWrapper(rrsTransactional);
                                } else { // not CICS ECI resource adapter
                                    if (isTraceOn && tc.isDebugEnabled()) {
                                        Tr.debug(this, tc, "Creating RRSLocalTransactionWrapper for use in Local Transaction under RRSTransactional adapter.");
                                    }
                                    wrapper = mcWrapper.getRRSLocalTransactionWrapper();
                                }
                            } // end else (local scopr)
                        } // end else (RRS-coordinated)

                        break;

                    // resource adapter supports both resource manager local and JTA transactions
                    case XATransaction:

                        if (!rrsTransactional) { // No RRS-coordinated transaction support
                            if (uowCoord.isGlobal()) { // global transaction scope
                                if (isJDBC && mcWrapper.getManagedConnection().getXAResource() instanceof com.ibm.tx.jta.OnePhaseXAResource) {
                                    if (isTraceOn && tc.isDebugEnabled()) {
                                        Tr.debug(this, tc, "Creating LocalTransactionWrapper for use in Global Transaction.  The resource adapter supports XA Transaction");
                                    }
                                    wrapper = mcWrapper.getLocalTransactionWrapper();
                                } else { // xaResource is two-phase
                                    if (isTraceOn && tc.isDebugEnabled()) {
                                        Tr.debug(this, tc, "Creating XATransactionWrapper for use in Global Transaction.  The resource adapter supports XA Transaction");
                                    }
                                    wrapper = mcWrapper.getXATransactionWrapper();
                                }
                            } // end of global transaction scope
                            else { // local transaction scope
                                if (isTraceOn && tc.isDebugEnabled()) {
                                    Tr.debug(this, tc, "Creating LocalTransactionWrapper for use in Local Transaction. The resource adapter supports XA Transaction.");
                                }
                                wrapper = mcWrapper.getLocalTransactionWrapper();
                            }
                        } else { // RRS-coordinated transaction support
                            if (uowCoord.isGlobal()) { // global transaction scope
                                if (isTraceOn && tc.isDebugEnabled()) {
                                    Tr.debug(this, tc, "Creating RRSGlobalTransactionWrapper for use in Global Transaction. RA supports RRS Coordinated Transactions.");
                                }
                                wrapper = mcWrapper.getRRSGlobalTransactionWrapper();
                            } else { // local transaction scope
                                if (!localTranSupportSet) {
                                    String mcfClass = mcWrapper.get_managedConnectionFactory().getClass().getName();
                                    if (mcfClass.equals("com.ibm.connector2.cics.ECIManagedConnectionFactory")) {
                                        mcWrapper.gConfigProps.setLocalTranSupport(raSupportsCCILocalTran(mcWrapper.get_managedConnectionFactory()));
                                        localTranSupportSet = true;
                                    }
                                }
                                if (mcWrapper.gConfigProps.cciLocalTranSupported) { // CICS ECI resource adapter
                                    if (isTraceOn && tc.isDebugEnabled()) {
                                        Tr.debug(this, tc, "Creating LocalTransactionWrapper for use in Local Transaction under RRSTransactional adapter.");
                                    }
                                    wrapper = mcWrapper.getLocalTransactionWrapper(rrsTransactional);
                                } else { // Not CICS ECI resource adapter
                                    wrapper = mcWrapper.getRRSLocalTransactionWrapper();
                                    if (isTraceOn && tc.isDebugEnabled()) {
                                        Tr.debug(this, tc, "Created RRSLocalTransactionWrapper for use in Local Transaction under RRSTransactional adapter.");
                                    }
                                }
                            } // end local scope
                        } // end RRS-coordinated transaction support

                        break;
                    default:
                } // end of switch
            } // end else transactional datasource

            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Created transaction wrapper@" + Integer.toHexString(wrapper.hashCode()));
            }

            /*
             * If we experience a problem during the addSync we won't have registered for synchronisation
             * so we need to tidy up properly here. Once we have registered for synchronisation we can always
             * tidy up at transaction completion.
             */

            try {
                registeredForSync = wrapper.addSync();
                // TODO The following code would exit regardless if we just registered for
                // synchronization in an LTC or a global transaction.  Is that what we want?
                // We're waiting for some data from JetStream or Transactions to determine
                // the proper course of action.
                if (mcWrapper.isConnectionSynchronizationProvider()) {
                    /*
                     * If this is a connection synchronization provider, they are
                     * resposible for all of the remaining transactions work.
                     */
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "This managed connection is a synchronization provider.");
                    }
                    if (isTraceOn && tc.isEntryEnabled()) {
                        Tr.exit(this, tc, "initializeForUOW");
                    }
                    return;
                }

            } catch (ResourceException e) {

                // Note: the wrapper handles all the appropriate logging of the exception.
                //  We catch it here so that we can do the appropriate cleanup.
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.ConnectionManager.initializeForUOW", "730", this);
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Exception:" + e);
                try {
                    mcWrapper.releaseToPoolManager();
                } catch (Exception ex) { // ignore.
                }
                throw e;

            } // end catch ResourceException

            //NOTE: only global transactions or local transaction with resolution control 'container at boundary'
            //  will be enlisted during allocate.
            //  Local transactions with resolution control 'application' will be enlisted during the
            //  localTransactionStarted event on the connectionEventListner.

            try {

                if (uowCoord != null) {

                    if (uowCoord.isGlobal()) {

                        // In global tran.

                        // If deferred enlistment is supported by the RA, then enlistment in a global tran
                        // will happen in the interactionPending() method of the ConnectionEventListener.
                        // Otherwise, enlist here.
                        //
                        // RRS Global Trans will opt out of deferred enlistment because in some cases it will be
                        //  necessary to track states via the enlistment, and this needs to be done as soon as
                        //  the connection is obtained

                        if (!mcWrapper.gConfigProps.isDynamicEnlistmentSupported() || rrsTransactional) {

                            wrapper.enlist();
                            enlisted = true;

                        }

                    } else {

                        // delete check for rrsTransactional
                        if (J2CUtilityClass.isContainerAtBoundary(mcWrapper.pm.connectorSvc.transactionManager)) {
                            // LTC with resolution of ContainerAtBoundary. Agressively enlist in the transaction.
                            wrapper.enlist();
                            enlisted = true;
                        }

                    }
                }

            } // end try
            catch (ResourceException e) {

                // NOTE: we only need to catch ResourceException here because our wrapper
                //  code only throws resource exceptions.

                // clean everything up!
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.ConnectionManager.initializeForUOW", "762", this);

                // Only mark stale and throw exception at this time since we've already registered
                // for synchronization.  Allow the transaction to be rolled back to clean this up.
                // mcWrapper.markStale(); 154675 - already marked stale
                // mcWrapper.releaseToPoolManager();

                if (isTraceOn && tc.isEntryEnabled()) {
                    Tr.exit(this, tc, "initializeForUOW", "completed cleanup due to exception.");
                }
                throw e;

            } // end catch ResourceException

        } // end UOW.GLOBAL_TRAN_ACTIVE

        // If we have not registered for Synchronization and have not enlisted, then we
        // are not involved in any way with the current transaction, thus we need to reset the
        // UOWCoord in the mcWrapper so that deferred enlistment will work for JMS.
        if (!registeredForSync && !enlisted && !originIsDeferred) {
            mcWrapper.setUOWCoordinator(null);
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "initializeForUOW");
        }

    }

    /**
     *
     * Lazy Transaction Enlistment Optimization (deferred enlistment, CEL.interactionPending())
     *
     * Note for code updates:
     *
     * This is deferred enlistment for 6.0 and later. If code changes are needed for this
     * method, the same changes may be needed for CEL.interactionPending(). New function type changes should only
     * be undated in this method.
     *
     * We do not want to add new function in CEL.interactionPending(). RAs using CEL.interactionPending(), should move to
     * this method. CEL.interactionPending() is deprecated starting in v6.0.
     */
    @Override
    public void lazyEnlist(ManagedConnection mc) throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "lazyEnlist");
        }

        /*
         * Get the mcw from the mc to mcw hash map.
         */
        MCWrapper mcWrapper = null;

        if (mc != null) {
            mcWrapper = (MCWrapper) _pm.getMCWFromMctoMCWMap(mc);

            if (mcWrapper == null) {
                if (isTraceOn && tc.isDebugEnabled()) {
                    String errorString = "The " + mc + " could not be matched with a mcWrapper for pool manager " + _pm.hashCode() + " Dumping the mc to mcWrapper table "
                                         + _pm.getMCtoMCWMapToString();
                    Tr.debug(this, tc, errorString);
                }
                String errorString = "The ManagedConnection from resource " + _pm.gConfigProps.cfName + " could not be enlisted with the current transaction.";
                Tr.error(tc, "FAILED_TO_ASSOCIATE_CONNECTION_J2CA0292", _pm.gConfigProps.cfName);
                ResourceException e = new ResourceException(errorString);
                throw e;
            }
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Used mc " + mc + " to find mcWrapper " + mcWrapper);
            }
        } else {
            if (isTraceOn && tc.isDebugEnabled()) {
                String errorString = "The managed connection is null, we can not find the matching managed connection wrapper in connection pool " + _pm.hashCode();
                Tr.debug(this, tc, errorString);
            }
            String errorString = "The Connection Manager lazyEnlist method requires a non-null ManagedConnection parameter.";
            Tr.error(tc, "FAILED_TO_ENLIST_CONNECTION_J2CA0293", _pm.gConfigProps.cfName);
            ResourceException e = new ResourceException(errorString);
            throw e;
        }

        /*
         * Moved and modified code from ConnectionEventListener interactionPending to this method.
         * Removed the call to interactionPending.
         */

        if (!_pm.gConfigProps.isDynamicEnlistmentSupported()) {
            if (isTraceOn && tc.isEntryEnabled()) {
                Tr.exit(this, tc, "lazyEnlist", "lazyEnlist Not Supported.  Nothing to do. Returning.");
            }
            return;
        }

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
        //
        // And, now that we have cleaned up the null ConnectionManager reference
        //  when processing the exception case we can allow the
        //  resetting of the coordinator for the transaction timeout scenario as
        //  well.  They will get an enlistment failure exception
        //  which should be consistent with the expected result.
        //

        UOWCoordinator uowCoord = mcWrapper.getUOWCoordinator();
        boolean uowCoordNotSet = false;
        if (uowCoord == null) {
            uowCoordNotSet = true;
            uowCoord = mcWrapper.updateUOWCoordinator();
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "uowCoord was null, updating it to current coordinator");
            }
        }

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Coordinator in effect: " + uowCoord);
        }

        if (uowCoord != null) {
            if (uowCoord.isGlobal()) {
                //NOTE: global transactions will be enlisted here only if the RA supports deferred enlistment.
                //  Local trans will be enlisted during the localTransactionStarted event on the
                //  connectionEventListner.

                TranWrapper wrapper = null;
                try {
                    // If deferred enlistment is supported by the RA, then enlistment in a global tran
                    // will happen here. Otherwise, enlistment is done in the allocate() method of the CM.
                    wrapper = mcWrapper.getCurrentTranWrapper();
                    if (wrapper == null) {
                        Tr.error(tc, "NULL_TRAN_WRAPPER_J2CA0057");
                        // Bad state.  Throw a Runtime exception.
                        if (isTraceOn && tc.isEntryEnabled()) {
                            Tr.exit(this, tc, "lazyEnlist", "No TranWrapper found.");
                        }
                        RuntimeException rte = new IllegalStateException("lazyEnlist: No TranWrapper found.");
                        throw rte;
                    }
                } catch (Exception e) {
                    // clean everything up!
                    com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.ConnectionManager.lazyEnlist", "1307", this);
                    Tr.error(tc, "ENLIST_FAILED_J2CA0074", new Object[] { "lazyEnlist", e, _pm.gConfigProps.cfName });
                    try {
                        mcWrapper.markStale();
                        mcWrapper.releaseToPoolManager();
                    } catch (Exception e2) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(e2, "com.ibm.ejs.j2c.ConnectionManager.lazyEnlist", "1316", this);
                    }
                    // Note that the enlist call will set RollbackOnly as appropriate.
                    // Throw a ResourceException to indicate enlistment failure to the ResourceAdapter
                    ResourceException re = new ResourceException(e.getMessage());
                    re.initCause(e);
                    throw re;
                }

                wrapper.enlist();

            } else {
                /*
                 * For a local transaction, if the uowCoord was not already set,
                 * we need to set it to null in the mcWrapper.
                 */
                if (uowCoordNotSet) {
                    mcWrapper.resetCoordinator();
                }
            }
        } else {
            // No transaction context.
            //
            // Now that we are requiring a transaction context to execute, we will go ahead with logging an
            //   error message and throwing an exception in this case.  There is no "cleanup" to do on the
            //   connection since that was taken care of by other processing (probably during the transaction
            //   timeout processing).
            //
            // If we get to this point (missing transaction context even after attempting to update the
            //  transaction context above), then this is an error whether we are supporting
            //  "no transaction mode" or not.  The RA is asking for us to enlist in a transaction.  If there
            //  is none, then we should log the message and throw the exception regardless.
            //
            //   We decided to loosen the requirement for a transaction context when lazyEnlist is
            //   invoked.  We will now enlist, if a transaction context exists.  Otherwise, we will
            //   conditionally log a message and just return.  This processing will now be consistent
            //   with non-deferred enlistment processing.
            //
            //   The conditional can be triggered by setting the logMissingTranContext property on
            //   an individual datasource or connection factory basis.  The other half of the conditional
            //   allows for non-transactional access to resources for JMS access from EJB 1.1
            //   modules.
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Transaction context does NOT exist");
            }
            if (_pm.gConfigProps.logMissingTranContext) {
                Tr.warning(tc, "MISSING_TRANSACTION_CONTEXT_J2CA0075", "lazyEnlist");
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "lazyEnlist");
        }
    }

    /**
     * Lazy Connection Association Optimization (smart handles)
     */
    @Override
    public void associateConnection(
                                    Object connection,
                                    ManagedConnectionFactory mcf,
                                    ConnectionRequestInfo cri) throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "associateConnection");
        }
        /*
         * Get the subject information
         */
        Subject subject = (containerManagedAuth ? getFinalSubject(cri, mcf, this) : null);
        // Give the security helper a chance to finalize what subject
        // should be used. When ThreadIdentitySupport = NOTALLOWED the
        // original subject will be unchanged.

        //subject = securityHelper.finalizeSubject(subject, cri, inuseCM.cmConfig);
        /*
         * Call associate connection
         */
        associateConnection(mcf, subject, cri, connection);
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "associateConnection");
        }
    }

    // This implements the public method with allows smartHandles which are currently
    // not associated with a connection to get reassociated with a valid connection.
    private void associateConnection(ManagedConnectionFactory mcf,
                                     Subject subject,
                                     ConnectionRequestInfo cri,
                                     Object connection) throws ResourceException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "associateConnection");

        UOWCurrent uowCurrent = (UOWCurrent) _pm.connectorSvc.transactionManager;
        UOWCoordinator uowCoord = uowCurrent == null ? null : uowCurrent.getUOWCoord();
        MCWrapper mcWrapper = null;
        Object credTokenObj = null;
        try { // Begin processing to get connection

            // Perform any security setup that may be needed
            // before we proceed to get a connection.

            credTokenObj = securityHelper.beforeGettingConnection(subject, cri);

            // Get an appropriate wrappered ManangedConnection.
            mcWrapper = allocateMCWrapper(mcf, cri,
                                          subject,
                                          uowCoord);

        } // end try block
        finally {
            // A "finally" clause is implemented to ensure
            // any thread identity pushed to the OS
            // thread is removed and the original identity
            // is restored.
            if (credTokenObj != null) {
                securityHelper.afterGettingConnection(subject, cri, credTokenObj);
            }
        }

        involveMCInTran(mcWrapper, uowCoord, this);

        // Reassociate the handle which was passed in with the ManagedConnection (via MCWrapper).
        // Note: since associateConnection is called to reassociate a smart handle which is
        //  currently not associated to any MC, the fromMCWrapper parm in the call below will
        //  be null.
        reassociateConnectionHandle(connection, null, mcWrapper, uowCoord);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "associateConnection");

    }

    // This implements the private method with allows reassociates a connection handle
    // with a new MCWrapper.  The fromMCWrapper can be null - needed for SmartHandle support.
    private void reassociateConnectionHandle(Object connection,
                                             MCWrapper fromMCWrapper,
                                             MCWrapper toMCWrapper,
                                             UOWCoordinator uowCoord) throws ResourceException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "reassociateConnectionHandle");

        try {
            toMCWrapper.associateConnection(connection, fromMCWrapper);
        } catch (ResourceException e) {

            /*
             * If the Resource Adapter throws a ResourceException and we are not in a
             * transaction and there are no other connections open on the ManagedConnection,
             * return the ManagedConnection to the pool before
             * rethrowing the exception. The ManagedConnection is probably OK - the exception
             * may only be a logon failure or similar so the MC shouldn't be 'lost'.
             *
             * If we are in a transaction, just throw the exception. Assume we will cleanup during
             * the aftercompletion call on the tran wrapper.
             */

            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ejs.j2c.ConnectionManager.reassociateConnectionHandle", "479", this);
            if (uowCoord == null && toMCWrapper.getHandleCount() == 0) {
                try {
                    toMCWrapper.releaseToPoolManager();
                } catch (Exception exp) { // don't rethrow, already on exception path
                    com.ibm.ws.ffdc.FFDCFilter.processException(exp, "com.ibm.ejs.j2c.ConnectionManager.reassociateConnectionHandle", "487", this);
                    //if (TraceComponent.isAnyTracingEnabled()  && tc.isDebugEnabled())
                    Tr.error(tc, "FAILED_CONNECTION_RELEASE_J2CA0022", new Object[] { exp, toMCWrapper.gConfigProps.cfName });
                }
            }
            throw e;
        } catch (java.lang.Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e,
                                                        "com.ibm.ejs.j2c.ConnectionManager.reassociateConnectionHandle",
                                                        "495", this);
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "reassociateConnectionHandle: Caught a Non resource exception from mc.associateConnection()");
            }
            Tr.error(tc, "FAILED_CONNECTION_J2CA0021", new Object[] { e, toMCWrapper.gConfigProps.cfName });
            /*
             * If the Resource Adapter throws an Exception and we are not in a
             * transaction and there are no other connections open on the ManagedConnection,
             * return the ManagedConnection to the pool before
             * rethrowing the exception. The ManagedConnection is probably OK - the exception
             * may only be a logon failure or similar so the MC shouldn't be 'lost'.
             *
             * If we are in a transaction, just throw the exception. Assume we will cleanup during
             * the aftercompletion call on the tran wrapper.
             */

            if (uowCoord == null && toMCWrapper.getHandleCount() == 0) {
                try {
                    toMCWrapper.releaseToPoolManager();
                } catch (Exception exp) { // don't rethrow, already on exception path
                    com.ibm.ws.ffdc.FFDCFilter.processException(exp,
                                                                "com.ibm.ejs.j2c.ConnectionManager.reassociateConnectionHandle",
                                                                "518", this);
                    //if (TraceComponent.isAnyTracingEnabled()  && tc.isDebugEnabled())
                    Tr.error(tc, "FAILED_CONNECTION_RELEASE_J2CA0022", new Object[] { exp, toMCWrapper.gConfigProps.cfName });
                }
            }
            ResourceException re = new ResourceException("reassociateConnectionHandle: caught Exception");
            re.initCause(e);
            throw re;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "reassociateConnectionHandle");

    }

    /**
     * This method is called by resource adapters that support lazy associable
     * connections in order to notify the connection manager that it moved the
     * a connection handle from the inactive state to closed.
     *
     * @see javax.resource.spi.LazyAssociableConnectionManager
     *
     *      An inactive handle is disassociated from a managed conection within the
     *      connection manager. This method provides the association and enables the
     *      connection manager to perform any clean-up necessary to maintain the
     *      integrity of a managed connection and its pool when an inactive connection
     *      is closed.
     *
     * @param connection The connection handle that is now closed.
     * @param managedConnectionFactory The factory that created the handle.
     */
    @Override
    public void inactiveConnectionClosed(
                                         Object connection, ManagedConnectionFactory managedConnectionFactory) {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "inactiveConnectionClosed");
        }

        // Ensure all connection handle lists are cleared of this handle.
        // NOTE: The handle may exist in the EJB and Web container lists, but
        // these lists are currently inaccessible to J2C.

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "inactiveConnectionClosed");
        }
    }

    /**
     * Get method for shareable.
     *
     * @return the shareable flag.
     */

    boolean shareable() {
        return shareable;
    }

    @Override
    public String toString() {
        final String nl = CommonFunction.nl;
        StringBuffer buf = new StringBuffer(256);
        buf.append("[ConnectionManager]@");
        buf.append(Integer.toHexString(this.hashCode()));
        buf.append(nl);
        buf.append("JNDI Name <");
        buf.append(gConfigProps.cfName);
        buf.append(">");
        buf.append(nl);
        buf.append("shareable <");
        buf.append(shareable);
        buf.append(">");
        buf.append(nl);
        return buf.toString();
    }

    /**
     * Gets the recovery token associated with this CM. This is called in MCWrapper.setConnectionManager().
     */
    public int getRecoveryToken() {
        return recoveryToken;
    }

    /*
     * Since we're convinced the CM instances are getting garbage collected,
     * we don't need the performance drag of calling finalize(). It was only for
     * debug purposes initially.
     *
     * protected void finalize() {
     *
     * if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
     * Tr.debug(this, tc, "ConnectionManager garbage collected");
     * }
     *
     * }
     */

    /**
     * This method returns a boolean value indicating whether
     * or not CCI Local Transaction support is provided
     * by the resource adapter.
     *
     *
     */
    private boolean raSupportsCCILocalTran(ManagedConnectionFactory mcf) throws ResourceException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        ConnectionFactory cf;
        ResourceAdapterMetaData raMetaData;
        boolean cciLocalTranSupported = false;

        if (isTraceOn
            && tc.isEntryEnabled())
            Tr.entry(this, tc, "raSupportsCCILocalTran");

        if (gConfigProps.transactionSupport == TransactionSupportLevel.XATransaction
            || gConfigProps.transactionSupport == TransactionSupportLevel.LocalTransaction) {
            cf = (ConnectionFactory) mcf.createConnectionFactory(this);
            raMetaData = cf.getMetaData();
            if (raMetaData != null)
                cciLocalTranSupported = raMetaData.supportsLocalTransactionDemarcation();
        }

        if (isTraceOn
            && tc.isEntryEnabled())
            Tr.exit(this, tc, "raSupportsCCILocalTran " + cciLocalTranSupported);

        return cciLocalTranSupported;
    }

    /**
     * Overrides the default deserialization for reading this object
     */
    private void readObject(ObjectInputStream s) throws java.io.IOException, java.lang.ClassNotFoundException {
        throw new UnsupportedOperationException(); // not serializable
    }

    // This is called by the RRA only when db2 reroute is being used
    @Override
    public void purgePool() throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "purgePool");
        }

        _pm.purgePoolContents();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "purgePool");
        }
    }

    // Method to check if the mcf supports the requested branch coupling type and obtains the appropriate XA start flag
    // Used by XATransactionWrapper.enlist()
    // Only called if couplingType indicates LOOSE or TIGHT
    protected int supportsBranchCoupling(int couplingType, ManagedConnectionFactory managedConnectionFactory) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        int startFlag;
        if (isJDBC) {
            startFlag = ((WSManagedConnectionFactory) managedConnectionFactory).getXAStartFlagForBranchCoupling(couplingType);
        } else {
            String bcInfo = "branch-coupling=LOOSE";
            if (couplingType == ResourceRefInfo.BRANCH_COUPLING_TIGHT)
                bcInfo = "branch-coupling=TIGHT";

            Tr.warning(tc, "IGNORE_FEATURE_J2CA0240", new Object[] { bcInfo, gConfigProps.cfName });
            startFlag = XAResource.TMNOFLAGS; // take default
        }
        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Branch coupling request for " + cmConfig.getCFDetailsKey() + " is " + couplingType + " startFlag is " + startFlag);
        }
        return startFlag;
    }

    // Method to check if the two branch coupling options are compatible for a match for the MCF
    // Used by SharedPool.getSharedConnection()
    // May be called if couplingType indicates LOOSE or TIGHT or is UNSET
    protected boolean matchBranchCoupling(int couplingType1, int couplingType2, ManagedConnectionFactory managedConnectionFactory) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        boolean matched = true;
        if (isJDBC && couplingType1 != couplingType2) {
            // ResourceRefInfo.BRANCH_COUPLING_UNSET can default to BRANCH_COUPLING_TIGHT or BRANCH_COUPLING_LOOSE
            if (couplingType1 == ResourceRefInfo.BRANCH_COUPLING_UNSET)
                couplingType1 = ((WSManagedConnectionFactory) managedConnectionFactory).getDefaultBranchCoupling();
            else if (couplingType2 == ResourceRefInfo.BRANCH_COUPLING_UNSET)
                couplingType2 = ((WSManagedConnectionFactory) managedConnectionFactory).getDefaultBranchCoupling();
            matched = couplingType1 == couplingType2;
        }
        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Match coupling request for " + couplingType1 + " and " + couplingType2 + " match is " + matched);
        }
        return matched;
    }

    /**
     * Returns the subject for container managed authentication.
     *
     * @param requestInfo - connection request information
     * @param mangedConnectionFactory - managed connection factory
     * @param CM - connection manager
     * @return subject for container managed authentication.
     * @throws ResourceException
     */
    private final Subject getFinalSubject(ConnectionRequestInfo requestInfo,
                                          final ManagedConnectionFactory mangedConnectionFactory, Object CM) throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        Subject subj = null;
        if (this.containerManagedAuth) {
            final Map<String, Object> loginConfigProps = (Map<String, Object>) this.cmConfig.getLoginConfigProperties().clone();
            String name = this.cmConfig.getLoginConfigurationName();
            final String loginConfigurationName = name == null ? connectionFactorySvc.getJaasLoginContextEntryName() : name;

            String authDataID = (String) loginConfigProps.get("DefaultPrincipalMapping");

            // If no authentication-alias is found in the bindings, then use the default container managed auth alias (if any)
            if (authDataID == null)
                authDataID = connectionFactorySvc.getContainerAuthDataID();

            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "login configuration name", loginConfigurationName);
                Tr.debug(this, tc, "container managed auth", authDataID);
            }

            if (authDataID != null || loginConfigurationName != null) {
                loginConfigProps.put("com.ibm.mapping.authDataAlias", authDataID);
                final AuthDataService authSvc = _pm.connectorSvc.authDataServiceRef.getServiceWithException();
                try {
                    subj = AccessController.doPrivileged(new PrivilegedExceptionAction<Subject>() {
                        @Override
                        public Subject run() throws LoginException {
                            return authSvc.getSubject(mangedConnectionFactory, loginConfigurationName, loginConfigProps);
                        }
                    });
                } catch (PrivilegedActionException e) {
                    FFDCFilter.processException(e.getCause(), getClass().getName(), "3070", this, new Object[] { this });
                    ResourceException r = new ResourceException(e.getCause());
                    throw r;
                }
            }

            subj = this.securityHelper.finalizeSubject(subj, requestInfo, this.cmConfig);
        } else {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Subject is", subj);
            }
        }
        return subj;
    }

    /** {@inheritDoc} */
    @Override
    public PurgePolicy getPurgePolicy() {
        return _pm.gConfigProps.getPurgePolicy();
    }

    /** {@inheritDoc} */
    @Override
    public ResourceRefInfo getResourceRefInfo() {
        return cmConfig;
    }

    /**
     * Register XA resource information with the transaction manager.
     *
     * @param tm the transaction manager.
     * @param xaResourceInfo information necessary for producing an XAResource object using the XAResourceFactory.
     * @param commitPriority priority to use when committing multiple XA resources.
     * @return the recovery ID (or -1 if an error occurs)
     */
    final int registerXAResourceInfo(EmbeddableWebSphereTransactionManager tm,
                                     CommonXAResourceInfo xaResourceInfo, int commitPriority, String qmid) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "registerXAResourceInfo");
        // Transaction service will use the filter we provide to query the service registry for an XAResourceFactory.
        // If possible, filter on the JNDI name because the id field is optional (in which case cfKey defaults to the JNDI name)
        // and any generated unique identifier (config.id) might not be consistent across server restarts.
        // In the case of app-defined resources (@DataSourceDefinition), however, the JNDI name is not guaranteed to be unique,
        // and so the unique identifer (which for app-defined resources is always specified) must be used instead.
        CMConfigDataImpl cmConfigData = (CMConfigDataImpl) (xaResourceInfo != null ? xaResourceInfo.getCmConfig() : this.cmConfig);
        String id = cmConfigData.getCfKey();
        String jndiName = cmConfigData.getJNDIName();
        String filter;
        if (jndiName == null || id.startsWith(AppDefinedResource.PREFIX))
            filter = FilterUtils.createPropertyFilter("config.displayId", id);
        else
            filter = FilterUtils.createPropertyFilter(ResourceFactory.JNDI_NAME, jndiName);

        // Pre-serialize resource info so that the transactions bundle can deserialize
        // back to the pre-serialized form without without having access to classes in our bundle.
        // Need to use List<Byte> instead of byte[] because XARecoveryWrapper does a shallow compare. Icck.
        ArrayList<Byte> resInfo;
        try {
            if (qmid != null)
                cmConfigData.setQmid(qmid);
            byte[] bytes = CommonFunction.serObjByte(cmConfigData);
            resInfo = new ArrayList<Byte>(bytes.length);
            for (byte b : bytes)
                resInfo.add(b);
        } catch (IOException x) {
            FFDCFilter.processException(x, getClass().getName(), "581", new Object[] { xaResourceInfo.getCmConfig() });
            throw new IllegalArgumentException(x);
        }

        int recoveryToken = tm.registerResourceInfo(filter, resInfo, commitPriority);
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "registerXAResourceInfo");

        return recoveryToken;
    }

    public synchronized Integer getQMIDRecoveryToken(String qmid, PoolManager pm) {
        Integer i = null;
        if (qmidcmConfigMap == null)
            qmidcmConfigMap = new HashMap<String, Integer>();
        else {
            i = qmidcmConfigMap.get(qmid);
        }
        if (i == null) {
            EmbeddableWebSphereTransactionManager tm = pm.connectorSvc.transactionManager;

            if (tm != null) {
                if (!rrsTransactional) {
                    i = new Integer(registerXAResourceInfo(tm, null, this.commitPriority, qmid));
                    qmidcmConfigMap.put(qmid, i);
                }
            }
        }
        return i;
    }
}
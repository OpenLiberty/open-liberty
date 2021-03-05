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

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.DissociatableManagedConnection;
import javax.resource.spi.LazyEnlistableManagedConnection;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAllocationException;
import javax.security.auth.Subject;

import com.ibm.ejs.cm.logger.TraceWriter;
import com.ibm.websphere.ce.j2c.ConnectionWaitTimeoutException;
import com.ibm.websphere.jca.pmi.JCAPMIHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.j2c.MCWrapper;
import com.ibm.ws.jca.adapter.WSManagedConnection;
import com.ibm.ws.jca.cm.JcaServiceUtilities;

/*
 * This class is a container for free connections
 */
public final class FreePool implements JCAPMIHelper {

    private static final TraceComponent tc = Tr.register(FreePool.class, J2CConstants.traceSpec, J2CConstants.messageFile);

    protected final MCWrapperList mcWrapperList;
    private final String nl = CommonFunction.nl;
    protected final Integer freeConnectionLockObject = new Integer(0);
    private PoolManager pm = null;
    protected int numberOfConnectionsInFreePool = 0;
    protected int numberOfConnectionsAssignedToThisFreePool = 0;

    ClassLoader raClassLoader;

    // waiter code
    private int fatalErrorNotificationTime = 0;

    /*
     * Constants
     */
    /**
     * Wrapper does not exist in the free pool. This value is false.
     */
    private final static boolean _mcWrapperDoesNotExistInFreePool = false;
    /**
     * Synchronize in method. This value is true.
     */
    private final static boolean _synchronizeInMethod = true;
    /**
     * Notify waiter. This value is false.
     */
    private final static boolean _notifyWaiter = false;
    /**
     * Decrement total counter. This value is true.
     */
    private final static boolean _decrementTotalCounter = true;

    /**
     * Optimistic successful get of a free connection
     */
    protected int fop_gets = 0;
    /**
     * Non-Optimistic successful get of a free connection
     */
    protected int fnop_gets = 0;
    /**
     * The number of requests placed on the waiter queue
     */
    protected int freePoolQueuedRequests = 0;
    /**
     * The total number of created manged connection for this
     * free pool.
     */
    protected int freePoolCreateManagedConnection = 0;
    /**
     * The total number of claimed victims for this free pool.
     * A claimed victim is a connection that did not match with
     * the current connection request, so we destroy the non-matching
     * connection and create a new connection for this request
     */
    protected int numberOfClaimedVictims = 0;
    //The following three are a futher breakdown of the numberOfClaimedVictims count.
    protected int numberOfClaimedVictims_CRI_Only_Mismatch = 0;
    protected int numberOfClaimedVictims_Subject_Only_Mismatch = 0;
    protected int numberOfClaimedVictims_CRI_Subject_Mismatch = 0;
    protected int numberOfClaimedVictims_MM_Only_Mismatch = 0;

    /**
     * Optimistic get not found. We did not need to search the free pool.
     * No connection were available.
     */
    protected int fop_get_notfound = 0;
    /**
     * Non-Optimistic get not found. We searched the free pool for connections and
     * found no matching connections
     */
    protected int fnop_get_notfound = 0;

    private J2CGlobalConfigProperties gConfigProps = null;

    /**
     *
     * FreePool constructor
     *
     * @param initialSize
     * @param poolManager
     * @param gConfigProps
     * @param raClassLoader
     */
    public FreePool(int initialSize,
                    PoolManager poolManager,
                    J2CGlobalConfigProperties gConfigProps,
                    ClassLoader raClassLoader) {

        super();
        if (initialSize < 1) {
            initialSize = 100;
        } else if (initialSize > J2CConstants.INITIAL_SIZE) {
            initialSize = J2CConstants.INITIAL_SIZE;
        }
        mcWrapperList = new MCWrapperList(initialSize);

        pm = poolManager;
        this.gConfigProps = gConfigProps;

        this.raClassLoader = raClassLoader;

    }

    /**
     * This method is only called from code synched on the <code>freeLockObject</code>,
     * so we do not have to worry about synchronizing access to
     * <code>waiterCount</code>.
     *
     */
    private void queueRequest(ManagedConnectionFactory managedConnectionFactory, long waitTimeout) throws ResourceAllocationException, ConnectionWaitTimeoutException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "queueRequest", waitTimeout);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Current connection pool", pm.toString());
        if (waitTimeout == 0) {
            --pm.waiterCount;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(
                         tc,
                         "Timeout.  Decremented waiterCount, which is now "
                             + pm.waiterCount
                             + " on datasource "
                             + gConfigProps.cfName);
            }
            Object[] connWaitTimeoutparms = new Object[] { "queueRequest", gConfigProps.cfName, 0, pm.waiterCount,
                                                           pm.totalConnectionCount.get() };
            Tr.error(
                     tc,
                     "POOL_MANAGER_EXCP_CCF2_0001_J2CA0045",
                     connWaitTimeoutparms);
            String connWaitTimeoutMessage = Tr.formatMessage(tc, "POOL_MANAGER_EXCP_CCF2_0001_J2CA0045", connWaitTimeoutparms);
            ConnectionWaitTimeoutException cwte = new ConnectionWaitTimeoutException(connWaitTimeoutMessage);
            com.ibm.ws.ffdc.FFDCFilter.processException(cwte, J2CConstants.DMSID_MAX_CONNECTIONS_REACHED, "192", this.pm);
            pm.activeRequest.decrementAndGet();

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "queueRequest", cwte);
            throw cwte;
        }
        if ((tc.isDebugEnabled())) {
            ++freePoolQueuedRequests;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                String poolStats = pm.gatherPoolStatisticalData();
                Tr.debug(this, tc, "Queueing Waiter for pool <" + gConfigProps.getXpathId() + ">. Current Pool Stats are:");
                Tr.debug(this, tc, poolStats);
            }
        }
        try {
            if (pm.displayInfiniteWaitMessage) {
                Tr.info(tc, "INFINITE_CONNECTION_WAIT_TIMEOUT_J2CA0127", gConfigProps.getXpathId());

                pm.displayInfiniteWaitMessage = false; // only display this message once per PM.
            }
            pm.activeRequest.decrementAndGet();
            if (waitTimeout < 0) {
                pm.waiterFreePoolLock.wait(0); //wait an infinite amount of time
            } else {
                pm.waiterFreePoolLock.wait(waitTimeout); //wait the specified amount of time
            }
            pm.requestingAccessToPool();
        } catch (InterruptedException ie) {
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Interupted waiting for a connection");
            }

            synchronized (pm.waiterFreePoolLock) {
                --pm.waiterCount;
            }

            if (tc.isDebugEnabled()) {
                if (pm.waiterCount == 0) {
                    pm.waitersEndedTime = System.currentTimeMillis();
                    Tr.debug(this, tc, "Waiters: requests for connections are no longer being queued. End Time:" + pm.waitersEndedTime);
                    Tr.debug(this, tc, "Waiters: total time waiter were in queue: " + (pm.waitersEndedTime - pm.waitersStartedTime));
                }
            }
            ResourceAllocationException throwMe = new ResourceAllocationException(ie.getMessage());
            throwMe.initCause(ie);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "queueRequest", throwMe);
            throw throwMe;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "queueRequest");
        }
    }

    /**
     * Return the mcWrapper to the free pool.
     *
     * @pre mcWrapper != null
     */
    protected void returnToFreePool(MCWrapper mcWrapper) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "returnToFreePool", gConfigProps.cfName);
        }
        if (mcWrapper.shouldBeDestroyed() || mcWrapper.hasFatalErrorNotificationOccurred(fatalErrorNotificationTime)
            || ((pm.agedTimeout != -1)
                && (mcWrapper.hasAgedTimedOut(pm.agedTimeoutMillis)))) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                if (mcWrapper.shouldBeDestroyed()) {
                    Tr.debug(this, tc, "Connection destroy flag is set, removing connection " + mcWrapper);
                }
                if (mcWrapper.hasFatalErrorNotificationOccurred(fatalErrorNotificationTime)) {
                    Tr.debug(this, tc, "Fatal error occurred, removing connection " + mcWrapper);
                }
                if (((pm.agedTimeout != -1) && (mcWrapper.hasAgedTimedOut(pm.agedTimeoutMillis)))) {
                    Tr.debug(this, tc, "Aged timeout exceeded, removing connection " + mcWrapper);
                }
                if (mcWrapper.isDestroyState()) {
                    Tr.debug(this, tc, "Mbean method purgePoolContents with option immediate was used." +
                                       "  Connection cleanup and destroy is being processed.");
                }
            }
            if (mcWrapper.isDestroyState()) {
                final FreePool tempFP = this;
                final MCWrapper tempMCWrapper = mcWrapper;
                ThreadSupportedCleanupAndDestroy tscd = new ThreadSupportedCleanupAndDestroy(pm.tscdList, tempFP, tempMCWrapper);
                pm.tscdList.add(tscd);
                pm.connectorSvc.execSvcRef.getServiceWithException().submit(tscd);
            } else {
                cleanupAndDestroyMCWrapper(mcWrapper); // cleanup, remove, then release mcWrapper
                // Do not return this mcWrapper back to the free pool.
                removeMCWrapperFromList(mcWrapper, _mcWrapperDoesNotExistInFreePool, _synchronizeInMethod, _notifyWaiter, _decrementTotalCounter);
            }
        } else {
            returnToFreePoolDelegated(mcWrapper); // Added to Help PMI Stuff.Unable to inject code in if/else conditional situations.
        } // end else -- the mcWrapper is not stale

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "returnToFreePool");
        }
    }

    protected void returnToFreePoolDelegated(MCWrapper mcWrapper) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "returnToFreePoolDelegated", gConfigProps.cfName);
        }
        try {
            mcWrapper.cleanup();
        } catch (Exception exn1) {
            String localMessage = exn1.getLocalizedMessage();
            if ((localMessage != null) && localMessage.equals("Skip logging for this failing connection")) {
                /*
                 * If the resource adapter throws a resource exception with the skip logging text,
                 * log this path only when debug is enabled.
                 *
                 * The resource apdater does not want normal logging of this failed managed connection.
                 */
                if (tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Connection failed, resource adapter requested skipping failure logging");
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "MCWrapper cleanup failed, datasource: " + gConfigProps.cfName);
                }
                com.ibm.ws.ffdc.FFDCFilter.processException(
                                                            exn1,
                                                            "com.ibm.ejs.j2c.poolmanager.FreePool.returnToFreePool",
                                                            "190",
                                                            this);
            }

            try {
                /*
                 * remove the mcw from the mcToMCWMap
                 */
                pm.removeMcToMCWMap(mcWrapper.getManagedConnectionWithoutStateCheck());

                mcWrapper.destroy();
            } catch (Exception exn2) {
                com.ibm.ws.ffdc.FFDCFilter.processException(
                                                            exn2,
                                                            "com.ibm.ejs.j2c.poolmanager.FreePool.returnToFreePool",
                                                            "210",
                                                            this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "datasource " + gConfigProps.cfName + ": ResourceException", exn2);
                }
            } finally {

                synchronized (pm.waiterFreePoolLock) {
                    pm.totalConnectionCount.decrementAndGet();
                    // waiter code
                    if (pm.waiterCount > 0) {
                        // there are requests waiting, so notify one of them
                        pm.waiterFreePoolLock.notify();
                    }
                } // end synchronized (freeLockObject)
            }
            // Error has occurred on cleanup, therefore the work is done
            // Return without adding this mcWrapper to the free pool.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "returnToFreePoolDelegated", exn1);
            return;

        }

        /*
         * Return mcWrapper to free pool
         */
        synchronized (pm.waiterFreePoolLock) {
            // waiter code
            if ((pm.waiterCount > 0) && (pm.waiterCount > pm.mcWrapperWaiterList.size())) {
                // there are requests waiting, so notify one of them
                pm.mcWrapperWaiterList.add(mcWrapper);
                mcWrapper.setPoolState(4);
                pm.waiterFreePoolLock.notify();
            } else {
                synchronized (freeConnectionLockObject) {
                    mcWrapperList.add(mcWrapper); // Add to end of list
                    mcWrapper.setPoolState(1);
                }
            }
            ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).setAlreadyBeingReleased(false);
        } // end synchronized (freeLockObject)

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "returnToFreePoolDelegated");
        }
    }

    /**
     * This method will try to cleanup and destroy the connection and remove the
     * mcWrapper from the free pool.
     *
     * - If removeFromFreePool is true, the mcWrapper exists in the free pool
     * - If removeFromFreePool is false, the mcWrapper do not exist in the free pool
     *
     * @param Managed connection wrapper
     * @param Remove  from free pool
     * @param Are     we already synchronized on the freeLockObject
     * @param Skip    waiter notify
     * @param Cleanup and Destroy MCWrapper
     * @pre mcWrapper != null
     * @throws ClassCastException
     */

    protected void removeMCWrapperFromList(
                                           MCWrapper mcWrapper,
                                           boolean removeFromFreePool,
                                           boolean synchronizationNeeded,
                                           boolean skipWaiterNotify,
                                           boolean decrementTotalCounter) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "removeMCWrapperFromList");
        }

        /*
         * This cast to UserData is safe, and does not need a
         * try/catch.
         */
        if (synchronizationNeeded) {
            synchronized (pm.waiterFreePoolLock) {
                synchronized (freeConnectionLockObject) {
                    if (removeFromFreePool) { // mcWrapper may not be in the free pool
                        mcWrapperList.remove(mcWrapper);
                        mcWrapper.setPoolState(0);
                    }
                    --numberOfConnectionsAssignedToThisFreePool;
                    if (decrementTotalCounter) {
                        pm.totalConnectionCount.decrementAndGet();
                    }
                    if (!skipWaiterNotify) {
                        // waiter code
                        //synchronized(pm.waiterFreePoolLock) {
                        pm.waiterFreePoolLock.notify();
                        //}
                    }
                }
            }
        } else {
            if (removeFromFreePool) { // mcWrapper may not be in the free pool
                mcWrapperList.remove(mcWrapper);
                mcWrapper.setPoolState(0);
                --numberOfConnectionsAssignedToThisFreePool;
            }
            if (decrementTotalCounter) {
                pm.totalConnectionCount.decrementAndGet();
            }
            // This code block may dead lock if we have the freePool lock and need to notify.
            // Since the current code does not use the notify code I am
            // commenting out the follow notify code
            //if (!skipWaiterNotify) {
            // waiter code
            //  synchronized(pm.waiterFreePoolLock) {
            //    pm.waiterFreePoolLock.notify();
            //  }
            //}
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "removeMCWrapperFromList");
        }
    }

    protected boolean cleanupAndDestroyMCWrapper(MCWrapper mcWrapper) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "cleanupAndDestroyMCWrapper");
        }
        boolean errorOccurred = false;
        /*
         * remove the mcw from the mcToMCWMap
         */
        pm.removeMcToMCWMap(mcWrapper.getManagedConnectionWithoutStateCheck());

        try {
            mcWrapper.cleanup();
        } catch (Exception exn1) {
            errorOccurred = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "MCWrapper cleanup failed, datasource: " + gConfigProps.cfName);
            }
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        exn1,
                                                        "com.ibm.ejs.j2c.poolmanager.FreePool.cleanupAndDestroyMCWrapper",
                                                        "1140",
                                                        this);
        }

        try {
            mcWrapper.destroy();
        } catch (Exception exn2) {
            errorOccurred = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "MCWrapper destroy failed, datasource: " + gConfigProps.cfName);
            }
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        exn2,
                                                        "com.ibm.ejs.j2c.poolmanager.FreePool.cleanupAndDestroyMCWrapper",
                                                        "300",
                                                        this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "cleanupAndDestroyMCWrapper");
        }
        return errorOccurred;
    }

    /**
     * Remove the parked managed connection. This method should only be called if do not have
     * SmartHandleSupport in effect. The PoolManager controls the SmartHandleSupported flag, so
     * we have to trust the PM not to call this method when smart handles is in effect.
     */
    protected void removeParkedConnection() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "removeParkedConnection");
        }

//        boolean errorOccured = false;

        if (pm.parkedMCWrapper != null) { // Only attempt to cleanup and destroy the parked MC if one exists...

            synchronized (pm.parkedConnectionLockObject) {

                if (pm.parkedMCWrapper != null) {
                    cleanupAndDestroyMCWrapper(pm.parkedMCWrapper);
                    //          try {
                    //            pm.parkedMCWrapper.cleanup();
                    //          }
                    //          catch (Exception exn1)
                    //            errorOccured = true;
                    //            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                    //              Tr.debug(this, tc,"parked MCWrapper cleanup failed, datasource: " + gConfigProps.pmiName);
                    //            }
                    //            com.ibm.ws.ffdc.FFDCFilter.processException(
                    //            exn1,
                    //            "com.ibm.ejs.j2c.poolmanager.FreePool.removeParkedConnection",
                    //            this);
                    //          }
                    //
                    //          try {
                    //            pm.parkedMCWrapper.destroy();                 // no PMI for this connection.
                    //          } catch (Exception exn1)
                    //            errorOccured = true;
                    //            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                    //              Tr.debug(this, tc,"parked MCWrapper destroy failed, datasource: " + gConfigProps.pmiName);
                    //            }
                    //            com.ibm.ws.ffdc.FFDCFilter.processException(
                    //            exn1,
                    //            "com.ibm.ejs.j2c.poolmanager.FreePool.removeParkedConnection",
                    //            this);
                    //          }

                    pm.parkedMCWrapper = null;

                    // Reset value to true to create a new parked connection.
                    pm.createParkedConnection = true;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(
                                 tc,
                                 "Reset the createParkedConnection flag to recreate a new parked connection");
                    }
                }
            } // end sync
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "removeParkedConnection");
        }
    }

    /**
     * Total number of managed connection wrappers containing managed connections
     */

    protected int getTotalConnectionCount() {
        return pm.totalConnectionCount.get();
    }

    /**
     * Return a mcWrapper from the free pool.
     */
    protected MCWrapper getFreeConnection(ManagedConnectionFactory managedConnectionFactory, Subject subject, ConnectionRequestInfo cri,
                                          int hashCode) throws ResourceAllocationException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getFreeConnection", gConfigProps.cfName);
        }

        MCWrapper mcWrapper = null;
        MCWrapper mcWrapperTemp1 = null;
        MCWrapper mcWrapperTemp2 = null;
        int mcwlSize = 0;
        int mcwlIndex = 0;

        synchronized (freeConnectionLockObject) {
            mcwlSize = mcWrapperList.size();
            if (mcwlSize > 0) {
                mcwlIndex = mcwlSize - 1;
                // Remove the first mcWrapper from the list (Optimistic)
                mcWrapperTemp1 = (MCWrapper) mcWrapperList.remove(mcwlIndex);
                mcWrapperTemp1.setPoolState(0);
            }

        }

        /*
         * At this point we may have a mcWrapper and we can release the free pool lock. This is a
         * very optimistic view of the world. We assume that the connection will match most of the time to
         * have released the lock.
         */
        if (mcWrapperTemp1 != null) {

            if (hashCode == mcWrapperTemp1.getSubjectCRIHashCode()) {
                if (((com.ibm.ejs.j2c.MCWrapper) mcWrapperTemp1).do_not_reuse_mcw) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Connection error occurred for this mcw " + mcWrapperTemp1 + ", mcw will not be reused");
                    }
                    synchronized (pm.waiterFreePoolLock) {
                        cleanupAndDestroyMCWrapper(mcWrapperTemp1);
                        synchronized (freeConnectionLockObject) {
                            --numberOfConnectionsAssignedToThisFreePool;
                        }
                        pm.totalConnectionCount.decrementAndGet();
                        if ((pm.waiterCount > 0) && (pm.waiterCount > pm.mcWrapperWaiterList.size())) {
                            pm.waiterFreePoolLock.notify();
                        }
                    }
                } else
                    mcWrapper = getMCWrapperFromMatch(subject, cri, managedConnectionFactory, mcWrapperTemp1);
            }

            if (mcWrapper == null) {
                /*
                 * In the 5.0.1 release, we had heavier locking which allowed a resource adapter
                 * to call connection error occurred during mc.matchManagedConnection. I am not
                 * 100% sure if this worked correctly in all cases, but is going to make sure it
                 * works. Any call to getMCWrapperFromMatch with calls mc.matchManagedConnection must
                 * be in a synchronized pm.waterFreePoolLock if its in a synchronized freeConnectionLockObject.
                 * Adding the pm.waiterFreePoolLock.
                 */
                synchronized (pm.waiterFreePoolLock) {
                    synchronized (freeConnectionLockObject) {
                        // We need to look through the list, since we didn't find a matching connection at the
                        // end of the list, we need to use get and only remove if we find a matching connection.
                        mcwlSize = mcWrapperList.size();
                        if (mcwlSize > 0) { // set this to 0 since mcWrappterTemp1 has already been removed.

                            mcwlIndex = mcwlSize - 1;
                            for (int i = mcwlIndex; i >= 0; --i) {

                                mcWrapperTemp2 = (MCWrapper) mcWrapperList.get(i);
                                if (hashCode == mcWrapperTemp2.getSubjectCRIHashCode()) {
                                    mcWrapper = getMCWrapperFromMatch(subject, cri, managedConnectionFactory, mcWrapperTemp2);
                                    if (((com.ibm.ejs.j2c.MCWrapper) mcWrapperTemp2).do_not_reuse_mcw) {
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                            Tr.debug(this, tc, "Connection error occurred for this mcw " + mcWrapperTemp2 + ", mcw will not be reused");
                                        }
                                        mcWrapperList.remove(i);
                                        cleanupAndDestroyMCWrapper(mcWrapperTemp2);
                                        synchronized (freeConnectionLockObject) {
                                            --numberOfConnectionsAssignedToThisFreePool;
                                        }
                                        pm.totalConnectionCount.decrementAndGet();
                                        if ((pm.waiterCount > 0) && (pm.waiterCount > pm.mcWrapperWaiterList.size())) {
                                            pm.waiterFreePoolLock.notify();
                                        }
                                    }
                                }
                                if (mcWrapper != null) {
                                    mcWrapperList.remove(i);
                                    mcWrapper.setPoolState(0);
                                    break;
                                }

                            } // for (int i = mcwlIndex; i >= 0; --i)

                        } // end if mcwlSize > 1

                    } // end synchronized (freeConnectionLockObject)

                    /*
                     * We need to add the first non-matching mcWrapper back into the free pool or waiter queue.
                     */
                    if (!((com.ibm.ejs.j2c.MCWrapper) mcWrapperTemp1).do_not_reuse_mcw) {
                        //synchronized (pm.waiterFreePoolLock) {
                        // waiter code
                        if ((pm.waiterCount > 0) && (pm.waiterCount > pm.mcWrapperWaiterList.size())) {

                            // there are requests waiting, so notify one of them
                            pm.mcWrapperWaiterList.add(mcWrapperTemp1);
                            pm.waiterFreePoolLock.notify();
                        } else {

                            synchronized (freeConnectionLockObject) {
                                mcWrapperList.add(mcWrapperTemp1); // Add to end of list
                                mcWrapperTemp1.setPoolState(1);
                            }

                        }
                        //} // end synchronized (waiterFreePoolLock)
                    } else {
                        // Cleanup mcWrapperTemp1 since it was removed from the free pool already, but not a match
                        if (((com.ibm.ejs.j2c.MCWrapper) mcWrapperTemp1).errorDuringExternalCall) {
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Connection error occurred for this mcw " + mcWrapperTemp1 + ", mcw will not be reused");
                            }
                            cleanupAndDestroyMCWrapper(mcWrapperTemp1);
                            synchronized (freeConnectionLockObject) {
                                --numberOfConnectionsAssignedToThisFreePool;
                            }
                            pm.totalConnectionCount.decrementAndGet();
                            if ((pm.waiterCount > 0) && (pm.waiterCount > pm.mcWrapperWaiterList.size())) {
                                pm.waiterFreePoolLock.notify();
                            }
                        }
                    }

                    if ((isTracingEnabled && tc.isDebugEnabled())) {
                        if (mcWrapper != null) {
                            ++fnop_gets;
                        }
                    }

                }
            } else {

                /*
                 * We found a connection the first try :-)
                 */
                if ((isTracingEnabled && tc.isDebugEnabled())) {
                    ++fop_gets;
                }

            } // end else

        }

        if ((isTracingEnabled && tc.isDebugEnabled())) {
            if (mcWrapper == null) {
                ++fnop_get_notfound;
            }
        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            if (isTracingEnabled && tc.isDebugEnabled()) {
                if (mcWrapper != null) {
                    Tr.debug(this, tc, "Returning mcWrapper " + mcWrapper);
                } else {
                    Tr.debug(this, tc, "MCWrapper was not found in Free Pool");
                }
            }
            Tr.exit(this, tc, "getFreeConnection");
        }

        return mcWrapper;

    }

    protected MCWrapper createOrWaitForConnection(
                                                  ManagedConnectionFactory managedConnectionFactory,
                                                  Subject subject,
                                                  ConnectionRequestInfo cri,
                                                  int hashMapBucket,
                                                  int maxFreePoolHashSize,
                                                  boolean createAConnection,
                                                  boolean connectionSharing, int hashCode) throws ResourceAllocationException, ConnectionWaitTimeoutException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "createOrWaitForConnection");
        }

        MCWrapper mcWrapper = null;
        boolean addingAConnection = false;

        if (!createAConnection) { // if createAConnection is true, we are claiming a victim and we just need to create a new
                                  // connection.
                                  /*
                                   * Check the totalConnectionCount to see if we can create a connection and if
                                   * maxConnections is zero just create a connection.
                                   *
                                   * changed maxConnections to use pm.maxConnections.
                                   */
            if ((pm.maxConnections == 0) || (pm.totalConnectionCount.get() < pm.maxConnections)) { // dirty read total connection count to save
                // synchronized call  - extra item
                // added (maxConnections == 0)
                synchronized (pm.pmCounterLock) {
                    if ((pm.totalConnectionCount.get() < pm.maxConnections) || pm.maxConnections == 0) {
                        /*
                         * We didn't find a connection in the free pool. Need to create a new one
                         * Add to the counters
                         */
                        pm.totalConnectionCount.incrementAndGet();
                        addingAConnection = true;

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "TotalConnectionCount is " + pm.totalConnectionCount.get());
                        }
                    }
                }
            }

        } else {
            addingAConnection = true;
        }
        /*
         * Starting the waiter code
         */
        long waitStartTime = 0L;
        long waitEndTime = 0L;
        if (!addingAConnection) {
            long waitTimeout = pm.connectionTimeout * 1000; //  convert connection timeout to milliseconds.
            long orig_waitTimeout = waitTimeout;
            long totalTimeWaited = 0;
            pm.activeRequest.decrementAndGet();
            synchronized (pm.waiterFreePoolLock) {
                pm.requestingAccessToPool();
                while (true) {
                    if (waitStartTime == 0) {
                        /*
                         *
                         * Before we synchronized on the pm.waiterFreePoolLock a connection could have
                         * been returned to the free pool.
                         *
                         * We need to check for this connection before we queue this thread. While we have
                         * the pm.waiterFreePoolLock, no connection will be return to any free pool. So,
                         * all we need to do is quickly look at the number of connection in each free pool
                         */
                        MCWrapper mcWrapperTemp = null;
                        for (int j = 0; j < maxFreePoolHashSize; ++j) {
                            /*
                             * *
                             * This is not double-checked locking. The following code diry reads the size of
                             * the mcWrapperList that may be change at any time by another thread. If it is greater than zero,
                             * we need to synchronize and check the value again. If it is still greater than
                             * zero, we have one or more mcWrappers to work with. In order to work with the
                             * mcWrapperList, we need to be synchronized.
                             */
                            if (pm.freePool[j].mcWrapperList.size() > 0) { //  Dirty read to avoid synchronize call
                                synchronized (pm.freePool[j].freeConnectionLockObject) {
                                    if (pm.freePool[j].mcWrapperList.size() > 0) {
                                        // A connection was returned to the a free pool
                                        mcWrapperTemp = (MCWrapper) pm.freePool[j].mcWrapperList.remove(0);
                                        mcWrapperTemp.setPoolState(0);
                                        mcWrapper = pm.freePool[j].getMCWrapperFromMatch(subject, cri, managedConnectionFactory, mcWrapperTemp);
                                        if (((com.ibm.ejs.j2c.MCWrapper) mcWrapperTemp).do_not_reuse_mcw) {
                                            /*
                                             * Connection error event did occur, the mcw was removed.
                                             */
                                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                                Tr.debug(this, tc, "Connection error occurred for this mcw " + mcWrapperTemp + ", mcw will not be reused");
                                            }

                                        }
                                        if (mcWrapper == null) {
                                            ManagedConnection mc = mcWrapperTemp.getManagedConnection();
                                            if (gConfigProps.sendClaimedVictomToGetConnection && mc instanceof WSManagedConnection) {
                                                ((WSManagedConnection) mc).setClaimedVictim();
                                                mcWrapper = mcWrapperTemp;
                                            } else {
                                                /*
                                                 * We are going to claim this connection as a victim. Log a debug
                                                 * message, cleanup and destroy the mcWrappers connection.
                                                 */
                                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                                    Tr.debug(this, tc, "Claiming victim " + mcWrapperTemp);
                                                }

                                                if (tc.isDebugEnabled()) {
                                                    ++numberOfClaimedVictims;
                                                    boolean subjectMismatch = false;
                                                    boolean criMismatch = false;
                                                    if (cri.equals(mcWrapperTemp.getCRI()) == false)
                                                        criMismatch = true;
                                                    Equals equalsHelper = new Equals();
                                                    equalsHelper.setSubjects(subject, mcWrapperTemp.getSubject());
                                                    if (!AccessController.doPrivileged(equalsHelper)) {
                                                        subjectMismatch = true;
                                                    }

                                                    if (criMismatch && subjectMismatch) {
                                                        ++numberOfClaimedVictims_CRI_Subject_Mismatch;
                                                    } else if (criMismatch) {
                                                        ++numberOfClaimedVictims_CRI_Only_Mismatch;
                                                    } else if (subjectMismatch) {
                                                        ++numberOfClaimedVictims_Subject_Only_Mismatch;
                                                    } else {
                                                        // matchManagedConnection only failed.
                                                        ++numberOfClaimedVictims_MM_Only_Mismatch;
                                                    }
                                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                                        Tr.debug(this, tc, "A Victim has been claimed for connection pool: " + gConfigProps.cfName);
                                                        Tr.debug(this, tc,
                                                                 "  Total Victim count                                    =  " + Integer.toString(numberOfClaimedVictims));
                                                        Tr.debug(this,
                                                                 tc,
                                                                 "    Victims due to both CRI and Subject Mismatch        =  "
                                                                     + Integer.toString(numberOfClaimedVictims_CRI_Subject_Mismatch));
                                                        Tr.debug(this,
                                                                 tc,
                                                                 "    Victims due to CRI mismatch only                    =  "
                                                                     + Integer.toString(numberOfClaimedVictims_CRI_Only_Mismatch));
                                                        Tr.debug(this,
                                                                 tc,
                                                                 "    Victims due to Subject mismatch only                =  "
                                                                     + Integer.toString(numberOfClaimedVictims_Subject_Only_Mismatch));
                                                        Tr.debug(this,
                                                                 tc,
                                                                 "    Victims due to failed matchManagedConnection() only =  "
                                                                     + Integer.toString(numberOfClaimedVictims_MM_Only_Mismatch));
                                                    }
                                                }

                                                pm.freePool[j].cleanupAndDestroyMCWrapper(
                                                                                          mcWrapperTemp);
                                                // cleanup, remove and then release mcw
                                                /*
                                                 * Remove the mcWrapper from the mcWrapperList.
                                                 */
                                                --pm.freePool[j].numberOfConnectionsAssignedToThisFreePool;
                                            }
                                        } // end if (mcWrapper == null)
                                        break;
                                    } // end if (pm.freePool[j][i].numberOfConnectionsInFreePool > 0)
                                } // end synchronized
                            } // end if (pm.freePool[j][i].numberOfConnectionsInFreePool > 0)
                        } // j loop
                        /*
                         * At this point we may or may not have a mcWrapper.
                         *
                         * 1. We didn't find any connections to claim or reauth (mcWrapperTemp = null).
                         * 2. We found a connection and we claimed it (mcWrapperTemp != null and mcWrapper = null).
                         * 3. We found a connection and we reauth is (mcWrapperTemp != null and mcWrapper != null).
                         * 4. We didn't find any connections to claim or reauth (mcWrapperTemp = null and mcWrapper = null), but if
                         * thread local connections is enabled, we will be able to do one more search.
                         *
                         * Therefore, if mcWrapperTemp is null (case 1), we know we have to queue this request.
                         * Otherwise we have a connection we can use or we will be creating a new connection (for cases 2 and 3 we
                         * will break out of the while(true)). For case 4, mcWrapperTemp is null and we will check for a tls matching connection.
                         */
                        if (mcWrapperTemp == null) {
                            if (pm.isThreadLocalConnectionEnabled && waitTimeout != 0) {
                                mcWrapper = pm.searchTLSForMatchingConnection(managedConnectionFactory, subject, cri);
                                if (mcWrapper != null)
                                    break;
                            }

                            if (tc.isDebugEnabled()) {
                                if (pm.waiterCount == 0)
                                    pm.waitersStartedTime = System.currentTimeMillis();
                                Tr.debug(this, tc, "Waiters: requests for connections are being queued. Start Time" + pm.waitersStartedTime);
                            }

                            // increment the number of requests waiting
                            pm.waiterCount++;

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(
                                         tc,
                                         "waitStartTime is zero.  waiterCount incremented to " + pm.waiterCount);
                            }

                            // get the wait start time
                            waitStartTime = System.currentTimeMillis();
                            // call to wait
                            queueRequest(managedConnectionFactory, waitTimeout);
                            // get the wait end time
                            waitEndTime = System.currentTimeMillis();
                        } else {
                            //We have a connections or we will be creating one.
                            break;
                        }
                    } // end if (waitStartTime == 0)

                    // NOTE:  When we get here, we either timed-out or got notified.

                    /*
                     * Look for a connection in the mcWrapperWaiterList
                     */
                    if (pm.mcWrapperWaiterList.size() > 0) {
                        /*
                         * Get a free connection from the mcWrapperWaiterList. If a connection in this list
                         * does not match, we will need to claim a victim in this list.
                         */
                        mcWrapper = pm.getFreeWaiterConnection(managedConnectionFactory, subject, cri);

                        if (mcWrapper == null) {
                            /*
                             * Claim a victim
                             */
                            MCWrapper mcWrapperTemp = (MCWrapper) pm.mcWrapperWaiterList.remove(0);
                            if (gConfigProps.sendClaimedVictomToGetConnection) {
                                ((WSManagedConnection) mcWrapperTemp.getManagedConnection()).setClaimedVictim();
                                mcWrapper = mcWrapperTemp;
                            } else {
                                if (tc.isDebugEnabled()) {
                                    ++numberOfClaimedVictims;
                                    boolean subjectMismatch = false;
                                    boolean criMismatch = false;
                                    if (cri != null) {
                                        if (cri.equals(mcWrapperTemp.getCRI()) == false)
                                            criMismatch = true;
                                    } else {
                                        if (mcWrapperTemp.getCRI() == null) {
                                            // both are null, they are equal.
                                        } else {
                                            criMismatch = true;
                                        }
                                    }
                                    Subject tempSubject = mcWrapperTemp.getSubject();
                                    if (subject != null && tempSubject != null) {
                                        Equals equalsHelper = new Equals();
                                        equalsHelper.setSubjects(subject, tempSubject);
                                        if (!AccessController.doPrivileged(equalsHelper)) {
                                            subjectMismatch = true;
                                        }
                                    } else {
                                        if (subject == null && tempSubject == null) {
                                            // both are null, they are equal
                                        } else {
                                            subjectMismatch = true;
                                        }
                                    }

                                    if (criMismatch && subjectMismatch) {
                                        ++numberOfClaimedVictims_CRI_Subject_Mismatch;
                                    } else if (criMismatch) {
                                        ++numberOfClaimedVictims_CRI_Only_Mismatch;
                                    } else if (subjectMismatch) {
                                        ++numberOfClaimedVictims_Subject_Only_Mismatch;
                                    } else {
                                        // matchManagedConnection only failed.
                                        ++numberOfClaimedVictims_MM_Only_Mismatch;
                                    }

                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(this, tc, pm.gatherClaimVictimStatisticalData());
                                    }
                                }

                                mcWrapperTemp.setPoolState(0);
                                cleanupAndDestroyMCWrapper(mcWrapperTemp);

                                pm.waiterCount--;

                                if (tc.isDebugEnabled()) {
                                    if (pm.waiterCount == 0) {
                                        pm.waitersEndedTime = System.currentTimeMillis();
                                        Tr.debug(this, tc, "Waiters: requests for connections are no longer being queued. End Time:" + pm.waitersEndedTime);
                                        Tr.debug(this, tc, "Waiters: total time waiter were in queue: " + (pm.waitersEndedTime - pm.waitersStartedTime));
                                    }
                                }

                                /*
                                 * Break and allow the code to create a new connection
                                 */
                                break;
                            }
                        } else {
                            //  we need to add code here.
                            if (((com.ibm.ejs.j2c.MCWrapper) mcWrapper).do_not_reuse_mcw) {
                                // We have already claimed a victim by having the resource adapter tell
                                // us to destroy one.
                                mcWrapper = null;
                                pm.waiterCount--;

                                if (tc.isDebugEnabled()) {
                                    if (pm.waiterCount == 0)
                                        pm.waitersEndedTime = System.currentTimeMillis();
                                    Tr.debug(this, tc, "Waiters: requests for connections are no longer being queued. End Time:" + pm.waitersEndedTime);
                                    Tr.debug(this, tc, "Waiters: total time waiter were in queue: " + (pm.waitersEndedTime - pm.waitersStartedTime));
                                }

                                /*
                                 * Break and allow the code to create a new connection
                                 */
                                break;
                            }
                        }
                    }
                    //else {
                    /*
                     * Instead of looking for a connection in the freePool we should try
                     * to create a connection. If we get into this leg of
                     * code, we are in an error case or the connection has
                     * been removed from the pool.
                     */
                    //mcWrapper = getFreeConnection(userData,oneUserIDOption,mcWrapperList,true);
                    //}
                    if (mcWrapper != null) {
                        // we found a connection
                        // decrement the waiter count, since we're no longer going to wait
                        pm.waiterCount--;

                        if (tc.isDebugEnabled()) {
                            if (pm.waiterCount == 0) {
                                pm.waitersEndedTime = System.currentTimeMillis();
                                Tr.debug(this, tc, "Waiters: requests for connections are no longer being queued. End Time:" + pm.waitersEndedTime);
                                Tr.debug(this, tc, "Waiters: total time waiter were in queue: " + (pm.waitersEndedTime - pm.waitersStartedTime));
                            }
                        }

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            long diff = waitEndTime - waitStartTime;
                            Tr.debug(
                                     tc,
                                     "Time waited was " + diff + " (milliseconds).  waiterCount decremented to " + pm.waiterCount);
                        }

                        // break out of the loop and continue processing
                        break;
                    }

                    /*
                     * Try to create a connection again.
                     * Check the totalConnectionCount to see if we can create a connection.
                     */
                    synchronized (pm.pmCounterLock) {
                        if (pm.totalConnectionCount.get() < pm.maxConnections) { // Can we create a connection?, changed maxConnections to use pm.maxConnections
                            /*
                             * We can create a connection.
                             */
                            pm.totalConnectionCount.incrementAndGet();
                            addingAConnection = true;
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "Total connection count is " + pm.totalConnectionCount.get());
                            }

                        }
                    }

                    /*
                     * If addingAConnection is true a little more work before creating the the connection
                     * and break out of the loop.
                     */
                    if (addingAConnection) {
                        // we are going to create a connection
                        // decrement the waiter count, since we're no longer going to wait
                        pm.waiterCount--;

                        if (tc.isDebugEnabled()) {
                            if (pm.waiterCount == 0) {
                                pm.waitersEndedTime = System.currentTimeMillis();
                                Tr.debug(this, tc, "Waiters: requests for connections are no longer being queued. End Time:" + pm.waitersEndedTime);
                                Tr.debug(this, tc, "Waiters: total time waiter were in queue: " + (pm.waitersEndedTime - pm.waitersStartedTime));
                            }
                        }

                        break;
                    }

                    // if there are waiters and we didn't find a connection and we're not going
                    // to create one two scenarios can get us here.
                    // a) we've waited, our timeout has expired and no connections are available
                    //      so we need to throw a connection waitTimeoutException
                    // b) This is the first waiter, in which case we need to wait and try again
                    // if there are not waiters, then we might have found a connection to return
                    // or found a spot to create one.  This is the third way into this branch.

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "We didn't find or are not going to create a connection.");
                    }
                    // NOTE: at this point, we know we didn't find a connection and we either
                    //  need to wait, or we have waited and timed out.

                    // since waitStartTime is not zero, we've waited already
                    if (waitStartTime != 0) {
                        /*
                         * Reload the waitTimeout incase it has changed.
                         */
                        long reloadedTime = pm.connectionTimeout * 1000;
                        if (orig_waitTimeout != reloadedTime) {
                            /*
                             * ConnectionTimeout has changed. Reset the time values
                             */
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "Changing connection timeout value from " + orig_waitTimeout +
                                                   " to " + reloadedTime + " the (orig_waitTimeout - reloeadedTime) is " +
                                                   (reloadedTime - orig_waitTimeout) + " waitTimeout + (orig_waitTimeout - reloeadedTime) is " +
                                                   (waitTimeout + (reloadedTime - orig_waitTimeout)));
                            }
                            waitTimeout = waitTimeout + (reloadedTime - orig_waitTimeout);
                            orig_waitTimeout = reloadedTime;
                        }
                        // fudge factor for time calculations
                        long kludge = 10L;
                        // find the amount of time waited
                        long timeWaited = waitEndTime - waitStartTime;
                        totalTimeWaited = totalTimeWaited + timeWaited;

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(
                                     tc,
                                     "waitStartTime is " + waitStartTime + " and timeWaited is " + timeWaited +
                                         " waitTimeout is " + waitTimeout + " total time waited is " + totalTimeWaited);
                        }

                        // handle both waiting slightly less than waitTimeout or waiting longer
                        //  than waitTimeout

                        if ((waitTimeout - timeWaited) <= kludge) {
                            // we timed out and didn't find a connection
                            // throw waitTimeoutException and decrement waiterCount

                            pm.waiterCount--;

                            if (tc.isDebugEnabled()) {
                                if (pm.waiterCount == 0) {
                                    pm.waitersEndedTime = System.currentTimeMillis();
                                    Tr.debug(this, tc, "Waiters: requests for connections are no longer being queued. End Time:" + pm.waitersEndedTime);
                                    Tr.debug(this, tc, "Waiters: total time waiter were in queue: " + (pm.waitersEndedTime - pm.waitersStartedTime));
                                }
                            }

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(
                                         tc,
                                         "Timeout.  Decremented waiterCount, which is now "
                                             + pm.waiterCount
                                             + " on datasource "
                                             + gConfigProps.cfName);
                            }
                            Object[] connWaitTimeoutparms = new Object[] { "createOrWaitForConnection", gConfigProps.cfName, totalTimeWaited, pm.waiterCount,
                                                                           pm.totalConnectionCount.get() };
                            Tr.error(
                                     tc,
                                     "POOL_MANAGER_EXCP_CCF2_0001_J2CA0045",
                                     connWaitTimeoutparms);
                            String connWaitTimeoutMessage = Tr.formatMessage(tc, "POOL_MANAGER_EXCP_CCF2_0001_J2CA0045", connWaitTimeoutparms);
                            ConnectionWaitTimeoutException cwte = new ConnectionWaitTimeoutException(connWaitTimeoutMessage);
                            /*
                             * The new ffdc prossException has a dependency on the DiagnosticModuleForJ2C and should not
                             * be changed without making the same changes in the Dia...J2C.
                             */
                            com.ibm.ws.ffdc.FFDCFilter.processException(cwte, J2CConstants.DMSID_MAX_CONNECTIONS_REACHED, "869", this.pm);
                            pm.activeRequest.decrementAndGet();
                            /*
                             * The following dumpComponentListWithHandles was moved from the ConnectionManager code to here.
                             * I remove the try/catch code in the ConnectionManager, but we still need the dump..Handles code for a
                             * ConnectionWaitTimeoutException.
                             */
                            // remove ConnectionHandleManager.getConnectionHandleManager().dumpComponentListWithHandles();
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                Tr.exit(this, tc, "createOrWaitForConnection", cwte);
                            throw cwte;
                        } // end if (... waitTimeout ...)
                        else {
                            // we should never have this case I think,
                            //  but if we waited, didn't timeout (meaning we were notified)
                            //  and didn't find a connection, then go back to sleep for whatever
                            //  time you have left.

                            // don't increment waiterCount since we've already done that once
                            waitTimeout = waitTimeout - (waitEndTime - waitStartTime);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "not finished waiting yet.  New waitTimeout is " + waitTimeout);
                            }

                            // get the wait start time
                            waitStartTime = System.currentTimeMillis();
                            // call to wait
                            queueRequest(managedConnectionFactory, waitTimeout);
                            // get the wait end time
                            waitEndTime = System.currentTimeMillis();
                        } // end else

                    } // end if (waitStartTime != 0)
                } // end while(true)
            } // end synchronized(freePool.freePoolLock)
        } // end else
        if (mcWrapper == null) {
            /*
             * No available connection in the free pool. Need to create a new one
             */
            mcWrapper = createManagedConnectionWithMCWrapper(managedConnectionFactory, subject, cri, connectionSharing, hashCode);
            if (mcWrapper == null) {
                Tr.error(
                         tc,
                         "FREEPOOL_GETFREECONNECTION_ERROR_J2CA1002",
                         new Object[] { mcWrapper, gConfigProps.cfName });
                ResourceAllocationException re = new ResourceAllocationException("getFreeConnection: MCWrapper is null.");
                com.ibm.ws.ffdc.FFDCFilter.processException(
                                                            re,
                                                            "com.ibm.ejs.j2c.poolmanager.FreePool.createOrWaitForConnection",
                                                            "674",
                                                            new Object[] { this, pm });
                pm.activeRequest.decrementAndGet();
                throw re;
            }
            mcWrapper.setHashMapBucket(hashMapBucket);
            mcWrapper.setMCWrapperList(mcWrapperList);
            ++numberOfConnectionsAssignedToThisFreePool;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Returning mcWrapper");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "createOrWaitForConnection", mcWrapper);
        }
        return mcWrapper;

    }

    /**
     *
     * Check for a matching managed connection
     *
     */
    /*
     * This method is already in a free pool sync block
     */
    private MCWrapper getMCWrapperFromMatch(
                                            Subject subject,
                                            ConnectionRequestInfo cri,
                                            ManagedConnectionFactory managedFactory,
                                            MCWrapper mcWrapperTemp) throws ResourceAllocationException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getMCWrapperFromMatch", mcWrapperTemp);
        }
        MCWrapper mcWrapper = null;
        ManagedConnection mc = null;
        /*
         * Create a new set for every matchManagedConnection. This currently is faster than having a object pool
         * of HashSet objects that would be require synchronization. In the future, we may need to use an object pool or
         * have a matchManagedConnection that support passing a managedConnection instead of a Set.
         */
        Set<ManagedConnection> freePoolMCSet = new HashSet<ManagedConnection>(1);
        try {
            freePoolMCSet.add(mcWrapperTemp.getManagedConnection());
        } catch (java.lang.IllegalStateException e) {
            /*
             * This was an addition to do to application threading issue,
             * where we had a problem with bad mcWrappers staying in the free pool.
             * Instead of leaving this bad mcWrapper in the free pool, we are going
             * to remove it and throw the same exception, This will reduce the chance
             * of bad mcWrappers staying in the pool resulting in an unrecoverable pool.
             */
            synchronized (freeConnectionLockObject) {
                mcWrapperList.remove(mcWrapperTemp);
            }
            throw e;
        }

        try {
            int poolState = mcWrapperTemp.getPoolState();
            mcWrapperTemp.setPoolState(50);
            mc = managedFactory.matchManagedConnections(freePoolMCSet, subject, cri);

            mcWrapperTemp.setPoolState(poolState);
        } catch (ResourceException exn1) {
            com.ibm.ws.ffdc.FFDCFilter.processException(exn1, "com.ibm.ejs.j2c.poolmanager.FreePool.getMCWrapperFromMatch", "786", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "datasource " + gConfigProps.cfName + ": ResourceException " + exn1);
                Tr.debug(this, tc, "Throwing ResourceAllocationException...");
                Tr.debug(this, tc, "match(), Pool contents ==> " + this);
            }
            ResourceAllocationException throwMe = new ResourceAllocationException(exn1.getMessage(), exn1.getErrorCode());
            throwMe.initCause(exn1.getCause());
            pm.activeRequest.decrementAndGet();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "getMCWrapperFromMatch", throwMe);
            throw (throwMe);
        }

        if (mc != null) {
            mcWrapper = mcWrapperTemp;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "getMCWrapperFromMatch", mcWrapper);
        }

        return mcWrapper;
    }

    /**
     * Return a mcWrapper. Create one if the
     * maximum connection limit has not been reached. If the maximum had been reached,
     * queue the request.
     *
     * @throws ResourceAllocationException
     */
    protected MCWrapper createManagedConnectionWithMCWrapper(
                                                             ManagedConnectionFactory managedConnectionFactory,
                                                             Subject subject,
                                                             ConnectionRequestInfo cri,
                                                             boolean connectionSharing, int hashCode) throws ResourceAllocationException { // This method is called within a synchronize block
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "createManagedConnectionWithMCWrapper");
        }

        ManagedConnection mc = null;
        MCWrapper mcWrapper = null;
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "raClassLoader", raClassLoader);

            JcaServiceUtilities jcasu = new JcaServiceUtilities();
            if (raClassLoader == null) {
                mc = managedConnectionFactory.createManagedConnection(subject, cri);
            } else {
                ClassLoader previousClassLoader = jcasu.beginContextClassLoader(raClassLoader);
                try {
                    mc = managedConnectionFactory.createManagedConnection(subject, cri);
                } finally {
                    jcasu.endContextClassLoader(raClassLoader, previousClassLoader);
                }
            }
            mcWrapper = new com.ibm.ejs.j2c.MCWrapper(pm, gConfigProps);
            mcWrapper.setManagedConnection(mc);

            /*
             * Many threads can hit the following code at the same time. What I a
             * expect to happen is that several threads will hit the synchronized
             * call until the checkForNoPoolingException is set to false.
             */
            if (pm.gConfigProps.checkManagedConnectionInstanceof) {
                /*
                 * Allow only one thread at a time through the following code
                 */
                /*
                 * Double-checked locking works for 32-bit primitive values. I am
                 * using a boolean primitive value.
                 */
                synchronized (pm.gConfigProps.checkManagedConnectionInstanceofLock) {
                    if (pm.gConfigProps.checkManagedConnectionInstanceof) {
                        /*
                         * Call matchManagedConnections
                         */
                        Set<ManagedConnection> s = new HashSet<ManagedConnection>();
                        s.add(mc);
                        try {
                            int poolState = mcWrapper.getPoolState();
                            mcWrapper.setPoolState(50);
                            managedConnectionFactory.matchManagedConnections(s, subject, cri);
                            mcWrapper.setPoolState(poolState);
                        } catch (NotSupportedException e) {
                            /*
                             * If we catch a the NotSupportedException, we need
                             * to turn off connection pooling.
                             */
                            gConfigProps.setConnectionPoolingEnabled(false);
                        }

                        if (mc instanceof DissociatableManagedConnection) {
                            /*
                             * set smart handle supported
                             */
                            pm.gConfigProps.setSmartHandleSupport(true);
                            pm.createParkedConnection = false;
                            pm.gConfigProps.setInstanceOfDissociatableManagedConnection(true);
                        }

                        if (mc instanceof DissociatableManagedConnection) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "Managed connection is an instance of DissociatableManagedConnection");
                            }
                            pm.gConfigProps.setInstanceOfDissociatableManagedConnection(true);
                        }

                        /*
                         * Deferred enlistment
                         */
                        if (mc instanceof LazyEnlistableManagedConnection) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "Managed connection is an instance of LazyEnlistableManagedConnection");
                            }
                            pm.gConfigProps.setDynamicEnlistmentSupported(true);
                        }

                        /*
                         * Create the writer for the managed connection.
                         */
                        pm.writer = new TraceWriter(mc.getClass().getName());
                        pm.traceWriter = (TraceWriter) pm.writer;
                        pm.printWriter = new PrintWriter(pm.writer);

                        /*
                         * We only want to execute this code once
                         * Set this value last in this synchronized block.
                         */
                        pm.gConfigProps.setCheckManagedConnectionInstanceof(false);

                    } // moved brace to include above call to setCheckManagedConnectionInstanceof
                } // end synchronize
            }

            if (tc.isDebugEnabled()) {
                ++freePoolCreateManagedConnection;
            }

            mcWrapper.setFatalErrorValue(fatalErrorNotificationTime + 1);
            mcWrapper.setSubjectCRIHashCode(hashCode);
            mcWrapper.setSubject(subject);
            ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).set_managedConnectionFactory(managedConnectionFactory);
            mcWrapper.setCRI(cri);
            mcWrapper.setSupportsReAuth(gConfigProps.raSupportsReauthentication);
            mcWrapper.setConnectionSynchronizationProvider(gConfigProps.connectionSynchronizationProvider);

            /*
             * Check to see if trace has been turned on for
             * the managed connection.
             */
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "pm = ", pm);
                Tr.debug(this, tc, "traceWriter = ", pm.traceWriter);
            }
            if (pm.traceWriter.isTraceEnabled()) {
                /*
                 * Set the log writer on mc
                 */
                mc.setLogWriter(pm.printWriter);
                mcWrapper.setLogWriterSet(true);
            }

            /*
             * Make sure conneciton pooling is enabled. The reaper is not needed for
             * non poolable resource adapters.
             *
             * First check to see if reaper time is set. We will not be starting the reaper thread unless
             * its set.
             *
             * Next, check to see if its already running.
             */
            if (gConfigProps.isConnectionPoolingEnabled()) {
                if ((pm.reapTime > 0)) {
                    if (pm.alarmThreadCounter.get() == 0) {
                        synchronized (pm.amLockObject) {
                            if (pm.alarmThreadCounter.get() == 0) {
                                /*
                                 * If totalConnectionCount is > minConnections and agedTimeout is not set, start
                                 * the reaper thread.
                                 *
                                 * If agedTimeout is set and totalConnectionCount is > 0 start the reaper
                                 * thread.
                                 */
                                if (pm.agedTimeout < 1) {
                                    if ((pm.totalConnectionCount.get() > pm.minConnections)) {
                                        if (pm.nonDeferredReaperAlarm) {
                                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                                Tr.debug(this, tc, "Creating non-deferrable alarm for reaper");
                                            }
                                            pm.alarmThreadCounter.incrementAndGet();
                                            try {
                                                pm.am = pm.connectorSvc.nonDeferrableSchedXSvcRef.getServiceWithException().schedule(pm, pm.reapTime, TimeUnit.SECONDS);
                                            } catch (Exception e) {
                                                pm.alarmThreadCounter.decrementAndGet();
                                                throw e;
                                            }
                                        } else {
                                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                                Tr.debug(this, tc, "Creating deferrable alarm for reaper");
                                            }
                                            pm.alarmThreadCounter.incrementAndGet();
                                            try {
                                                pm.am = pm.connectorSvc.deferrableSchedXSvcRef.getServiceWithException().schedule(pm, pm.reapTime, TimeUnit.SECONDS);
                                            } catch (Exception e) {
                                                pm.alarmThreadCounter.decrementAndGet();
                                                throw e;
                                            }
                                        }
                                    }
                                } else {
                                    if (pm.totalConnectionCount.get() > 0) {
                                        if (pm.nonDeferredReaperAlarm) {
                                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                                Tr.debug(this, tc, "Creating non-deferrable alarm for reaper");
                                            }
                                            pm.alarmThreadCounter.incrementAndGet();
                                            try {
                                                pm.am = pm.connectorSvc.nonDeferrableSchedXSvcRef.getServiceWithException().schedule(pm, pm.reapTime, TimeUnit.SECONDS);
                                            } catch (Exception e) {
                                                pm.alarmThreadCounter.decrementAndGet();
                                                throw e;
                                            }
                                        } else {
                                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                                Tr.debug(this, tc, "Creating deferrable alarm for reaper");
                                            }
                                            pm.alarmThreadCounter.incrementAndGet();
                                            try {
                                                pm.am = pm.connectorSvc.deferrableSchedXSvcRef.getServiceWithException().schedule(pm, pm.reapTime, TimeUnit.SECONDS);
                                            } catch (Exception e) {
                                                pm.alarmThreadCounter.decrementAndGet();
                                                throw e;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (ResourceException exn) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        exn,
                                                        "com.ibm.ejs.j2c.poolmanager.FreePool.createManagedConnectionWithMCWrapper",
                                                        "199",
                                                        this);
            // call exceptionList method to print stack traces of current and linked exceptions
            Object[] parms = new Object[] { "createManagedConnectionWithMCWrapper", CommonFunction.exceptionList(exn), "ResourceAllocationException", gConfigProps.cfName };
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "POOL_MANAGER_EXCP_CCF2_0002_J2CA0046", parms);
            }
            ResourceAllocationException throwMe = new ResourceAllocationException(exn.getMessage(), exn.getErrorCode());
            throwMe.initCause(exn.getCause());
            pm.activeRequest.decrementAndGet();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "createManagedConnectionWithMCWrapper", throwMe);
            throw (throwMe);

        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ejs.j2c.poolmanager.FreePool.createManagedConnectionWithMCWrapper",
                                                        "545",
                                                        this);
            // call exceptionList method to print stack traces of current and linked exceptions
            Object[] parms = new Object[] { "createManagedConnectionWithMCWrapper", CommonFunction.exceptionList(e), "ResourceAllocationException", gConfigProps.cfName };
            Tr.error(tc, "POOL_MANAGER_EXCP_CCF2_0002_J2CA0046", parms);
            ResourceAllocationException throwMe = new ResourceAllocationException(e.getMessage());
            throwMe.initCause(e);
            pm.activeRequest.decrementAndGet();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "createManagedConnectionWithMCWrapper", throwMe);
            throw (throwMe);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "createManagedConnectionWithMCWrapper", mcWrapper);
        }
        return mcWrapper;

    }

    /**
     * Return the number of free connections
     */

    protected int getNumberOfConnectionInFreePool() {
        return mcWrapperList.size();
    }

    /**
     * Remove the mcWrappers from the arraylist. Cleanup and destroy the
     * managed connections.
     */
    protected void removeCleanupAndDestroyAllFreeConnections() { // removed iterator code
        // This method is called within a synchronize block
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "removeCleanupAndDestroyAllFreeConnections");
        }
        int mcWrapperListIndex = mcWrapperList.size() - 1;
        for (int i = mcWrapperListIndex; i >= 0; --i) {
            MCWrapper mcw = (MCWrapper) mcWrapperList.remove(i);
            mcw.setPoolState(0);
            --numberOfConnectionsAssignedToThisFreePool;
            cleanupAndDestroyMCWrapper(mcw);
            pm.totalConnectionCount.decrementAndGet();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "removeCleanupAndDestroyAllFreeConnections");
        }
    }

    protected void nullPMref() {

        pm = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "FreePool nulled PM ref");
        }

    }

    /**
     * During server shutdown, try to cleanup and destroy the managed connections
     * nicely. This method should only be called during server server shutdown.
     */
    protected void cleanupAndDestroyAllFreeConnections() {
        // This method is called within a synchronize block
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "cleanupAndDestroyAllFreeConnections");
        }
        int mcWrapperListIndex = mcWrapperList.size() - 1;
        for (int i = mcWrapperListIndex; i >= 0; --i) {
            MCWrapper mcw = (MCWrapper) mcWrapperList.remove(i);
            mcw.setPoolState(0);
            pm.totalConnectionCount.decrementAndGet();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Calling cleanup and destroy on MCWrapper " + mcw);
            }
            cleanupAndDestroyMCWrapper(mcw);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "cleanupAndDestroyAllFreeConnections");
        }
    }

    /**
     *
     * Return the mcWrapperList size
     *
     * @return
     */
    protected int getSize() {
        return mcWrapperList.size();
    }

    /**
     *
     * Return the mcWrapperList
     *
     * @return
     */
    protected MCWrapperList getMCWrapperList() {
        return mcWrapperList;
    }

    /**
     * Print information about this object
     *
     * @return
     * @concurrency concurrent
     */

    @Override
    public String toString() {

        StringBuffer aBuffer = new StringBuffer();

        aBuffer.append("FreePool object:");
        aBuffer.append("  Number of connection in free pool: ");
        synchronized (freeConnectionLockObject) {
            aBuffer.append(this.getNumberOfConnectionInFreePool());
            aBuffer.append(nl);
            aBuffer.append(mcWrapperList);
        }

        return aBuffer.toString();

    }

    protected boolean removeMCWrapperFromList(MCWrapper mcw) {
        synchronized (freeConnectionLockObject) {
            boolean removed = mcWrapperList.remove(mcw);
            if (removed) {
                mcw.setPoolState(0);
            }
            return removed;
        }
    }

    /**
     * This method should only be called when a fatal error occurs or
     * when an attempt is made to remove all of the connections from
     * the pool.
     */
    protected void incrementFatalErrorValue(int value1) {
        /*
         * value1 and value2 are index values for the free pools.
         * When value1 and value2 are 0, we are in free pool .
         */
        if (fatalErrorNotificationTime == Integer.MAX_VALUE - 1) {
            /*
             * We need to start over at zero. All connection
             * fatal error values need to be reset to zero.
             */
            fatalErrorNotificationTime = 0;
            if (value1 == 0) {
                /*
                 * We only want to do this once. When the values value1 and value2 are 0
                 * are are processing the first free pool. When we are processing all of the
                 * rest of the free pool, this code will not be executed.
                 */
                pm.mcToMCWMapWrite.lock();
                try {
                    Collection<MCWrapper> mcWrappers = pm.mcToMCWMap.values();
                    Iterator<MCWrapper> mcWrapperIt = mcWrappers.iterator();
                    while (mcWrapperIt.hasNext()) {
                        MCWrapper mcw = mcWrapperIt.next();
                        if (!mcw.isParkedWrapper()) {
                            /*
                             * Reset the fatal error value to zero.
                             * This connection will be cleaned up and
                             * destroyed when returned to the free pool.
                             */
                            mcw.setFatalErrorValue(0);
                        }
                    }
                } finally {
                    pm.mcToMCWMapWrite.unlock();
                }
            }
        } else {
            ++fatalErrorNotificationTime;
        }
    }

    /**
     * @return
     */
    protected int getFatalErrorNotificationTime() {
        return fatalErrorNotificationTime;
    }

    /**
    *
    */
    protected void setFatalErrorNotificationTime(int value) {
        fatalErrorNotificationTime = value;
    }

    //Add these inner classes for doPrivileged calls to UserData.equals and
    //UserData.HashCode.  doPrivileged calls were removed from UserData object due to
    //security concerns.

    static class Equals implements PrivilegedAction<Boolean> {

        Subject _s1, _s2;

        public final void setSubjects(Subject s1, Subject s2) {
            _s1 = s1;
            _s2 = s2;
        }

        @Override
        public Boolean run() {
            boolean subjectsMatch = false;

            if (checkCredentials(_s1.getPrivateCredentials(), _s2.getPrivateCredentials())) {
                // if the private credentials match check public creds.
                subjectsMatch = checkCredentials(_s1.getPublicCredentials(), _s2.getPublicCredentials());
            }
            return subjectsMatch;
        }

        /**
         * This method is replacing checkPrivateCredentials and checkPublicCredentials. The code in both methods
         * contained the same logic.
         *
         * This method needs to be called two times. The first time with private credentials and the second time
         * with public credentials. Both calls must return true for the Subjects to be equal.
         *
         * This new method fixes apar The implementation of Set.equals(Set) is synchronized for the
         * Subject object. This can not be synchronized for the J2C and RRA code implementations. We may be
         * able to code this differently, but I believe this implementation performs well and allows for trace
         * messages during subject processing. This method assumes the Subject's private and public credentials
         * are not changing during the life of a managed connection and managed connection wrapper.
         *
         * @param s1Credentials
         * @param s2Credentials
         * @return
         */
        private boolean checkCredentials(Set<Object> s1Credentials, Set<Object> s2Credentials) {
            boolean rVal = false;

            if (s1Credentials != s2Credentials) {
                if (s1Credentials != null) {
                    if (s2Credentials != null) {
                        /*
                         * Check to see if the sizes are equal. If the first one and second one are
                         * equal, then check one of them to see if they are empty.
                         * If both are empty, they are equal, If one is empty and the other is not,
                         * they are not equal.
                         */
                        int it1size = s1Credentials.size();
                        int it2size = s2Credentials.size();
                        if (it1size == it2size) {
                            if (it1size == 0) {
                                if (TraceComponent.isAnyTracingEnabled()
                                    && tc.isDebugEnabled())
                                    Tr.debug(this, tc, "Processing credential sets, both are empty, They are equal");
                                return true;
                            }
                        } else {
                            if (TraceComponent.isAnyTracingEnabled()
                                && tc.isDebugEnabled())
                                Tr.debug(
                                         tc,
                                         "Processing credential sets, sets do not contain the same number of elements. They are not equal");
                            return false;
                        }

                        if (it1size > 1) {
                            /*
                             * This is the slow path. In most cases, we should not use this code path.
                             * We should have no objects or one object for each set.
                             *
                             * This is an unsynchronized unordered equals of two Sets.
                             */
                            Iterator<Object> it1 = s1Credentials.iterator();
                            int objectsEqual = 0;
                            while (it1.hasNext()) {
                                Object s1Cred = it1.next();
                                Iterator<Object> it2 = s2Credentials.iterator();
                                while (it2.hasNext()) {
                                    Object s2Cred = it2.next();
                                    if (s1Cred != null) {
                                        if (!s1Cred.equals(s2Cred)) {
                                            // Objects are not equal
                                            continue;
                                        }
                                    } else {
                                        if (s2Cred != null) {
                                            // Objects are not equal, one object is null");
                                            continue;
                                        }
                                    }
                                    ++objectsEqual;
                                    break;
                                }
                            }
                            // if(it2.hasNext()){
                            // if (TraceComponent.isAnyTracingEnabled() &&
                            // tc.isDebugEnabled()) Tr.debug(this, tc, " - Object sets do not
                            // contain the same number of elements, they are not equal");
                            // } else {
                            // have same number of private credentials, they are =
                            if (objectsEqual == it1size) {
                                // add trace at this point.
                                rVal = true;
                            }
                            // }
                        } else { // optimized path since we only have one object in both
                                 // sets to compare.
                            Iterator<Object> it1 = s1Credentials.iterator();
                            Iterator<Object> it2 = s2Credentials.iterator();

                            Object s1Cred = it1.next();
                            Object s2Cred = it2.next();
                            if (s1Cred != null) {
                                if (!s1Cred.equals(s2Cred)) {
                                    if (TraceComponent.isAnyTracingEnabled()
                                        && tc.isDebugEnabled())
                                        Tr.debug(this, tc, "PK69110 - Objects are not equal");
                                    return false;
                                }
                            } else {
                                if (s2Cred != null) {
                                    if (TraceComponent.isAnyTracingEnabled()
                                        && tc.isDebugEnabled())
                                        Tr.debug(this, tc,
                                                 "PK69110 - Objects are not equal, one objest is null");
                                    return false;
                                }
                            }
                            rVal = true;
                        }
                    } // second check for null
                } // first check for null
            } else {
                rVal = true;
            }

            return rVal;
        }
    }

    //The Below two methods are added to support PMI data for connection pools.This can be avoid if we expose com.ibm.ejs.jca,but currently as per JIM
    //it should not be done as j2c code is partial implementation only for JDBC and JMS.In future when j2c code is fully implemented its better to
    //remove the interface JCAPMIHelper and implemented methods and update ConnectionPoolMonitor.java to use the exposed j2c code.
    @Override
    public String getUniqueId() {
        return this.gConfigProps.getXpathId();
    }

    @Override
    public boolean getParkedValue() {
        return false; //Not used any where..Just dummy method.
    }

    @Override
    public String getJNDIName() {
        return this.gConfigProps.getJNDIName();
    }
    //PMIHelper methods end here
}
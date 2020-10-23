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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.PrintWriter;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.resource.ResourceException;
import javax.resource.spi.ApplicationServerInternalException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAllocationException;
import javax.resource.spi.ValidatingManagedConnectionFactory;
import javax.security.auth.Subject;

import com.ibm.ejs.cm.logger.TraceWriter;
import com.ibm.ejs.ras.RasHelper;
import com.ibm.websphere.ce.j2c.ConnectionWaitTimeoutException;
import com.ibm.websphere.jca.pmi.JCAPMIHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.j2c.MCWrapper;
import com.ibm.ws.jca.adapter.PurgePolicy;
import com.ibm.ws.jca.adapter.WSManagedConnection;
import com.ibm.ws.jca.adapter.WSManagedConnectionFactory;
import com.ibm.ws.jca.cm.AbstractConnectionFactoryService;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.util.WSThreadLocal;

public final class PoolManager implements Runnable, PropertyChangeListener, VetoableChangeListener, JCAPMIHelper {

    private static final TraceComponent tc = Tr.register(PoolManager.class,
                                                         J2CConstants.traceSpec,
                                                         J2CConstants.messageFile);

    protected static final String alertResourceBundleName = "com.ibm.ws.j2c.resources.J2CAMessages";

    final String nl = CommonFunction.nl;

    final ConnectorServiceImpl connectorSvc;
    protected J2CGlobalConfigProperties gConfigProps;

    protected boolean reaperThreadStarted = false;
    private final Integer taskTimerLockObject = new Integer(0);
    protected int waitSkip = 0;
    protected boolean displayInfiniteWaitMessage;

    protected final ConcurrentHashMap<MCWrapper, ArrayList<MCWrapper>> tlsArrayLists = new ConcurrentHashMap<MCWrapper, ArrayList<MCWrapper>>();

    private SharedPool[] sharedPool;
    protected FreePool[] freePool;
    protected final MCWrapperList mcWrapperWaiterList;
    protected final HashMap<ManagedConnection, MCWrapper> mcToMCWMap;

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock mcToMCWMapRead = rwl.readLock();
    final Lock mcToMCWMapWrite = rwl.writeLock();

    private ClassLoader raClassLoader;

    protected final Integer pmCounterLock = new Integer(0);
    protected final Integer destroyMCWrapperListLock = new Integer(0);
    protected final AtomicInteger totalConnectionCount = new AtomicInteger(0);
    protected final Integer poolManagerBalancePoolLock = new Integer(0);
    protected final Integer waiterFreePoolLock = new Integer(0);
    protected int waiterCount = 0;
    protected boolean allowConnectionRequests = true;
    private boolean connectionPoolShutDown = false;
    protected final Integer poolManagerTestConnectionLock = new Integer(0);

    private int collectorCount = 0;
    private final ArrayList<MCWrapper> mcWrappersToDestroy = new ArrayList<MCWrapper>(50);

    private final static boolean mcWrapperDoesExistInFreePool = true;
    private final static boolean synchronizedAllReady = false;
    private final static boolean dontNotifyWaiter = true;
    private final static boolean dontDecrementTotalCounter = false;

    protected boolean unusedTimeoutEnabled = false;
    protected int connectionTimeout = 0;
    protected int maxConnections = 0;
    protected int minConnections = 0;
    protected PurgePolicy purgePolicy = null;
    protected int reapTime = 0;
    private int unusedTimeout = 0;
    protected int agedTimeout = 0;
    protected long agedTimeoutMillis = 0;
    protected int holdTimeLimit = 10;
    protected int maxSharedBuckets = 0;
    protected int maxFreePoolHashSize = 0;
    protected int maxNumberOfMCsAllowableInThread = 0;
    protected boolean throwExceptionOnMCThreadCheck = false;

    protected long waitersStartedTime;
    protected long waitersEndedTime;

    protected MCWrapper parkedMCWrapper = null;
    protected boolean createParkedConnection = false;
    protected final Integer parkedConnectionLockObject = new Integer(0);
    protected int totalPoolConnectionRequests = 0;
    protected ScheduledFuture<?> am = null;
    protected final Integer amLockObject = new Integer(0);
    private boolean pmQuiesced = false;
    protected final AtomicInteger activeRequest = new AtomicInteger(0);
    protected final Integer updateToPoolInProgressLockObject = new Integer(0);
    protected boolean updateToPoolInProgress = false;
    protected int updateToPoolInProgressSleepTime = 250;
    protected final AtomicInteger activeTLSRequest = new AtomicInteger(0);
    protected final Integer updateToTLSPoolInProgressLockObject = new Integer(0);
    protected final AtomicBoolean updateToTLSPoolInProgress = new AtomicBoolean(false);
    protected int updateToTLSPoolInProgressSleepTime = 250;
    protected Writer writer = null;
    protected TraceWriter traceWriter = null;
    protected PrintWriter printWriter = null;
    protected Vector<ThreadSupportedCleanupAndDestroy> tscdList = new Vector<ThreadSupportedCleanupAndDestroy>();
    protected boolean logSerialReuseMessage = true;
    private boolean _quiesce;
    private Date _quiesceTime;
    protected boolean nonDeferredReaperAlarm = false;

    double claimedVictimPercent = 0;

    private boolean enableInuseConnectionDestroy = false;

    private WSThreadLocal<ArrayList<MCWrapper>> localConnection_ = null;
    protected int maxCapacity = 0;
    protected boolean isThreadLocalConnectionEnabled = false;

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public transient ConcurrentHashMap<String, AtomicInteger> getConnectionMap = new ConcurrentHashMap<String, AtomicInteger>();

    protected final AtomicInteger alarmThreadCounter = new AtomicInteger(0);

    public PoolManager(
                       AbstractConnectionFactoryService cfSvc,
                       Properties dsProps,
                       J2CGlobalConfigProperties gConfigProps,
                       ClassLoader raClassLoader) {

        super();

        this.raClassLoader = raClassLoader;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "<init>");
        }

        this.connectorSvc = (ConnectorServiceImpl) cfSvc.getConnectorService();
        this.gConfigProps = gConfigProps;

        enableInuseConnectionDestroy = true;

        logSerialReuseMessage = true;

        this.connectionTimeout = gConfigProps.getConnectionTimeout();
        this.maxConnections = gConfigProps.getMaxConnections();
        this.minConnections = gConfigProps.getMinConnections();
        this.purgePolicy = gConfigProps.getPurgePolicy();
        this.reapTime = gConfigProps.getReapTime();
        this.unusedTimeout = gConfigProps.getUnusedTimeout();
        this.agedTimeout = gConfigProps.getAgedTimeout();
        this.agedTimeoutMillis = (long) this.agedTimeout * 1000;
        this.maxFreePoolHashSize = gConfigProps.getMaxFreePoolHashSize();
        this.maxSharedBuckets = gConfigProps.getMaxSharedBuckets();

        this.maxNumberOfMCsAllowableInThread = gConfigProps.getMaxNumberOfMCsAllowableInThread();
        this.throwExceptionOnMCThreadCheck = gConfigProps.getThrowExceptionOnMCThreadCheck();

        this.holdTimeLimit = gConfigProps.getOrphanConnHoldTimeLimitSeconds();

        this.maxCapacity = gConfigProps.getnumConnectionsPerThreadLocal();

        this.connectionTimeout = gConfigProps.getConnectionTimeout();

        if (this.maxCapacity < 1) {
            this.localConnection_ = null;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "PoolManager: Thread local connection Disabled");
                Tr.debug(this, tc, "Maximum connection thread local storage capacity is " + this.maxCapacity);
            }
        } else {
            this.localConnection_ = new WSThreadLocal<ArrayList<MCWrapper>>() {
                @Override
                protected ArrayList<MCWrapper> initialValue() {
                    return new ArrayList<MCWrapper>(2);
                }
            };
            isThreadLocalConnectionEnabled = true;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "PoolManager: Thread local connection ENABLED");
                Tr.debug(this, tc, "Maximum connection thread local storage capacity is " + this.maxCapacity);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {

            StringBuffer sb = new StringBuffer();
            sb.append(nl);
            sb.append("maxSharedBuckets = ");
            sb.append(maxSharedBuckets);
            sb.append(nl);
            sb.append("maxFreePoolHashSize = ");
            sb.append(maxFreePoolHashSize);
            sb.append(nl);
            sb.append("holdTimeLimit = ");
            sb.append(holdTimeLimit);
            sb.append(nl);
            Tr.debug(this, tc, sb.toString());
        }

        int initialMCWLSize = maxConnections;

        sharedPool = new SharedPool[maxSharedBuckets];
        freePool = new FreePool[maxFreePoolHashSize];
        if (maxConnections > J2CConstants.INITIAL_SIZE) {
            mcToMCWMap = new HashMap<ManagedConnection, MCWrapper>(J2CConstants.INITIAL_SIZE);
        } else {
            mcToMCWMap = new HashMap<ManagedConnection, MCWrapper>(maxConnections);
        }

        for (int i = 0; i < maxSharedBuckets; ++i) {
            sharedPool[i] = new SharedPool(maxConnections, this);
        }
        for (int j = 0; j < maxFreePoolHashSize; ++j) {
            freePool[j] = new FreePool(initialMCWLSize, this, this.gConfigProps, raClassLoader);
        }

        if (initialMCWLSize > J2CConstants.INITIAL_SIZE) {
            mcWrapperWaiterList = new MCWrapperList(J2CConstants.INITIAL_SIZE);
        } else if (initialMCWLSize > 0) {
            mcWrapperWaiterList = new MCWrapperList(initialMCWLSize);
        } else {
            mcWrapperWaiterList = new MCWrapperList(50);
        }

        _quiesce = false;
        _quiesceTime = null;

        gConfigProps.addPropertyChangeListener(this);
        gConfigProps.addVetoableChangeListener(this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Initial pool properties used by PoolManager " + gConfigProps.getXpathId());
            Tr.debug(this, tc, "Connection Timeout                      = " + connectionTimeout + " (seconds)");
            Tr.debug(this, tc, "Maximum Connections                     = " + maxConnections);
            Tr.debug(this, tc, "Minimum Connections                     = " + minConnections);
            Tr.debug(this, tc, "Purge Policy                            = " + purgePolicy);
            Tr.debug(this, tc, "Reclaim Connection Thread Time Interval = " + reapTime + " (seconds)");
            Tr.debug(this, tc, "Unused Timeout                          = " + unusedTimeout + " (seconds)");
            Tr.debug(this, tc, "Aged Timeout                            = " + agedTimeout + " (seconds)");
            Tr.debug(this, tc, "Free Pool Distribution Table Size       = " + maxFreePoolHashSize);
            Tr.debug(this, tc, "Number Of Shared Pool Partitions        = " + maxSharedBuckets);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "<init>");
        }

    }

    /**
     * This method fatalErrorNotification marks a ManagedConnection stale in the
     * pool manager's connection pools.
     *
     * @param Managed connection wrapper
     * @param object  affinity
     *
     * @concurrency concurrent
     */
    public void fatalErrorNotification(ManagedConnectionFactory managedConnectionFactory, MCWrapper mcWrapper, Object affinity) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "fatalErrorNotification");
        }

        requestingAccessToPool();
        if (mcWrapper != null) {
            mcWrapper.markStale();
        }
        if (gConfigProps.connectionPoolingEnabled) {
            if (gConfigProps.getPurgePolicy() != null) {
                // New with jdbc 4.1 support, if the connection was aborted, skip the entire pool connection purge.
                boolean aborted = mcWrapper != null && mcWrapper.getManagedConnectionWithoutStateCheck() instanceof WSManagedConnection
                                  && ((WSManagedConnection) mcWrapper.getManagedConnectionWithoutStateCheck()).isAborted();
                if (gConfigProps.getPurgePolicy() == PurgePolicy.EntirePool && !aborted) {
                    // The remove parked connection code was delete here
                    // Reset fatalErrorNotificationTime and remove all free connections
                    ArrayList<MCWrapper> destroyMCWrapperList = new ArrayList<MCWrapper>();
                    synchronized (destroyMCWrapperListLock) {
                        for (int j = 0; j < gConfigProps.getMaxFreePoolHashSize(); ++j) {
                            //  ffdc uses this method and was locking freePool without
                            // locking waiter pool first, causing a deadlock.
                            synchronized (waiterFreePoolLock) {
                                synchronized (freePool[j].freeConnectionLockObject) {
                                    /*
                                     * If a connection gets away, by setting fatalErrorNotificationTime we will
                                     * guarantee when the connection is returned to the free pool, it will be destroyed
                                     */
                                    freePool[j].incrementFatalErrorValue(j);
                                    /*
                                     * Move as many connections as we can in the free pool to the destroy list
                                     */
                                    if (freePool[j].mcWrapperList.size() > 0) {
                                        // freePool[j].removeCleanupAndDestroyAllFreeConnections();
                                        int mcWrapperListIndex = freePool[j].mcWrapperList.size() - 1;
                                        for (int k = mcWrapperListIndex; k >= 0; --k) {
                                            MCWrapper mcw = (MCWrapper) freePool[j].mcWrapperList.remove(k);
                                            mcw.setPoolState(0);
                                            destroyMCWrapperList.add(mcw);
                                            --freePool[j].numberOfConnectionsAssignedToThisFreePool;
                                        }

                                    } // end if

                                } // end free lock
                            } // end waiter lock

                        } // end for j

                    } // end syc for destroyMCWrapperListLock
                    /*
                     * we need to cleanup and destroy connections in the local
                     * destroy list.
                     */
                    for (int i = 0; i < destroyMCWrapperList.size(); ++i) {
                        MCWrapper mcw = destroyMCWrapperList.get(i);
                        freePool[0].cleanupAndDestroyMCWrapper(mcw);
                        this.totalConnectionCount.decrementAndGet();
                    }
                } // end "EntirePool" purge policy
                else {
                    /*
                     * We only need to check for the validating mcf support if purge
                     * policy is not "EntirePool" If the purge policy is "EntierPool" all
                     * of the connection will be destroyed. We will have no connection to
                     * validate.
                     *
                     * If ValidatingMCFSupported == true this code will attempt to cleanup
                     * and destroy the connections returned by the method
                     * getInvalidConnections().
                     *
                     * If the connection is active, the connections will be marked stale.
                     *
                     * New with jdbc 4.1 support, if the connection was aborted, skip the validate connections call.
                     */
                    if (gConfigProps.validatingMCFSupported
                        && !aborted) {
                        validateConnections(managedConnectionFactory, false);
                    }
                    /*
                     * Mark all connections to be pretested for
                     * purge policy = ValidateAllConnections. If pretest fails a new
                     * connection will be created. Moved to here and corrected to avoid
                     * marking empty wrappers --
                     *
                     */
                    if (gConfigProps.getPurgePolicy() == PurgePolicy.ValidateAllConnections) {
                        mcToMCWMapWrite.lock();
                        try {
                            Collection<MCWrapper> s = mcToMCWMap.values();
                            Iterator<MCWrapper> i = s.iterator();
                            while (i.hasNext()) {
                                com.ibm.ejs.j2c.MCWrapper mcw = (com.ibm.ejs.j2c.MCWrapper) i.next();
                                if (mcw.getState() != com.ibm.ejs.j2c.MCWrapper.STATE_INACTIVE) {
                                    mcw.setPretestThisConnection(true);
                                }
                            }
                        } finally {
                            mcToMCWMapWrite.unlock();
                        }
                    }
                } // end of PurgePolicy NOT EntirePool
            } // end of PurgePolicy not null
        } // end of connection pooling enabled
        else {
            /*
             *
             * Connection pooling in the free pool is disabled. No other proccessing
             * in this method is needed.
             */
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Pooling disabled, fatal error processing completed.");
            }
        }
        activeRequest.decrementAndGet();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "fatalErrorNotification");
        }
    }

    /*
     * When prepopulateEnabled is true, this pool is not being
     * used and we need to reset the idle times. When this pool is
     * being used, it should not reset the idle times enabling the reaper
     * to work as designed.
     */
    protected void validateConnections(ManagedConnectionFactory managedConnectionFactory, boolean prepopulateEnabled) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "validateConnections", gConfigProps.cfName);
        }

        // Get all of the managed connections wrappers.
        Set<ManagedConnection> mcSet = new HashSet<ManagedConnection>();
        ArrayList<MCWrapper> invalidMCWList = new ArrayList<MCWrapper>();
        // We do not allow other destroy processing to occur at the same time, so get the destroyMCWrapperListLock
        synchronized (destroyMCWrapperListLock) {
            for (int j = 0; j < gConfigProps.getMaxFreePoolHashSize(); ++j) {
                // Since we do not know if we are removing connections until after getInvalidConnections
                // is called for each thread, we need to get the waiter pool lock first
                // before the freeConnectionLockObject lock, otherwise we may cause a deadlock.
                synchronized (waiterFreePoolLock) {
                    synchronized (freePool[j].freeConnectionLockObject) {
                        // Move as many invalid connections as we can from the free pool to the invalid list
                        if (freePool[j].mcWrapperList.size() > 0) {
                            int mcWrapperListIndex = freePool[j].mcWrapperList.size() - 1;
                            for (int k = mcWrapperListIndex; k >= 0; --k) {
                                MCWrapper mcw = (MCWrapper) freePool[j].mcWrapperList.get(k);
                                if (mcw.getPoolState() == 1 && !mcw.isStale()) {
                                    // Only add valid managed connections to this set.
                                    mcSet.add(mcw.getManagedConnection());
                                    mcw.setPoolState(50); // set the state to 50, which basically means we are interacting with the resource adapter
                                    Set<?> set = null;
                                    try {
                                        set = ((ValidatingManagedConnectionFactory) managedConnectionFactory).getInvalidConnections(mcSet);
                                    } catch (ResourceException e) {
                                        Object[] parms = new Object[] { "validateConnections", CommonFunction.exceptionList(e), "ResourceException", gConfigProps.cfName };
                                        Tr.error(tc, "ATTEMPT_TO_VALIDATE_MC_CONNECTIONS_J2CA0285", parms);
                                    } finally {
                                        // we do a check above to ensure we are in the free pool (1), therefore we shouldn't have to worry
                                        // about capturing the current state and setting it back here - it is expected to be in the free pool.
                                        mcSet.clear();
                                        mcw.setPoolState(MCWrapper.ConnectionState_freePool);
                                    }

                                    if (set != null && !set.isEmpty()) {
                                        freePool[j].mcWrapperList.remove(k);
                                        mcw.setPoolState(0);
                                        invalidMCWList.add(mcw);
                                        --freePool[j].numberOfConnectionsAssignedToThisFreePool;
                                    }

                                    if (prepopulateEnabled) {
                                        // When prepopulate is enabled, we need to reset
                                        // the idle time out to keep the good connections
                                        // in the pool.  All of the bad connections will be
                                        // destroyed before we leave this method.  The idle
                                        // time will not matter for them.  :-)
                                        ((com.ibm.ejs.j2c.MCWrapper) mcw).resetIdleTimeOut();
                                    }

                                } // end if (mcw.getPoolState() == 1 && !mcw.isStale())
                            } // end k
                        } // end if (freePool[j].mcWrapperList.size() > 0)
                    } // end sync for freeConnectionLockObject
                } // end sync for waiterFreePoolLock
            } // end j
        } // end sync for destroyMCWrapperListLock

        // We need to cleanup and destroy connections in the invalidMCWList
        // which have already been removed from the free pools
        for (MCWrapper mcw : invalidMCWList) {
            freePool[0].cleanupAndDestroyMCWrapper(mcw);
            synchronized (waiterFreePoolLock) {
                this.totalConnectionCount.decrementAndGet();
                if (waiterCount > 0) {
                    waiterFreePoolLock.notify();
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Returning from getInvalidConnections with " + mcSet.size()
                               + " managed connections and " + invalidMCWList.size() + " invalid connections.");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "validateConnections");
        }
    }

    /**
     * Check for a connection in the free pool. If a connection
     * is found, cleanup and destroy the connection and remove the mcwrapper
     * from the free pool.
     *
     * @param mcw
     */
    private void checkForConnectionInFreePool(MCWrapper mcw) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "checkForConnectionInFreePool");
        }
        int hashMapBucket = mcw.getHashMapBucket();

        /*
         * Attempt to remove the mcw from the free pool
         */
        if (freePool[hashMapBucket].removeMCWrapperFromList(mcw)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Managed connection wrapper was removed from the free pool wrapper list.  mcw is " + mcw);
            }
            /*
             * The connection was in the free pool, cleaup and destroy the mc.
             */
            freePool[hashMapBucket].cleanupAndDestroyMCWrapper(mcw);
            /*
             * Finish the work needed in the free pool for this mcw.
             *
             * Parms on the removeMCWrapperFromList are:
             * MCWrapper object = mcw
             * removeFromFreePool = false (we alread removed the connection)
             * synchronizationNeeded = true (we are not sync'ed, so the method will need to sync)
             * skipWaiterNotify = false (if there are waiters, we should wake-up one of them)
             * decrementTotalCounter = true (if we are going to wake-up a waiter, we need to allow
             * the waiter to create a connection if total is at its max)
             */
            freePool[hashMapBucket].removeMCWrapperFromList(mcw, false, true, false, true);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "checkForConnectionInFreePool");
        }
    }

    /**
     * This method during server shut down will cleanup
     * and destroy connection nicely.
     *
     * @throws ResourceException
     * @concurrency concurrent
     */
    public void serverShutDown() throws ResourceException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "serverShutDown");
        }

        connectionPoolShutDown = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Shutting down pool manager connections in free pool ");
            Tr.debug(this, tc, this.toString());
        }

        // Remove parked connection if it exists
        if ((gConfigProps.isSmartHandleSupport() == false)) {
            freePool[0].removeParkedConnection();
        }
        // Reset fatalErrorNotificationTime and remove all free connections
        for (int j = 0; j < gConfigProps.getMaxFreePoolHashSize(); ++j) {
            synchronized (freePool[j].freeConnectionLockObject) {
                /*
                 * If a connection gets away, by setting fatalErrorNotificationTime will
                 * guaranty when the connection is returned to the free pool, it will be
                 */
                freePool[j].incrementFatalErrorValue(j);
                /*
                 * Destroy as many connections as we can in the free pool
                 */
                if (freePool[j].mcWrapperList.size() > 0) {
                    try { // added a try\catch block
                        freePool[j].cleanupAndDestroyAllFreeConnections();
                    } catch (Exception ex) {
                        if (tc.isDebugEnabled())
                            Tr.debug(this, tc, "Exception during destroy of freepool connection: ", new Object[] { freePool[j], ex });
                    }
                }
            } // end free lock
        } //end for j

        /*
         * we can not process inuse connection by default during server shutdown. This
         * was a change in behavior from 6.0.2 and earlier. By trying to destroy
         * connection that could be
         * used by an application on a different thread, we can encounter unnecessary
         * delays or exceptions that will create pmrs.
         *
         * If this code is needed by some customers, they will be able to enable destroying inuse
         * connection by using a system value enableInuseConnectionDestroy = true. This is not
         * recommenced, since this should not be needed. All applications and services should be
         * stopped before we get this code. If a connection is still inuse by the time we
         * enter this server shutdown code, its very likely the connection is in error and will
         * need to manually be recovered or the service was not stopped first. It should always be
         * understood that manual recovery of XA resource may be needed if connection failure
         * occurs or a process is stopped unexpectedly
         *
         * This may not make sence, but please do not add any inuse connection code unless it can be
         * enabled. In future release, we may what to add this system value to a gui connection pool
         * property. This is the way we should have added this. :-)
         *
         * One other note, if a connection starts to move from the freepool to the inuse pool while
         * we are destroying managed connections, some interesting things may occur. :-) :-). It is
         * like we are going to cause the problem fixed.
         */
        if (enableInuseConnectionDestroy) {

            /*
             * Adding the allow connection request = false in an attempt to stop new connection
             * requests from occurring. The error will be misleading, but we will have the
             * stack of the application or service attempting to use a new connections during
             * server shutdown. It should be ok to reuse this var since this should not happen
             * very much.
             */
            allowConnectionRequests = false;

            // Managed connections currently in use should be destroyed in order to give the
            // resource adapter a chance to end the transaction branch. Check for in-use connections
            // in both the sharable and unsharable pools.

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "Destroying sharable, in-use connections");

            for (int i = 0; i < maxSharedBuckets; ++i) {
                synchronized (sharedPool[i].sharedLockObject) {
                    if (sharedPool[i].getMCWrapperListSize() > 0) {
                        MCWrapper[] mcw = sharedPool[i].getMCWrapperList();

                        for (int j = 0; j < sharedPool[i].getMCWrapperListSize(); ++j) {
                            try {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(this, tc, "Destroying inuse managed connection " + mcw[j]);
                                }
                                ManagedConnection mc = mcw[j].getManagedConnectionWithoutStateCheck();
                                if (mc != null)
                                    mc.destroy();
                            } catch (ResourceException resX) {
                                // No FFDC needed; we expect some resource adapters will throw errors here.
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    Tr.debug(this, tc, "error during destroy of sharable, in-use connection: ", new Object[] { mcw[j], resX });
                            }
                        } // end loop through the list of connections in the bucket
                    }
                } // end synchronize on shared pool bucket
            } // end loop through shared pool buckets

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "Destroying unsharable, in-use connections");

            MCWrapper[] mcw = getUnSharedPoolConnections();

            for (int j = 0; j < mcw.length; ++j) {
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Destroying inuse managed connection " + mcw[j]);
                    }
                    ManagedConnection mc = mcw[j].getManagedConnectionWithoutStateCheck();
                    if (mc != null)
                        mc.destroy();
                } catch (ResourceException resX) {
                    // No FFDC needed; we expect some resource adapters will throw errors here.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "error during destroy of unsharable in-use connection: ", new Object[] { mcw[j], resX });
                }
            } // end loop through unsharable, in-use connections
        }

        // Ensure that the reap thread has terminated for this pool manager
        // If we don't force closure here, dynamic config changes may cause
        // duplicate reaper threads for the same resource
        synchronized (amLockObject) {
            if (am != null && !am.isDone()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Alarm for pool manager has not completed yet.  Cancelling now.");
                am.cancel(false);
                if (am.isDone())
                    alarmThreadCounter.decrementAndGet();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "serverShutDown");
        }

    }

    /**
     * Quiesce connections
     *
     * @pre quiesceIfPossible has been called.
     *
     * @throws ResourceException
     * @concurrency concurrent
     */
    private void quiesce() throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "quiesce", gConfigProps.cfName);
        }

        // Remove parked connection if it exists
        if ((gConfigProps.isSmartHandleSupport() == false)) {
            freePool[0].removeParkedConnection();
        }

        if (parkedMCWrapper != null) {
            parkedMCWrapper.clearMCWrapper();
        }

        pmQuiesced = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "quiesce", gConfigProps.cfName);
        }

    }

    /**
     * This method returns the parked connction.
     *
     * @return parkedMCWrapper
     */
    public MCWrapper getParkedConnection() {
        /*
         * The parked connection is keep in only one of the free
         * pool buckets.
         */
        return parkedMCWrapper;
    }

    /**
     * This method releases ManagedConnection. If the affinity is used and the connection is associated
     * with the affinity, it is registered as unused. Otherwise it is prepared
     * for reuse (using <code>cleanup</code> method and then registered as unused.
     *
     * @param managed  ManagedConnection A connection to release
     * @param affinity Object, an affinity, can be represented using <code>Identifier</code> interface.
     *
     * @concurrency concurrent
     * @throws ResourceException
     * @throws ApplicationServerInternalException
     */
    public void release(MCWrapper mcWrapper, Object affinity) throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "release", mcWrapper, affinity,
                     "Pool contents ==>", toString2(1));
        }
        if (((com.ibm.ejs.j2c.MCWrapper) mcWrapper).isAlreadyBeingReleased()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "AddingJMS - release - Already releasing managed connection " + mcWrapper);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(this, tc, "release", new Object[] { mcWrapper.getManagedConnectionWithoutStateCheck(), "Pool contents ==>", this });
            }
            return;
        }
        ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).setAlreadyBeingReleased(true);
        /*
         * Added for holding out connection request while
         * a free or shared pool is being updated
         */
        requestingAccessToPool();

        // start  thread local fast path...
        if (localConnection_ != null) {
            if (mcWrapper.getPoolState() == MCWrapper.ConnectionState_sharedTLSPool || mcWrapper.getPoolState() == MCWrapper.ConnectionState_unsharedTLSPool) {
                if (mcWrapper.isDestroyState() || mcWrapper.isStale() || mcWrapper.hasFatalErrorNotificationOccurred(freePool[0].getFatalErrorNotificationTime())
                    || ((this.agedTimeout != -1)
                        && (mcWrapper.hasAgedTimedOut(this.agedTimeoutMillis)))) {
                    removeMCWFromTLS(mcWrapper);
                    return;

                } else {
                    try {
                        if (waiterCount > 0) {
                            /*
                             * If we have waiters, its likely the max connections and tls settings are not correct.
                             * When we have waiters, we need to try and remove one connection from this thread local
                             * and send the mcw to the waiter queue. By sending the mcw to the waiter queue, this connection may be assigned
                             * to a different thread local.
                             */
                            synchronized (waiterFreePoolLock) {
                                if ((waiterCount > 0) && (waiterCount > mcWrapperWaiterList.size())) {
                                    // there are requests waiting
                                    ArrayList<MCWrapper> mh = localConnection_.get();
                                    if (mh != null) {
                                        requestingAccessToTLSPool();
                                        // remove a mcw from this thread local.
                                        mh.remove(mcWrapper);
                                        tlsArrayLists.remove(mcWrapper);
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                            ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).setThreadID(((com.ibm.ejs.j2c.MCWrapper) mcWrapper).getThreadID() + "-release-waiter-removed");
                                            Tr.debug(this, tc, "removed mcWrapper from thread local " + mcWrapper);
                                        }
                                        endingAccessToTLSPool();
                                    }
                                    ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).tlsCleanup();
                                    mcWrapper.setSharedPoolCoordinator(null);
                                    mcWrapperWaiterList.add(mcWrapper);
                                    mcWrapper.setPoolState(MCWrapper.ConnectionState_waiterPool);
                                    // notify a waiter.
                                    waiterFreePoolLock.notify();
                                    activeRequest.decrementAndGet();
                                    ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).setAlreadyBeingReleased(false);
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                        Tr.exit(this, tc, "release");
                                    return;
                                }
                            } // end synchronized (waiterFreePoolLock)
                        }
                        mcWrapper.setSharedPoolCoordinator(null);
                        ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).tlsCleanup();
                        /*
                         * Our goal is for this mcw to stay on this thread, but if needed, after
                         * switching the state to freeTLSPool, this mcwrapper can be removed from this
                         * threads thread local storage and added to a different thread local storage or be placed
                         * in the main pool of connections. This should be the only place in the code that we set the
                         * pool state to MCWrapper.ConnectionState_freeTLSPool.
                         */
                        mcWrapper.setPoolState(MCWrapper.ConnectionState_freeTLSPool);
                        activeRequest.decrementAndGet();
                        ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).setAlreadyBeingReleased(false);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.exit(this, tc, "release", new Object[] { mcWrapper.getManagedConnectionWithoutStateCheck(), "Pool contents ==>", this });
                        }
                        return;
                    } catch (ResourceException re) {
                        removeMCWFromTLS(mcWrapper);
                        return;
                    }
                }
            }
            if (mcWrapper.getPoolState() == MCWrapper.ConnectionState_freeTLSPool) {
                // Already released, do nothing.
                ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).setAlreadyBeingReleased(false);
                activeRequest.decrementAndGet();
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(this, tc, "release", new Object[] { mcWrapper.getManagedConnectionWithoutStateCheck(), "Pool contents ==>", this });
                }
                return;
            }
        }
        //  end  thread local fast path...

        if (mcWrapper.isInSharedPool()) {
            ((SharedPool) mcWrapper.getSharedPool()).removeSharedConnection(mcWrapper);
            mcWrapper.setSharedPoolCoordinator(null);
            mcWrapper.setSharedPool(null);
            mcWrapper.setInSharedPool(false);
        }

        // Check to see if the pool is disabled.
        if (!gConfigProps.connectionPoolingEnabled || (mcWrapper instanceof com.ibm.ejs.j2c.MCWrapper && ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).isMCAborted())) {
            // The pool is disabled, cleanup and destroy the connection.
            // No pooling of connection is allowed

            // remove the mcw from the mcToMCWMap
            mcToMCWMapWrite.lock();
            try {
                mcToMCWMap.remove(mcWrapper.getManagedConnectionWithoutStateCheck());
            } finally {
                mcToMCWMapWrite.unlock();
            }

            try {
                mcWrapper.cleanup();
            } catch (Exception exn1) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "MCWrapper cleanup failed, datasource: " + gConfigProps.cfName);
                boolean aborted = mcWrapper.getManagedConnection() instanceof WSManagedConnection
                                  && ((WSManagedConnection) mcWrapper.getManagedConnection()).isAborted();
                if (!aborted) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(exn1, "com.ibm.ejs.j2c.poolmanager.PoolManager.release", "1131", this);
                }

            }
            try {
                mcWrapper.destroy();
            } catch (Exception exn2) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "MCWrapper destroy failed, datasource: " + gConfigProps.cfName);
                com.ibm.ws.ffdc.FFDCFilter.processException(exn2, "com.ibm.ejs.j2c.poolmanager.PoolManager.release", "1144", this);
            }
            synchronized (waiterFreePoolLock) {
                this.totalConnectionCount.decrementAndGet();
                if (waiterCount > 0)
                    waiterFreePoolLock.notify();
            }
        } else {
            // Move the pooled connection from the inuse state to the FreePool
            if (!mcWrapper.isInSharedPool())
                mcWrapper.setPoolState(0);

            int hashMapBucket = mcWrapper.getHashMapBucket();
            freePool[hashMapBucket].returnToFreePool(mcWrapper);
        }

        activeRequest.decrementAndGet();

        if (_quiesce) {
            /*
             * QuiesceIfPossible should be called for the first time in ConnectionFactoryDetailsImpl.stop()
             * at which point _quiesce will be set to true. At that time if there are no connections in the pool
             * the pool will be quiesced, otherwise we will try again each time a connection is returned to the
             * pool (here).
             */
            quiesceIfPossible();
        }

        ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).setAlreadyBeingReleased(false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "released managed connection " + mcWrapper.getManagedConnectionWithoutStateCheck(),
                     "pool contents ==>", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "release");
        }
    }

    /**
     * @param mcWrapper
     */
    private void removeMCWFromTLS(MCWrapper mcWrapper) {
        // Need to remove it from TLS and decrease total connection count.
        ArrayList<MCWrapper> mh = localConnection_.get();
        if (mh != null) {
            requestingAccessToTLSPool();
            // remove the bad connection primary connection being returned.
            mh.remove(mcWrapper);
            tlsArrayLists.remove(mcWrapper);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).setThreadID(((com.ibm.ejs.j2c.MCWrapper) mcWrapper).getThreadID() + "-release-destroy-removed");
                Tr.debug(this, tc, "removed mcWrapper from thread local " + mcWrapper);
            }
            endingAccessToTLSPool();
            removeConnectionFromPool(mcWrapper);
        }
        activeRequest.decrementAndGet();
        ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).setAlreadyBeingReleased(false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "release", new Object[] { mcWrapper.getManagedConnectionWithoutStateCheck(), "Pool contents ==>", this });
        }
    }

    private void removeConnectionFromPool(MCWrapper mcWrapper) {
        freePool[0].cleanupAndDestroyMCWrapper(mcWrapper); //cleanup, remove, then release mcWrapper
        // Do not return this mcWrapper back to the free pool.
        synchronized (waiterFreePoolLock) {
            mcWrapper.setPoolState(MCWrapper.ConnectionState_noPool);
            this.totalConnectionCount.decrementAndGet();
            waiterFreePoolLock.notify();

        }

    }

    /**
     * This method reserves connection. If unused connection exists, it is returned,
     * otherwise new connection is created using ManagedConnectionFactory.
     *
     * @param Subject               connection security context
     * @param ConnectionRequestInfo requestInfo
     * @param Object                affinity
     * @param boolean               connectionSharing
     * @param boolean               enforceSerialReuse
     * @param int                   commitPriority
     *
     * @return MCWrapper
     * @concurrency concurrent
     * @throws ResourceException
     * @throws ResourceAllocationException
     */
    public MCWrapper reserve(
                             ManagedConnectionFactory managedConnectionFactory,
                             Subject subject,
                             ConnectionRequestInfo requestInfo,
                             Object affinity,
                             boolean connectionSharing,
                             boolean enforceSerialReuse,
                             int commitPriority,
                             int branchCoupling) throws javax.resource.ResourceException, ResourceAllocationException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        //boolean normalSharing = true;
        // This is to be a parm past in when j2c code is ready
        //  If this is false we don't check the for an existing shared
        //  Connection, we hand out a new shared connection.

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.entry(this, tc, "reserve");

        if (isTracingEnabled && tc.isDebugEnabled()) {

            StringBuffer sbuff = new StringBuffer(250);
            sbuff.append("input parms... ");
            sbuff.append(nl);
            sbuff.append(" subject = ");
            if (subject == null) {
                sbuff.append("null");
            } else {
                // synchronized(this) {
                SubjectToString subjectToString = new SubjectToString();
                subjectToString.setSubject(subject);
                sbuff.append(AccessController.doPrivileged(subjectToString));
                // }
            }
            sbuff.append(" affinity = ");
            sbuff.append(affinity);
            sbuff.append(nl);
            sbuff.append(" Shared connection = ");
            sbuff.append(connectionSharing);
            sbuff.append(nl);
            sbuff.append(" Force new MC = ");
            sbuff.append(enforceSerialReuse);
            sbuff.append(nl);
            sbuff.append(" commitPriority = ");
            sbuff.append(commitPriority);
            sbuff.append(nl);
            sbuff.append(" branchCoupling = ");
            sbuff.append(branchCoupling);
            sbuff.append(nl);
            sbuff.append(" Connection Request Information = ");
            sbuff.append(requestInfo);
            Tr.debug(this, tc, sbuff.toString());
            Tr.debug(this, tc, "reserve(), Pool contents ==> " + this.toString2(1));

        }

        /*
         * Added for holding out connection request while
         * a free or shared pool is being updated
         */
        requestingAccessToPool();

        // Count the number of managed connection on this thread.
        //  If we find matching thread,
        //  add to a counter and check if we exceeded the max number
        //  allowed set by customer using custom properties maxNumberOfMCsAllowableInThread.  Throw
        //  an exception if we exceed the number.  :-)
        if (maxNumberOfMCsAllowableInThread == 0) {
            // don't check the number of connections when using a lightweight server
            // and maxNumberOfMCsAllowableInThread is 0 (in the lighweight server 0 means no limit)
        } else if (maxNumberOfMCsAllowableInThread > 0) {
            // if the custom property is set, lets use it.
            checkForMCsOnThread(maxNumberOfMCsAllowableInThread);
        }

        if ((isTracingEnabled && tc.isDebugEnabled())) {
            ++totalPoolConnectionRequests;
        }

        com.ibm.ws.j2c.MCWrapper mcWrapper = null;

        int sharedbucket = 0;

        /*
         * Check affinity and connectionSharing. We need to check for a shared connection
         * first. If a matching one exists, reuse it.
         */
        if (affinity != null && connectionSharing) {
            // Start -  - thread local fast path...
            if (localConnection_ != null) {
                ArrayList<MCWrapper> mh = localConnection_.get();
                if (mh != null) {
                    requestingAccessToTLSPool();
                    int arraySize = mh.size();
                    if (arraySize > 0) {
                        MCWrapper freeLocalConnection = null;
                        if (arraySize == 1) {
                            // fast path, we only have one.
                            MCWrapper localConnection = mh.get(0);
                            if (localConnection.getPoolState() == MCWrapper.ConnectionState_freeTLSPool) {
                                // We have a free connection, save it and use it if we do not have a matching affinity.
                                freeLocalConnection = getMCWrapperFromMatch(subject, requestInfo, managedConnectionFactory, localConnection); // Need to add code for handling resource exception.
                            } else if (localConnection.getPoolState() == MCWrapper.ConnectionState_sharedTLSPool) {
                                if (localConnection.getSharedPoolCoordinator() != null &&
                                    localConnection.getSharedPoolCoordinator().equals(affinity) &&
                                    isBranchCouplingCompatible(commitPriority, branchCoupling, localConnection)) {
                                    /*
                                     * No call to matchManagedConnection is occurring to check for matching connection.
                                     * When this feature is enabled, its assumed user of the feature know what they are doing.
                                     *
                                     * I am tempted to add the matching code here, but, not sure of the performance hit.
                                     */
                                    //MCWrapper mcwtemp = getMCWrapperFromMatch(subject, requestInfo, managedConnectionFactory, localConnection); // Need to add code for handling resource exception.
                                    if (isCRIsMatching(requestInfo, localConnection) &&
                                        isSubjectsMatching(subject, localConnection)) { // we have a matching connection.

                                        if (enforceSerialReuse && (localConnection.getHandleCount() >= 1)) {
                                            logLTCSerialReuseInfo(affinity, gConfigProps.cfName,
                                                                  localConnection, this);
                                        } // end enforceSerialReuse && (mcWrapperTemp.getHandleCount() >= 1)
                                        else {
                                            endingAccessToTLSPool();
                                            activeRequest.decrementAndGet();
                                            if (isTracingEnabled && tc.isEntryEnabled())
                                                Tr.exit(this, tc, "reserve", new Object[] { localConnection, localConnection.getManagedConnection() });
                                            return localConnection;
                                        }
                                    }
                                }
                            }

                        } else {
                            for (int i = 0; i < arraySize; ++i) {
                                MCWrapper localConnection = mh.get(i);
                                // check for affinity.  check pool state to see if its active.
                                // if the checks are matching, then life is good.
                                if (localConnection.getPoolState() == MCWrapper.ConnectionState_freeTLSPool) {
                                    // We have a free connection, save it and use it if we do not have a matching affinity.
                                    if (freeLocalConnection == null) {
                                        freeLocalConnection = getMCWrapperFromMatch(subject, requestInfo, managedConnectionFactory, localConnection); // Need to add code for handling resource exception.
                                    }
                                } else if (localConnection.getPoolState() == MCWrapper.ConnectionState_sharedTLSPool) {
                                    if (localConnection.getSharedPoolCoordinator() != null &&
                                        localConnection.getSharedPoolCoordinator().equals(affinity) &&
                                        isBranchCouplingCompatible(commitPriority, branchCoupling, localConnection)) {
                                        /*
                                         * No call to matchManagedConnection is occurring to check for matching connection.
                                         * When this feature is enabled, its assumed user of the feature know what they are doing.
                                         *
                                         * I am tempted to add the matching code here, but, not sure of the performance hit.
                                         */
                                        //MCWrapper mcwtemp = getMCWrapperFromMatch(subject, requestInfo, managedConnectionFactory, localConnection); // Need to add code for handling resource exception.
                                        if (isCRIsMatching(requestInfo, localConnection) &&
                                            isSubjectsMatching(subject, localConnection)) { // we have a matching connection.

                                            if (enforceSerialReuse && (localConnection.getHandleCount() >= 1)) {
                                                logLTCSerialReuseInfo(affinity, gConfigProps.cfName,
                                                                      localConnection, this);
                                            } // end enforceSerialReuse && (mcWrapperTemp.getHandleCount() >= 1)
                                            else {
                                                endingAccessToTLSPool();
                                                activeRequest.decrementAndGet();
                                                if (isTracingEnabled && tc.isEntryEnabled())
                                                    Tr.exit(this, tc, "reserve", new Object[] { localConnection, localConnection.getManagedConnection() });
                                                return localConnection;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (freeLocalConnection != null) {
                            freeLocalConnection.setPoolState(MCWrapper.ConnectionState_sharedTLSPool);
                            freeLocalConnection.setSharedPoolCoordinator(affinity);
                            freeLocalConnection.markInUse();
                            endingAccessToTLSPool();
                            activeRequest.decrementAndGet();
                            if (isTracingEnabled && tc.isEntryEnabled())
                                Tr.exit(this, tc, "reserve", new Object[] { freeLocalConnection, freeLocalConnection.getManagedConnection() });
                            return freeLocalConnection;
                        }
                        //}
                    }
                    endingAccessToTLSPool();
                }
            }
            // End -  - thread local fast path...
            /*
             * Looking in the shared pool for an existing connection with an affinity
             */
            sharedbucket = Math.abs(affinity.hashCode() % maxSharedBuckets); // Calculate the buck values for the shared bucket

            if (isTracingEnabled && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Searching for shared connection in partition " + sharedbucket);
            }

            mcWrapper = sharedPool[sharedbucket].getSharedConnection(affinity,
                                                                     subject,
                                                                     requestInfo,
                                                                     enforceSerialReuse,
                                                                     gConfigProps.getXpathId(),
                                                                     commitPriority,
                                                                     branchCoupling);
            // Start -  - thread local fast path...
        } else {
            // The connection is not shared.  This is a once only use thread pool.  We will look for free
            // TLS connections, if we find one and move it to inuse, it can not be used again until
            // returned to the TLS
            if (localConnection_ != null) {
                ArrayList<MCWrapper> mh = localConnection_.get();
                if (mh != null) {
                    requestingAccessToTLSPool();
                    int arraySize = mh.size();
                    if (arraySize > 0) {
                        for (int i = 0; i < arraySize; ++i) {
                            MCWrapper localConnection = mh.get(i);
                            // check for affinity.  check pool state to see if its active.
                            // if the checks are matching, then life is good.
                            if (localConnection.getPoolState() == MCWrapper.ConnectionState_freeTLSPool) {
                                // We have a free connection, save it and use it if we do not have a matching affinity.
                                MCWrapper mcwtemp = getMCWrapperFromMatch(subject, requestInfo, managedConnectionFactory, localConnection); // Need to add code for handling resource exception.
                                if (mcwtemp != null) { // we have a matching connection.
                                    localConnection.setPoolState(MCWrapper.ConnectionState_unsharedTLSPool);
                                    localConnection.markInUse();
                                    endingAccessToTLSPool();
                                    activeRequest.decrementAndGet();
                                    if (isTracingEnabled && tc.isEntryEnabled())
                                        Tr.exit(this, tc, "reserve", new Object[] { localConnection, localConnection.getManagedConnection() });
                                    return localConnection;
                                }

                            }
                        }
                    }
                    endingAccessToTLSPool();
                }
            }
            // End -  - thread local fast path...
        }

        /*
         * If mcWrapper is null, we did not find a shared connection. We need to look for an existing
         * connection in the free pool.
         */
        if (mcWrapper == null) {

            /*
             * Test connection code added
             */
            if (!allowConnectionRequests) {

                /*
                 * Need to throw an exception. It is OK to do an un-synchronized read of
                 * the allowConnectionRequests.
                 */
                ResourceAllocationException throwMe = null;
                if (connectionPoolShutDown) {
                    Object[] parms = new Object[] { "reserve",
                                                    "Pool requests blocked, connection pool is being shut down.",
                                                    "ResourceAllocationException", gConfigProps.cfName };
                    Tr.error(tc, "POOL_MANAGER_EXCP_CCF2_0002_J2CA0046", parms);
                    throwMe = new ResourceAllocationException("Pool requests blocked for " + gConfigProps.getXpathId()
                                                              + ", connection pool is being shut down.");
                } else {
                    Object[] parms = new Object[] { "reserve",
                                                    "Failed preTestConnection. Pool requests blocked until the test connection thread is successful.",
                                                    "ResourceAllocationException", gConfigProps.cfName };
                    Tr.error(tc, "POOL_MANAGER_EXCP_CCF2_0002_J2CA0046", parms);
                    throwMe = new ResourceAllocationException("Failed preTestConnection. Pool requests blocked for " + gConfigProps.getXpathId()
                                                              + " until the test connection thread is successful.");
                }

                activeRequest.decrementAndGet();

                if (isTracingEnabled && tc.isEntryEnabled()) {
                    Tr.exit(this, tc, "reserve", throwMe);
                }
                throw (throwMe);

            }

            int hashCode = computeHashCode(subject, requestInfo);
            int hashMapBucket = hashCode % maxFreePoolHashSize;
            /*
             * If we have waiters, we don't have any free connection, move to the
             * create or wait code. If we don't have any waiters, we need to look for
             * a free connection.
             *
             * If connectionPooling is disabled, there is no reason to check the free
             * pool, go directly to create or wait code.
             */
            if (waiterCount < 1 && gConfigProps.connectionPoolingEnabled) {
                /*
                 * Looking in the free pool for an existing free connection. If one
                 * exist and it matches the Subject and CRI information, we will use it.
                 */

                if (freePool[hashMapBucket].mcWrapperList.size() > 0) {
                    mcWrapper = freePool[hashMapBucket].getFreeConnection(managedConnectionFactory, subject, requestInfo, hashCode);
                } else {
                    if (isTracingEnabled && tc.isDebugEnabled()) {
                        ++freePool[hashMapBucket].fop_get_notfound;
                    }
                }
                /*
                 * If mcWrapper is null, we need to check the free pool for non-matching
                 * connection. If we find a free connection that does not match and the
                 * free pool has reached its maxConnections, we need to remove the free
                 * connection and create a new connection for this request.
                 */
                if (mcWrapper == null) {
                    /*
                     * If one user id option is used there will be no victim to claim or
                     * if maxConnections is zero, move on to the create or wait code
                     */
                    if (maxConnections != 0) {
                        boolean tryToClaimVictim = false;
                        if (totalConnectionCount.get() >= maxConnections) {
                            // remove synchronized block and second totalConnectionCount check
                            /*
                             * We only need to try and claim a victim if we have reached
                             * our connection max.
                             */
                            tryToClaimVictim = true;
                        }
                        if (tryToClaimVictim) {
                            // We need to look for a victim
                            /*
                             * searchHashMapBucket is the first hashMapBucket being
                             * searched. We only need to search through the hash map bucket
                             * since there is only one free pool bucket.
                             */
                            int searchHashMapBucket = hashMapBucket;
                            int tempLocalHashMapBucket = hashMapBucket;
                            for (int i = 0; i < maxFreePoolHashSize; ++i) {
                                /*
                                 *
                                 * This is not double-checked locking. The following code dirty
                                 * reads the size of the mcWrapperList that may be change at
                                 * any time by another thread. If it is greater than zero, we
                                 * need to synchronize and check the value again. If it is
                                 * still greater than zero, we have one or more mcWrappers to
                                 * work with. In order to work with the mcWrapperList, we need
                                 * to be synchronized.
                                 */
                                if (freePool[searchHashMapBucket].mcWrapperList.size() > 0) {
                                    try {

                                        synchronized (freePool[searchHashMapBucket].freeConnectionLockObject) {

                                            if (freePool[searchHashMapBucket].mcWrapperList.size() > 0) {

                                                /*
                                                 * claimVictim will return a true if in has claimed a
                                                 * victim, Since we have locked this free pool, we
                                                 * know a victim will be claimed.
                                                 */
                                                mcWrapper = claimVictim(managedConnectionFactory, searchHashMapBucket, subject,
                                                                        requestInfo);
                                            }
                                        } // end sync, only need the sync for claimVictim.
                                        if (mcWrapper == null) {
                                            mcWrapper = freePool[hashMapBucket].createOrWaitForConnection(managedConnectionFactory,
                                                                                                          subject,
                                                                                                          requestInfo,
                                                                                                          hashMapBucket, maxFreePoolHashSize, true,
                                                                                                          connectionSharing,
                                                                                                          hashCode);
                                        }
                                        break;

                                        //}

                                        //} // end sync - moved sync up, since we do not need it for the createorwaitforConnections and some resource adapters have not been returning from createManagedConnection

                                    } catch (ConnectionWaitTimeoutException e) {
                                        /*
                                         * All we need to do is throw the
                                         * ConnectionWaitTimeoutException exception.
                                         */
                                        throw e;
                                    } catch (ResourceAllocationException e) {
                                        /*
                                         * Start of new code for defect The following
                                         * notify code was moved from the create or wait code due
                                         * to the free pool lock required at the time.
                                         *
                                         * We need to reduce the totalConnectionCount and notify a
                                         * waiter if one exists. This will allow the wait a chance
                                         * at creating a connection to return to the requester.
                                         */
                                        if (e.getCause() instanceof InterruptedException) {
                                            if (isTracingEnabled && tc.isDebugEnabled()) {
                                                Tr.debug(tc, "Thread was interrupted, skipping decrement of total connection count");
                                            }
                                            synchronized (waiterFreePoolLock) {
                                                if (waiterCount > 0) {
                                                    waiterFreePoolLock.notify();
                                                }
                                            }
                                        } else {
                                            synchronized (waiterFreePoolLock) {
                                                int totalCount = this.totalConnectionCount.decrementAndGet();
                                                if (isTracingEnabled && tc.isDebugEnabled()) {
                                                    Tr.debug(tc, "Decrement of total connection count " + totalCount);
                                                }
                                                if (waiterCount > 0) {
                                                    waiterFreePoolLock.notify();
                                                }

                                            }
                                        }
                                        throw e;
                                    }

                                }

                                /*
                                 * move to the next hash bucket to be searched
                                 */
                                searchHashMapBucket = (++tempLocalHashMapBucket) % maxFreePoolHashSize;

                            }

                        }

                    }
                    /*
                     * If mcWrapper is null, will create a new connection or wait for a
                     * connection to become available.
                     */
                    if (mcWrapper == null) {

                        try {
                            mcWrapper = freePool[hashMapBucket].createOrWaitForConnection(managedConnectionFactory,
                                                                                          subject,
                                                                                          requestInfo,
                                                                                          hashMapBucket, maxFreePoolHashSize,
                                                                                          false, connectionSharing,
                                                                                          hashCode);
                        } catch (ConnectionWaitTimeoutException e) {
                            /*
                             * All we need to do is throw the ConnectionWaitTimeoutException
                             * exception.
                             */
                            throw e;
                        } catch (ResourceAllocationException e) {
                            /*
                             * Start of new code for defect The following notify code
                             * was moved from the create or wait code due to the free pool
                             * lock required at the time.
                             *
                             * We need to reduce the totalConnectionCount and notify a waiter
                             * if one exists. This will allow the wait a chance at creating a
                             * connection to return to the requester.
                             */
                            if (e.getCause() instanceof InterruptedException) {
                                if (isTracingEnabled && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Thread was interrupted, skipping decrement of total connection count");
                                }
                                synchronized (waiterFreePoolLock) {
                                    if (waiterCount > 0) {
                                        waiterFreePoolLock.notify();
                                    }
                                }
                            } else {
                                synchronized (waiterFreePoolLock) {
                                    int totalCount = this.totalConnectionCount.decrementAndGet();
                                    if (isTracingEnabled && tc.isDebugEnabled()) {
                                        Tr.debug(tc, "Decrement of total connection count " + totalCount);
                                    }
                                    if (waiterCount > 0) {
                                        waiterFreePoolLock.notify();
                                    }

                                }
                            }
                            throw e;
                        }
                    }
                }
            } // end if waiterCount > 0
            else {
                if ((isTracingEnabled && tc.isDebugEnabled())) {
                    ++waitSkip;
                }

                /*
                 * if waiterCount is one or greater there are no connection available,
                 * Call the createOrWaitForConnection code
                 */

                try {

                    mcWrapper = freePool[hashMapBucket].createOrWaitForConnection(managedConnectionFactory,
                                                                                  subject,
                                                                                  requestInfo,
                                                                                  hashMapBucket, maxFreePoolHashSize, false,
                                                                                  connectionSharing,
                                                                                  hashCode);

                } catch (ConnectionWaitTimeoutException e) {
                    /*
                     * All we need to do is throw the ConnectionWaitTimeoutException
                     * exception.
                     */
                    throw e;
                } catch (ResourceAllocationException e) {
                    /*
                     * Start of new code for defect The following notify code was
                     * moved from the create or wait code due to the free pool lock
                     * required at the time.
                     *
                     * We need to reduce the totalConnectionCount and notify a waiter if
                     * one exists. This will allow the wait a chance at creating a
                     * connection to return to the requester.
                     */
                    if (e.getCause() instanceof InterruptedException) {
                        if (isTracingEnabled && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Thread was interrupted, skipping decrement of total connection count");
                        }
                        synchronized (waiterFreePoolLock) {
                            if (waiterCount > 0) {
                                waiterFreePoolLock.notify();
                            }
                        }
                    } else {
                        synchronized (waiterFreePoolLock) {
                            int totalCount = this.totalConnectionCount.decrementAndGet();
                            if (isTracingEnabled && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Decrement of total connection count " + totalCount);
                            }
                            if (waiterCount > 0) {
                                waiterFreePoolLock.notify();
                            }

                        }
                    }
                    throw e;
                }

            }

            // if the mcWrapper is null, the following code will not be executed.
            ManagedConnection mc = mcWrapper.getManagedConnection();
            if (((managedConnectionFactory instanceof WSManagedConnectionFactory &&
                  ((WSManagedConnectionFactory) managedConnectionFactory).isPooledConnectionValidationEnabled())
                 || ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).isPretestThisConnection())
                && gConfigProps.validatingMCFSupported) {
                /*
                 * Reset pretest value
                 */
                ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).setPretestThisConnection(false);
                /*
                 * We need to test the connection before we return the mcWrapper from
                 * the free pool. Note: We do not need to test shared connections
                 * because the connection is already being used. If there is a
                 * connection problem, it should fail in the application using the
                 * shared connection.
                 */
                int poolState = mcWrapper.getPoolState();
                mcWrapper.setPoolState(50);
                ValidatingManagedConnectionFactory validatingMCF = ((ValidatingManagedConnectionFactory) managedConnectionFactory);
                Set<?> invalid = validatingMCF.getInvalidConnections(Collections.singleton(mc));
                if (invalid.isEmpty())
                    mcWrapper.setPoolState(poolState);
                else {
                    /*
                     * Before we try to create a new connection, we need to destroy the
                     * connection that failed the preTestConnection
                     */
                    freePool[0].cleanupAndDestroyMCWrapper(mcWrapper);

                    /*
                     * We are going to try calling the test connection again.
                     */
                    try {
                        /*
                         * Try to create a new connection.
                         */
                        mcWrapper = freePool[0].createManagedConnectionWithMCWrapper(managedConnectionFactory, subject,
                                                                                     requestInfo,
                                                                                     connectionSharing,
                                                                                     hashCode);
                        mcWrapper.setHashMapBucket(hashMapBucket);
                    } catch (ResourceException re) {
                        preTestFailed(managedConnectionFactory, subject, requestInfo, hashMapBucket, re);
                    }
                    mc = mcWrapper.getManagedConnection();
                    invalid = validatingMCF.getInvalidConnections(Collections.singleton(mc));
                    if (invalid.isEmpty())
                        allowConnectionRequests = true;
                    else
                        preTestFailed(managedConnectionFactory, subject, requestInfo, hashMapBucket, new ResourceAllocationException());
                }
            }

            mcWrapper.markInUse();
            if (gConfigProps.raSupportsReauthentication) {
                mcWrapper.setHashMapBucketReAuth(hashMapBucket);
            }

            /*
             *
             * While the connection is inuse we need continued support of WAS pooling
             * functions except for pooling connections in the free pool. By
             * commenting out the following check, we will store inuse connection in
             * the shared or unshared pools. When the connection is close and the
             * transaction committed, the connection will not be pooled in the free
             * pool.
             */
            // if (gConfigProps.connectionPoolingEnabled) {
            if (affinity != null && connectionSharing && !((com.ibm.ejs.j2c.MCWrapper) mcWrapper).isEnlistmentDisabled()) { // add to shared pool, 723884
                if (gConfigProps.isConnectionSynchronizationProvider()) {
                    /*
                     * If we are a SynchronizationProvider and shareable, log a message
                     * and put this connection in the unshareable pool. Connection sharing
                     * is not allowed.
                     */
                    if (isTracingEnabled && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Shareable connections are not allowed with connections that are a SynchronizationProvider.  This connection will not be shareable.");
                    }
                    mcWrapper.setPoolState(3);
                    mcWrapper.setInSharedPool(false);
                } else {
                    //  save on thread local?
                    if (isThreadLocalConnectionEnabled && localConnection_ != null) {
                        ArrayList<MCWrapper> mh = localConnection_.get();
                        requestingAccessToTLSPool();
                        if (mh.size() < maxCapacity) {
                            mcWrapper.setPoolState(MCWrapper.ConnectionState_sharedTLSPool);
                            mcWrapper.setSharedPoolCoordinator(affinity);
                            mh.add(mcWrapper);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).setThreadID(((com.ibm.ejs.j2c.MCWrapper) mcWrapper).getThreadID() + "-reserve-added");
                                Tr.debug(this, tc, "Added mcWrapper from thread local " + mcWrapper);
                            }
                            tlsArrayLists.put(mcWrapper, mh);
                        } else {
                            sharedPool[sharedbucket].setSharedConnection(affinity, mcWrapper);
                            mcWrapper.setInSharedPool(true);
                        }
                        endingAccessToTLSPool();
                        //localConnection_.set(mh);
                    } else {
                        sharedPool[sharedbucket].setSharedConnection(affinity, mcWrapper);
                        mcWrapper.setInSharedPool(true);
                    }
                    // Not creating the parked connection ConnectionManager.parkHandle is not used.
                }
            } else { // add it to the used pool.
                if (isThreadLocalConnectionEnabled && localConnection_ != null) {
                    ArrayList<MCWrapper> mh = localConnection_.get();
                    requestingAccessToTLSPool();
                    if (mh.size() < maxCapacity) {
                        mcWrapper.setPoolState(MCWrapper.ConnectionState_unsharedTLSPool);
                        //mcWrapper.setSharedPoolCoordinator(affinity);
                        mh.add(mcWrapper);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).setThreadID(((com.ibm.ejs.j2c.MCWrapper) mcWrapper).getThreadID() + "-reserve-added");
                            Tr.debug(this, tc, "Added mcWrapper from thread local " + mcWrapper);
                        }
                        tlsArrayLists.put(mcWrapper, mh);
                    } else {
                        mcWrapper.setPoolState(3);
                        mcWrapper.setInSharedPool(false);
                    }
                    endingAccessToTLSPool();
                    //localConnection_.set(mh);
                } else {
                    mcWrapper.setPoolState(MCWrapper.ConnectionState_unsharedPool);
                    mcWrapper.setInSharedPool(false);
                }
            }
            // }
        }

        /*
         * Check to see if trace has been turned on for the managed connection.
         */
        if (traceWriter.isTraceEnabled()) {
            /*
             * If trace has been turned on, check to see if we already set the log
             * writer.
             */
            if (!mcWrapper.isLogWriterSet()) {
                /*
                 * Set the log writer on all mc's
                 */
                turnOnLogWriter();
            }
        } else {
            /*
             * If trace has been turned off, check to see if we already set the log
             * writer to null.
             */
            if (mcWrapper.isLogWriterSet()) {
                /*
                 * Set a null log writer on the mc
                 */
                turnOffLogWriter();
            }
        }
        activeRequest.decrementAndGet();

        if (isTracingEnabled && tc.isDebugEnabled()) {
            if (mcWrapper.getPoolState() == 3) {
                // unshared connections may or may not be in a transaction, but they can be in a scope of a transaction
                // Save the transaction scope for easier debugging of problems
                ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).setUnSharedPoolCoordinator(affinity);
            }
        }

        if (isTracingEnabled && tc.isEntryEnabled())
            Tr.exit(this, tc, "reserve", new Object[] { mcWrapper, mcWrapper.getManagedConnection() });
        return mcWrapper;

    }

    /**
     *
     * Search all TLS locations for a matching connection.
     *
     * For maximum performance, we should not be using this method is the max connections and tls value is configured optimally.
     *
     * @param managedConnectionFactory
     * @param subject
     * @param cri
     * @return
     * @throws ResourceAllocationException
     */
    protected com.ibm.ws.j2c.MCWrapper searchTLSForMatchingConnection(
                                                                      ManagedConnectionFactory managedConnectionFactory,
                                                                      Subject subject,
                                                                      ConnectionRequestInfo cri) throws ResourceAllocationException {
        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "searchTLSForMatchingConnection");
        }
        MCWrapper mcWrapper = null;
        if (tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Attempting to reassign a tls connection to waiting thread");
        }
        boolean searchTLS = false;
        /*
         * We can purge connections in the connection pool if we are not in thread sensitive code.
         */
        updateToTLSPoolInProgress.set(true); // set to true for pausing pool.
        synchronized (updateToTLSPoolInProgressLockObject) {
            try {
                sleep(20);
            } catch (InterruptedException e) {
            }

            // We expect one active request because this is a request.
            if (activeTLSRequest.get() > 1) {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                } // Give the active requests a little time to clear.
                if (activeTLSRequest.get() > 1) {
                    // We can not search tls
                    if (tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "To many active requests to search tls, active request value is " + activeRequest.get());
                    }
                } else {
                    searchTLS = true;
                }
            } else {
                searchTLS = true;
            }
            if (searchTLS) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Searching tls for a connection.");
                }
                Iterator<Entry<MCWrapper, ArrayList<MCWrapper>>> it = tlsArrayLists.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<MCWrapper, ArrayList<MCWrapper>> e = it.next();
                    MCWrapper mcw = e.getKey();
                    if (mcw.getPoolState() == MCWrapper.ConnectionState_freeTLSPool) {
                        ArrayList<MCWrapper> mh = e.getValue();
                        mcWrapper = getMCWrapperFromMatch(subject, cri, managedConnectionFactory, mcw);// Need to add code for handling resource exception.
                        if (mcWrapper != null) { // we have a matching connection.
                            mh.remove(mcw); // remove from thread local storage.
                            tlsArrayLists.remove(mcw); // remove from list
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                ((com.ibm.ejs.j2c.MCWrapper) mcWrapper).setThreadID(((com.ibm.ejs.j2c.MCWrapper) mcWrapper).getThreadID() + "-search-removed");
                                Tr.debug(this, tc, "removed mcWrapper from thread local " + mcWrapper);
                            }
                            break;
                        }
                    }
                }
            }
            updateToTLSPoolInProgress.set(false); // set to false for releasing pool.
            updateToTLSPoolInProgressLockObject.notifyAll();
        }
        if (tc.isDebugEnabled()) {
            if (mcWrapper != null) {
                Tr.debug(this, tc, "Found matching tls connection " + mcWrapper);
            }
            Tr.debug(this, tc, "Current pool information" + this.toString());
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "searchTLSForMatchingConnection");
        }
        return mcWrapper;
    }

    // Count the number of managed connection on this thread.
    //  If we find matching thread,
    //  add to a counter and check if we exceeded the max number
    //  allowed set by customer using custom properties maxNumberOfMCsAllowableInThread.  Throw
    //  an exception if we exceed the number.  :-)
    private void checkForMCsOnThread(int maxNumberOfMCsAllowableInThread2) throws ResourceException {
        // Get the thread id.
        String ivThreadId = RasHelper.getThreadId();
        // Counter for matching threads.
        int numberOfMatchingThreads = 0;
        // Store matching thread ids for the MCW.
        ArrayList<com.ibm.ejs.j2c.MCWrapper> matchingMCWsOnThisThread = new ArrayList<com.ibm.ejs.j2c.MCWrapper>();

        //synchronized (mcToMCWMapLock) {
        mcToMCWMapWrite.lock();
        try {
            // Search the mc to mcw map for managed connection already being used on this thread.
            Collection<MCWrapper> s = mcToMCWMap.values();
            Iterator<MCWrapper> i = s.iterator();
            while (i.hasNext()) {
                com.ibm.ejs.j2c.MCWrapper mcw = (com.ibm.ejs.j2c.MCWrapper) i.next();
                if (ivThreadId.equals(mcw.getThreadID())) {
                    // We have a matching thread id that already has a managed connection inuse.
                    // Store the managed connection wrapper for debug if needed.
                    matchingMCWsOnThisThread.add(mcw);
                    ++numberOfMatchingThreads;
                    // If the number of matching threads exceeds the max set by the customer, start processing the mcw and
                    // thread information
                    if (numberOfMatchingThreads >= maxNumberOfMCsAllowableInThread2) {
                        StringBuffer matchingThreadBuffer = new StringBuffer(1000);
                        for (int j = 0; j < numberOfMatchingThreads; ++j) {
                            // Process the mcw and other data for the resource exception.
                            mcw = matchingMCWsOnThisThread.get(j);
                            long startHoldTime = mcw.getHoldTimeStart();
                            long holdTime = System.currentTimeMillis() - startHoldTime;
                            Date startDateTime = new Date(startHoldTime);
                            long timeInUseInSeconds = holdTime / 1000;
                            Throwable t = mcw.getInitialRequestStackTrace();
                            if (t != null) {
                                matchingThreadBuffer.append("  " + mcw);
                                matchingThreadBuffer.append("     Start time inuse " + startDateTime + " Time inuse " + timeInUseInSeconds + " (seconds)" + nl);
                                matchingThreadBuffer.append("     Last allocation time " + new Date(mcw.getLastAllocationTime()) + nl);
                                matchingThreadBuffer.append("       getConnection stack trace information:" + nl);
                                StackTraceElement[] ste = t.getStackTrace();
                                for (int k = 0; k < ste.length; ++k) {
                                    if (k > 1)
                                        matchingThreadBuffer.append("          " + ste[k].toString() + nl);
                                }
                                matchingThreadBuffer.append(nl);
                            }
                        }
                        String debugText = "Exceeded the number of allowable managed connection on thread " +
                                           ivThreadId + ".  " + maxNumberOfMCsAllowableInThread2 +
                                           " managed connections are already being used on this thread.  " +
                                           "  Managed connection being used on this thread " + nl +
                                           matchingThreadBuffer.toString();
                        if (tc.isDebugEnabled()) {
                            Tr.debug(this, tc, debugText);
                        }

                        throw new ResourceException(debugText);
                    }
                }
            }
        } finally {
            mcToMCWMapWrite.unlock();
        }
    }

    private void turnOnLogWriter() throws ResourceException {
        mcToMCWMapWrite.lock();
        try {
            Iterator<MCWrapper> mcwIt = mcToMCWMap.values().iterator();
            while (mcwIt.hasNext()) {
                MCWrapper mcw = mcwIt.next();
                /**
                 * Set the log writer mc
                 */
                mcw.getManagedConnection().setLogWriter(printWriter);
                /*
                 * Update the mcw log writer flag
                 */
                mcw.setLogWriterSet(true);
            }
        } finally {
            mcToMCWMapWrite.unlock();
        }
    }

    private void turnOffLogWriter() throws ResourceException {
        mcToMCWMapWrite.lock();
        try {
            Iterator<MCWrapper> mcwIt = mcToMCWMap.values().iterator();
            while (mcwIt.hasNext()) {
                MCWrapper mcw = mcwIt.next();
                /*
                 * Set a null log writer on all mc's
                 */
                mcw.getManagedConnection().setLogWriter(null);
                /*
                 * Update the mcw log writer flag
                 */
                mcw.setLogWriterSet(false);
            }
        } finally {
            mcToMCWMapWrite.unlock();
        }
    }

    /**
     * @param subject
     * @param cri
     * @param hashMapBucket
     * @param exn
     * @throws ResourceAllocationException
     */
    private void preTestFailed(ManagedConnectionFactory managedConnectionFactory, Subject subject, ConnectionRequestInfo cri, int hashMapBucket,
                               Exception exn) throws ResourceAllocationException {

        synchronized (poolManagerTestConnectionLock) {

            this.totalConnectionCount.decrementAndGet();
            /*
             * Call the fatal error notification code to apply the
             * purge policy. No connection will be removed from the
             * pool if the purge policy is FailingConnectionOnly.
             */
            fatalErrorNotification(managedConnectionFactory, null, null);

            // call exceptionList method to print stack traces of current and linked exceptions
            Object[] parms = new Object[] {
                                            "reserve",
                                            CommonFunction.exceptionList(exn),
                                            "ResourceAllocationException",
                                            gConfigProps.cfName };
            Tr.error(tc, "POOL_MANAGER_EXCP_CCF2_0002_J2CA0046", parms);
            ResourceAllocationException throwMe = new ResourceAllocationException(exn.getMessage());
            throwMe.initCause(exn);
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Abnormal exit from method preTestFailed");
            }
            activeRequest.decrementAndGet();
            throw (throwMe);
        }
    }

    /**
     * Look for active connection requests and connections
     * in the shared and un-shared pools.
     *
     * @param value
     * @return
     */
    private boolean checkForActiveConnections(int value) {
        /*
         * Check for active connections in the SharedPool
         */
        boolean activeConnections = false;

        /*
         * We have active connections or active pool requests if active requests
         * > 0 or the pool state is not = 1. In either case, we will consider the
         * pool active if statement is true.
         */
        if (value == 0) {
            mcToMCWMapWrite.lock();
            try {
                Collection<MCWrapper> mcWrappers = mcToMCWMap.values();
                Iterator<MCWrapper> mcWrapperIt = mcWrappers.iterator();
                while (mcWrapperIt.hasNext()) {
                    MCWrapper mcw = mcWrapperIt.next();
                    /*
                     * We have atleast one active connection.
                     */
                    /*
                     * If the value is zero, both tests are used.
                     */
                    if (activeRequest.get() > 0) {
                        activeConnections = true;
                        break;
                    } else {
                        int poolState = mcw.getPoolState();
                        if ((poolState != 1) && (poolState != 9)) {
                            activeConnections = true;
                            break;
                        }
                    }
                }
            } finally {
                mcToMCWMapWrite.unlock();
            }
        } else {
            /*
             * Only activeRequest is used.
             */
            if (activeRequest.get() > 0) {
                activeConnections = true;
            }
        }

        return activeConnections;
    }

    synchronized public String gatherPoolStatisticalData() {
        /*
         * Calculate the stat data
         */
        StringBuffer rVal = new StringBuffer();
        /*
         * System.out.println's are used in the method to output
         * this information without trace being active.
         * Do not change the System.out.println's to use Tr.
         */
        rVal.append("*************************************" + nl);
        rVal.append("*************************************" + nl);
        // rVal.append( sb.toString() );
        long total_sop_gets = 0;
        long total_snop_gets = 0;
        long total_sop_gets_notfound = 0;
        long total_snop_gets_notfound = 0;
        //long total_optimisticGetFreeConnection = 0;
        //long total_nonOptimisticGetFreeConnection = 0;
        long total_fop_gets = 0;
        long total_fnop_gets = 0;
        long total_fop_get_notfound = 0;
        long total_fnop_get_notfound = 0;
        long total_freePoolQueuedRequests = 0;
        long total_freePoolCreateManagedConnection = 0;
        long total_numberOfClaimedVictims = 0;
        long total_numberOfClaimedVictims_CRIMM = 0;
        long total_numberOfClaimedVictims_SubjectMM = 0;
        long total_numberOfClaimedVictims_CRISubjectMM = 0;
        for (int j = 0; j < maxFreePoolHashSize; ++j) {
            total_fop_gets = freePool[j].fop_gets + total_fop_gets;
            total_fnop_gets = freePool[j].fnop_gets + total_fnop_gets;
            total_numberOfClaimedVictims = freePool[j].numberOfClaimedVictims + total_numberOfClaimedVictims;
            total_numberOfClaimedVictims_CRIMM = freePool[j].numberOfClaimedVictims_CRI_Only_Mismatch + total_numberOfClaimedVictims_CRIMM;
            total_numberOfClaimedVictims_SubjectMM = freePool[j].numberOfClaimedVictims_Subject_Only_Mismatch + total_numberOfClaimedVictims_SubjectMM;
            total_numberOfClaimedVictims_CRISubjectMM = freePool[j].numberOfClaimedVictims_CRI_Subject_Mismatch + total_numberOfClaimedVictims_CRISubjectMM;
            total_fop_get_notfound = freePool[j].fop_get_notfound + total_fop_get_notfound;
            total_fnop_get_notfound = freePool[j].fnop_get_notfound + total_fnop_get_notfound;
            total_freePoolCreateManagedConnection = freePool[j].freePoolCreateManagedConnection + total_freePoolCreateManagedConnection;
            total_freePoolQueuedRequests = freePool[j].freePoolQueuedRequests + total_freePoolQueuedRequests;
        } //end for j
        for (int i = 0; i < maxSharedBuckets; ++i) {
            total_sop_gets = sharedPool[i].sop_gets + total_sop_gets;
            total_snop_gets = sharedPool[i].snop_gets + total_snop_gets;
            total_sop_gets_notfound = sharedPool[i].sop_gets_notfound + total_sop_gets_notfound;
            total_snop_gets_notfound = sharedPool[i].snop_gets_notfound + total_snop_gets_notfound;
        }

        /*
         * Shared Pool optimisations
         *
         * total_sop_gets = Total number of optimistic shared connection gets
         * total_snop_gets = Total number of non-optimistic shared connection gets
         * total_sop_gets_notfound = Total number of optimistic connection not found in shared pool
         * total_snop_gets_notfound = Total number of non-optimistic connection not found in shared pool
         *
         * totalShared = Total number of shared pool requests.
         * sharedGood = Total number of optimistic shared pool accesses.
         * sharedBad = Total number of non-optimistic share pool accesses.
         *
         * sharedPoolGoodAccessPercent = The higher the percent the better the share pool is running.
         */
        rVal.append("Total number of connection requests " + totalPoolConnectionRequests + nl);
        rVal.append("Shared good optimistic gets " + total_sop_gets + nl);
        rVal.append("Shared non optimistic gets " + total_snop_gets + nl);
        rVal.append("Shared sop_gets_notfound " + total_sop_gets_notfound + nl);
        rVal.append("Shared snop_gets_notfound " + total_snop_gets_notfound + nl);
        long totalShared = total_sop_gets +
                           total_snop_gets +
                           total_sop_gets_notfound +
                           total_snop_gets_notfound;
        rVal.append("    Total number of connection requests " + (totalShared) + nl);
        long sharedGood = total_sop_gets + total_sop_gets_notfound;
        rVal.append("Total good shared pool access " + (sharedGood) + nl);
        long sharedBad = total_snop_gets + total_snop_gets_notfound;
        rVal.append("Total bad shared pool access " + (sharedBad) + nl);
        long sharedPoolGoodAccessPercent = 100;
        if (totalShared > 0) {
            sharedPoolGoodAccessPercent = (sharedGood * 100) / totalShared;
        }
        rVal.append("    Good Shared Access " + (sharedPoolGoodAccessPercent) + "%" + nl);
        if (sharedPoolGoodAccessPercent < 80) {
            rVal.append("Need to increase the shared pool partition size" + nl + nl);
        } else {
            rVal.append(nl);
        }

        /*
         * Free Pool optimisations.
         *
         * total_fop_gets = Total number of optimistic free connection gets
         * total_fnop_gets = Total number of non-optimistic free connection gets
         * total_fop_gets_notfound = Total number of optimistic connection not found in free pool
         * total_fnop_gets_notfound = Total number of non-optimistic connection not found in free pool
         *
         * totalFree = Total number of free pool requests.
         * freeGood = Total number of optimistic free pool accesses.
         * freeBad = Total number of non-optimistic free pool accesses.
         *
         * freePoolGoodAccessPercent = The higher the percent the better the free pool is running.
         *
         * claimVictimCount = Total number of victims claimed
         */

        rVal.append("Free good optimistic gets " + total_fop_gets + nl);
        rVal.append("Free non optimistic gets " + total_fnop_gets + nl);
        rVal.append("Free good optimistic get not found " + total_fop_get_notfound + nl);
        rVal.append("Free non optimistic get not found " + total_fnop_get_notfound + nl);
        rVal.append("Wait skip code " + waitSkip + nl);
        rVal.append("Number of connection created " + total_freePoolCreateManagedConnection + nl);

        long totalFree = total_fop_gets +
        //total_freePoolCreateManagedConnection +
                         total_fnop_gets +
                         total_fnop_get_notfound +
                         total_fop_get_notfound +
                         waitSkip;
        rVal.append("Free pool accesses " + totalFree + nl);
        long freeGood = total_fop_gets + total_fop_get_notfound + waitSkip;
        rVal.append("Total good free pool access " + (freeGood) + nl);
        long freeBad = total_fnop_gets + total_fnop_get_notfound;
        rVal.append("Total bad free pool access " + (freeBad) + nl);

        rVal.append("Total Waiters " + total_freePoolQueuedRequests + nl);
        long freePoolGoodAccessPercent = 0;
        if (freeGood > 0) {
            freePoolGoodAccessPercent = (freeGood * 100) / totalFree;
        }
        rVal.append("    Good Free Access " + freePoolGoodAccessPercent + "%" + nl + nl);
        if (freePoolGoodAccessPercent < 90) {
            rVal.append("Need to increase the max connections pool partition size" + nl);
        }

        rVal.append("Numer of claimed victims " + total_numberOfClaimedVictims + nl);
        claimedVictimPercent = 0;
        if (total_numberOfClaimedVictims > 0) {
            claimedVictimPercent = total_numberOfClaimedVictims * 100d / totalFree;
        }
        rVal.append("Percent of connections claimed as a victim " + claimedVictimPercent + nl);
        rVal.append("  Victims claimed due to Subject mismatch only    " + total_numberOfClaimedVictims_SubjectMM + nl);
        rVal.append("  Victims claimed due to CRI mismatch only        " + total_numberOfClaimedVictims_CRIMM + nl);
        rVal.append("  Victims claimed due to Subject and CRI mismatch " + total_numberOfClaimedVictims_CRISubjectMM + nl);

        return rVal.toString();
    }

    synchronized public String gatherClaimVictimStatisticalData() {
        /*
         * Calculate the stat data
         */
        StringBuffer rVal = new StringBuffer();
        rVal.append("*************************************" + nl);
        rVal.append("*************************************" + nl);
        long total_numberOfClaimedVictims = 0;
        long total_numberOfClaimedVictims_CRIMM = 0;
        long total_numberOfClaimedVictims_SubjectMM = 0;
        long total_numberOfClaimedVictims_CRISubjectMM = 0;
        long total_fop_gets = 0;
        long total_fnop_gets = 0;
        long total_fop_get_notfound = 0;
        long total_fnop_get_notfound = 0;

        for (int j = 0; j < maxFreePoolHashSize; ++j) {
            total_fop_gets = freePool[j].fop_gets + total_fop_gets;
            total_fnop_gets = freePool[j].fnop_gets + total_fnop_gets;
            total_numberOfClaimedVictims = freePool[j].numberOfClaimedVictims + total_numberOfClaimedVictims;
            total_numberOfClaimedVictims_CRIMM = freePool[j].numberOfClaimedVictims_CRI_Only_Mismatch + total_numberOfClaimedVictims_CRIMM;
            total_numberOfClaimedVictims_SubjectMM = freePool[j].numberOfClaimedVictims_Subject_Only_Mismatch + total_numberOfClaimedVictims_SubjectMM;
            total_numberOfClaimedVictims_CRISubjectMM = freePool[j].numberOfClaimedVictims_CRI_Subject_Mismatch + total_numberOfClaimedVictims_CRISubjectMM;
            total_fop_get_notfound = freePool[j].fop_get_notfound + total_fop_get_notfound;
            total_fnop_get_notfound = freePool[j].fnop_get_notfound + total_fnop_get_notfound;
        } //end for j

        long totalFree = total_fop_gets +
        //total_freePoolCreateManagedConnection +
                         total_fnop_gets +
                         total_fnop_get_notfound +
                         total_fop_get_notfound +
                         waitSkip;

        rVal.append("Numer of claimed victims " + total_numberOfClaimedVictims + nl);
        rVal.append("Free pool accesses " + totalFree + nl);
        long claimedVictimPercent = 0;
        if (total_numberOfClaimedVictims > 0) {
            claimedVictimPercent = (total_numberOfClaimedVictims * 100) / totalFree;
        }
        rVal.append("Percent of connections claimed as a victim " + claimedVictimPercent + nl);
        rVal.append("  Victims claimed due to Subject mismatch only    " + total_numberOfClaimedVictims_SubjectMM + nl);
        rVal.append("  Victims claimed due to CRI mismatch only        " + total_numberOfClaimedVictims_CRIMM + nl);
        rVal.append("  Victims claimed due to Subject and CRI mismatch " + total_numberOfClaimedVictims_CRISubjectMM + nl);

        return rVal.toString();
    }

    /**
     * Purge the connection from the pool. Any connection in the freepool will
     * be removed, cleaned up and destroyed and any active connection will be marked
     * to be destroyed. The parked connection will not be destroyed.
     */
    public void purgePoolContents() throws ResourceException {
        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "purgePoolContents", gConfigProps.cfName);
        }
        ArrayList<MCWrapper> destroyMCWrapperList = new ArrayList<MCWrapper>();
        synchronized (destroyMCWrapperListLock) {
            for (int j = 0; j < gConfigProps.getMaxFreePoolHashSize(); ++j) {
                synchronized (freePool[j].freeConnectionLockObject) {
                    /*
                     * If a connection gets away, by setting fatalErrorNotificationTime will
                     * guaranty when the connection is returned to the free pool, it will be
                     */
                    freePool[j].incrementFatalErrorValue(j);
                    /*
                     * Move as many connections as we can in the free pool to the destroy list
                     */
                    if (freePool[j].mcWrapperList.size() > 0) {
                        //freePool[j][i].removeCleanupAndDestroyAllFreeConnections();
                        int mcWrapperListIndex = freePool[j].mcWrapperList.size() - 1;
                        for (int k = mcWrapperListIndex; k >= 0; --k) {
                            MCWrapper mcw = (MCWrapper) freePool[j].mcWrapperList.remove(k);
                            if (!mcw.isMarkedForPurgeDestruction()) {
                                mcw.setPoolState(0);
                                mcw.markForPurgeDestruction();
                                destroyMCWrapperList.add(mcw);
                                --freePool[j].numberOfConnectionsAssignedToThisFreePool;
                            }
                        }
                    }
                } // end free lock
            } //end for j
        } //end syc for destroyMCWrapperListLock
        if (localConnection_ != null) {
            boolean purgeTLS = false;
            /*
             * We can purge connections in the connection pool if we are not in thread sensitive code.
             */
            updateToTLSPoolInProgress.set(true); // set to true for pausing pool.
            synchronized (updateToTLSPoolInProgressLockObject) {
                try {
                    sleep(20);
                } catch (InterruptedException e) {
                }
                if (activeTLSRequest.get() > 0) {
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                    }
                    if (activeTLSRequest.get() > 0) {
                        // We can not purge tls
                        if (tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "To many active requests to search tls, active request value is " + activeRequest.get());
                        }
                    } else {
                        purgeTLS = true;
                    }
                } else {
                    purgeTLS = true;
                }
                if (purgeTLS) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Searching tls for a connections to purge.");
                    }
                    Iterator<Entry<MCWrapper, ArrayList<MCWrapper>>> it = tlsArrayLists.entrySet().iterator();
                    while (it.hasNext()) {
                        Entry<MCWrapper, ArrayList<MCWrapper>> e = it.next();
                        MCWrapper mcw = e.getKey();
                        if (mcw.getPoolState() == MCWrapper.ConnectionState_freeTLSPool) {
                            ArrayList<MCWrapper> mh = e.getValue();
                            mh.remove(mcw);
                            tlsArrayLists.remove(mcw);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                ((com.ibm.ejs.j2c.MCWrapper) mcw).setThreadID(((com.ibm.ejs.j2c.MCWrapper) mcw).getThreadID() + "-purge-removed");
                                Tr.debug(this, tc, "removed mcWrapper from thread local " + mcw);
                            }
                            if (!mcw.isMarkedForPurgeDestruction()) {
                                mcw.setPoolState(0);
                                mcw.markForPurgeDestruction();
                                destroyMCWrapperList.add(mcw);
                            }
                        }
                    }
                }
                updateToTLSPoolInProgress.set(false); // set to false for releasing pool.
                updateToTLSPoolInProgressLockObject.notifyAll();
            }
        }
        /*
         * we need to cleanup and destroy connections in the local destroy list.
         */
        for (int i = 0; i < destroyMCWrapperList.size(); ++i) {
            MCWrapper mcw = destroyMCWrapperList.get(i);
            freePool[0].cleanupAndDestroyMCWrapper(mcw);
            this.totalConnectionCount.decrementAndGet();
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "purgePoolContents");
        }
    }

    /**
     * This will remove all of the free pool connections. If there are
     * active connection in the shared or un-shared pool, these connection
     * will be remove when they are being returned to the free pool.
     *
     * @param value "immediate" will result an immediate purge of the pool.
     *                  value "abort" will result in purging the pool via Connection.abort()
     *                  Any other value will call purgePoolContents().
     * @throws ResourceException
     */
    public void purgePoolContents(String value) throws ResourceException {

        if (!"immediate".equalsIgnoreCase(value) && !"abort".equalsIgnoreCase(value)) {
            purgePoolContents();
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "purgePoolContents");

        boolean purgeWithAbort = "abort".equalsIgnoreCase(value);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Clearing free pool connections");

        for (int j = 0; j < maxFreePoolHashSize; ++j) {
            synchronized (freePool[j].freeConnectionLockObject) {
                /*
                 * If a connection gets away, by setting fatalErrorNotificationTime will
                 * guaranty when the connection is returned to the free pool, it will be
                 */
                freePool[j].incrementFatalErrorValue(j);
                /*
                 * Destroy as many connections as we can in the free pool
                 */
                if (freePool[j].mcWrapperList.size() > 0) {
                    int mcWrapperListIndex = freePool[j].mcWrapperList.size() - 1;
                    for (int k = mcWrapperListIndex; k >= 0; --k) {
                        MCWrapper mcw = (MCWrapper) freePool[j].mcWrapperList.remove(k);

                        if (mcw.isMarkedForPurgeDestruction())
                            continue;
                        mcw.setDestroyState();
                        mcw.markForPurgeDestruction();
                        --freePool[j].numberOfConnectionsAssignedToThisFreePool;

                        if (mcw.getManagedConnection() instanceof WSManagedConnection) {
                            ((WSManagedConnection) mcw.getManagedConnection()).markStale();
                        }

                        if (purgeWithAbort && mcw instanceof com.ibm.ejs.j2c.MCWrapper
                            && ((com.ibm.ejs.j2c.MCWrapper) mcw).abortMC()) {
                            // The MCW aborted the connection sucessfully
                        } else {
                            if (purgeWithAbort && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "Unable to purge connection with abort.  Using ThreadSupportedCleanupAndDestroy.", mcw);
                            final MCWrapper tempMCWrapper = mcw;
                            final FreePool tempFP = freePool[j];
                            ThreadSupportedCleanupAndDestroy tscd = new ThreadSupportedCleanupAndDestroy(tscdList, tempFP, tempMCWrapper);
                            tscdList.add(tscd);
                            connectorSvc.execSvcRef.getServiceWithException().submit(tscd);
                            this.totalConnectionCount.decrementAndGet();
                        }
                    }
                }
            } // end free lock
        } //end for j

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Marking inuse pool connections stale");
        }

        for (int i = 0; i < maxSharedBuckets; ++i) {
            synchronized (sharedPool[i].sharedLockObject) {
                if (sharedPool[i].getMCWrapperListSize() > 0) {
                    MCWrapper[] mcwl = sharedPool[i].getMCWrapperList();
                    for (int j = 0; j < sharedPool[i].getMCWrapperListSize(); ++j) {
                        if (!mcwl[j].isDestroyState()) {
                            mcwl[j].setDestroyState();

                            if (purgeWithAbort && mcwl[j] instanceof com.ibm.ejs.j2c.MCWrapper
                                && ((com.ibm.ejs.j2c.MCWrapper) mcwl[j]).abortMC()) {
                                // The MCW aborted the connection sucessfully
                            } else {
                                if (mcwl[j].getManagedConnection() instanceof WSManagedConnection) {
                                    ((WSManagedConnection) mcwl[j].getManagedConnection()).markStale();
                                }
                                this.totalConnectionCount.decrementAndGet();
                            }
                        }
                    }
                }
            }
        }
        MCWrapper[] mcwl = getUnSharedPoolConnections();
        int mcwlLength = mcwl.length;
        for (int j = 0; j < mcwlLength; ++j) {
            if (!mcwl[j].isDestroyState()) {
                mcwl[j].setDestroyState();

                if (purgeWithAbort && mcwl[j] instanceof com.ibm.ejs.j2c.MCWrapper
                    && ((com.ibm.ejs.j2c.MCWrapper) mcwl[j]).abortMC()) {
                    // The MCW aborted the connection sucessfully
                } else {
                    if (mcwl[j].getManagedConnection() instanceof WSManagedConnection) {
                        ((WSManagedConnection) mcwl[j].getManagedConnection()).markStale();
                    }
                    this.totalConnectionCount.decrementAndGet();
                }
            }
        } // end for loop

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "purgePoolContents");
        }
    }

    /**
     * The caller of this method must have a lock on the PoolManagers
     * waiterFreePoolLock object.
     *
     * @param subject
     * @param cri
     * @return
     * @throws ResourceAllocationException
     * @throws ConnectionWaitTimeoutException
     */
    protected MCWrapper getFreeWaiterConnection(ManagedConnectionFactory managedConnectionFactory, Subject subject,
                                                ConnectionRequestInfo cri) throws ResourceAllocationException, ConnectionWaitTimeoutException {
        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getFreeWaiterConnection", gConfigProps.cfName);
        }
        MCWrapper mcWrapper = null;
        MCWrapper mcWrapperTemp = null;

        int mcwlSize = mcWrapperWaiterList.size();
        if (mcwlSize > 0) {
            int mcwlIndex = mcwlSize - 1;
            // Remove the first mcWrapper from the list (Optimistic)
            mcWrapperTemp = (MCWrapper) mcWrapperWaiterList.remove(mcwlIndex);
            mcWrapperTemp.setPoolState(0);
            mcWrapper = getMCWrapperFromMatch(subject, cri, managedConnectionFactory, mcWrapperTemp);
            if (((com.ibm.ejs.j2c.MCWrapper) mcWrapperTemp).do_not_reuse_mcw) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Connection error occurred for this mcw " + mcWrapperTemp + ", mcw will not be reuse");
                }
                freePool[0].cleanupAndDestroyMCWrapper(mcWrapperTemp);
                if ((waiterCount > 0) && (waiterCount > mcWrapperWaiterList.size())) {
                    waiterFreePoolLock.notify();
                }
                if (tc.isEntryEnabled()) {
                    Tr.exit(this, tc, "getFreeWaiterConnection", new Object[] { "Returning destroyed mcWrapper", mcWrapperTemp });
                }
                return mcWrapperTemp;
            }
            if (mcWrapper == null) {
                // We need to add the mcWrapper back, we don't have a
                // matching connection
                mcWrapperWaiterList.add(mcWrapperTemp);
                mcWrapperTemp.setPoolState(4);
                // We need to look through the list, since we didn't find a matching connection at
                // the end of the list, we need to use get and only remove if we find a matching
                // connection.
                for (int i = mcwlIndex - 1; i >= 0; --i) {
                    mcWrapperTemp = (MCWrapper) mcWrapperWaiterList.get(i);
                    mcWrapper = getMCWrapperFromMatch(subject, cri, managedConnectionFactory, mcWrapperTemp);
                    if (((com.ibm.ejs.j2c.MCWrapper) mcWrapperTemp).do_not_reuse_mcw) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "Connection error occurred for this mcw " + mcWrapperTemp + ", mcw will not be reuse");
                        }
                        mcWrapperWaiterList.remove(i);
                        freePool[0].cleanupAndDestroyMCWrapper(mcWrapperTemp);
                        if ((waiterCount > 0) && (waiterCount > mcWrapperWaiterList.size())) {
                            waiterFreePoolLock.notify();
                        }
                        if (tc.isEntryEnabled()) {
                            Tr.exit(this, tc, "getFreeWaiterConnection", new Object[] { "Returning destroyed mcWrapper", mcWrapperTemp });
                        }
                        return mcWrapperTemp;
                    }
                    if (mcWrapper != null) {
                        mcWrapperWaiterList.remove(i);
                        mcWrapper.setPoolState(0);
                        break;
                    }
                }
            } // end else
        } // end if
        if (tc.isEntryEnabled()) {
            if (tc.isDebugEnabled()) {
                // only print this information if tc.isEntryEnabled() and tc.isDebugEnabled()
                if (mcWrapper != null) {
                    Tr.debug(this, tc, "Returning mcWrapper " + mcWrapper);

                } else {
                    Tr.debug(this, tc, "MCWrapper was not found in Free Pool");
                }
            }
            Tr.exit(this, tc, "getFreeWaiterConnection", mcWrapper);
        }
        return mcWrapper;
    }

    /**
     * @param mcWrapperPool
     * @param hashCode
     * @param bucketNumber
     * @param subject
     * @param cri
     * @return
     * @throws ResourceAllocationException
     */
    protected MCWrapper claimVictim(ManagedConnectionFactory managedConnectionFactory,
                                    int hashCode,
                                    Subject subject,
                                    ConnectionRequestInfo cri) throws ResourceAllocationException { // we are synchronized
        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "claimVictim");
        }
        MCWrapper mcWrapper = null;
        int mcWrapperListSize = freePool[hashCode].mcWrapperList.size();
        if (mcWrapperListSize > 0) {
            /*
             * Remove the oldest connection from the mcWrapperList. We are going to either return the mcw or
             * cleanup and destroy it.
             */
            MCWrapper mcw = (MCWrapper) freePool[hashCode].mcWrapperList.remove(0);
            mcw.setPoolState(0);

            /*
             * Try to see if the connection matches one last time
             */
            mcWrapper = getMCWrapperFromMatch(subject, cri, managedConnectionFactory, mcw);
            if (((com.ibm.ejs.j2c.MCWrapper) mcw).do_not_reuse_mcw) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Connection error occurred for this mcw " + mcw + ", mcw will not be reuse");
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Claiming victim " + mcw);
                }
                freePool[hashCode].cleanupAndDestroyMCWrapper(mcw); // cleanup, remove and then release mcw
                --freePool[hashCode].numberOfConnectionsAssignedToThisFreePool;
                if (tc.isEntryEnabled()) {
                    Tr.exit(this, tc, "claimVictim", mcWrapper);
                }
                return mcWrapper;
            }

            if (mcWrapper == null) {
                ManagedConnection mc = mcw.getManagedConnection();
                if (gConfigProps.sendClaimedVictomToGetConnection && mc instanceof WSManagedConnection) {
                    ((WSManagedConnection) mc).setClaimedVictim();
                    mcWrapper = mcw;
                } else {
                    /*
                     * We are going to claim this connection as a victim. Log a debug
                     * message, cleanup and destroy the mcWrappers connection.
                     */
                    if (tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Claiming victim " + mcw);
                    }
                    freePool[hashCode].cleanupAndDestroyMCWrapper(mcw); // cleanup, remove and then release mcw
                    /*
                     * Remove the mcWrapper from the mcWrapperList.
                     */
                    --freePool[hashCode].numberOfConnectionsAssignedToThisFreePool;
                    if (tc.isDebugEnabled()) {
                        ++freePool[hashCode].numberOfClaimedVictims;
                        boolean subjectMismatch = false;
                        boolean criMismatch = false;
                        if (((cri == null) && (mcw.getCRI() != null)) ||
                            ((cri != null) && (mcw.getCRI() == null)) ||
                            ((cri != null) && (cri.equals(mcw.getCRI()) == false))) {
                            criMismatch = true;
                        }

                        if (((subject == null) && (mcw.getSubject() != null)) ||
                            ((subject != null) && (mcw.getSubject() == null))) {
                            subjectMismatch = true;
                        } else if (subject != null) {
                            Equals equalsHelper = new Equals();
                            equalsHelper.setSubjects(subject, mcw.getSubject());
                            if (!AccessController.doPrivileged(equalsHelper)) {
                                subjectMismatch = true;
                            }
                        }

                        if (criMismatch && subjectMismatch) {
                            ++freePool[hashCode].numberOfClaimedVictims_CRI_Subject_Mismatch;
                        } else if (criMismatch) {
                            ++freePool[hashCode].numberOfClaimedVictims_CRI_Only_Mismatch;
                        } else if (subjectMismatch) {
                            ++freePool[hashCode].numberOfClaimedVictims_Subject_Only_Mismatch;
                        }
                        if (tc.isDebugEnabled()) {
                            Tr.debug(this, tc, gatherClaimVictimStatisticalData());
                        }
                    }
                }
            }
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "claimVictim", mcWrapper);
        }
        return mcWrapper;
    }

    /**
     * @param subject
     * @param cri
     * @param managedFactory
     * @param hashCode
     * @param bucketNumber
     * @param mcWrapperTemp
     * @return
     * @throws ResourceAllocationException
     */
    protected MCWrapper getMCWrapperFromMatch(
                                              Subject subject,
                                              ConnectionRequestInfo cri,
                                              ManagedConnectionFactory managedFactory,
                                              MCWrapper mcWrapperTemp) throws ResourceAllocationException {

        if (tc.isEntryEnabled()) {
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
        freePoolMCSet.add(mcWrapperTemp.getManagedConnection());
        try {
            int poolState = mcWrapperTemp.getPoolState();
            mcWrapperTemp.setPoolState(50);
            mc = managedFactory.matchManagedConnections(
                                                        freePoolMCSet,
                                                        subject,
                                                        cri);
            mcWrapperTemp.setPoolState(poolState);
        } catch (javax.resource.ResourceException exn1) {
            com.ibm.ws.ffdc.FFDCFilter.processException(
                                                        exn1,
                                                        "com.ibm.ejs.j2c.poolmanager.PoolManager.getMCWrapperFromMatch",
                                                        "786",
                                                        this);
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "datasource " + gConfigProps.cfName + ": ResourceException " + exn1);
                Tr.debug(this, tc, "Throwing ResourceAllocationException...");
                Tr.debug(this, tc, "match(), Pool contents ==> " + this);
            }
            ResourceAllocationException throwMe = new ResourceAllocationException("ResourceException");
            throwMe.initCause(exn1.getCause());
            if (tc.isEntryEnabled()) {
                Tr.exit(this, tc, "getMCWrapperFromMatch", exn1);
            }
            activeRequest.decrementAndGet();
            throw (throwMe);
        }
        if (mc != null) {
            // Now that we have the matching mc, we need to return the mcWrapper
            mcWrapper = mcWrapperTemp;
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "getMCWrapperFromMatch", mcWrapper);
        }

        return mcWrapper;
    }

    /**
     * Starts the reclaim collection thread that remove unused connection based on
     * the UnusedTimeout, agedTime and the ReapTime.
     */

    protected void startReclaimConnectionThread() {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "startReclaimConnectionThread");
        }

        //long reapTime;

        synchronized (taskTimerLockObject) {
            if (!reaperThreadStarted) {
                if (reapTime > 0) {
                    if ((this.unusedTimeout > 0)
                        || (this.agedTimeout > 0)) {
                        final PoolManager tempPM = this;
                        reaperThreadStarted = true;
                        AccessController.doPrivileged(new PrivilegedAction<Void>() {
                            @Override
                            public Void run() {
                                new TaskTimer(tempPM);
                                return null;
                            }
                        });

                        if (tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "Started reclaim connection thread for pool " + gConfigProps.getXpathId());
                        }
                    }

                }
            }
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "startReclaimConnectionThread");
        }

    }

    /*
     * Used for toString trace reduction optimization.
     */
    private class ToStringStackElements {
        /**
         * @param ste
         * @param numberOfOccurrents
         */
        public ToStringStackElements(StackTraceElement[] ste, int numberOfOccurrents) {
            super();
            this.ste = ste;
            this.numberOfOccurrents = numberOfOccurrents;
        }

        StackTraceElement[] ste = null;
        int numberOfOccurrents = 0;
    }

    /**
     * Returns current state of the Pool Manager as String.
     *
     * @return java.lang.String
     */
    @Override
    public String toString() {
        /*
         * Zero is the original toString behavior.
         */
        return toString2(0);
    }

    /**
     * Returns current state of the Pool Manager as String.
     *
     * <pre>
     * options:<br>
     * 0 - default behavior, dump everything avalable.<br>
     * 1 - dump everything except connection leak information.<br>
     * </pre>
     *
     * @return java.lang.String
     * @concurrency concurrent
     */
    public String toString2(int options) {

        StringBuffer aBuffer = new StringBuffer(500);
        StringBuffer bBuffer = new StringBuffer(500);
        ArrayList<ToStringStackElements> throwableStackTraceElements = new ArrayList<ToStringStackElements>();

        aBuffer.append("JNDI name:");
        aBuffer.append(gConfigProps.cfName);
        aBuffer.append(nl);
        aBuffer.append("PoolManager object:");
        aBuffer.append(this.hashCode());
        aBuffer.append(nl);
        aBuffer.append("Total number of connections: ");
        if (connectionPoolShutDown) {
            aBuffer.append("purging ");
        }
        aBuffer.append(totalConnectionCount.get());
        aBuffer.append(" (max/min ");
        aBuffer.append(maxConnections);
        aBuffer.append("/");
        aBuffer.append(minConnections);
        aBuffer.append(", reap/unused/aged ");
        aBuffer.append(reapTime);
        aBuffer.append("/");
        aBuffer.append(unusedTimeout);
        aBuffer.append("/");
        aBuffer.append(agedTimeout);
        aBuffer.append(", connectiontimeout/purge ");
        aBuffer.append(connectionTimeout);
        aBuffer.append("/");
        aBuffer.append(purgePolicy);
        aBuffer.append(")");

        /*
         * Log additional information only if function is enabled.
         */
        if (localConnection_ != null) {
            bBuffer.append(", maxTLS ");
            bBuffer.append(maxCapacity);
        }

        if (bBuffer.length() > 1) {
            aBuffer.append(nl);
            aBuffer.append("                              ");
            aBuffer.append(" (");
            aBuffer.append(bBuffer.substring(2));
            aBuffer.append(")");
        }

        bBuffer.delete(0, bBuffer.length());

        if (_quiesce) { //  include quiesce time
            aBuffer.append("  quiesce time:");
            aBuffer.append(_quiesceTime);
        }

        aBuffer.append(nl);

        if (waiterCount > 0) {
            aBuffer.append("The waiter count is ");
            aBuffer.append(waiterCount);
            aBuffer.append(nl);
            aBuffer.append("The mcWrappers in waiter queue ");
            try {
                aBuffer.append(mcWrapperWaiterList);
            } catch (ConcurrentModificationException cm) {
                aBuffer.append("info not available");
            }
            aBuffer.append(nl);
        }

        if (!gConfigProps.connectionPoolingEnabled) {
            aBuffer.append("Connection pooling is disabled, free connections are not pooled.");
        }
        /*
         * Shared connection information
         */
        int totalNumberOfSharedConnections = 0;
        StringBuffer connectionLeakBuffer = null;
        long currentTime = 0;
        int holdTimeLimit_loc = -1;
        if (options == 0) {
            holdTimeLimit_loc = holdTimeLimit;
        }
        if (holdTimeLimit_loc > -1) {
            connectionLeakBuffer = new StringBuffer();
            currentTime = System.currentTimeMillis();
        }
        aBuffer.append("Shared Connection information (shared partitions " + maxSharedBuckets + ")");
        aBuffer.append(nl);
        boolean atleastOne = false;
        currentTime = System.currentTimeMillis();
        mcToMCWMapWrite.lock();
        try {
            int mcToMCWSize = mcToMCWMap.size();
            if (mcToMCWSize > 0) {
                Object[] tempObject = mcToMCWMap.values().toArray();
                for (int ti = 0; ti < mcToMCWSize; ++ti) {
                    MCWrapper mcw = (MCWrapper) tempObject[ti];
                    if (mcw.getPoolState() == 2) {
                        aBuffer.append("    ");
                        if (mcw.isDestroyState()) {
                            aBuffer.append("Connection marked for thread supported cleanup and destroy.  Waiting ");
                            aBuffer.append("for transaction end and connection close - ");
                        } else {
                            if (mcw.isStale() || mcw.hasFatalErrorNotificationOccurred(freePool[0].getFatalErrorNotificationTime())
                                || ((this.agedTimeout != -1)
                                    && (mcw.hasAgedTimedOut(this.agedTimeoutMillis)))) {
                                aBuffer.append("Connection marked to be destroyed.  Waiting ");
                                aBuffer.append("for transaction end and connection close - ");
                            }
                        }
                        aBuffer.append(mcw.getSharedPoolCoordinator());
                        aBuffer.append("  ");
                        aBuffer.append(mcw);
                        if (holdTimeLimit_loc > -1) {
                            dumpHoldTimeAndStackInfo(mcw, aBuffer, connectionLeakBuffer, currentTime, throwableStackTraceElements);
                        }
                        ++totalNumberOfSharedConnections;
                        atleastOne = true;
                    }
                }
            }
        } finally {
            mcToMCWMapWrite.unlock();
        }
        if (atleastOne) {
            aBuffer.append("  Total number of connection in shared pool: ");
            aBuffer.append(totalNumberOfSharedConnections);
        } else {
            aBuffer.append("  No shared connections");
            aBuffer.append(nl);
        }

        /*
         * Free pool connection information
         */
        int totalNumberOfFreeConnections = 0;
        aBuffer.append(nl);
        aBuffer.append("Free Connection information (free distribution table " + maxFreePoolHashSize + ")");
        aBuffer.append(nl);
        atleastOne = false;
        mcToMCWMapWrite.lock();
        try {
            int mcToMCWSize = mcToMCWMap.size();
            if (mcToMCWSize > 0) {
                Object[] tempObject = mcToMCWMap.values().toArray();
                for (int ti = 0; ti < mcToMCWSize; ++ti) {
                    MCWrapper mcw = (MCWrapper) tempObject[ti];
                    if (mcw.getPoolState() == 1) {
                        aBuffer.append("  (" + mcw.getHashMapBucket() + ")");
                        aBuffer.append(mcw);
                        ++totalNumberOfFreeConnections;
                        atleastOne = true;
                    }
                }
            }
        } finally {
            mcToMCWMapWrite.unlock();
        }
        aBuffer.append(nl);
        if (atleastOne) {
            aBuffer.append("  Total number of connection in free pool: ");
            aBuffer.append(totalNumberOfFreeConnections);
        } else {
            aBuffer.append("  No free connections");
            aBuffer.append(nl);
        }

        /*
         * Inuse UnShared connection information
         */
        aBuffer.append(nl);
        int totalNumberOfUnSharedConnections = 0;
        aBuffer.append("UnShared Connection information");
        aBuffer.append(nl);
        atleastOne = false;
        mcToMCWMapWrite.lock();
        try {
            int mcToMCWSize = mcToMCWMap.size();
            if (mcToMCWSize > 0) {
                Object[] tempObject = mcToMCWMap.values().toArray();
                for (int ti = 0; ti < mcToMCWSize; ++ti) {
                    MCWrapper mcw = (MCWrapper) tempObject[ti];
                    if (mcw.getPoolState() == 3) {
                        aBuffer.append("    ");
                        if (mcw.isStale()
                            || mcw.hasFatalErrorNotificationOccurred(freePool[0].getFatalErrorNotificationTime())
                            || ((this.agedTimeout != -1) && (mcw.hasAgedTimedOut(this.agedTimeoutMillis)))) {
                            aBuffer.append("Connection marked to be destroyed.  Waiting ");
                            aBuffer.append("for transaction end and connection close - ");
                        }
                        aBuffer.append(mcw);
                        if (holdTimeLimit_loc > -1) {
                            dumpHoldTimeAndStackInfo(mcw, aBuffer, connectionLeakBuffer, currentTime, throwableStackTraceElements);
                        }
                        ++totalNumberOfUnSharedConnections;
                        atleastOne = true;
                    }
                }
            }
        } finally {
            mcToMCWMapWrite.unlock();
        }

        if (atleastOne) {
            aBuffer.append("  Total number of connection in unshared pool: ");
            aBuffer.append(totalNumberOfUnSharedConnections);
            aBuffer.append(nl);
        } else {
            aBuffer.append("  No unshared connections");
            aBuffer.append(nl);
        }
        int holdTimeLimit_loc_disabled = -1;
        if (localConnection_ != null) {
            displaySharedTLSConnections(aBuffer, connectionLeakBuffer, currentTime,
                                        holdTimeLimit_loc_disabled, throwableStackTraceElements);
            displayFreeTLSConnections(aBuffer, connectionLeakBuffer, currentTime,
                                      holdTimeLimit_loc_disabled, throwableStackTraceElements);
            displayUnsharedTLSConnections(aBuffer, connectionLeakBuffer, currentTime,
                                          holdTimeLimit_loc_disabled, throwableStackTraceElements);
        }
        // This is only for additional trace if needed.
        //    displayAllConnectionInPool(aBuffer, connectionLeakBuffer, currentTime,
        //              holdTimeLimit_loc_disabled);
        int tscdSize = tscdList.size();
        if (tscdSize > 0) {
            aBuffer.append(nl + "Thread supported cleanup and destroy connection information" + nl);
            Object o = null;
            for (int i = 0; i < tscdList.size(); ++i) {
                /*
                 * Print as many mcWrapper in the list as possible.
                 */
                try {
                    o = tscdList.get(i);
                } catch (IndexOutOfBoundsException e) {
                    /*
                     * If this occurs, the list size most likely changed.
                     */
                    break;
                }
                if (o != null) {
                    aBuffer.append("  " + o.toString());
                }
            }
            aBuffer.append("  Total number of thread supported cleanup and destroy connections requests being processed: " + tscdSize + " (not included in total connection count)"
                           + nl);
        }
        /*
         * The initial length of the connectionLeakBuffer is 36.
         * If the length is larger than 36, we have added information
         * for the trace.
         */
        if (holdTimeLimit_loc > -1) {
            if (connectionLeakBuffer.length() > 0) {
                aBuffer.append(nl);
                aBuffer.append("Connection Leak Logic Information: (Note, applications using managed connections " +
                               "in this list may not be following the recommended " +
                               "getConnection(), use connection, close() connection " +
                               " programming model pattern)" + nl);
                aBuffer.append(connectionLeakBuffer);
            }
        }
        /*
         * All done return string buffer.
         */
        return aBuffer.toString();

    }

    private void displaySharedTLSConnections(StringBuffer aBuffer,
                                             StringBuffer connectionLeakBuffer, long currentTime,
                                             int holdTimeLimit_loc, ArrayList<ToStringStackElements> throwableStackTraceElements) {
        boolean atleastOne;
        /*
         * Inuse Shared TLS connection information
         */
        aBuffer.append(nl);
        int totalNumberOfSharedTLSConnections = 0;
        aBuffer.append("Shared TLS Connection information");
        aBuffer.append(nl);
        atleastOne = false;
        mcToMCWMapWrite.lock();
        try {
            int mcToMCWSize = mcToMCWMap.size();
            if (mcToMCWSize > 0) {
                Object[] tempObject = mcToMCWMap.values().toArray();
                for (int ti = 0; ti < mcToMCWSize; ++ti) {
                    MCWrapper mcw = (MCWrapper) tempObject[ti];
                    if (mcw.getPoolState() == MCWrapper.ConnectionState_sharedTLSPool) {
                        aBuffer.append("    ");
                        if (mcw.isStale()
                            || mcw.hasFatalErrorNotificationOccurred(freePool[0].getFatalErrorNotificationTime())
                            || ((this.agedTimeout != -1) && (mcw.hasAgedTimedOut(this.agedTimeoutMillis)))) {
                            aBuffer.append("Connection marked to be destroyed.  Waiting ");
                            aBuffer.append("for transaction end and connection close - ");
                        }
                        aBuffer.append(mcw.getSharedPoolCoordinator());
                        aBuffer.append("  ");
                        aBuffer.append(mcw);
                        if (holdTimeLimit_loc > -1) {
                            dumpHoldTimeAndStackInfo(mcw, aBuffer, connectionLeakBuffer, currentTime, throwableStackTraceElements);
                        }
                        ++totalNumberOfSharedTLSConnections;
                        atleastOne = true;
                    }
                }
            }
        } finally {
            mcToMCWMapWrite.unlock();
        }
        if (atleastOne) {
            aBuffer.append("  Total number of connection in shared TLS pool: ");
            aBuffer.append(totalNumberOfSharedTLSConnections);
            aBuffer.append(nl);
        } else {
            aBuffer.append("  No shared TLS connections");
            aBuffer.append(nl);
        }
    }

    private void displayFreeTLSConnections(StringBuffer aBuffer,
                                           StringBuffer connectionLeakBuffer, long currentTime,
                                           int holdTimeLimit_loc, ArrayList<ToStringStackElements> throwableStackTraceElements) {
        boolean atleastOne;
        /*
         * Inuse UnShared connection information
         */
        aBuffer.append(nl);
        int totalNumberOfFreeSharedTLSConnections = 0;
        aBuffer.append("Free TLS Connection information");
        aBuffer.append(nl);
        atleastOne = false;
        mcToMCWMapWrite.lock();
        try {
            int mcToMCWSize = mcToMCWMap.size();
            if (mcToMCWSize > 0) {
                Object[] tempObject = mcToMCWMap.values().toArray();
                for (int ti = 0; ti < mcToMCWSize; ++ti) {
                    MCWrapper mcw = (MCWrapper) tempObject[ti];
                    if (mcw.getPoolState() == MCWrapper.ConnectionState_freeTLSPool) {
                        aBuffer.append("    ");
                        if (mcw.isStale()
                            || mcw.hasFatalErrorNotificationOccurred(freePool[0].getFatalErrorNotificationTime())
                            || ((this.agedTimeout != -1) && (mcw.hasAgedTimedOut(this.agedTimeoutMillis)))) {
                            aBuffer.append("Connection marked to be destroyed.  Waiting ");
                            aBuffer.append("for transaction end and connection close - ");
                        }
                        aBuffer.append(mcw);
                        if (holdTimeLimit_loc > -1) {
                            dumpHoldTimeAndStackInfo(mcw, aBuffer, connectionLeakBuffer, currentTime, throwableStackTraceElements);
                        }
                        ++totalNumberOfFreeSharedTLSConnections;
                        atleastOne = true;
                    }
                }
            }
        } finally {
            mcToMCWMapWrite.unlock();
        }
        if (atleastOne) {
            aBuffer.append("  Total number of connection in free TLS pool: ");
            aBuffer.append(totalNumberOfFreeSharedTLSConnections);
            aBuffer.append(nl);
        } else {
            aBuffer.append("  No free TLS connections");
            aBuffer.append(nl);
        }
    }

    private void displayUnsharedTLSConnections(StringBuffer aBuffer,
                                               StringBuffer connectionLeakBuffer, long currentTime,
                                               int holdTimeLimit_loc, ArrayList<ToStringStackElements> throwableStackTraceElements) {
        boolean atleastOne;
        /*
         * Inuse UnShared connection information
         */
        aBuffer.append(nl);
        int totalNumberOfUnSharedTLSConnections = 0;
        aBuffer.append("UnShared TLS Connection information");
        aBuffer.append(nl);
        atleastOne = false;
        mcToMCWMapWrite.lock();
        try {
            int mcToMCWSize = mcToMCWMap.size();
            if (mcToMCWSize > 0) {
                Object[] tempObject = mcToMCWMap.values().toArray();
                for (int ti = 0; ti < mcToMCWSize; ++ti) {
                    MCWrapper mcw = (MCWrapper) tempObject[ti];
                    if (mcw.getPoolState() == MCWrapper.ConnectionState_unsharedTLSPool) {
                        aBuffer.append("    ");
                        if (mcw.isStale()
                            || mcw.hasFatalErrorNotificationOccurred(freePool[0].getFatalErrorNotificationTime())
                            || ((this.agedTimeout != -1) && (mcw.hasAgedTimedOut(this.agedTimeoutMillis)))) {
                            aBuffer.append("Connection marked to be destroyed.  Waiting ");
                            aBuffer.append("for transaction end and connection close - ");
                        }
                        if (mcw.getSharedPoolCoordinator() != null) {
                            aBuffer.append(mcw.getSharedPoolCoordinator());
                            aBuffer.append("  ");
                        }
                        aBuffer.append(mcw);
                        if (holdTimeLimit_loc > -1) {
                            dumpHoldTimeAndStackInfo(mcw, aBuffer, connectionLeakBuffer, currentTime, throwableStackTraceElements);
                        }
                        ++totalNumberOfUnSharedTLSConnections;
                        atleastOne = true;
                    }
                }
            }
        } finally {
            mcToMCWMapWrite.unlock();
        }
        if (atleastOne) {
            aBuffer.append("  Total number of connection in unshared TLS pool: ");
            aBuffer.append(totalNumberOfUnSharedTLSConnections);
            aBuffer.append(nl);
        } else {
            aBuffer.append("  No unshared TLS connections");
            aBuffer.append(nl);
        }
    }

    /**
     * Any connection thats been inuse for more than holdTimeLimit time, will have a start time and
     * connection leak information added to the trace. The connection leak information will only
     * be added if it is enabled. By default, connection leak logic is enabled by group WAS.j2c.
     */
    private void dumpHoldTimeAndStackInfo(MCWrapper mcw, StringBuffer aBuffer, StringBuffer connectionLeakBuffer, long currentTime,
                                          ArrayList<ToStringStackElements> mcwInitialStackList) {
        Throwable t = null;
        long startHoldTime = ((com.ibm.ejs.j2c.MCWrapper) mcw).getHoldTimeStart();
        if (startHoldTime != 0) { //  - if start time is zero we don't have enough information to dump the stack information.
            long holdTime = currentTime - startHoldTime;
            if (holdTime > holdTimeLimit * 1000) {
                /*
                 * Remove nl chars from current buffer, normally the length should be one.
                 */
                for (int i = 0; i < nl.length(); ++i) {
                    aBuffer.deleteCharAt(aBuffer.length() - 1);
                }
                //aBuffer.deleteCharAt(aBuffer.length()-1);
                Date startDateTime = new Date(startHoldTime);
                long lastAllTime = ((com.ibm.ejs.j2c.MCWrapper) mcw).getLastAllocationTime();
                String startDateAllocationTime = startHoldTime == lastAllTime ? " Start time same as last allocation time " : " Last allocation time " + new Date(lastAllTime);
                long timeInUseInSeconds = holdTime / 1000;
                /*
                 * Add the new in-use information to the trace.
                 */
                aBuffer.append(" Start time inuse " + startDateTime + " Time inuse " + timeInUseInSeconds + " (seconds) " + startDateAllocationTime + nl);
                t = ((com.ibm.ejs.j2c.MCWrapper) mcw).getInitialRequestStackTrace();
                if (t != null) {
                    /*
                     * Add the connection leak logic to the trace.
                     */
                    connectionLeakBuffer.append("  " + mcw);
                    connectionLeakBuffer.append("     Start time inuse " + startDateTime + " Time inuse " + timeInUseInSeconds + " (seconds)" + nl);
                    connectionLeakBuffer.append("    " + startDateAllocationTime + nl);
                    connectionLeakBuffer.append("       getConnection stack trace information:");

                    StackTraceElement[] mcwInitialStack = t.getStackTrace();
                    boolean stringBuffersEqual = false;
                    int matchingMCStack = -1;
                    int occurred = -1;
                    if (mcwInitialStackList.size() != 0) {
                        // process stack information looking for duplicates.
                        int j = 0;
                        for (ToStringStackElements previousMCWSTE : mcwInitialStackList) {
                            boolean stackLinesEqual = true;
                            if (mcwInitialStack.length == previousMCWSTE.ste.length) {
                                int i = 0;
                                for (Object currentMCWSTE : mcwInitialStack) {
                                    // search through stack comparing each line in stack
                                    if (!currentMCWSTE.toString().equals(previousMCWSTE.ste[i++].toString())) {
                                        stackLinesEqual = false;
                                        break;
                                    }
                                }
                                if (stackLinesEqual == true) {
                                    // stacks are equal
                                    stringBuffersEqual = true;
                                    matchingMCStack = ++j;
                                    occurred = ++previousMCWSTE.numberOfOccurrents;
                                    break;
                                }
                            }
                            ++j;
                        }
                    }
                    if (stringBuffersEqual) {
                        // stack matches existing stack already dumped.
                        connectionLeakBuffer.append("          Matches stack number " + matchingMCStack + " occurred " + occurred + " times" + nl);
                    } else {
                        // add stack and dump the stack information
                        mcwInitialStackList.add(new ToStringStackElements(mcwInitialStack, 1));
                        connectionLeakBuffer.append("          Stack number " + mcwInitialStackList.size() + nl);
                        for (Object o : mcwInitialStack) {
                            connectionLeakBuffer.append("          " + o.toString() + nl);
                        }
                    }
                    connectionLeakBuffer.append(nl);
                }
            }
        }
    }

    /**
     * This calls reclaimConnections to remove unusedTimeout and
     * agedTimeout connections.
     * This will get executed every reapTime seconds.
     */

    public void executeTask() {
        // Add to the colloctorCount
        collectorCount++;
        // If the colloctorCount is greater than 1, then we are already
        // running reclaimConnections.  We do not what more then one running
        // at a time.
        if (collectorCount > 1) {
            // This condition will happen if the reapTime is set to low. Example:
            // The reapTime = 1 second and the reclaim connections takes more than
            // 1 second to finish.
            collectorCount--;
            return;
        } else {
            // Running the reclaimConnections code to remove unusedTimeout and
            // agedTimeout connections.
            reclaimConnections();
            collectorCount--;
        }
    }

    private boolean needToReclaimTLSConnections() {
        boolean removemcw = false;
        // check to see if there is a need to reap any connections.
        mcToMCWMapRead.lock();
        try {
            Collection<MCWrapper> s = mcToMCWMap.values();
            Iterator<MCWrapper> i = s.iterator();
            while (i.hasNext()) {
                MCWrapper mcw = i.next();
                if (mcw.getPoolState() == MCWrapper.ConnectionState_freeTLSPool) {
                    if (agedTimeout != -1) {
                        if (mcw.hasAgedTimedOut(agedTimeoutMillis)) {
                            removemcw = true;
                            break;
                        }
                    }
                    if (!removemcw && unusedTimeout != -1) {
                        if (mcw.hasIdleTimedOut(unusedTimeout * 1000)
                            && (totalConnectionCount.get() > minConnections)) {
                            removemcw = true;
                            break;
                        }
                    }
                }
            }
        } finally {
            mcToMCWMapRead.unlock();
        }
        return removemcw;
    }

    /**
     * Do we need to reclaim connections.
     *
     */
    private boolean needToReclaimConnections() {
        boolean removemcw = false;
        for (int j = 0; j < maxFreePoolHashSize; ++j) {
            synchronized (freePool[j].freeConnectionLockObject) {
                int localtotalConnectionCount = totalConnectionCount.get();
                int mcwlSize = freePool[j].mcWrapperList.size();
                for (int k = 0; k < mcwlSize; ++k) {
                    MCWrapper mcw = (MCWrapper) freePool[j].mcWrapperList.get(k);
                    if (agedTimeout != -1) {
                        if (mcw.hasAgedTimedOut(agedTimeoutMillis)) {
                            removemcw = true;
                            break;
                        }
                    }
                    if (!removemcw && unusedTimeout != -1) {
                        if (mcw.hasIdleTimedOut(unusedTimeout * 1000)
                            && (localtotalConnectionCount > minConnections)) {
                            removemcw = true;
                            break;
                        }
                    }
                }
            }
            if (removemcw) {
                break;
            }
        }
        return removemcw;
    }

    /**
     * Remove connections that have reached there unusedTimeout or
     * agedTimeout values.
     */
    private void reclaimConnections() {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "reclaimConnections");
        }
        if (totalConnectionCount.get() == 0) {
            /*
             * If there are no connection, we have nothing to reclaim
             */
            if (tc.isEntryEnabled()) {
                Tr.exit(this, tc, "reclaimConnections", "Total connection count is zero");
            }
            return;
        }

        // This is only used for debug information
        int totalNumberOfConnectionRemoved = 0;

        /*
         * Look through the free pools for expired Aged and UnusedTimeout values
         */
        for (int j = 0; j < maxFreePoolHashSize; ++j) {
            synchronized (freePool[j].freeConnectionLockObject) {
                boolean removemcw = false;
                int mcwlSize = freePool[j].mcWrapperList.size();
                for (int k = 0; k < mcwlSize; ++k) {
                    MCWrapper mcw = (MCWrapper) freePool[j].mcWrapperList.get(k);
                    if (!removemcw && agedTimeout != -1) {
                        if (mcw.hasAgedTimedOut(agedTimeoutMillis)) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "Aged timeout reclaim connection " + mcw);
                            }
                            removemcw = true;
                        }
                    }
                    if (!removemcw && unusedTimeout != -1) {
                        if (mcw.hasIdleTimedOut(unusedTimeout * 1000)
                            && (this.totalConnectionCount.get() > minConnections)) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "Unused timeout reclaim connection " + mcw);
                            }
                            removemcw = true;
                        }
                    }

                    if (removemcw) {
                        mcWrappersToDestroy.add(mcw);
                        this.totalConnectionCount.decrementAndGet();
                        removemcw = false;
                    }
                } // end of for k loop
                for (int k = 0; k < mcWrappersToDestroy.size(); ++k) {
                    freePool[j].removeMCWrapperFromList(
                                                        mcWrappersToDestroy.get(k),
                                                        mcWrapperDoesExistInFreePool,
                                                        synchronizedAllReady,
                                                        dontNotifyWaiter, // Notifies will be done after cleanupAndDestroyMCWrapper
                                                        // processing
                                                        dontDecrementTotalCounter);
                }

            } // end of synchronized
            if (localConnection_ != null && needToReclaimTLSConnections()) {
                boolean reapTLS = false;
                /*
                 * We can reap connections in the connection pool if we are not in thread sensitive code.
                 */
                updateToTLSPoolInProgress.set(true); // set to true for pausing pool.
                synchronized (updateToTLSPoolInProgressLockObject) {
                    try {
                        sleep(20);
                    } catch (InterruptedException e) {
                    }
                    if (activeTLSRequest.get() > 0) {
                        try {
                            sleep(100);
                        } catch (InterruptedException e) {
                        }
                        if (activeTLSRequest.get() > 0) {
                            // We can not purge tls
                            if (tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "To many active requests to search tls, active request value is " + activeRequest.get());
                            }
                        } else {
                            reapTLS = true;
                        }
                    } else {
                        reapTLS = true;
                    }
                    if (reapTLS) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "Searching tls for a connections to reap.");
                        }

                        Iterator<Entry<MCWrapper, ArrayList<MCWrapper>>> it = tlsArrayLists.entrySet().iterator();
                        while (it.hasNext()) {
                            Entry<MCWrapper, ArrayList<MCWrapper>> e = it.next();
                            MCWrapper mcw = e.getKey();
                            if (mcw.getPoolState() == MCWrapper.ConnectionState_freeTLSPool) {
                                ArrayList<MCWrapper> mh = e.getValue();
                                if (agedTimeout != -1) {
                                    if (mcw.hasAgedTimedOut(agedTimeoutMillis)) {
                                        mh.remove(mcw);
                                        tlsArrayLists.remove(mcw);
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                            ((com.ibm.ejs.j2c.MCWrapper) mcw).setThreadID(((com.ibm.ejs.j2c.MCWrapper) mcw).getThreadID() + "-reclaim-removed");
                                            Tr.debug(this, tc, "removed mcWrapper from thread local " + mcw);
                                        }
                                        mcw.setPoolState(MCWrapper.ConnectionState_noPool);
                                        mcWrappersToDestroy.add(mcw);
                                        this.totalConnectionCount.decrementAndGet();
                                        continue;
                                    }
                                }
                                if (unusedTimeout != -1) {
                                    if (mcw.hasIdleTimedOut(unusedTimeout * 1000)
                                        && (this.totalConnectionCount.get() > minConnections)) {
                                        mh.remove(mcw);
                                        tlsArrayLists.remove(mcw);
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                            ((com.ibm.ejs.j2c.MCWrapper) mcw).setThreadID(((com.ibm.ejs.j2c.MCWrapper) mcw).getThreadID() + "-reclaim-removed");
                                            Tr.debug(this, tc, "removed mcWrapper from thread local " + mcw);
                                        }
                                        mcw.setPoolState(MCWrapper.ConnectionState_noPool);
                                        mcWrappersToDestroy.add(mcw);
                                        this.totalConnectionCount.decrementAndGet();
                                    }
                                }
                            }
                        }
                    }
                    updateToTLSPoolInProgress.set(false); // set to false for releasing pool.
                    updateToTLSPoolInProgressLockObject.notifyAll();
                }
            }

            MCWrapper mcw;
            int destroyListSize = mcWrappersToDestroy.size();
            totalNumberOfConnectionRemoved = totalNumberOfConnectionRemoved + destroyListSize;
            for (int k = destroyListSize; k > 0; --k) {
                mcw = mcWrappersToDestroy.remove(k - 1);
                mcw.setInSharedPool(false); // : This mcwrapper is not in shared pool.
                freePool[j].cleanupAndDestroyMCWrapper(mcw);
            }
            for (int k = 0; k < destroyListSize; ++k) { // free one connection at
                //  at a time.
                synchronized (waiterFreePoolLock) {

                    if (waiterCount > 0) {
                        // there are requests waiting, so notify one of them
                        waiterFreePoolLock.notify();
                    }
                } // end sync
            }
        } // end for (int j
        if (tc.isEntryEnabled()) {
            if (tc.isDebugEnabled()) {
                // This information should only be printed if tc.isEntryEnabled()
                // and tc.isDebugEnabled()
                if (totalNumberOfConnectionRemoved > 0) {
                    Tr.debug(
                             tc,
                             "Total number of connections removed are " + totalNumberOfConnectionRemoved);
                } else {
                    Tr.debug(this, tc, "No connection were removed");
                }
                Tr.debug(this, tc, "Current state of pool:");
                Tr.debug(this, tc, this.toString());
            }
            Tr.exit(this, tc, "reclaimConnections");
        }
    }

    private static class SubjectToString implements PrivilegedAction<String> {

        Subject subject;

        public final void setSubject(Subject s) {
            subject = s;
        }

        @Override
        public String run() {
            return subject.toString();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.j2c.poolmanager.PoolManager#getDBRequestMonitorPool()
     */
    //public Object getDBRequestMonitorPool() {
    //   return dbrequestMonitorPool;
    // }
    /**
     * 173212 - computeHashCode
     *
     * @param subject
     * @param cri
     * @return
     */
    protected final int computeHashCode(Subject subject, ConnectionRequestInfo cri) {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "computeHashCode");
        }

        if (isTracingEnabled && tc.isDebugEnabled()) {

            StringBuffer sbuff = new StringBuffer();
            sbuff.append("computeHashCode for Subject ");
            if (subject == null) {
                sbuff.append("null");
            } else {
                // synchronized (this) {
                SubjectToString subjectToString = new SubjectToString();
                subjectToString.setSubject(subject);
                sbuff.append(AccessController.doPrivileged(subjectToString));
                // }
            }
            if (cri == null) {
                sbuff.append(" and CRI null");
            } else {
                sbuff.append(" and CRI " + cri.toString());
            }
            Tr.debug(this, tc, sbuff.toString());

        }
        /*
         * Future improvement can be if we may
         * want to check subject a cri before
         * computing hashcode again
         */

        int sHash, cHash, hashCode;

        if (subject != null) {

            // RRA currently returns false for re-authentication support
            if (!gConfigProps.raSupportsReauthentication) {
                /*
                 * If we are the rra or re-authentication is not supported,
                 * get the subjects hash code and use it in the computed
                 * hash code value.
                 */
                SubjectHashCode subjectHashCode = new SubjectHashCode();
                subjectHashCode.setSubject(subject);
                sHash = AccessController.doPrivileged(subjectHashCode);
            } else {
                /*
                 * If re-authentication is supported, do
                 * not use the subjects hash code in the computed hash code value.
                 * Use the default value of 1.
                 */
                sHash = 1;
            }

        } else {
            sHash = 1;
        }

        if (cri != null) {
            cHash = cri.hashCode();
        } else {
            cHash = 1;
        }

        hashCode = Math.abs((sHash / 2) + (cHash / 2));

        if (isTracingEnabled && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Subject's hash code is " + sHash + " and the CRI's hash code is " + cHash);
            Tr.debug(this, tc, "computeHashCode, hashCode is " + hashCode);
        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "computeHashCode");
        }

        return hashCode;

    }

    /**
     * Get the mcw from the hash map
     *
     * @param mc
     * @return
     */
    public Object getMCWFromMctoMCWMap(Object mc) {
        Object mcw = null;
        mcToMCWMapRead.lock();
        try {
            mcw = mcToMCWMap.get(mc);
        } finally {
            mcToMCWMapRead.unlock();
        }
        return mcw;
    }

    /**
     *
     * @return
     *
     *         Changed this method to return the a
     *         string instead of the object reference and changed
     *         the method name.
     */
    public String getMCtoMCWMapToString() {
        String mcToMCWMapString;
        mcToMCWMapRead.lock();
        try {
            mcToMCWMapString = mcToMCWMap.toString();
        } finally {
            mcToMCWMapRead.unlock();
        }
        return mcToMCWMapString;
    }

    /**
     * Put the mcw in the hash map
     *
     * @param mc
     * @param mcw
     */
    public void putMcToMCWMap(ManagedConnection mc, MCWrapper mcw) {
        mcToMCWMapWrite.lock();
        try {
            mcToMCWMap.put(mc, mcw);
        } finally {
            mcToMCWMapWrite.unlock();
        }
    }

    /**
     * Remove the mcw from the hash map
     *
     * @param mc, mcw
     */
    public void removeMcToMCWMap(Object mc) {
        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "removeMcToMCWMap");
        }
        mcToMCWMapWrite.lock();
        try {
            mcToMCWMap.remove(mc);
        } finally {
            mcToMCWMapWrite.unlock();
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(this, tc, mcToMCWMap.size() + " connections remaining in mc to mcw table");
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "removeMcToMCWMap");
        }
    }

    /*
     * Add inner class for doPrivileged call
     */
    static class SubjectHashCode implements PrivilegedAction<Integer> {

        Subject subject;

        public final void setSubject(Subject s1) {
            subject = s1;
        }

        @Override
        public Integer run() {

            int privateC = 0;
            int publicC = 0;
            if (subject.getPrivateCredentials() != null) {
                privateC = subject.getPrivateCredentials().hashCode() / 2;
            }
            if (subject.getPublicCredentials() != null) {
                publicC = subject.getPublicCredentials().hashCode() / 2;
            }

            return Integer.valueOf(privateC + publicC);
        }

    }

    /**
     * The caller of the method must call endingAccessToPool()
     */
    protected void requestingAccessToPool() {
        /*
         * Added for holding out connection request while
         * a free or shared pool is being updated
         */
        if (updateToPoolInProgress) {
            synchronized (updateToPoolInProgressLockObject) {
                while (updateToPoolInProgress) {
                    try {
                        /*
                         * Wait for 250 milliseconds and check if update to pool is
                         * finished. The update is complete when updateToPoolInProgress is
                         * changed to false and a updateToPoolInProgressLockObject.notifyAll()
                         * is called.
                         */
                        updateToPoolInProgressLockObject.wait(updateToPoolInProgressSleepTime);
                    } catch (InterruptedException e1) {
                        // We don't need to do anything here.
                        // e1.printStackTrace();
                    }
                }
            }
        }
        activeRequest.incrementAndGet();
        if (updateToPoolInProgress) {
            activeRequest.decrementAndGet();
            synchronized (updateToPoolInProgressLockObject) {
                while (updateToPoolInProgress) {
                    try {
                        /*
                         * Wait for 250 milliseconds and check if update to pool is
                         * finished. The update is complete when updateToPoolInProgress is
                         * changed to false and a updateToPoolInProgressLockObject.notifyAll()
                         * is called.
                         */
                        updateToPoolInProgressLockObject.wait(updateToPoolInProgressSleepTime);
                    } catch (InterruptedException e1) {
                        // We don't need to do anything here.
                        // e1.printStackTrace();
                    }
                }
            }
            activeRequest.incrementAndGet();
        }
    }

    protected void requestingAccessToTLSPool() {
        /*
         * Added for holding out connection request while
         * a free or shared pool is being updated
         */
        if (updateToTLSPoolInProgress.get()) {
            synchronized (updateToTLSPoolInProgressLockObject) {
                while (updateToTLSPoolInProgress.get()) {
                    try {
                        /*
                         * Wait for 250 milliseconds and check if update to pool is
                         * finished. The update is complete when updateToTLSPoolInProgress is
                         * changed to false and a updateToTLSPoolInProgressLockObject.notifyAll()
                         * is called.
                         */
                        updateToTLSPoolInProgressLockObject.wait(updateToTLSPoolInProgressSleepTime);
                    } catch (InterruptedException e1) {
                        // We don't need to do anything here.
                        // e1.printStackTrace();
                    }
                }
            }
        }
        activeTLSRequest.incrementAndGet();
        if (updateToTLSPoolInProgress.get()) {
            activeTLSRequest.decrementAndGet();
            synchronized (updateToTLSPoolInProgressLockObject) {
                while (updateToTLSPoolInProgress.get()) {
                    try {
                        /*
                         * Wait for 250 milliseconds and check if update to pool is
                         * finished. The update is complete when updateToTLSPoolInProgress is
                         * changed to false and a updateToTLSPoolInProgressLockObject.notifyAll()
                         * is called.
                         */
                        updateToTLSPoolInProgressLockObject.wait(updateToTLSPoolInProgressSleepTime);
                    } catch (InterruptedException e1) {
                        // We don't need to do anything here.
                        // e1.printStackTrace();
                    }
                }
            }
            activeTLSRequest.incrementAndGet();
        }
    }

    protected void endingAccessToTLSPool() {
        activeTLSRequest.decrementAndGet();
    }

    public void moveMCWrapperFromUnSharedToShared(Object value1, Object affinity) {
        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "moveMCWrapperFromUnSharedToShared");
        }
        MCWrapper mcWrapper = (MCWrapper) value1;
        int sharedbucket = Math.abs(affinity.hashCode() % maxSharedBuckets);
        // Start - Need code here, but more work is needed.
        if (localConnection_ != null && mcWrapper.getPoolState() == MCWrapper.ConnectionState_unsharedTLSPool) {
            mcWrapper.setPoolState(MCWrapper.ConnectionState_sharedTLSPool);
            mcWrapper.setSharedPoolCoordinator(affinity);
            // This mcwrapper is already in thread local, nothing else to do.
        } else {
            // End - Need code here, but more work is needed.
            sharedPool[sharedbucket].setSharedConnection(affinity, mcWrapper);
            mcWrapper.setInSharedPool(true);
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "moveMCWrapperFromUnSharedToShared");
        }
    }

    public MCWrapper[] getUnSharedPoolConnections() {
        MCWrapper[] m = null;
        int j = 0;
        mcToMCWMapWrite.lock();
        try {
            Object[] o = mcToMCWMap.values().toArray();
            int objectSize = o.length;
            m = new MCWrapper[objectSize];
            MCWrapper mcw = null;
            for (int i = 0; i < objectSize; ++i) {
                mcw = (MCWrapper) o[i];
                if (mcw.getPoolState() == 3) {
                    m[j] = mcw;
                    ++j;
                }
            }
        } finally {
            mcToMCWMapWrite.unlock();
        }
        MCWrapper[] mreturn = new MCWrapper[j];
        System.arraycopy(m, 0, mreturn, 0, j);
        return mreturn;
    }

    /**
     * This method can be submitted to a ScheduledExecutorService to run in the future.
     */
    @Override
    public void run() {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "run", "alarm");
        }

        if (isTracingEnabled && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "run: alarm, Pool contents ==> " + this.toString());
            Tr.debug(this, tc, "reaperThreadStarted: ", reaperThreadStarted);
        }

        if (needToReclaimConnections() || needToReclaimTLSConnections()) {
            // Create thread to reclaim connections
            startReclaimConnectionThread();
        }

        if (isTracingEnabled && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "run: alarm, Pool contents ==> " + this.toString());
            Tr.debug(this, tc, "reaperThreadStarted: ", reaperThreadStarted);
        }

        synchronized (amLockObject) {
            if (this.agedTimeout < 1) {
                if ((totalConnectionCount.get() > minConnections) && alarmThreadCounter.get() >= 0) {
                    createReaperAlarm();
                } else {
                    if (alarmThreadCounter.get() > 0)
                        alarmThreadCounter.decrementAndGet();
                    if (isTracingEnabled && tc.isDebugEnabled())
                        Tr.debug(tc, "Alarm thread was NOT started. Number of alarm threads is " + alarmThreadCounter.get());
                }
            } else {
                if (totalConnectionCount.get() > 0 && alarmThreadCounter.get() >= 0) {
                    createReaperAlarm();
                } else {
                    if (alarmThreadCounter.get() > 0)
                        alarmThreadCounter.decrementAndGet();
                    if (isTracingEnabled && tc.isDebugEnabled())
                        Tr.debug(tc, "Alarm thread was NOT started. Number of alarm threads is " + alarmThreadCounter.get());
                }
            }
        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "run");
        }

    }

    /**
     * QuisceIfPossible should be called when a connection pool is no longer needed,
     * instead of directly calling quiesce(). This method should be invoked the
     * first time by ConnectionFactoryDetailsImpl.stop(). Subsequently it will
     * be invoked at the end of PoolManager.release().
     *
     * This method does three things
     * <UL>
     * <LI> If _quiesce == false, set it to true and set _quiesceTime. </LI>
     * <LI> If _quiesce == false, increment the fatal error value for every
     * FreePool and purge any existing connections. </LI>
     * <LI> If the connection pool is empty, call quiesce to completely clean up. </LI>
     * </UL>
     */
    public void quiesceIfPossible() throws ResourceException {

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "quiesceIfPossible", _quiesce);
        }
        if (!_quiesce) { // only do this the first time we're called
            _quiesce = true;
            _quiesceTime = new Date(System.currentTimeMillis());

            //  Begin block copied from serverShutdown()
            for (int j = 0; j < maxFreePoolHashSize; ++j) {
                synchronized (freePool[j].freeConnectionLockObject) {
                    /*
                     * If a connection gets away, by setting fatalErrorNotificationTime will
                     * guaranty when the connection is returned to the free pool, it will be
                     */
                    freePool[j].incrementFatalErrorValue(j);
                    /*
                     * Destroy as many connections as we can in the free pool
                     */
                    if (freePool[j].mcWrapperList.size() > 0) {
                        freePool[j].cleanupAndDestroyAllFreeConnections();
                    }
                } // end free lock
            } //end for j
              // End block copied from serverShutdown()

        }

        if (totalConnectionCount.get() == 0) {
            quiesce();
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "quiesceIfPossible");
        }

    }

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

    @Override
    public synchronized void propertyChange(PropertyChangeEvent event) {
        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "propertyChange", event.getPropertyName());
        }

        String propName = event.getPropertyName();
        if (propName.equals("orphanConnHoldTimeLimitSeconds")) {
            int value = ((Integer) event.getNewValue()).intValue();
            if (tc.isInfoEnabled()) {
                logPropertyChangeMsg("holdTimeLimit", holdTimeLimit, value);
            }
            holdTimeLimit = value;
        } else if (propName.equals("minConnections")) {
            int value = ((Integer) event.getNewValue()).intValue();
            if (tc.isInfoEnabled()) {
                logPropertyChangeMsg("minConnections", minConnections, value);
            }
            this.minConnections = value;
        } else if (propName.equals("purgePolicy")) {
            PurgePolicy value = (PurgePolicy) (event.getNewValue());
            if (tc.isInfoEnabled()) {
                logPropertyChangeMsg("purgePolicy", purgePolicy.toString(), value.toString());
            }
            this.purgePolicy = value;
        } else if (propName.equals("reapTime")) {
            int value = ((Integer) event.getNewValue()).intValue();
            if (tc.isInfoEnabled()) {
                logPropertyChangeMsg("reapTime", reapTime, value);
            }
            this.reapTime = value;
            checkForStartingReaperThread();
        } else if (propName.equals("unusedTimeoutEnabled")) {
            boolean value = ((Boolean) event.getNewValue()).booleanValue();
            if (tc.isInfoEnabled()) {
                logPropertyChangeMsg("unusedTimeoutEnabled", unusedTimeoutEnabled, value);
            }
            unusedTimeoutEnabled = value;
        } else if (propName.equals("connectionTimeout")) {
            int value = ((Integer) event.getNewValue()).intValue();
            if (tc.isInfoEnabled()) {
                logPropertyChangeMsg("connectionTimeout", connectionTimeout, value);
            }
            connectionTimeout = value;
            displayInfiniteWaitMessage = (connectionTimeout == -1);
            synchronized (waiterFreePoolLock) {
                waiterFreePoolLock.notifyAll();
            }
        } else if (propName.equals("unusedTimeout")) {
            int value = ((Integer) event.getNewValue()).intValue();
            if (tc.isInfoEnabled()) {
                logPropertyChangeMsg("unusedTimeout", unusedTimeout, value);
            }
            unusedTimeout = value;
            if (value > -1) {
                unusedTimeoutEnabled = true;
            } else {
                unusedTimeoutEnabled = false;
            }
            checkForStartingReaperThread();
        } else if (propName.equals("agedTimeout")) {
            int value = ((Integer) event.getNewValue()).intValue();
            if (tc.isInfoEnabled()) {
                logPropertyChangeMsg("agedTimeout", agedTimeout, value);
            }
            this.agedTimeout = value;
            this.agedTimeoutMillis = (long) value * 1000;
            checkForStartingReaperThread();
        } else if (propName.equals("holdTimeLimit")) {
            int value = ((Integer) event.getNewValue()).intValue();
            if (tc.isInfoEnabled()) {
                logPropertyChangeMsg("holdTimeLimit", holdTimeLimit, value);
            }
            this.holdTimeLimit = value;
        } else if (propName.equals("maxNumberOfMCsAllowableInThread")) {
            int value = ((Integer) event.getNewValue()).intValue();
            if (tc.isInfoEnabled()) {
                logPropertyChangeMsg("maxNumberOfMCsAllowableInThread", maxNumberOfMCsAllowableInThread, value);
            }
            this.maxNumberOfMCsAllowableInThread = value;
        } else if (propName.equals("throwExceptionOnMCThreadCheck")) {
            boolean value = ((Boolean) event.getNewValue()).booleanValue();
            if (tc.isInfoEnabled()) {
                logPropertyChangeMsg("throwExceptionOnMCThreadCheck", throwExceptionOnMCThreadCheck, value);
            }
            this.throwExceptionOnMCThreadCheck = value;
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "propertyChange", propName);
        }
    }

    /**
     *
     * Pulled code out of method to be shared by updates for reaper, unused time out or aged time out.
     *
     * If any of the values are updated, we need to check if the reaper needs to be started.
     *
     * @param value
     */
    private void checkForStartingReaperThread() {
        boolean isAnyTracingEnabled = TraceComponent.isAnyTracingEnabled();
        if (isAnyTracingEnabled && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Property change occurred, checking reaper thread status for pool. Current number of alarm threads is " + alarmThreadCounter.get());
        }
        if (gConfigProps.connectionPoolingEnabled) {
            if (this.reapTime > 0) {
                synchronized (amLockObject) {
                    /*
                     * If totalConnectionCount is > minConnections
                     * and agedTimeout is not set, start the reaper
                     * thread.
                     *
                     * If agedTimeout is set and
                     * totalConnectionCount is > 0 start the reaper
                     * thread.
                     */
                    if (this.agedTimeout < 1) {
                        if ((totalConnectionCount.get() > minConnections)) {
                            createReaperAlarm();
                        }
                    } else {
                        if (totalConnectionCount.get() > 0) {
                            createReaperAlarm();
                        }
                    }
                }
            }
        }
        if (isAnyTracingEnabled && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Property change occurred, number of alarm threads is " + alarmThreadCounter.get());
        }
    }

    /**
     * Creates a new alarm thread.
     *
     * Method that combines common code from run() and checkForStartingReaperThread()
     *
     */
    private void createReaperAlarm() {
        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (pmQuiesced) {
            if (isTracingEnabled && tc.isDebugEnabled())
                Tr.debug(tc, " PM has been Quiesced, so cancel old reaper alarm.");
            if (am != null)
                am.cancel(false);
            return;
        }

        if (nonDeferredReaperAlarm) {
            if (isTracingEnabled && tc.isDebugEnabled())
                Tr.debug(this, tc, "Creating non-deferrable alarm for reaper");

            if (am != null) {
                // If an alarm thread already exists, cancel it and create a
                // new one with the latest reapTime
                am.cancel(false);
                if (am.isDone()) {
                    alarmThreadCounter.decrementAndGet();
                    if (isTracingEnabled && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Previous alarm thread cancelled.");

                    alarmThreadCounter.incrementAndGet();
                    try {
                        am = connectorSvc.nonDeferrableSchedXSvcRef.getServiceWithException().schedule(this, reapTime, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        alarmThreadCounter.decrementAndGet();
                        throw new RuntimeException(e);
                    }
                } // end am.isDone() == true
            } // end am != null
            else { // am == null
                if (isTracingEnabled && tc.isDebugEnabled())
                    Tr.debug(this, tc, "No previous alarm thread exists. Creating a new one.");

                alarmThreadCounter.incrementAndGet();
                try {
                    am = connectorSvc.nonDeferrableSchedXSvcRef.getServiceWithException().schedule(this, reapTime, TimeUnit.SECONDS);
                } catch (Exception e) {
                    alarmThreadCounter.decrementAndGet();
                    throw new RuntimeException(e);
                }
            } // am == null
        } // end nonDeferredReaperAlarm == true
        else { // nonDeferredReaperAlarm == false
            if (isTracingEnabled && tc.isDebugEnabled())
                Tr.debug(this, tc, "Creating deferrable alarm for reaper");

            if (am != null) {
                // If an alarm thread already exists, cancel it and create a
                // new one with the latest reapTime
                am.cancel(false);
                if (am.isDone()) {
                    alarmThreadCounter.decrementAndGet();
                    if (isTracingEnabled && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Previous alarm thread cancelled.");

                    alarmThreadCounter.incrementAndGet();
                    try {
                        am = connectorSvc.deferrableSchedXSvcRef.getServiceWithException().schedule(this, reapTime, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        alarmThreadCounter.decrementAndGet();
                        throw new RuntimeException(e);
                    }
                } // end am.isDone() == true
            } // end am != null
            else { // am == null
                if (isTracingEnabled && tc.isDebugEnabled())
                    Tr.debug(this, tc, "No previous alarm thread exists. Creating a new one.");

                alarmThreadCounter.incrementAndGet();
                try {
                    am = connectorSvc.deferrableSchedXSvcRef.getServiceWithException().schedule(this, reapTime, TimeUnit.SECONDS);
                } catch (Exception e) {
                    alarmThreadCounter.decrementAndGet();
                    throw new RuntimeException(e);
                }
            } // am == null
        } // end nonDeferredReaperAlarm == false
    }

    @Override
    public synchronized void vetoableChange(PropertyChangeEvent event) throws PropertyVetoException {
        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "vetoableChange", event.getPropertyName());
        }

        String propName = event.getPropertyName();
        if (propName.equals("maxConnections")) {
            int value = ((Integer) event.getNewValue()).intValue();
            int oldMaxConnection = maxConnections;
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "vetoableChange " + propName +
                                   " new/old " + value + "/" + maxConnections +
                                   " total connection count " + totalConnectionCount.get());
            }

            this.maxConnections = value;
            /*
             * If the total connection count is greater than the new max
             * connections, try to reduce the size of the total connection count
             * to the new max connections.
             */
            if (totalConnectionCount.get() > value && value != 0) {
                /*
                 * At this point we could try to use the reaper, but the reaper
                 * only reduces connection in the free pool based on the unused
                 * time out and aged time out values. When the pool is busy, we
                 * need to be able to mark active connections for deletion to
                 * reduce the size of the pool.
                 *
                 * A better way of getting the pool size down is to go through
                 * the mcToMCWMap and select connections to be destroyed. If
                 * they are in the freePool, they can be destroy immediately, if
                 * they are active connections, they can be marked for deletion
                 * when they are returning to the free pool.
                 *
                 * It will be very difficult to predict how fast the pool will
                 * reduce its size to the new max connection, but the following
                 * code will try to reduce the size as quickly as posible.
                 */
                //if (totalConnectionCount > value) {
                /*
                 * First, try to remove connections from the free pool to
                 * get the max connections down to the new value
                 */
                for (int j = 0; j < maxFreePoolHashSize; ++j) {
                    synchronized (freePool[j].freeConnectionLockObject) {
                        int mcwlSize = freePool[j].mcWrapperList.size();
                        for (int k = mcwlSize - 1; k > -1; --k) {
                            MCWrapper mcw = (MCWrapper) freePool[j].mcWrapperList.remove(k);
                            freePool[j].cleanupAndDestroyMCWrapper(mcw);
                            this.totalConnectionCount.decrementAndGet();
                            if (totalConnectionCount.get() <= value) {
                                break;
                            }
                        }
                        if (totalConnectionCount.get() <= value) {
                            break;
                        }
                    }
                }
                if (totalConnectionCount.get() > value) {
                    /*
                     * Second, mark totalConnectionCount - value number of
                     * connections for deletion whether they are active or not.
                     * Active connection will be destroyed when returned to the
                     * free pool.
                     */
                    int reduceConnectionCount = totalConnectionCount.get() - value;
                    mcToMCWMapWrite.lock();
                    try {
                        Collection<MCWrapper> mcWrappers = mcToMCWMap.values();
                        Iterator<MCWrapper> mcWrapperIt = mcWrappers.iterator();
                        while (mcWrapperIt.hasNext()) {
                            MCWrapper mcw = mcWrapperIt.next();
                            if (!mcw.isParkedWrapper()) {
                                if (totalConnectionCount.get() > value && reduceConnectionCount > 0) {
                                    /*
                                     * Mark the connection for destruction.
                                     */
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(tc, "Reducing pool size " + reduceConnectionCount, mcw);
                                    }
                                    mcw.setDestroyConnectionOnReturn();
                                    --reduceConnectionCount;
                                } else {
                                    break;
                                }
                            }
                        }
                    } finally {
                        mcToMCWMapWrite.unlock();
                    }
                }

                /*
                 * Update the maxConnections
                 */
                if (tc.isInfoEnabled()) {
                    logPropertyChangeMsg("maxConnections", oldMaxConnection, maxConnections);
                }
            }
        } else if (propName.equals("maxFreePoolHashSize")) {
            int value = ((Integer) event.getNewValue()).intValue();
            /*
             * Set updateToPoolInProgress to true
             * to put connection request to sleep while the free pool
             * is being updated.
             */
            updateToPoolInProgress = true;
            synchronized (updateToPoolInProgressLockObject) {

                if (checkForActiveConnections(1)) {
                    updateToPoolInProgress = false;
                    updateToPoolInProgressLockObject.notifyAll();
                    String errorString = "There are active connections in the Pool.  The"
                                         + " freePoolHashValue cannot be changed at this " + "time.";
                    PropertyVetoException e = new PropertyVetoException(errorString, event);
                    throw e;
                } else {
                    /*
                     * Set the new value;
                     */
                    if (tc.isInfoEnabled()) {
                        logPropertyChangeMsg("maxFreePoolHashSize", maxFreePoolHashSize, value);
                    }
                    maxFreePoolHashSize = value;
                    int fatalErrorNotificationTime = freePool[0].getFatalErrorNotificationTime();
                    /*
                     * Create the new free pools.
                     */
                    FreePool[] newFreePool = new FreePool[maxFreePoolHashSize];
                    for (int j = 0; j < maxFreePoolHashSize; ++j) {
                        newFreePool[j] = new FreePool(maxConnections, this, gConfigProps, raClassLoader);
                        newFreePool[j].setFatalErrorNotificationTime(fatalErrorNotificationTime);
                    }
                    mcToMCWMapWrite.lock();
                    try {
                        Collection<MCWrapper> mcWrappers = mcToMCWMap.values();
                        Iterator<MCWrapper> mcWrapperIt = mcWrappers.iterator();
                        while (mcWrapperIt.hasNext()) {
                            MCWrapper mcw = mcWrapperIt.next();
                            if (!mcw.isParkedWrapper()) {
                                int hashCode = computeHashCode(mcw.getSubject(), mcw.getCRI());
                                int hashMapBucket = hashCode % value;
                                mcw.setHashMapBucket(hashMapBucket);
                                mcw.setSubjectCRIHashCode(hashCode);
                                if (mcw.getPoolState() == 1) {
                                    /*
                                     * Need to change the hashcode or free pool buck
                                     * and move this connection in the free pool.
                                     */
                                    newFreePool[hashMapBucket].getMCWrapperList().add(mcw);
                                }
                            }
                        }
                    } finally {
                        mcToMCWMapWrite.unlock();
                    }
                    freePool = newFreePool;
                    updateToPoolInProgress = false;
                    updateToPoolInProgressLockObject.notifyAll();
                }
            }
        } // end maxFreePoolHashSize
        else if (propName.equals("maxSharedBuckets")) {
            int value = ((Integer) event.getNewValue()).intValue();
            /*
             * Set updateToPoolInProgress to true
             * to put connection request to sleep while the free pool
             * is being updated.
             */
            updateToPoolInProgress = true;
            synchronized (updateToPoolInProgressLockObject) {
                if (checkForActiveConnections(1)) {
                    updateToPoolInProgress = false;
                    updateToPoolInProgressLockObject.notifyAll();
                    String errorString = "There are active connections in the Pool.  The"
                                         + " sharedPoolBuckets cannot be changed at this " + "time.";
                    PropertyVetoException e = new PropertyVetoException(errorString, event);
                    throw e;
                } else {
                    /*
                     * Set the new value;
                     */
                    if (tc.isInfoEnabled()) {
                        logPropertyChangeMsg("maxSharedBuckets", maxSharedBuckets, value);
                    }
                    maxSharedBuckets = value;
                    /*
                     * Create the free pool with the new value
                     */
                    SharedPool[] newSharedPool = new SharedPool[maxSharedBuckets];
                    for (int i = 0; i < maxSharedBuckets; ++i) {
                        newSharedPool[i] = new SharedPool(maxConnections, this);
                    }
                    mcToMCWMapWrite.lock();
                    try {
                        Collection<MCWrapper> mcWrappers = mcToMCWMap.values();
                        Iterator<MCWrapper> mcWrapperIt = mcWrappers.iterator();
                        while (mcWrapperIt.hasNext()) {
                            MCWrapper mcw = mcWrapperIt.next();
                            if (!mcw.isParkedWrapper()) {
                                if (mcw.getPoolState() == 2) {
                                    Object coordinator = mcw.getSharedPoolCoordinator();
                                    int sharedPoolBucket = Math.abs(coordinator.hashCode() % value);
                                    /*
                                     * Need to move this connection in the shared pool.
                                     */
                                    newSharedPool[sharedPoolBucket].setSharedConnection(coordinator, mcw);
                                }
                            }
                        }
                    } finally {
                        mcToMCWMapWrite.unlock();
                    }
                    sharedPool = newSharedPool;
                    updateToPoolInProgress = false;
                    updateToPoolInProgressLockObject.notifyAll();
                }
            }
        } else if (propName.equals("numConnectionsPerThreadLocal")) {
            int value = ((Integer) event.getNewValue()).intValue();
            if (this.localConnection_ != null) {
                // thread local already exist.
                if (value > 0) {
                    try {
                        purgePoolContents();
                    } catch (ResourceException e) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "An exception occurred when attempting to purge the pool. " + e);
                        }
                    }
                    /*
                     * We are enabling this function and thread local already exist.
                     */
                    if (tc.isInfoEnabled()) {
                        logPropertyChangeMsg("numConnectionsPerThreadLocal", maxCapacity, value);
                    }
                    this.maxCapacity = value; // set max capacity
                    isThreadLocalConnectionEnabled = true; // enabled thread local
                } else {
                    /*
                     * We are disabling this function and thread local already exist.
                     */
                    if (tc.isInfoEnabled()) {
                        logPropertyChangeMsg("numConnectionsPerThreadLocal", maxCapacity, value);
                    }
                    this.maxCapacity = value; // set max capacity
                    isThreadLocalConnectionEnabled = false; // disable thread local
                    try {
                        purgePoolContents(); // purge the pool to remove connections from thread local
                    } catch (ResourceException e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "An exception occurred when attempting to purge the pool. " + e);
                        }
                    }
                }
            } else {
                /*
                 * thread local connections does not exist. In this case we need to make sure
                 * we do not have an inflight reserve or release request in sensitive code areas.
                 *
                 * To do this, we set updateToPoolInProgress to true
                 * to put connection request to sleep while the free pool
                 * is being updated. We can only create thread local connections if we have no active
                 * connection requests.
                 */
                updateToPoolInProgress = true;
                synchronized (updateToPoolInProgressLockObject) {
                    if (checkForActiveConnections(1)) {
                        updateToPoolInProgress = false;
                        updateToPoolInProgressLockObject.notifyAll();
                        String errorString = "There are active connections in the Pool.  The"
                                             + " numConnectionsPerThreadLocal cannot be changed at this " + "time.";
                        PropertyVetoException e = new PropertyVetoException(errorString, event);
                        throw e;
                    } else {
                        if (value > 0) {
                            try {
                                purgePoolContents();
                            } catch (ResourceException e) {
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(this, tc, "An exception occurred when attempting to purge the pool. " + e);
                                }
                            }
                            this.maxCapacity = value;
                            this.localConnection_ = new WSThreadLocal<ArrayList<MCWrapper>>() {
                                @Override
                                protected ArrayList<MCWrapper> initialValue() {
                                    return new ArrayList<MCWrapper>(2);
                                }
                            };
                            isThreadLocalConnectionEnabled = true; // enabled thread local
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "PoolManager: Thread local connection ENABLED");
                            }
                        }
                        if (tc.isInfoEnabled()) {
                            logPropertyChangeMsg("numConnectionsPerThreadLocal", maxCapacity, value);
                        }
                        updateToPoolInProgress = false;
                        updateToPoolInProgressLockObject.notifyAll();
                    }
                }
            }
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "vetoableChange");
        }
    }

    private void logPropertyChangeMsg(String msgKey, boolean oldValue, boolean newValue) {
        if (oldValue == newValue)
            return;
        if (tc.isDebugEnabled()) {
            Tr.debug(this, tc, msgKey, oldValue, newValue, gConfigProps.cfName);
        }
    }

    private void logPropertyChangeMsg(String msgKey, int oldValue, int newValue) {
        if (oldValue == newValue)
            return;
        if (tc.isDebugEnabled()) {
            Tr.debug(this, tc, msgKey, oldValue, newValue, gConfigProps.cfName);
        }
    }

    private void logPropertyChangeMsg(String msgKey, String oldValue, String newValue) {
        if (oldValue.equals(newValue))
            return;
        if (tc.isDebugEnabled()) {
            Tr.debug(this, tc, msgKey, oldValue, newValue, gConfigProps.cfName);
        }
    }

    public final J2CGlobalConfigProperties getGConfigProps() {
        return gConfigProps;
    }

    protected static void logLTCSerialReuseInfo(Object affinity, String pmiName, MCWrapper mcWrapperTemp, PoolManager pm) {
        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();
        if (isTracingEnabled && tc.isDebugEnabled()) {
            Tr.debug(tc, "allocateConnection_Common:  HandleCount = " + mcWrapperTemp.getHandleCount());
        }

        if (pm.logSerialReuseMessage) {
            Tr.info(tc, "ATTEMPT_TO_SHARE_LTC_CONNECTION_J2CA0086", new Object[] { mcWrapperTemp, pmiName });
            pm.logSerialReuseMessage = false;
        }

        if (isTracingEnabled && tc.isDebugEnabled()) {
            Tr.debug(tc, "Attempt to share connection within LTC (J2CA0086)");
            Tr.debug(tc, "mcWrapper = " + mcWrapperTemp);
            Tr.debug(tc, "pmiName   = " + pmiName);
        }
    }

    private static boolean isCRIsMatching(ConnectionRequestInfo cri, MCWrapper mcWrapperTemp) {
        ManagedConnection mc = mcWrapperTemp.getManagedConnection();
        ConnectionRequestInfo mcWrapperCRI = mc instanceof WSManagedConnection ? ((WSManagedConnection) mc).getConnectionRequestInfo() : mcWrapperTemp.getCRI();
        return cri == mcWrapperCRI || cri != null && cri.equals(mcWrapperCRI);
    }

    protected boolean isSubjectsMatching(Subject subject, MCWrapper mcWrapperTemp) {
        boolean subjectMatch = false;
        Subject mcWrapperSubject = mcWrapperTemp.getSubject();

        if ((subject == null) && (mcWrapperSubject == null)) {
            subjectMatch = true;
        } else {

            if ((subject != null) && (mcWrapperSubject != null)) {

                Equals e = new Equals();
                e.setSubjects(subject, mcWrapperTemp.getSubject());

                if (AccessController.doPrivileged(e)) {
                    subjectMatch = true;
                }

            }

        }
        return subjectMatch;
    }

    protected static boolean isBranchCouplingCompatible(int commitPriority,
                                                        int branchCoupling,
                                                        MCWrapper mcWrapperTemp) {
        boolean cmConfigDataIsCompatible = false;
        ConnectionManager cm = ((com.ibm.ejs.j2c.MCWrapper) mcWrapperTemp).getCm();
        ResourceRefInfo resRefInfo = cm.getResourceRefInfo();
        if (resRefInfo.getCommitPriority() == commitPriority) {
            int tempBranchCoupling = resRefInfo.getBranchCoupling();
            if (branchCoupling == tempBranchCoupling) { // Check if they match first for performance
                cmConfigDataIsCompatible = true;
            } else {
                cmConfigDataIsCompatible = cm.matchBranchCoupling(branchCoupling, tempBranchCoupling, ((com.ibm.ejs.j2c.MCWrapper) mcWrapperTemp).get_managedConnectionFactory());
            }
        }
        return cmConfigDataIsCompatible;
    }

    /**
     * Invoke Thread.sleep indirectly, to temporarily avoid FindBugs complaints about existing code.
     *
     * @param ms number of milliseconds
     * @throws InterruptedException if interrupted
     */
    private void sleep(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    @Override
    public String getUniqueId() {
        return this.gConfigProps.getXpathId();
    }

    @Override
    public boolean getParkedValue() {
        return false;
    }

    @Override
    public String getJNDIName() {
        return this.gConfigProps.getJNDIName();
    }

    /**
     * @return
     */
    protected AtomicInteger getTotalConnectionCount() {
        return totalConnectionCount;
    }
}

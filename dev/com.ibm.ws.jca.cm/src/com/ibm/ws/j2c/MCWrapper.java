/*******************************************************************************
 * Copyright (c) 1997, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.j2c;

import java.util.BitSet;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.security.auth.Subject;

public interface MCWrapper {

    /*
     * This will indicate where the connection is in the pool
     * code.
     *
     * Valid states are:
     * 0 = Not in any pool, currently in transition.
     * 1 = In free pool
     * 2 = In shared pool
     * 3 = In unshared pool
     * 4 = In waiter pool
     * 5 = In shared thread local storage pool
     * 6 = In free thread local storage pool
     * 7 = In unshared thread local storage pool
     * 9 = In the parked connection pool, if one is needed, there should only be one in this pool
     */
    public static final int ConnectionState_noPool = 0;
    public static final int ConnectionState_freePool = 1;
    public static final int ConnectionState_sharedPool = 2;
    public static final int ConnectionState_unsharedPool = 3;
    public static final int ConnectionState_waiterPool = 4;
    public static final int ConnectionState_sharedTLSPool = 5;
    public static final int ConnectionState_freeTLSPool = 6;
    public static final int ConnectionState_unsharedTLSPool = 7;
    public static final int ConnectionState_parkedPool = 9;

    /**
     * Sets the <code>ManagedConnection</code>. Required.
     * The <code>ManagedConnection</code> is required by the <code>MCWrapper</code> and
     * related objects. Once the ManagedConnection has been set, subsequent calls to
     * this method will cause an exception.
     * <p>
     * Handles PMI notification of ManagedConnection creation.
     */
    void setManagedConnection(ManagedConnection mc);

    ManagedConnection getManagedConnection();

    /**
     * We must remove the managed connection from the mcToMCWMap
     * reguardless of its state. Therefore this method does not
     * check the state, it just returns the managed connection.
     */
    ManagedConnection getManagedConnectionWithoutStateCheck();

    /**
     * Calls <code>cleanup()</code> on the wrappered <code>ManagedConnection<code>.
     * Also reinitializes its own state such that it may be placed in the PoolManagers
     * pool for reuse. All objects will be retained. <code>cleanup()</code> will
     * as be propogated to all associated objects, such as the transaction wrappers,
     * so that they may reset their state for reuse.
     * <p>
     * PMI notification of freeing the ManagedConnection back to pool.
     * <p>
     * unusedTimeStamp will be updated.
     *
     * @exception ResourceException
     */
    void cleanup() throws ResourceException;

    /**
     * Calls <code>destroy()</code> on the wrappered <code>ManagedConnection<code>.
     * Also nulls out its reference to the ManagedConnection and any other connection
     * related variable and resets internal state. <code>destroy()</code> will
     * as be propogated to all associated objects, such as the transaction wrappers,
     * so that they may release any connection related resources.
     * <p>
     * Handles PMI notification of ManagedConnection destruction.
     *
     * @exception ResourceException
     */
    void destroy() throws ResourceException;

    /**
     * Marks this connection for destruction. Used as part of purging the entire
     * connection pool. Called on connections which are in use at the time of
     * the pool being purged.
     * <p>
     * If this object is marked stale when cleanup() is called, a call to destroy() will
     * happen under the covers.
     * <p>
     * The <code>ConnectionManager</code> will also check the isStale value when ever it
     * processes a ConnectionError event and the pool purge policy is set to EntirePool.
     * If this wrapper is marked stale, it will know it does not need to initiate a
     * purge of the pool. This avoid reentrancy problems.
     */
    void markStale();

    boolean hasFatalErrorNotificationOccurred(int fatalErrorNotificationTime);

    boolean hasAgedTimedOut(long timeoutValue);

    boolean hasIdleTimedOut(long timeoutValue);

    void markInUse();

    boolean isStale();

    Object getSharedPool();

    void setSharedPool(Object sharedPool);

    String getStateString();

    long getCreatedTimeStamp();

    long getUnusedTimeStamp();

    void decrementHandleCount();

    void incrementHandleCount();

    int getHandleCount();

    Object getMCWrapperList();

    void setMCWrapperList(Object userData);

    void setSharedPoolCoordinator(Object sharedPoolCoordinator);

    Object getSharedPoolCoordinator();

    int getHashMapBucket();

    void setHashMapBucket(int hashMapBucket);

    void clearMCWrapper();

    void setParkedWrapper(boolean parkedWrapper);

    boolean isParkedWrapper();

    /**
     * Set the pending user data, if the mc.getConnection
     * is successful, we need set the this.userData to
     * this.userDataPending. The switch is done in
     * updateUserDataForReauthentication.
     *
     * @param userDataPending
     */
    void setSupportsReAuth(boolean supportsReAuth);

    void setHashMapBucketReAuth(int hashMapBucket);

    int getHashMapBucketReAuth();

    void setSubject(Subject subject);

    Subject getSubject();

    void setCRI(ConnectionRequestInfo cri);

    ConnectionRequestInfo getCRI();

    public boolean isLogWriterSet();

    public void setLogWriterSet(boolean b);

    /**
     * This will indicate where the connection is in the pool
     * code.
     *
     * Valid states are:
     * 0 = Not in any pool, currently in transition.
     * 1 = In free pool
     * 2 = In shared pool
     * 3 = In unshared pool
     * 4 = In waiter pool
     * 5 = In shared thread local storage pool
     * 6 = In free thread local storage pool
     * 7 = In unshared thread local storage pool
     * 9 = In the parked connection pool, if one is needed, there should only be one in this pool
     *
     * Defined Constants:
     * ConnectionState_noPool = 0;
     * ConnectionState_freePool = 1;
     * ConnectionState_sharedPool = 2;
     * ConnectionState_unsharedPool = 3;
     * ConnectionState_waiterPool = 4;
     * ConnectionState_sharedTLSPool = 5;
     * ConnectionState_freeTLSPool = 6;
     * ConnectionState_unsharedTLSPool = 7;
     * ConnectionState_parkedPool = 9;
     */
    public int getPoolState();

    /**
     * This will indicate where the connection is in the pool
     * code.
     *
     * Valid states are:
     * 0 = Not in any pool, currently in transition.
     * 1 = In free pool
     * 2 = In shared pool
     * 3 = In unshared pool
     * 4 = In waiter pool
     * 5 = In shared thread local storage pool
     * 6 = In free thread local storage pool
     * 7 = In unshared thread local storage pool
     * 9 = In the parked connection pool, if one is needed, there should only be one in this pool
     *
     * Defined Constants:
     * ConnectionState_noPool = 0;
     * ConnectionState_freePool = 1;
     * ConnectionState_sharedPool = 2;
     * ConnectionState_unsharedPool = 3;
     * ConnectionState_waiterPool = 4;
     * ConnectionState_sharedTLSPool = 5;
     * ConnectionState_freeTLSPool = 6;
     * ConnectionState_unsharedTLSPool = 7;
     * ConnectionState_parkedPool = 9;
     */
    public void setPoolState(int i);

    void setInSharedPool(boolean value1);

    boolean isInSharedPool();

    /**
     * Support for 1PC Optimization
     *
     * @param b
     */
    void setConnectionSynchronizationProvider(boolean b);

    /**
     * This method is used for marking a connection to destroy.
     * The connection state does not matter. The connection still
     * can be useable. When the connection is returned to the
     * free pool, this connection will be cleaned up and destroyed.
     *
     * This method may be called when total connection count is being
     * decreased.
     */
    void setDestroyConnectionOnReturn();

    /**
     * Changing the fatal error code from using a long with
     * a value of currentTimeMillis to an int value.
     *
     * This will perform better than comparing to long values
     * and I need a way to dynamically set the fatal error value
     * without adding synchronization.
     *
     * Initially this value will be 1 more then the free pools
     * fatal error value. When a fatal error occurs, the value in
     * the free pool is increased by 1. When this fatal error value
     * is check on the connection return to the free pool, if it is
     * not greater than the free pools fatal error value, the connection
     * will be cleaned up and destroyed.
     */
    void setFatalErrorValue(int value);

    /**
     * A destroy state of true for a connection will result in the connection being
     * destoryed when returned to the free pool. In addition, ANY connection pool requests
     * for this connection will result in a ResourceException.
     *
     * @return
     */
    boolean isDestroyState();

    /**
     * A destroy state of true for a connection will result in the connection being
     * destoryed when returned to the free pool. In addition, ANY connection pool requests
     * for this connection will result in a ResourceException.
     *
     * The destroyState is set to true when this method is used.
     *
     * @return
     */
    void setDestroyState();

    /**
     * Store the combined Subject and CRI hash code
     * to improve performance.
     *
     * @param hashCode
     */
    void setSubjectCRIHashCode(int hashCode);

    /**
     * Using the rule, if two objects are equal, there
     * hash codes must be equal, therefore, if the hash codes
     * are not equal, the objects are not equal.
     *
     * By comparing hash codes first before using equal methods,
     * performance will increase significantly for changing requests
     * and slow down insignificantly for non-changing requests.
     *
     * @return
     */
    int getSubjectCRIHashCode();

    /**
     * ShouldBeDestroyed is used by the free pool when a connection is returned to
     * the free pool. It replaces a check for isStale() to account for the new
     * _transactionErrorOccurred flag.
     *
     * @return true if this MCWrapper should be destroyed.
     */
    boolean shouldBeDestroyed();

    /**
     * Marks this MCWrapper for destruction as a result of calling purgePoolContents().
     */
    void markForPurgeDestruction();

    /**
     * Returns whether or not this MCWrapper is set to be destroyed from purgePoolContents()
     *
     * @return true if this MCWrapper is to be destroyed as a result of purgePoolContents(), else false
     */
    boolean isMarkedForPurgeDestruction();
}

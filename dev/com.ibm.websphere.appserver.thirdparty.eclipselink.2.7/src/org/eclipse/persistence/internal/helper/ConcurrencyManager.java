/*
 * Copyright (c) 1998, 2021 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021 IBM Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation from Oracle TopLink
package org.eclipse.persistence.internal.helper;

import java.io.Serializable;
import java.io.StringWriter;
import java.security.AccessController;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.persistence.config.SystemProperties;
import org.eclipse.persistence.exceptions.ConcurrencyException;
import org.eclipse.persistence.internal.identitymaps.CacheKey;
import org.eclipse.persistence.internal.localization.ToStringLocalization;
import org.eclipse.persistence.internal.localization.TraceLocalization;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedGetSystemProperty;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;

/**
 * INTERNAL:
 * <p>
 * <b>Purpose</b>: To maintain concurrency for a particular task.
 * It is a wrappers of a semaphore that allows recursive waits by a single thread.
 * <p>
 * <b>Responsibilities</b>:
 * <ul>
 * <li> Keep track of the active thread.
 * <li> Wait all other threads until the first thread is done.
 * <li> Maintain the depth of the active thread.
 * </ul>
 */
public class ConcurrencyManager implements Serializable {

    public static final Map<Thread, DeferredLockManager> DEFERRED_LOCK_MANAGERS = initializeDeferredLockManagers();
    // Used for logging in case of dead-lock detection. Unique instance id.
    private static final AtomicLong CONCURRENCY_MANAGER_ID = new AtomicLong(0);

    protected static boolean shouldTrackStack = PrivilegedAccessHelper.getSystemProperty(SystemProperties.RECORD_STACK_ON_LOCK) != null;

    protected AtomicInteger numberOfReaders;
    protected AtomicInteger depth;
    protected AtomicInteger numberOfWritersWaiting;
    protected volatile transient Thread activeThread;

    protected boolean lockedByMergeManager;
    protected Exception stack;

    // Extended logging info fields
    // Unique ID assigned each time when a new instance of a concurrency manager is created
    private final long concurrencyManagerId = CONCURRENCY_MANAGER_ID.incrementAndGet();
    // Creation date
    private final Date concurrencyManagerCreationDate = new Date();
    // In case if two threads are working on the exact same entity that leads to both threads wanting to release the same cache key
    // there is tracking each increment of number of readers and their release.
    private final AtomicLong totalNumberOfKeysAcquiredForReading = new AtomicLong(0);
    // Same as totalNumberOfKeysAcquiredForReading but incremented each time the cache key is suffering to release cache key.
    private final AtomicLong totalNumberOfKeysReleasedForReading = new AtomicLong(0);
    // Total number of times the cache key caused a blow up because it suffered a release of cache key when the counter
    // was set to 0. It should happen if an entity being shared by two threads.
    private final AtomicLong totalNumberOfKeysReleasedForReadingBlewUpExceptionDueToCacheKeyHavingReachedCounterZero = new AtomicLong(0);

    private static final Map<Thread, ConcurrencyManager> THREADS_TO_WAIT_ON_ACQUIRE_READ_LOCK = new ConcurrentHashMap<>();
    private static final Map<Thread, String> THREADS_TO_WAIT_ON_ACQUIRE_READ_LOCK_NAME_OF_METHOD_CREATING_TRACE = new ConcurrentHashMap<>();
    private static final Map<Thread, ConcurrencyManager> THREADS_TO_WAIT_ON_ACQUIRE = new ConcurrentHashMap<>();
    private static final Map<Thread, String> THREADS_TO_WAIT_ON_ACQUIRE_NAME_OF_METHOD_CREATING_TRACE = new ConcurrentHashMap<>();
    // Holds as a keys threads that needed to acquire one or more read locks on different cache keys.
    private static final Map<Thread, ReadLockManager> READ_LOCK_MANAGERS = new ConcurrentHashMap<>();
    private static final Set<Thread> THREADS_WAITING_TO_RELEASE_DEFERRED_LOCKS = ConcurrentHashMap.newKeySet();
    private static final Map<Thread, String> THREADS_WAITING_TO_RELEASE_DEFERRED_LOCKS_BUILD_OBJECT_COMPLETE_GOES_NOWHERE = new ConcurrentHashMap<>();

    private static final String ACQUIRE_METHOD_NAME = ConcurrencyManager.class.getName() + ".acquire(...)";
    private static final String ACQUIRE_READ_LOCK_METHOD_NAME = ConcurrencyManager.class.getName() + ".acquireReadLock(...)";
    private static final String ACQUIRE_WITH_WAIT_METHOD_NAME = ConcurrencyManager.class.getName() + ".acquireWithWait(...)";
    private static final String ACQUIRE_DEFERRED_LOCK_METHOD_NAME = ConcurrencyManager.class.getName() + ".acquireDeferredLock(...)";

    /**
     * Initialize the newly allocated instance of this class.
     * Set the depth to zero.
     */
    public ConcurrencyManager() {
        this.depth  = new AtomicInteger(0);
        this.numberOfReaders = new AtomicInteger(0);
        this.numberOfWritersWaiting = new AtomicInteger(0);
    }

    /**
     * Wait for all threads except the active thread.
     * If the active thread just increment the depth.
     * This should be called before entering a critical section.
     */
    public void acquire() throws ConcurrencyException {
        this.acquire(false);
    }

    /**
     * Wait for all threads except the active thread.
     * If the active thread just increment the depth.
     * This should be called before entering a critical section.
     * called with true from the merge process, if true then the refresh will not refresh the object
     */
    public synchronized void acquire(boolean forMerge) throws ConcurrencyException {
        //Flag the time when we start the while loop
        final long whileStartTimeMillis = System.currentTimeMillis();
        Thread currentThread = Thread.currentThread();
        DeferredLockManager lockManager = getDeferredLockManager(currentThread);
        ReadLockManager readLockManager = getReadLockManager(currentThread);

        // Waiting to acquire cache key will now start on the while loop
        // NOTE: this step bares no influence in acquiring or not acquiring locks
        // is just storing debug metadata that we can use when we detect the system is frozen in a dead lock
        final boolean currentThreadWillEnterTheWhileWait = ((this.activeThread != null) || (this.numberOfReaders.get() > 0)) && (this.activeThread != currentThread);
        if(currentThreadWillEnterTheWhileWait) {
            putThreadAsWaitingToAcquireLockForWriting(currentThread, ACQUIRE_METHOD_NAME);
        }
        while (((this.activeThread != null) || (this.numberOfReaders.get() > 0)) && (this.activeThread != Thread.currentThread())) {
            // This must be in a while as multiple threads may be released, or another thread may rush the acquire after one is released.
            try {
                this.numberOfWritersWaiting.incrementAndGet();
                wait(ConcurrencyUtil.SINGLETON.getAcquireWaitTime());
                // Run a method that will fire up an exception if we having been sleeping for too long
                ConcurrencyUtil.SINGLETON.determineIfReleaseDeferredLockAppearsToBeDeadLocked(this, whileStartTimeMillis, lockManager, readLockManager, ConcurrencyUtil.SINGLETON.isAllowInterruptedExceptionFired());
            } catch (InterruptedException exception) {
                // If the thread is interrupted we want to make sure we release all of the locks the thread was owning
                releaseAllLocksAcquiredByThread(lockManager);
                // Improve concurrency manager metadata
                // Waiting to acquire cache key is is over
                if (currentThreadWillEnterTheWhileWait) {
                    removeThreadNoLongerWaitingToAcquireLockForWriting(currentThread);
                }
                throw ConcurrencyException.waitWasInterrupted(exception.getMessage());
            } finally {
                // Since above we increments the number of writers
                // whether or not the thread is exploded by an interrupt
                // we need to make sure we decrement the number of writer to not allow the code to be corrupted
                this.numberOfWritersWaiting.decrementAndGet();
            }
        } // end of while loop
        // Waiting to acquire cache key is is over
        if(currentThreadWillEnterTheWhileWait) {
            removeThreadNoLongerWaitingToAcquireLockForWriting(currentThread);
        }
        if (this.activeThread == null) {
            this.activeThread = Thread.currentThread();
            if (shouldTrackStack){
                this.stack = new Exception();
            }
        }
        this.lockedByMergeManager = forMerge;
        this.depth.incrementAndGet();
    }

    /**
     * If the lock is not acquired already acquire it and return true.
     * If it has been acquired already return false
     * Added for CR 2317
     */
    public boolean acquireNoWait() throws ConcurrencyException {
        return acquireNoWait(false);
    }

    /**
     * If the lock is not acquired already acquire it and return true.
     * If it has been acquired already return false
     * Added for CR 2317
     * called with true from the merge process, if true then the refresh will not refresh the object
     */
    public synchronized boolean acquireNoWait(boolean forMerge) throws ConcurrencyException {
        if ((this.activeThread == null && this.numberOfReaders.get() == 0) || (this.activeThread == Thread.currentThread())) {
            //if I own the lock increment depth
            acquire(forMerge);
            return true;
        } else {
            return false;
        }
    }

    /**
     * If the lock is not acquired already acquire it and return true.
     * If it has been acquired already return false
     * Added for CR 2317
     * called with true from the merge process, if true then the refresh will not refresh the object
     */
    public synchronized boolean acquireWithWait(boolean forMerge, int wait) throws ConcurrencyException {
        final Thread currentThread = Thread.currentThread();
        if ((this.activeThread == null && this.numberOfReaders.get() == 0) || (this.activeThread == currentThread)) {
            // if I own the lock increment depth
            acquire(forMerge);
            return true;
        } else {
            try {
                putThreadAsWaitingToAcquireLockForWriting(currentThread, ACQUIRE_WITH_WAIT_METHOD_NAME);
                wait(wait);
            } catch (InterruptedException e) {
                return false;
            } finally {
                removeThreadNoLongerWaitingToAcquireLockForWriting(currentThread);
            }
            if ((this.activeThread == null && this.numberOfReaders.get() == 0)
                    || (this.activeThread == currentThread)) {
                acquire(forMerge);
                return true;
            }
            return false;
        }
    }

    /**
     * If the activeThread is not set, acquire it and return true.
     * If the activeThread is set, it has been acquired already, return false.
     * Added for Bug 5840635
     * Call with true from the merge process, if true then the refresh will not refresh the object.
     */
    public synchronized boolean acquireIfUnownedNoWait(boolean forMerge) throws ConcurrencyException {
        // Only acquire lock if active thread is null. Do not check current thread.
        if (this.activeThread == null && this.numberOfReaders.get() == 0) {
             // if lock is unowned increment depth
            acquire(forMerge);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Add deferred lock into a hashtable to avoid deadlock
     */
    public void acquireDeferredLock() throws ConcurrencyException {
        Thread currentThread = Thread.currentThread();
        DeferredLockManager lockManager = getDeferredLockManager(currentThread);
        ReadLockManager readLockManager = getReadLockManager(currentThread);
        if (lockManager == null) {
            lockManager = new DeferredLockManager();
            putDeferredLock(currentThread, lockManager);
        }
        lockManager.incrementDepth();
        synchronized (this) {
            final long whileStartTimeMillis = System.currentTimeMillis();
            final boolean currentThreadWillEnterTheWhileWait = this.numberOfReaders.get() != 0;
            if(currentThreadWillEnterTheWhileWait) {
                putThreadAsWaitingToAcquireLockForWriting(currentThread, ACQUIRE_DEFERRED_LOCK_METHOD_NAME);
            }
            while (this.numberOfReaders.get() != 0) {
                // There are readers of this object, wait until they are done before determining if
                //there are any other writers.  If not we will wait on the readers for acquire.  If another
                //thread is also waiting on the acquire then a deadlock could occur.  See bug 3049635
                //We could release all active locks before releasing deferred but the object may not be finished building
                //we could make the readers get a hard lock, but then we would just build a deferred lock even though
                //the object is not being built.
                try {
                    this.numberOfWritersWaiting.incrementAndGet();
                    wait(ConcurrencyUtil.SINGLETON.getAcquireWaitTime());
                    ConcurrencyUtil.SINGLETON.determineIfReleaseDeferredLockAppearsToBeDeadLocked(this, whileStartTimeMillis, lockManager, readLockManager, ConcurrencyUtil.SINGLETON.isAllowInterruptedExceptionFired());
                } catch (InterruptedException exception) {
                    // If the thread is interrupted we want to make sure we release all of the locks the thread was owning
                    releaseAllLocksAcquiredByThread(lockManager);
                    if (currentThreadWillEnterTheWhileWait) {
                        removeThreadNoLongerWaitingToAcquireLockForWriting(currentThread);
                    }
                    throw ConcurrencyException.waitWasInterrupted(exception.getMessage());
                } finally {
                    this.numberOfWritersWaiting.decrementAndGet();
                }
            }
            if (currentThreadWillEnterTheWhileWait) {
                removeThreadNoLongerWaitingToAcquireLockForWriting(currentThread);
            }
            if ((this.activeThread == currentThread) || (!isAcquired())) {
                lockManager.addActiveLock(this);
                acquire();
            } else {
                lockManager.addDeferredLock(this);
                if (AbstractSessionLog.getLog().shouldLog(SessionLog.FINER) && this instanceof CacheKey) {
                    AbstractSessionLog.getLog().log(SessionLog.FINER, SessionLog.CACHE, "acquiring_deferred_lock", ((CacheKey)this).getObject(), currentThread.getName());
                }
            }
        }
    }

    /**
     * Check the lock state, if locked, acquire and release a deferred lock.
     * This optimizes out the normal deferred-lock check if not locked.
     */
    public void checkDeferredLock() throws ConcurrencyException {
        // If it is not locked, then just return.
        if (this.activeThread == null) {
            return;
        }
        acquireDeferredLock();
        releaseDeferredLock();
    }

    /**
     * Check the lock state, if locked, acquire and release a read lock.
     * This optimizes out the normal read-lock check if not locked.
     */
    public void checkReadLock() throws ConcurrencyException {
        // If it is not locked, then just return.
        if (this.activeThread == null) {
            return;
        }
        acquireReadLock();
        releaseReadLock();
    }

    /**
     * Wait on any writer.
     * Allow concurrent reads.
     */
    public synchronized void acquireReadLock() throws ConcurrencyException {
        final Thread currentThread = Thread.currentThread();
        final long whileStartTimeMillis = System.currentTimeMillis();
        DeferredLockManager lockManager = getDeferredLockManager(currentThread);
        ReadLockManager readLockManager = getReadLockManager(currentThread);
        final boolean currentThreadWillEnterTheWhileWait = (this.activeThread != null) && (this.activeThread != currentThread);
        if (currentThreadWillEnterTheWhileWait) {
            putThreadAsWaitingToAcquireLockForReading(currentThread, ACQUIRE_READ_LOCK_METHOD_NAME);
        }
        // Cannot check for starving writers as will lead to deadlocks.
        while ((this.activeThread != null) && (this.activeThread != Thread.currentThread())) {
            try {
                wait(ConcurrencyUtil.SINGLETON.getAcquireWaitTime());
                ConcurrencyUtil.SINGLETON.determineIfReleaseDeferredLockAppearsToBeDeadLocked(this, whileStartTimeMillis, lockManager, readLockManager, ConcurrencyUtil.SINGLETON.isAllowInterruptedExceptionFired());
            } catch (InterruptedException exception) {
                releaseAllLocksAcquiredByThread(lockManager);
                if (currentThreadWillEnterTheWhileWait) {
                    removeThreadNoLongerWaitingToAcquireLockForReading(currentThread);
                }
                throw ConcurrencyException.waitWasInterrupted(exception.getMessage());
            }
        }
        if (currentThreadWillEnterTheWhileWait) {
            removeThreadNoLongerWaitingToAcquireLockForReading(currentThread);
        }
        try {
            addReadLockToReadLockManager();
        } finally {
            this.numberOfReaders.incrementAndGet();
            this.totalNumberOfKeysAcquiredForReading.incrementAndGet();
        }
    }

    /**
     * If this is acquired return false otherwise acquire readlock and return true
     */
    public synchronized boolean acquireReadLockNoWait() {
        if ((this.activeThread == null) || (this.activeThread == Thread.currentThread())) {
            acquireReadLock();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Return the active thread.
     */
    public Thread getActiveThread() {
        return activeThread;
    }

    /**
     * Return the deferred lock manager from the thread
     */
    public static DeferredLockManager getDeferredLockManager(Thread thread) {
        return getDeferredLockManagers().get(thread);
    }

    /**
     * Return the deferred lock manager hashtable (thread - DeferredLockManager).
     */
    protected static Map<Thread, DeferredLockManager> getDeferredLockManagers() {
        return DEFERRED_LOCK_MANAGERS;
    }

    /**
     * Init the deferred lock managers (thread - DeferredLockManager).
     */
    protected static Map initializeDeferredLockManagers() {
        return new ConcurrentHashMap();
    }

    /**
     * Return the current depth of the active thread.
     */
    public int getDepth() {
        return depth.get();
    }

    /**
     * Number of writer that want the lock.
     * This is used to ensure that a writer is not starved.
     */
    public int getNumberOfReaders() {
        return numberOfReaders.get();
    }

    /**
     * Number of writers that want the lock.
     * This is used to ensure that a writer is not starved.
     */
    public int getNumberOfWritersWaiting() {
        return numberOfWritersWaiting.get();
    }

    /**
     * Return if a thread has acquire this manager.
     */
    public boolean isAcquired() {
        return depth.get() > 0;
    }

    /**
     * INTERNAL:
     * Used byt the refresh process to determine if this concurrency manager is locked by
     * the merge process.  If it is then the refresh should not refresh the object
     */
    public boolean isLockedByMergeManager() {
        return this.lockedByMergeManager;
    }

    /**
     * Check if the deferred locks of a thread are all released.
     * Should write dead lock diagnostic information into the {@link #THREADS_WAITING_TO_RELEASE_DEFERRED_LOCKS_BUILD_OBJECT_COMPLETE_GOES_NOWHERE}.
     * <br>
     * @param thread
     *            the current thread to be explored. It starts by being the thread that it is stuck but then it evolves
     *            to be other that have acquired locks our main thread was needing but whcich themslves are stuck...
     *            threads in the deffered lock chain that are going nowhere themselves.
     * @param recursiveSet
     *            this prevents the algorithm going into an infinite loop of expanding the same thread more than once.
     * @param parentChainOfThreads
     *            this starts by being a basket containing the current thread, but each time we go deeper it evolves to
     *            contain the thread we will explore next.
     * @return true if object is complete
     */
    public static boolean isBuildObjectOnThreadComplete(Thread thread, Map recursiveSet, List<Thread> parentChainOfThreads, boolean deadLockDiagnostic) {
        if (recursiveSet.containsKey(thread)) {
            return true;
        }
        recursiveSet.put(thread, thread);

        DeferredLockManager lockManager = getDeferredLockManager(thread);
        if (lockManager == null) {
            return true;
        }

        Vector deferredLocks = lockManager.getDeferredLocks();
        for (Enumeration deferredLocksEnum = deferredLocks.elements();
             deferredLocksEnum.hasMoreElements();) {
            ConcurrencyManager deferedLock = (ConcurrencyManager)deferredLocksEnum.nextElement();
            Thread activeThread = null;
            if (deferedLock.isAcquired()) {
                activeThread = deferedLock.getActiveThread();

                // the active thread may be set to null at anypoint
                // if added for CR 2330
                if (activeThread != null) {
                    DeferredLockManager currentLockManager = getDeferredLockManager(activeThread);
                    if (currentLockManager == null) {
                        // deadlock diagnostic extension
                        if (deadLockDiagnostic && parentChainOfThreads != null) {
                            StringBuilder justificationForReturningFalse = new StringBuilder();
                            enrichStringBuildingExplainWhyThreadIsStuckInIsBuildObjectOnThreadComplete(parentChainOfThreads, deferedLock, activeThread, false, justificationForReturningFalse);
                            setJustificationWhyMethodIsBuildingObjectCompleteReturnsFalse(justificationForReturningFalse.toString());
                        }
                        return false;
                    } else if (currentLockManager.isThreadComplete()) {
                        activeThread = deferedLock.getActiveThread();
                        // The lock may suddenly finish and no longer have an active thread.
                        if (activeThread != null) {
                            // deadlock diagnostic extension
                            List<Thread> currentChainOfThreads = null;
                            if (deadLockDiagnostic) {
                                currentChainOfThreads = (parentChainOfThreads == null) ? new ArrayList<>() : new ArrayList<>(parentChainOfThreads);
                                currentChainOfThreads.add(activeThread);
                            }
                            if (!isBuildObjectOnThreadComplete(activeThread, recursiveSet, currentChainOfThreads, deadLockDiagnostic)) {
                                return false;
                            }
                        }
                    } else {
                        if (deadLockDiagnostic && parentChainOfThreads != null) {
                            StringBuilder justificationForReturningFalse = new StringBuilder();
                            enrichStringBuildingExplainWhyThreadIsStuckInIsBuildObjectOnThreadComplete(parentChainOfThreads, deferedLock, activeThread, true, justificationForReturningFalse);
                            setJustificationWhyMethodIsBuildingObjectCompleteReturnsFalse(justificationForReturningFalse.toString());
                        }
                        return false;
                    }
                }
            }
        }
        if (parentChainOfThreads != null && parentChainOfThreads.size() == 1) {
            clearJustificationWhyMethodIsBuildingObjectCompleteReturnsFalse();
        }
        return true;
    }

    /**
     * When the recursive algorithm decides to return false it is because it is confronted with a cache key that had to
     * be deferred. And the cache key is either being owned by a thread that did not flage itsef as being finished and
     * waiting in the wait for deferred locks. Or the thread that ows the cache key is not playing nice - and not using
     * deferred locks - so it has acquire the cache key, it is going about its business (e.g. committing a transaction
     * or perhaps doing object building. Normally, but not always, in object building threads do have a lock manager,
     * but sometimes not when they agressive acquire lock policy. )
     *
     * @param chainOfThreadsExpandedInRecursion
     *            This the chaing threads that were expanded as we went down with the recursion
     * @param finalDeferredLockCausingTrouble
     *            this is a lock that was deferred either by current thread or by a thread that is also itself waiting
     *            around . This lock is what is causing us ultimately to return FALSE, because the lock is still ACUIRED
     *            so not yet free. And the thread that owns it is also still not finished yet.
     *
     * @param activeThreadOnDeferredLock
     *            this is the thread that was spotted as owning/being actively owning the the deferred lock. So we can
     *            consider this thread as being the ultimate cause of why the current thread and perhaps a hole chaing
     *            of related threads are not evolving. But certainly the current thread.
     * @param hasDeferredLockManager
     *            Some threads have deferred lock managers some not. Not clear when they do. But threads doing object
     *            building typically end up creating a deferred lock manager when they find themselves unable to acquire
     *            an object and need to defer on the cache key.
     * @param justification
     *            this is what we want to populate it will allow us to build a trace to explain why the thread on the
     *            wait for deferred lock is going nowhere. This trace will be quite important to help us interpret the
     *            massive dumps since it is quite typical to find threads in this state.
     *
     */
    public static void enrichStringBuildingExplainWhyThreadIsStuckInIsBuildObjectOnThreadComplete(
            List<Thread> chainOfThreadsExpandedInRecursion,
            ConcurrencyManager finalDeferredLockCausingTrouble,
            Thread activeThreadOnDeferredLock,
            boolean hasDeferredLockManager,
            StringBuilder justification) {
        // (a) summarize the threads navigated via deferred locks
        int currentThreadNumber = 0;
        for (Thread currentExpandedThread : chainOfThreadsExpandedInRecursion) {
            currentThreadNumber++;
            justification.append(TraceLocalization.buildMessage("concurrency_manager_build_object_thread_complete_1", new Object[] {currentThreadNumber, currentExpandedThread.getName()}));
        }
        justification.append(TraceLocalization.buildMessage("concurrency_manager_build_object_thread_complete_2"));
        // (b) Described the cache key blocking us from finishing the oject building
        String cacheKeyStr = ConcurrencyUtil.SINGLETON.createToStringExplainingOwnedCacheKey(finalDeferredLockCausingTrouble);
        justification.append(TraceLocalization.buildMessage("concurrency_manager_build_object_thread_complete_3", new Object[] {cacheKeyStr}));
        // (c) Describe the thread that has acquired the cache key and is not done yet
        justification.append(TraceLocalization.buildMessage("concurrency_manager_build_object_thread_complete_4", new Object[] {activeThreadOnDeferredLock, hasDeferredLockManager}));
    }

    /**
     * Return if this manager is within a nested acquire.
     */
    public boolean isNested() {
        return depth.get() > 1;
    }

    public void putDeferredLock(Thread thread, DeferredLockManager lockManager) {
        getDeferredLockManagers().put(thread, lockManager);
    }

    /**
     * Decrement the depth for the active thread.
     * Assume the current thread is the active one.
     * Raise an error if the depth become < 0.
     * The notify will release the first thread waiting on the object,
     * if no threads are waiting it will do nothing.
     */
    public synchronized void release() throws ConcurrencyException {
        if (this.depth.get() == 0) {
            throw ConcurrencyException.signalAttemptedBeforeWait();
        } else {
            this.depth.decrementAndGet();
        }
        if (this.depth.get() == 0) {
            this.activeThread = null;
            if (shouldTrackStack){
                this.stack = null;
            }
            this.lockedByMergeManager = false;
            notifyAll();
        }
    }

    /**
     * Release the deferred lock.
     * This uses a deadlock detection and resolution algorithm to avoid cache deadlocks.
     * The deferred lock manager keeps track of the lock for a thread, so that other
     * thread know when a deadlock has occurred and can resolve it.
     */
    public void releaseDeferredLock() throws ConcurrencyException {
        Thread currentThread = Thread.currentThread();
        DeferredLockManager lockManager = getDeferredLockManager(currentThread);
        ReadLockManager readLockManager = getReadLockManager(currentThread);
        if (lockManager == null) {
            return;
        }
        int depth = lockManager.getThreadDepth();

        if (depth > 1) {
            lockManager.decrementDepth();
            return;
        }

        // If the set is null or empty, means there is no deferred lock for this thread, return.
        if (!lockManager.hasDeferredLock()) {
            lockManager.releaseActiveLocksOnThread();
            removeDeferredLockManager(currentThread);
            return;
        }

        lockManager.setIsThreadComplete(true);

        final long whileStartTimeMillis = System.currentTimeMillis();
        boolean releaseAllLocksAquiredByThreadAlreadyPerformed = false;
        boolean currentThreadRegisteredAsWaitingForisBuildObjectOnThreadComplete = false;

        clearJustificationWhyMethodIsBuildingObjectCompleteReturnsFalse();
        // Thread have three stages, one where they are doing work (i.e. building objects)
        // two where they are done their own work but may be waiting on other threads to finish their work,
        // and a third when they and all the threads they are waiting on are done.
        // This is essentially a busy wait to determine if all the other threads are done.
        while (true) {
            boolean isBuildObjectCompleteSlow = ConcurrencyUtil.SINGLETON.tooMuchTimeHasElapsed(whileStartTimeMillis, ConcurrencyUtil.SINGLETON.getBuildObjectCompleteWaitTime());
            try{
                // 2612538 - the default size of Map (32) is appropriate
                Map recursiveSet = new IdentityHashMap();
                if (isBuildObjectOnThreadComplete(currentThread, recursiveSet, Arrays.asList(currentThread), isBuildObjectCompleteSlow)) {// Thread job done.
                    // Remove from debug metadata the fact that the current thread needed to wait
                    // for one or more build objects to be completed by other threads.
                    if(currentThreadRegisteredAsWaitingForisBuildObjectOnThreadComplete) {
                        THREADS_WAITING_TO_RELEASE_DEFERRED_LOCKS.remove(currentThread);
                    }
                    clearJustificationWhyMethodIsBuildingObjectCompleteReturnsFalse();
                    lockManager.releaseActiveLocksOnThread();
                    removeDeferredLockManager(currentThread);
                    AbstractSessionLog.getLog().log(SessionLog.FINER, SessionLog.CACHE, "deferred_locks_released", currentThread.getName());
                    return;
                } else {// Not done yet, wait and check again.
                    try {
                        // Add debug metadata to concurrency manager state
                        // The current thread will now be waiting for other threads to build the object(s) it could not acquire
                        if(!currentThreadRegisteredAsWaitingForisBuildObjectOnThreadComplete) {
                            currentThreadRegisteredAsWaitingForisBuildObjectOnThreadComplete = true;
                            THREADS_WAITING_TO_RELEASE_DEFERRED_LOCKS.add(currentThread);
                        }
                        Thread.sleep(20);
                        ConcurrencyUtil.SINGLETON.determineIfReleaseDeferredLockAppearsToBeDeadLocked(this, whileStartTimeMillis, lockManager, readLockManager, ConcurrencyUtil.SINGLETON.isAllowInterruptedExceptionFired());
                    } catch (InterruptedException interrupted) {
                        THREADS_WAITING_TO_RELEASE_DEFERRED_LOCKS.remove(currentThread);
                        AbstractSessionLog.getLog().logThrowable(SessionLog.SEVERE, SessionLog.CACHE, interrupted);
                        releaseAllLocksAcquiredByThread(lockManager);
                        releaseAllLocksAquiredByThreadAlreadyPerformed = true;
                        clearJustificationWhyMethodIsBuildingObjectCompleteReturnsFalse();
                        throw ConcurrencyException.waitWasInterrupted(interrupted.getMessage());
                    }
                }
            } catch (Error error) {
                if (!releaseAllLocksAquiredByThreadAlreadyPerformed) {
                    THREADS_WAITING_TO_RELEASE_DEFERRED_LOCKS.remove(currentThread);
                    AbstractSessionLog.getLog().logThrowable(SessionLog.SEVERE, SessionLog.CACHE, error);
                    releaseAllLocksAcquiredByThread(lockManager);
                    clearJustificationWhyMethodIsBuildingObjectCompleteReturnsFalse();
                }
                throw error;
            }
        }
    }

    /**
     * Decrement the number of readers.
     * Used to allow concurrent reads.
     */
    public synchronized void releaseReadLock() throws ConcurrencyException {
        if (this.numberOfReaders.get() == 0) {
            this.totalNumberOfKeysReleasedForReadingBlewUpExceptionDueToCacheKeyHavingReachedCounterZero.incrementAndGet();
            try {
                removeReadLockFromReadLockManager();
            } catch (Exception e) {
                AbstractSessionLog.getLog().logThrowable(SessionLog.SEVERE, SessionLog.CACHE, e);
            }
            throw ConcurrencyException.signalAttemptedBeforeWait();
        } else {
            try {
                removeReadLockFromReadLockManager();
            } finally {
                this.numberOfReaders.decrementAndGet();
                this.totalNumberOfKeysReleasedForReading.incrementAndGet();
            }
        }
        if (this.numberOfReaders.get() == 0) {
            notifyAll();
        }
    }

    /**
     * Remove the deferred lock manager for the thread
     */
    public static DeferredLockManager removeDeferredLockManager(Thread thread) {
        return getDeferredLockManagers().remove(thread);
    }

    /**
     * Set the active thread.
     */
    public void setActiveThread(Thread activeThread) {
        this.activeThread = activeThread;
    }

    /**
     * Set the current depth of the active thread.
     */
    protected void setDepth(int depth) {
        this.depth.set(depth);
    }

    /**
     * INTERNAL:
     * Used by the mergemanager to let the read know not to refresh this object as it is being
     * loaded by the merge process.
     */
    public void setIsLockedByMergeManager(boolean state) {
        this.lockedByMergeManager = state;
    }

    /**
     * Track the number of readers.
     */
    protected void setNumberOfReaders(int numberOfReaders) {
        this.numberOfReaders.set(numberOfReaders);
    }

    /**
     * Number of writers that want the lock.
     * This is used to ensure that a writer is not starved.
     */
    protected void setNumberOfWritersWaiting(int numberOfWritersWaiting) {
        this.numberOfWritersWaiting.set(numberOfWritersWaiting);
    }

    public synchronized void transitionToDeferredLock() {
        Thread currentThread = Thread.currentThread();
        DeferredLockManager lockManager = getDeferredLockManager(currentThread);
        if (lockManager == null) {
            lockManager = new DeferredLockManager();
            putDeferredLock(currentThread, lockManager);
        }
        lockManager.incrementDepth();
        lockManager.addActiveLock(this);
    }

    /**
     * For the thread to release all of its locks.
     *
     * @param lockManager
     *            the deferred lock manager
     */
    public void releaseAllLocksAcquiredByThread(DeferredLockManager lockManager) {
        Thread currentThread = Thread.currentThread();

        //When this method is invoked during an acquire lock sometimes there is no lock manager
        if (lockManager == null) {
            String cacheKeyToString = ConcurrencyUtil.SINGLETON.createToStringExplainingOwnedCacheKey(this);
            StringWriter writer = new StringWriter();
            writer.write(TraceLocalization.buildMessage("concurrency_manager_release_locks_acquired_by_thread_1", new Object[] {currentThread.getName(), cacheKeyToString}));
            AbstractSessionLog.getLog().log(SessionLog.SEVERE, SessionLog.CACHE, writer.toString(), new Object[] {}, false);
            return;
        }

        //Release the active locks on the thread
        StringWriter writer = new StringWriter();
        writer.write(TraceLocalization.buildMessage("concurrency_manager_release_locks_acquired_by_thread_2", new Object[] {currentThread.toString()}));
        AbstractSessionLog.getLog().log(SessionLog.SEVERE, SessionLog.CACHE, writer.toString(), new Object[] {}, false);
        lockManager.releaseActiveLocksOnThread();
        removeDeferredLockManager(currentThread);
    }

    /**
     * The method is not synchronized because for now we assume that each thread will ask for its own lock manager. If
     * we were writing a dead lock detection mechanism then a ThreadA could be trying understand the ReadLocks of a
     * ThreadB and this would no longer be true.
     *
     * @param thread
     *            The thread for which we want to have look at the acquired read locks.
     * @return Never null if the read lock manager does not yet exist for the current thread. otherwise its read log
     *         manager is returned.
     */
    protected static ReadLockManager getReadLockManager(Thread thread) {
        Map<Thread, ReadLockManager> readLockManagers = getReadLockManagers();
        return readLockManagers.get(thread);
    }

    /**
     * Return the deferred lock manager hashtable (thread - DeferredLockManager).
     */
    protected static Map<Thread, ReadLockManager> getReadLockManagers() {
        return READ_LOCK_MANAGERS;
    }

    /**
     * Print the nested depth.
     */
    @Override
    public String toString() {
        Object[] args = { Integer.valueOf(getDepth()) };
        return Helper.getShortClassName(getClass()) + ToStringLocalization.buildMessage("nest_level", args);
    }

    public Exception getStack() {
        return stack;
    }

    public void setStack(Exception stack) {
        this.stack = stack;
    }

    public static boolean shouldTrackStack() {
        return shouldTrackStack;
    }

    /**
     * INTERNAL:
     * This can be set during debugging to record the stacktrace when a lock is acquired.
     * Then once IdentityMapAccessor.printIdentityMapLocks() is called the stack call for each
     * lock will be printed as well.  Because locking issues are usually quite time sensitive setting
     * this flag may inadvertently remove the deadlock because of the change in timings.
     *
     * There is also a system level property for this setting. "eclipselink.cache.record-stack-on-lock"
     * @param shouldTrackStack
     */
    public static void setShouldTrackStack(boolean shouldTrackStack) {
        ConcurrencyManager.shouldTrackStack = shouldTrackStack;
    }

    private static String getPropertyRecordStackOnLock() {
        return (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) ?
                AccessController.doPrivileged(new PrivilegedGetSystemProperty(SystemProperties.RECORD_STACK_ON_LOCK))
                : System.getProperty(SystemProperties.RECORD_STACK_ON_LOCK);
    }

    /**
     * Normally this mehtod should only be called from withing the concurrency manager.
     * However the write lock manager while it is building clones also does some while loop waiting
     * to try to acquire a cache key this acquiring logic is not being managed directly inside of the wait manager.
     *
     */
    public void putThreadAsWaitingToAcquireLockForWriting(Thread thread, String methodName) {
        THREADS_TO_WAIT_ON_ACQUIRE.put(thread, this);
        THREADS_TO_WAIT_ON_ACQUIRE_NAME_OF_METHOD_CREATING_TRACE.put(thread, methodName);
    }

    /**
     * The thread has acquired the lock for writing or decided to defer acquiring the lock putting this lock into its
     * deferred lock list.
     */
    public void removeThreadNoLongerWaitingToAcquireLockForWriting(Thread thread) {
        THREADS_TO_WAIT_ON_ACQUIRE.remove(thread);
        THREADS_TO_WAIT_ON_ACQUIRE_NAME_OF_METHOD_CREATING_TRACE.remove(thread);
    }

    /**
     * The thread is trying to acquire a read lock but it is not being able to make process on getting the read lock.
     *
     * @param methodName
     *            metadata to help us debug trace leaking. If we start blowing up threads we do not want the traces
     *            created by the current thread to remain.
     */
    public void putThreadAsWaitingToAcquireLockForReading(Thread currentThread, String methodName) {
        THREADS_TO_WAIT_ON_ACQUIRE_READ_LOCK.put(currentThread, this);
        THREADS_TO_WAIT_ON_ACQUIRE_READ_LOCK_NAME_OF_METHOD_CREATING_TRACE.put(currentThread, methodName);
    }

    public void removeThreadNoLongerWaitingToAcquireLockForReading(Thread thread) {
        THREADS_TO_WAIT_ON_ACQUIRE_READ_LOCK.remove(thread);
        THREADS_TO_WAIT_ON_ACQUIRE_READ_LOCK_NAME_OF_METHOD_CREATING_TRACE.remove(thread);
    }

    /** Getter for {@link #concurrencyManagerId} */
    public long getConcurrencyManagerId() {
        return concurrencyManagerId;
    }

    /** Getter for {@link #concurrencyManagerCreationDate} */
    public Date getConcurrencyManagerCreationDate() {
        return concurrencyManagerCreationDate;
    }

    /** Getter for {@link #totalNumberOfKeysAcquiredForReading} */
    public long getTotalNumberOfKeysAcquiredForReading() {
        return totalNumberOfKeysAcquiredForReading.get();
    }

    /** Getter for {@link #totalNumberOfKeysReleasedForReading} */
    public long getTotalNumberOfKeysReleasedForReading() {
        return totalNumberOfKeysReleasedForReading.get();
    }

    /** Getter for {@link #totalNumberOfKeysReleasedForReadingBlewUpExceptionDueToCacheKeyHavingReachedCounterZero} */
    public long getTotalNumberOfKeysReleasedForReadingBlewUpExceptionDueToCacheKeyHavingReachedCounterZero() {
        return totalNumberOfKeysReleasedForReadingBlewUpExceptionDueToCacheKeyHavingReachedCounterZero.get();
    }

    /** Getter for {@link #THREADS_TO_WAIT_ON_ACQUIRE} */
    public static Map<Thread, ConcurrencyManager> getThreadsToWaitOnAcquire() {
        return new HashMap<>(THREADS_TO_WAIT_ON_ACQUIRE);
    }

    /** Getter for {@link #THREADS_TO_WAIT_ON_ACQUIRE_NAME_OF_METHOD_CREATING_TRACE} */
    public static Map<Thread, String> getThreadsToWaitOnAcquireMethodName() {
        return new HashMap<>(THREADS_TO_WAIT_ON_ACQUIRE_NAME_OF_METHOD_CREATING_TRACE);
    }

    /** Getter for {@link #THREADS_TO_WAIT_ON_ACQUIRE_READ_LOCK} */
    public static Map<Thread, ConcurrencyManager> getThreadsToWaitOnAcquireReadLock() {
        return THREADS_TO_WAIT_ON_ACQUIRE_READ_LOCK;
    }

    /** Getter for {@link #THREADS_TO_WAIT_ON_ACQUIRE_READ_LOCK_NAME_OF_METHOD_CREATING_TRACE} */
    public static Map<Thread, String> getThreadsToWaitOnAcquireReadLockMethodName() {
        return THREADS_TO_WAIT_ON_ACQUIRE_READ_LOCK_NAME_OF_METHOD_CREATING_TRACE;
    }

    /** Getter for {@link #THREADS_WAITING_TO_RELEASE_DEFERRED_LOCKS} */
    public static Set<Thread> getThreadsWaitingToReleaseDeferredLocks() {
        return new HashSet<>(THREADS_WAITING_TO_RELEASE_DEFERRED_LOCKS);
    }

    /** Getter for {@link #THREADS_WAITING_TO_RELEASE_DEFERRED_LOCKS_BUILD_OBJECT_COMPLETE_GOES_NOWHERE} */
    public static Map<Thread, String> getThreadsWaitingToReleaseDeferredLocksJustification() {
        return new HashMap<>(THREADS_WAITING_TO_RELEASE_DEFERRED_LOCKS_BUILD_OBJECT_COMPLETE_GOES_NOWHERE);
    }

    /**
     * The current thread has incremented the number of readers on the current cache key. It also wants to record into
     * the read lock manager that this thread has acquired the cache key. This method should be user in all places where
     * the cache key nunber of readers is incremented.
     */
    protected void addReadLockToReadLockManager() {
        Thread currentThread = Thread.currentThread();
        ReadLockManager readLockManager = getReadLockManagerEnsureResultIsNotNull(currentThread);
        ConcurrencyManager concurrencyManagerCacheKey = this;
        readLockManager.addReadLock(concurrencyManagerCacheKey);
    }

    /**
     * The current thread is about to decrement the number of readers in cache key. The thread also wants to update the
     * read lock manager and remove the cache key that has previously been aquired from there.
     */
    protected void removeReadLockFromReadLockManager() {
        Thread currentThread = Thread.currentThread();
        ReadLockManager readLockManager = getReadLockManager(currentThread);
        if (readLockManager != null) {
            ConcurrencyManager concurrencyManagerCacheKey = this;
            readLockManager.removeReadLock(concurrencyManagerCacheKey);
            removeReadLockManagerIfEmpty(currentThread);
        } else {
            // We have a problem we do not want ever see a decrement on the number of readers if we
            // are not tracing one or more predecessor add read lock keys.
            // so we will put the error message into a fresh new read lock manager
            final int currentNumberOfReaders = this.numberOfReaders.get();
            final int decrementedNumberOfReaders = currentNumberOfReaders - 1;
            String errorMessage = ConcurrencyUtil.SINGLETON.readLockManagerProblem01CreateLogErrorMessageToIndicateThatCurrentThreadHasNullReadLockManagerWhileDecrementingNumberOfReaders(currentNumberOfReaders, decrementedNumberOfReaders, this);
            readLockManager = getReadLockManagerEnsureResultIsNotNull(currentThread);
            readLockManager.addRemoveReadLockProblemsDetected(errorMessage);
        }
    }

    /**
     * Same as {@link #getReadLockManager(Thread)} but in this case a not null result is ensured
     *
     * @param thread
     *            the thread wanting its read lock manager
     * @return the read lock manager for the current thread.
     */
    protected static ReadLockManager getReadLockManagerEnsureResultIsNotNull(Thread thread) {
        Map<Thread, ReadLockManager> readLockManagers = getReadLockManagers();
        if (!readLockManagers.containsKey(thread)) {
            ReadLockManager  readLockManager = new ReadLockManager();
            readLockManagers.putIfAbsent(thread, readLockManager);
            return readLockManager;
        }
        return readLockManagers.get(thread);
    }

    /**
     * Just like we see that the satic map of deffered locks is cleared of cache values for
     * the current thread we also want to try to keep the static map of acquired read locks by a thread light weight by
     * removing the association between the current thread and a read lock manager whenever the read lock manager
     * becomes empty.
     *
     * @param thread
     *            the thread that wants its read lock manager destroyed if it is empty.
     */
    protected static void removeReadLockManagerIfEmpty(Thread thread) {
        Map<Thread, ReadLockManager> readLockManagers = getReadLockManagers();
        if (readLockManagers.containsKey(thread)) {
            ReadLockManager readLockManager = readLockManagers.get(thread);
            if (readLockManager.isEmpty()) {
                readLockManagers.remove(thread);
            }
        }
    }

    /**
     * Clear the justification why the {@link #isBuildObjectOnThreadComplete(Thread, Map, List, boolean) } is
     * going nowhere.
     *
     * <P>
     * WHEN TO INVOKE: <br>
     * Should be invoked if we decide to blowup a thread with the explosive approach, for a thread in wait for release
     * deferred lock. We do not want to keep traces of threads that left eclipselink code. <br>
     *
     * Should be infokved when the algorithm returns TRUE - build object is complete. <br>
     * Should be invoked when we are not yet stuck for sufficient time and the release defferred logic algorithm is
     * using the {@link #isBuildObjectOnThreadComplete(Thread, Map, List, boolean)} instead of the more verbose and slower
     * {@link #isBuildObjectOnThreadComplete(Thread, Map, List, boolean)}.
     */
    public static void clearJustificationWhyMethodIsBuildingObjectCompleteReturnsFalse() {
        THREADS_WAITING_TO_RELEASE_DEFERRED_LOCKS_BUILD_OBJECT_COMPLETE_GOES_NOWHERE.remove(Thread.currentThread());
    }

    /**
     * See {@link #clearJustificationWhyMethodIsBuildingObjectCompleteReturnsFalse()} in this case we want to store the
     * justification computed by the
     * {@link #enrichStringBuildingExplainWhyThreadIsStuckInIsBuildObjectOnThreadComplete(List, ConcurrencyManager, Thread, boolean, StringBuilder)}
     *
     * @param justification
     *            a string that helps us understand why the recursive algorithm returned false, building object is not
     *            yet complete.
     */
    public static void setJustificationWhyMethodIsBuildingObjectCompleteReturnsFalse(String justification) {
        THREADS_WAITING_TO_RELEASE_DEFERRED_LOCKS_BUILD_OBJECT_COMPLETE_GOES_NOWHERE.put(Thread.currentThread(), justification);
    }
}

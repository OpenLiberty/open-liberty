/*
 * Copyright (c) 2020 Oracle, IBM and/or its affiliates. All rights reserved.
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
//     IBM - ConcurrencyUtil call of ThreadMXBean.getThreadInfo() needs doPriv
package org.eclipse.persistence.internal.helper;

import org.eclipse.persistence.config.SystemProperties;
import org.eclipse.persistence.internal.helper.type.CacheKeyToThreadRelationships;
import org.eclipse.persistence.internal.helper.type.ConcurrencyManagerState;
import org.eclipse.persistence.internal.helper.type.DeadLockComponent;
import org.eclipse.persistence.internal.helper.type.ReadLockAcquisitionMetadata;
import org.eclipse.persistence.internal.identitymaps.CacheKey;
import org.eclipse.persistence.internal.localization.TraceLocalization;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedGetSystemProperty;
import org.eclipse.persistence.internal.security.PrivilegedGetThreadInfo;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;

import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.security.AccessController;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ConcurrencyUtil {

    public static final ConcurrencyUtil SINGLETON = new ConcurrencyUtil();

    private static final long DEFAULT_ACQUIRE_WAIT_TIME = 0L;
    private static final long DEFAULT_MAX_ALLOWED_SLEEP_TIME_MS = 40000L;
    private static final long DEFAULT_MAX_ALLOWED_FREQUENCY_TINY_DUMP_LOG_MESSAGE = 40000L;
    private static final long DEFAULT_MAX_ALLOWED_FREQUENCY_MASSIVE_DUMP_LOG_MESSAGE = 60000L;
    private static final boolean DEFAULT_INTERRUPTED_EXCEPTION_FIRED = true;
    private static final boolean DEFAULT_CONCURRENCY_EXCEPTION_FIRED = true;
    private static final boolean DEFAULT_TAKING_STACKTRACE_DURING_READ_LOCK_ACQUISITION = false;

    private long acquireWaitTime = getLongProperty(SystemProperties.CONCURRENCY_MANAGER_ACQUIRE_WAIT_TIME, DEFAULT_ACQUIRE_WAIT_TIME);
    private long maxAllowedSleepTime = getLongProperty(SystemProperties.CONCURRENCY_MANAGER_MAX_SLEEP_TIME, DEFAULT_MAX_ALLOWED_SLEEP_TIME_MS);
    private long maxAllowedFrequencyToProduceTinyDumpLogMessage = getLongProperty(SystemProperties.CONCURRENCY_MANAGER_MAX_FREQUENCY_DUMP_TINY_MESSAGE, DEFAULT_MAX_ALLOWED_FREQUENCY_TINY_DUMP_LOG_MESSAGE);
    private long maxAllowedFrequencyToProduceMassiveDumpLogMessage = getLongProperty(SystemProperties.CONCURRENCY_MANAGER_MAX_FREQUENCY_DUMP_MASSIVE_MESSAGE, DEFAULT_MAX_ALLOWED_FREQUENCY_MASSIVE_DUMP_LOG_MESSAGE);
    private boolean allowInterruptedExceptionFired = getBooleanProperty(SystemProperties.CONCURRENCY_MANAGER_ALLOW_INTERRUPTED_EXCEPTION, DEFAULT_INTERRUPTED_EXCEPTION_FIRED);
    private boolean allowConcurrencyExceptionToBeFiredUp = getBooleanProperty(SystemProperties.CONCURRENCY_MANAGER_ALLOW_CONCURRENCY_EXCEPTION, DEFAULT_CONCURRENCY_EXCEPTION_FIRED);
    private boolean allowTakingStackTraceDuringReadLockAcquisition = getBooleanProperty(SystemProperties.CONCURRENCY_MANAGER_ALLOW_STACK_TRACE_READ_LOCK, DEFAULT_TAKING_STACKTRACE_DURING_READ_LOCK_ACQUISITION);

    /**
     * Thread local variable that allows the current thread to know when was the last time that this specific thread
     * produced the "tiny dump" indicating that the thread is stuck.
     */
    private final ThreadLocal<Date> threadLocalDateWhenCurrentThreadLastComplainedAboutBeingStuckInDeadLock = new ThreadLocal<>();

    /**
     * Whenever we produce a tiny dump log message we will give it a unit identifier
     */
    private final AtomicLong currentTinyMessageLogDumpNumber = new AtomicLong(0);

    /**
     * Whenever we produce a massive dump log message we will give it a unit identifier
     */
    private final AtomicLong currentMassiveDumpMessageLogDumpNumber = new AtomicLong(0);

    private final Object dateWhenLastConcurrencyManagerStateFullDumpWasPerformedLock = new Object();

    /**
     * Whenever we decide to log  a massive dump of the state of the concurrency manager we need to make this date move forward.
     * <P>
     * This variable is telling any thread that might be considering the possibility of logging a massive dump log message,
     * when a massive dump was last performed, thus allowing threads to avoid spamming too much.
     *
     * <P>
     * NOTE: <br>
     * Needs to be accessed in a synchronized method.
     */
    private long dateWhenLastConcurrencyManagerStateFullDumpWasPerformed = 0L;

    /**
     * When we are explaining where read locks were acquired, the first time we see a new stack trace we create a stack
     * trace id. Then for all other read locks we just say in the massive please go have a look at stack trace xxx.
     */
    private final AtomicLong stackTraceIdAtomicLong = new AtomicLong(0);

    private ConcurrencyUtil() {
    }

    /**
     * Throw an interrupted exception if appears that eclipse link code is taking too long to release a deferred lock.
     *
     * @param whileStartTimeMillis
     *            the start date of the while tru loop for releasing a deferred lock
     * @param callerIsWillingToAllowInterruptedExceptionToBeFiredUpIfNecessary
     *            this flag is to allow the write lock manager to say that it is afraid of a concurrency exception being
     *            fire up because the thread in a dead lock might be trying to do a commit and blowing these threads up
     *            is most likely too dangerous and possibly the eclipselink code is not robust enough to code with such
     *            scenarios We do not care so much about blowing up exception during object building but during
     *            committing of transactions we are very afraid
     * @throws InterruptedException
     *             we fire an interrupted exception to ensure that the code blows up and releases all of the locks it
     *             had.
     */
    public void determineIfReleaseDeferredLockAppearsToBeDeadLocked(ConcurrencyManager concurrencyManager,
                                                                    final long whileStartTimeMillis, DeferredLockManager lockManager, ReadLockManager readLockManager,
                                                                    boolean callerIsWillingToAllowInterruptedExceptionToBeFiredUpIfNecessary)
            throws InterruptedException {
        // (a) Determine if we believe to be dealing with a dead lock

        final long maxAllowedSleepTimeMillis = ConcurrencyUtil.SINGLETON.getMaxAllowedSleepTime();
        long whileCurrentTimeMillis = System.currentTimeMillis();
        long elapsedTime = whileCurrentTimeMillis - whileStartTimeMillis;
        boolean tooMuchTimeHasElapsed = tooMuchTimeHasElapsed(whileStartTimeMillis, maxAllowedSleepTimeMillis);
        if (!tooMuchTimeHasElapsed) {
            // this thread is not stuck for that long let us allow the code to continue waiting for the lock to be acquired
            // or for the deferred locks to be considered as finished
            return;
        }

        // (b) We believe this is a dead lock
        // before we start spamming the server log lets make sure this thread has not spammed the server log  too recently
        if(threadLocalDateWhenCurrentThreadLastComplainedAboutBeingStuckInDeadLock.get() == null) {
            // make sure the thread local variable never returns null
            threadLocalDateWhenCurrentThreadLastComplainedAboutBeingStuckInDeadLock.set(new Date(0));
        }
        Date dateWhenTinyCurrentThreadBeingStuckMessageWasLastLogged = threadLocalDateWhenCurrentThreadLastComplainedAboutBeingStuckInDeadLock.get();
        final long maxAllowedFrequencyToDumpTinyMessage = getMaxAllowedFrequencyToProduceTinyDumpLogMessage();
        boolean tooMuchTimeHasElapsedSinceLastLoggingOfTinyMessage = tooMuchTimeHasElapsed(dateWhenTinyCurrentThreadBeingStuckMessageWasLastLogged.getTime(), maxAllowedFrequencyToDumpTinyMessage);

        if(!tooMuchTimeHasElapsedSinceLastLoggingOfTinyMessage) {
            // this thread has recently logged a small message about the fact that it is stuck
            // no point in logging another message like that for some time
            // let us allow for this thread to silently continue stuck without logging anything
            return ;
        }

        // (c) This thread it is dead lock since the whileStartDate indicates a dead lock and
        // this thread has been keeping silent about the problem for some time since the dateWhenTinyCurrentThreadBeingStuckMessageWasLastLogged
        // indicates that quite some time has elapsed since we have last spammed the server log
        // we now start by spamming into the server log a "tiny message" specific to the current thread
        String tinyErrorMessage = currentThreadIsStuckForSomeTimeProduceTinyLogMessage(elapsedTime, concurrencyManager, lockManager, readLockManager);

        // (d) next step is to log into the server log the massive dump log message where we try to explaing the concrrency mangaer state
        // only one thread will suceed in doing the massive dump ever 1 minute or so
        // we do not want that a massive dump is log all the time
        dumpConcurrencyManagerInformationIfAppropriate();

        // (e) Finaly we need to check what the user wants us to when we decide that we are in the middle of a dead lock
        // and we have dumped whatever information we could dump
        // does the user want us to blow up the thread to try release acquired locks and allow other threads to move forward
        // or is the user afraid that we fire up a thread interrupted exception because if the dead lock does not resolve
        // production will be serously affect by aborted business process that should normally have suceeded and after N rerties
        // (e.g. 3 jms MDB message retries) the process is aborted as failed making live system recovery extermelly difficult?
        // the project must decide how to forward here...
        // a frozen system seems for the time being the safest course of action
        // because the interrupted exception might be leaving the cocurrency manager corrupted in terms f the cache keys and the readers on the cache keys
        // NOTE:
        // This project has reported that our blowing up of the JTA transaction
        // to release the dead lock is not being 100% effective the system can still freeze forever
        // And if interrupting the thread and releasing its resources is not effective
        // then we are worse off.
        // Best is to leave the system frozen and simply spam into the log of the server
        // the current state of cache
        boolean allowConcurrencyExceptionToBeFiredUp = isAllowConcurrencyExceptionToBeFiredUp();
        if (allowConcurrencyExceptionToBeFiredUp) {
            // The first check if in general concurrency excpetions to resolve the dead locks can be fired is passed
            // but we do one final check. The WriteLockManager is afraid of seing its thread being blown up
            // so the write lock manager will be prohibiting this exception from being fired up
            if (callerIsWillingToAllowInterruptedExceptionToBeFiredUpIfNecessary) {
                throw new InterruptedException(tinyErrorMessage);
            }

        } else {
            AbstractSessionLog.getLog().log(SessionLog.SEVERE, SessionLog.CACHE,"concurrency_manager_allow_concurrency_exception_fired_up");
        }
    }

    /**
     * @return "eclipselink.concurrency.manager.waittime" persistence property value.
     */
    public long getAcquireWaitTime() {
        return this.acquireWaitTime;
    }

    public void setAcquireWaitTime(long acquireWaitTime) {
        this.acquireWaitTime = acquireWaitTime;
    }

    /**
     * @return property to control how long we are willing to wait before firing up an exception
     */
    public long getMaxAllowedSleepTime() {
        return this.maxAllowedSleepTime;
    }

    public void setMaxAllowedSleepTime(long maxAllowedSleepTime) {
        this.maxAllowedSleepTime = maxAllowedSleepTime;
    }

    /**
     * Just like we have a massive dump log message see {@link #getMaxAllowedFrequencyToProduceMassiveDumpLogMessage()}
     * we also want threads to produce "tiny" dump about the fact that they rae stuck. We want to avoid these threads
     * spaming too much the server log ... once the log message is out there not much point in continuously pumping the
     * same log message out over and over again. Controlling how frequently the tiny dump is important especially when
     * the user configures the hacked eclipselink to not fire up a blow up exception and instead to allow eclipselink to
     * remain frozen forever.
     *
     * @return the frequency with which we are allowed to create a tiny dump log message
     */
    public long getMaxAllowedFrequencyToProduceTinyDumpLogMessage() {
        return this.maxAllowedFrequencyToProduceTinyDumpLogMessage;
    }

    public void setMaxAllowedFrequencyToProduceTinyDumpLogMessage(long maxAllowedFrequencyToProduceTinyDumpLogMessage) {
        this.maxAllowedFrequencyToProduceTinyDumpLogMessage = maxAllowedFrequencyToProduceTinyDumpLogMessage;
    }

    /**
     * If the system is perceived to be frozen and not evolving any longer, we will allow that every so often (e.g. once
     * a minute) the logic complaining that the thread is stuck and going nowhere logs a very big dump message where the
     * FULL concurrency manager state is explained. So that we can (manually) try to understand the dead lock based on
     * the dumped information
     *
     * See also {@link #dateWhenLastConcurrencyManagerStateFullDumpWasPerformed}.
     */
    public long getMaxAllowedFrequencyToProduceMassiveDumpLogMessage() {
        return this.maxAllowedFrequencyToProduceMassiveDumpLogMessage;
    }

    public void setMaxAllowedFrequencyToProduceMassiveDumpLogMessage(long maxAllowedFrequencyToProduceMassiveDumpLogMessage) {
        this.maxAllowedFrequencyToProduceMassiveDumpLogMessage = maxAllowedFrequencyToProduceMassiveDumpLogMessage;
    }

    public boolean isAllowInterruptedExceptionFired() {
        return this.allowInterruptedExceptionFired;
    }

    public void setAllowInterruptedExceptionFired(boolean allowInterruptedExceptionFired) {
        this.allowInterruptedExceptionFired = allowInterruptedExceptionFired;
    }

    /**
     * @return true if we are supposed to be firing up exception to abort the thread in a dead lock, false we are afraid
     *         of trying to abort the transaction and not managing to resolve the dead lock and prefer to system frozen
     *         and be forced into restarting it.
     */
    public boolean isAllowConcurrencyExceptionToBeFiredUp() {
        return this.allowConcurrencyExceptionToBeFiredUp;
    }

    public void setAllowConcurrencyExceptionToBeFiredUp(boolean allowConcurrencyExceptionToBeFiredUp) {
        this.allowConcurrencyExceptionToBeFiredUp = allowConcurrencyExceptionToBeFiredUp;
    }

    public boolean isAllowTakingStackTraceDuringReadLockAcquisition() {
        return this.allowTakingStackTraceDuringReadLockAcquisition;
    }

    public void setAllowTakingStackTraceDuringReadLockAcquisition(boolean allowTakingStackTraceDuringReadLockAcquisition) {
        this.allowTakingStackTraceDuringReadLockAcquisition = allowTakingStackTraceDuringReadLockAcquisition;
    }

    /**
     *
     * @return A to string of the cache key (e.g. that we are trying to lock
     */
    public String createToStringExplainingOwnedCacheKey(ConcurrencyManager concurrencyManager) {
        String cacheKeyClass = concurrencyManager.getClass().getCanonicalName();
        Thread activeThreadObj = concurrencyManager.getActiveThread();
        String activeThread = activeThreadObj != null ? activeThreadObj.getName() : "Null";
        long concurrencyManagerId = concurrencyManager.getConcurrencyManagerId();
        Date concurrencyManagerCreationDate = concurrencyManager.getConcurrencyManagerCreationDate();
        if (concurrencyManager instanceof CacheKey) {
            CacheKey cacheKey = (CacheKey) concurrencyManager;
            Object primaryKey = cacheKey.getKey();
            Object cacheKeyObject = cacheKey.getObject();
            String canonicalName = cacheKeyObject != null ? cacheKeyObject.getClass().getCanonicalName()
                    : TraceLocalization.buildMessage("concurrency_util_owned_cache_key_null");
            return TraceLocalization.buildMessage("concurrency_util_owned_cache_key_is_cache_key", new Object[] {canonicalName, primaryKey, cacheKeyObject, cacheKeyClass, activeThread,
                    concurrencyManager.getNumberOfReaders(), concurrencyManagerId,
                    concurrencyManagerCreationDate
                    // metadata of number of times the cache key suffered increases in number readers
                    , cacheKey.getTotalNumberOfKeysAcquiredForReading(),
                    cacheKey.getTotalNumberOfKeysReleasedForReading(),
                    cacheKey.getTotalNumberOfKeysReleasedForReadingBlewUpExceptionDueToCacheKeyHavingReachedCounterZero()});

        } else {
            return TraceLocalization.buildMessage("concurrency_util_owned_cache_key_is_not_cache_key", new Object[] {cacheKeyClass, concurrencyManager, activeThread,
                    concurrencyManagerId, concurrencyManagerCreationDate,
                    concurrencyManager.getTotalNumberOfKeysAcquiredForReading(),
                    concurrencyManager.getTotalNumberOfKeysReleasedForReading(), concurrencyManager
                    .getTotalNumberOfKeysReleasedForReadingBlewUpExceptionDueToCacheKeyHavingReachedCounterZero()});

        }
    }

    /**
     * We have a thread that is not evolving for quite some while. This is a fairy good indication of eclipselink being
     * stuck in a dead lock. So we log some information about the thread that is stuck.
     *
     * @param elapsedTime
     *            how many ms have passed since the thread stopped moving
     * @param concurrencyManager
     *            the current cache key that the thread is trying to acquire or the object where the thread is waiting
     *            for the release deferred locks .
     * @param lockManager
     *            the lock manager
     * @param readLockManager
     *            the read lock manager
     * @return Return the string with the tiny message we logged on the server log. This message can be interesting if
     *         we decide to fire up an interrupted exception
     */
    protected String currentThreadIsStuckForSomeTimeProduceTinyLogMessage(long elapsedTime, ConcurrencyManager concurrencyManager, DeferredLockManager lockManager, ReadLockManager readLockManager) {
        // We believe this is a dead lock so now we will log some information
        Thread currentThread = Thread.currentThread();
        String threadName = currentThread.getName();
        String currentCacheKeyContext = createToStringExplainingOwnedCacheKey(concurrencyManager);
        StringWriter errorMessage = new StringWriter();
        long messageNumber = currentTinyMessageLogDumpNumber.incrementAndGet();

        // (i) Create a big head to explain the cache key we were in when we blow up
        errorMessage.write(TraceLocalization.buildMessage("concurrency_util_header_current_cache_key", new Object[] {threadName}));
        // explain the cache key itself where the problem is taking place
        errorMessage.write(TraceLocalization.buildMessage("concurrency_util_stuck_thread_tiny_log_cache_key", new Object[] { messageNumber, threadName, currentCacheKeyContext, elapsedTime }));

        // (ii) Add information about the cache keys where the current thread was set as actively owning
        errorMessage.write(createStringWithSummaryOfActiveLocksOnThread(lockManager, threadName));

        // (iii) Now very interesting as well are all of the objects that current thread could not acquire the
        // deferred locks are essential
        errorMessage.write(createStringWithSummaryOfDeferredLocksOnThread(lockManager, threadName));

        // (iv) Add information about all cache keys te current thread acquired with READ permission
        errorMessage.write(createStringWithSummaryOfReadLocksAcquiredByThread(readLockManager, threadName));

        AbstractSessionLog.getLog().log(SessionLog.SEVERE, SessionLog.CACHE, errorMessage.toString(), new Object[] {}, false);
        threadLocalDateWhenCurrentThreadLastComplainedAboutBeingStuckInDeadLock.set(new Date());
        return errorMessage.toString();
    }

    private boolean tooMuchTimeHasElapsed(final long whileStartTimeMillis, final long maxAllowedSleepTimeMs) {
        if (maxAllowedSleepTimeMs == 0L) {
            return false;
        }
        long elapsedTime = System.currentTimeMillis() - whileStartTimeMillis;
        return elapsedTime > maxAllowedSleepTimeMs;
    }

    /**
     * Invoke the {@link #dumpConcurrencyManagerInformationStep01(Map, Map, Map, Map, Map, Set, Map)} if sufficient time has passed.
     * This log message will potentially create a massive dump in the server log file. So we need to check when was the
     * last time that the masive dump was produced and decide if we can log again the state of the concurrency manager.
     *
     * The access to dateWhenLastConcurrencyManagerStateFullDumpWasPerformedLock is synchronized, because we do not want
     * two threads in parallel to star deciding to dump the complete state of the concurrency manager at the same time.
     * Only one thread should succeed in producing the massive dump in a given time window.
     *
     */
    public void dumpConcurrencyManagerInformationIfAppropriate() {
        // We do not want create a big synchronized method that would be dangerous
        // but we want to make sure accessing the dateWhenLastConcurrencyManagerStateFullDumpWasPerformedLock is only
        // done
        // by cone thread at a time
        synchronized (dateWhenLastConcurrencyManagerStateFullDumpWasPerformedLock) {
            final long maxAllowedFrequencyToProduceMassiveDumpLogMessage = getMaxAllowedFrequencyToProduceMassiveDumpLogMessage();
            boolean tooMuchTimeHasElapsedSinceLastLoggingOfMassiveMessage = tooMuchTimeHasElapsed(
                    dateWhenLastConcurrencyManagerStateFullDumpWasPerformed,
                    maxAllowedFrequencyToProduceMassiveDumpLogMessage);
            if (!tooMuchTimeHasElapsedSinceLastLoggingOfMassiveMessage) {
                // before we can fire to the serverlog such a gigantic message
                // we need to allow for more time to pass (e.g. once a minute should be fine)
                // it is not like production will be waiting for half an hour for a fozen system to magically
                // start working if we do 30 dumps in a half an hour ... it is really irrelevant
                return;
            }

            // we should proceed with making the big log dump - update the date of when the big dump was last done
            dateWhenLastConcurrencyManagerStateFullDumpWasPerformed = System.currentTimeMillis();
        }

        // do the "MassiveDump" logging if enough time has passed since the previous massive dump logging
        Map<Thread, DeferredLockManager> deferredLockManagers = ConcurrencyManager.getDeferredLockManagers();
        Map<Thread, ReadLockManager> readLockManagersOriginal = ConcurrencyManager.getReadLockManagers();
        Map<Thread, ConcurrencyManager> mapThreadToWaitOnAcquireOriginal = ConcurrencyManager.getThreadsToWaitOnAcquire();
        Map<Thread, ConcurrencyManager> mapThreadToWaitOnAcquireReadLockOriginal = ConcurrencyManager.getThreadsToWaitOnAcquireReadLock();
        Map<Thread, Set<ConcurrencyManager>> mapThreadToWaitOnAcquireInsideWriteLockManagerOriginal = WriteLockManager.getThreadToFailToAcquireCacheKeys();
        Set<Thread> setThreadWaitingToReleaseDeferredLocksOriginal = ConcurrencyManager.getThreadsWaitingToReleaseDeferredLocks();
        Map<Thread, Set<Object>> mapThreadToObjectIdWithWriteLockManagerChangesOriginal = WriteLockManager.getMapWriteLockManagerThreadToObjectIdsWithChangeSet();
        dumpConcurrencyManagerInformationStep01(deferredLockManagers, readLockManagersOriginal,
                mapThreadToWaitOnAcquireOriginal, mapThreadToWaitOnAcquireInsideWriteLockManagerOriginal,
                mapThreadToWaitOnAcquireReadLockOriginal,
                setThreadWaitingToReleaseDeferredLocksOriginal, mapThreadToObjectIdWithWriteLockManagerChangesOriginal);
    }

    /**
     * The current working thread is having problems. It seems to not go forward being stuck either trying to acquire a
     * cache key for writing, as a deferred cache key or it is at the end of the process and it is waiting for some
     * other thread to finish building some objects it needed to defer.
     *
     * Now that the system is frozen we want to start spamming into the server log file the state of the concurrency
     * manager since this might help us understand the situation of the system.
     *
     *
     * @param deferredLockManagers
     *            static map coming from the concurrency manager telling us all the threds and their defferred locks and
     *            active locks
     * @param readLockManagersOriginal
     *            static map coming from the concurrency manager telling us all the threads and their read locks
     * @param mapThreadToWaitOnAcquireOriginal
     *            static map of threads that have registered themselves as waiting for some cache key
     *
     * @param mapThreadToWaitOnAcquireInsideWriteLockManagerOriginal
     *            this map relates to the fact that the write lock manager during transaction commits is very illusive.
     *            The write lock manger is not allowing itself to get stuck on acquiring any cache key. It uses waits
     *            with timings and therefore the locks needed to write and that cannot be obtained are not appearing
     *            inside our tracebility maps of the concurrency manager. We needed add the
     *            {@link org.eclipse.persistence.internal.helper.WriteLockManager#THREAD_TO_FAIL_TO_ACQUIRE_CACHE_KEYS}
     *            but semantically this map is 100 percent the same thing as the mapThreadToWaitOnAcquireOriginal. It
     *            still represents a thread wanting to grab a write lock and not managing to get it. Being stuck in that
     *            step. Wo we will want to fuse together the (mapThreadToWaitOnAcquireOriginal and the
     *            mapThreadToWaitOnAcquireInsideWriteLockManagerOriginal) to make our lives much easier.
     *
     *
     * @param setThreadWaitingToReleaseDeferredLocksOriginal
     *            static map of threads that have stopped going deeped in the recursion of object building and are
     *            waiting for the confirmation that some of the objects they needed to build are finished building.
     *
     * @param mapThreadToObjectIdWithWriteLockManagerChangesOriginal
     *            The write lock manager has been tweaked to store information about objects ids that the current thread
     *            has in its hands and that will required for write locks to be acquired by a committing thread. This
     *            information is especially interesting if any thread participating in a dead lock is getting stuck in
     *            the acquisition of write locks as part of the commit process. This information might end up revealing
     *            a thread that has done too many changes and is creating a bigger risk fo dead lock. The more resources
     *            an individual thread tries to grab the worse it is for the concurrency layer. The size of the change
     *            set can be interesting.
     */
    protected void dumpConcurrencyManagerInformationStep01(Map<Thread, DeferredLockManager> deferredLockManagers,
                                                           Map<Thread, ReadLockManager> readLockManagersOriginal,
                                                           Map<Thread, ConcurrencyManager> mapThreadToWaitOnAcquireOriginal,
                                                           Map<Thread, Set<ConcurrencyManager>> mapThreadToWaitOnAcquireInsideWriteLockManagerOriginal,
                                                           Map<Thread, ConcurrencyManager> mapThreadToWaitOnAcquireReadLockOriginal,
                                                           Set<Thread> setThreadWaitingToReleaseDeferredLocksOriginal,
                                                           Map<Thread, Set<Object>> mapThreadToObjectIdWithWriteLockManagerChangesOriginal) {

        // (a) create object to represent our cache state.
        ConcurrencyManagerState concurrencyManagerState = createConcurrencyManagerState(
                deferredLockManagers, readLockManagersOriginal, mapThreadToWaitOnAcquireOriginal,
                mapThreadToWaitOnAcquireInsideWriteLockManagerOriginal, mapThreadToWaitOnAcquireReadLockOriginal,
                setThreadWaitingToReleaseDeferredLocksOriginal, mapThreadToObjectIdWithWriteLockManagerChangesOriginal);
        dumpConcurrencyManagerInformationStep02(concurrencyManagerState);
    }

    /**
     * Dump the server log all of the information that we managed to aggregate about the current state of the
     * concurrency manager.
     *
     * @param concurrencyManagerState a snapshot of the current state of the concurrency manager and threads accessing locks.
     */
    protected void dumpConcurrencyManagerInformationStep02(ConcurrencyManagerState concurrencyManagerState) {
        StringWriter writer = new StringWriter();
        long messageNumber = currentMassiveDumpMessageLogDumpNumber.incrementAndGet();

        writer.write(TraceLocalization.buildMessage("concurrency_util_dump_concurrency_manager_information_step02_01", new Object[] {messageNumber}));
        // (a) Log information about the current threads in the system and there stack traces
        // PAGE 01 of logging information
        writer.write(createInformationThreadDump());
        // (b) log information about the threads that are waiting to acquire WRITE/DEFERRED locks
        // PAGE 02 of logging information
        writer.write(createInformationAboutAllThreadsWaitingToAcquireCacheKeys(concurrencyManagerState.getUnifiedMapOfThreadsStuckTryingToAcquireWriteLock()));
        // (c) log information about the threads that are waiting to acquire READ locks
        // PAGE 03 of logging information
        writer.write(createInformationAboutAllThreadsWaitingToAcquireReadCacheKeys(concurrencyManagerState.getMapThreadToWaitOnAcquireReadLockClone()));
        // (c) An interesting summary of information as well is to tell the user about the threads
        // that have finished their part of object building and now would like for othe threads to finish the object
        // building of locks they had to defer
        // PAGE 04 of logging information
        writer.write(createInformationAboutAllThreadsWaitingToReleaseDeferredLocks(concurrencyManagerState.getSetThreadWaitingToReleaseDeferredLocksClone()));
        // (d) Now we log information from the prespective of a THREAD to resources it has acquired and those
        // it needed to defer
        // PAGE 05 of logging information
        writer.write(createInformationAboutAllResourcesAcquiredAndDeferredByAllThreads(concurrencyManagerState));
        // (e) Dump information by going from cache key to the threads with some sort of relationship to the key
        // PAGE 06 of logging information
        writer.write(createInformationAboutCacheKeysAndThreadsMakingUseOfTheCacheKey(
                concurrencyManagerState.getMapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey()));
        // (f) Try to explain the reason for the dead lock:
        // PAGE 07: we try to find out the reason for the dead lock
        // but based on what we have seen so far it is mostly due to cache key corruption
        // with the number of readers increased
        String deadLockExplanation = dumpDeadLockExplanationIfPossible(concurrencyManagerState);
        writer.write(deadLockExplanation);
        // (g) Final header
        writer.write(TraceLocalization.buildMessage("concurrency_util_dump_concurrency_manager_information_step02_02", new Object[] {messageNumber}));
        // there should be no risk that the string is simply to big. the max string size in java is 2pow31 chars
        // which means 2 GB string... we can be fairly confident we are not logging 2 GB in a single message.
        // not even in the largest of sites...
        AbstractSessionLog.getLog().log(SessionLog.SEVERE, SessionLog.CACHE, writer.toString(), new Object[] {}, false);
    }

    /**
     * Log information focusing on the different cache keys where threads have hooks on the thread.
     *
     * @param mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey
     *            a map that we have constructed where the map keys are cache keys that are of some sort of interest to
     *            one or more threads (e.g. cache keys with a read lock, acquired or deferred)
     *
     */
    private String createInformationAboutCacheKeysAndThreadsMakingUseOfTheCacheKey(
            Map<ConcurrencyManager, CacheKeyToThreadRelationships> mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey) {
        // (a) Create a header string of information
        StringWriter writer = new StringWriter();
        int numberOfCacheKeysGettingDescribed = mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey.size();
        writer.write(TraceLocalization.buildMessage("concurrency_util_cache_keys_threads_making_use_cache_key_01", new Object[] {numberOfCacheKeysGettingDescribed}));
        int currentCacheKeyNumber = 0;
        for(Map.Entry<ConcurrencyManager, CacheKeyToThreadRelationships> currentEntry : mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey.entrySet()) {
            currentCacheKeyNumber++;
            // (b) put a clear separator from the line above
            writer.write(TraceLocalization.buildMessage("concurrency_util_cache_keys_threads_making_use_cache_key_02", new Object[] {currentCacheKeyNumber, numberOfCacheKeysGettingDescribed}));
            // (c) Describe the current cache key
            ConcurrencyManager cacheKey = currentEntry.getKey();
            String cacheKeyToString = createToStringExplainingOwnedCacheKey(cacheKey);
            CacheKeyToThreadRelationships dto = currentEntry.getValue();
            writer.write(TraceLocalization.buildMessage("concurrency_util_cache_keys_threads_making_use_cache_key_03", new Object[] {currentCacheKeyNumber, cacheKeyToString,
                    dto.getThreadNamesThatAcquiredActiveLock(), dto.getThreadNamesThatAcquiredDeferredLock(), dto.getThreadNamesThatAcquiredReadLock(),
                    dto.getThreadNamesKnownToBeStuckTryingToAcquireLock(), dto.getThreadNamesKnownToBeStuckTryingToAcquireLockForReading()}));
        }
        writer.write(TraceLocalization.buildMessage("concurrency_util_cache_keys_threads_making_use_cache_key_04"));
        return writer.toString();
    }

    protected String dumpDeadLockExplanationIfPossible(ConcurrencyManagerState concurrencyManagerState) {
        // (a) Step one - try to detect dead lock
        final long startTimeMillis = System.currentTimeMillis();
        List<DeadLockComponent> deadLockExplanation = Collections.emptyList();
        long deadLockDetectionTotalExecutionTimeMs = 0l;
        try {
            deadLockExplanation = ExplainDeadLockUtil.SINGLETON.explainPossibleDeadLockStartRecursion(concurrencyManagerState);
        } catch (Exception codeIsBuggyAndBlowingUp) {
            // we are unsure if the code will actually work and help
            // therefore we make sure we catch any blowup coming from here
            AbstractSessionLog.getLog().logThrowable(SessionLog.SEVERE, SessionLog.CACHE, new Exception(
                    TraceLocalization.buildMessage("concurrency_util_dump__dead_lock_explanation_01"),
                    codeIsBuggyAndBlowingUp));
        } finally {
            final long endTimeMillis = System.currentTimeMillis();
            deadLockDetectionTotalExecutionTimeMs = endTimeMillis - startTimeMillis;
        }
        // (b) explain what has happened
        StringWriter writer = new StringWriter();
        writer.write(TraceLocalization.buildMessage("concurrency_util_dump__dead_lock_explanation_02"));
        if (deadLockExplanation.isEmpty()) {
            writer.write(TraceLocalization.buildMessage("concurrency_util_dump__dead_lock_explanation_03"));
        } else {
            // (i) Write out a summary of how many threads are involved in the deadloc
            writer.write(TraceLocalization.buildMessage("concurrency_util_dump__dead_lock_explanation_04", new Object[] {deadLockExplanation.size()}));
            // (ii) Print them all out
            for (int currentThreadNumber = 0; currentThreadNumber < deadLockExplanation.size(); currentThreadNumber++) {
                writer.write(TraceLocalization.buildMessage("concurrency_util_dump__dead_lock_explanation_05", new Object[] {currentThreadNumber + 1, deadLockExplanation.get(currentThreadNumber).toString()}));
            }
        }
        // (c) return the string that tries to explain the reason for the dead lock
        writer.write(TraceLocalization.buildMessage("concurrency_util_dump__dead_lock_explanation_06", new Object[] {deadLockDetectionTotalExecutionTimeMs}));
        return writer.toString();
    }

    /**
     * create a DTO that tries to represent the current snapshot of the concurrency manager and write lock manager cache
     * state
     */
    public ConcurrencyManagerState createConcurrencyManagerState(
            Map<Thread, DeferredLockManager> deferredLockManagers,
            Map<Thread, ReadLockManager> readLockManagersOriginal,
            Map<Thread, ConcurrencyManager> mapThreadToWaitOnAcquireOriginal,
            Map<Thread, Set<ConcurrencyManager>> mapThreadToWaitOnAcquireInsideWriteLockManagerOriginal,
            Map<Thread, ConcurrencyManager> mapThreadToWaitOnAcquireReadLockOriginal,
            Set<Thread> setThreadWaitingToReleaseDeferredLocksOriginal,
            Map<Thread, Set<Object>> mapThreadToObjectIdWithWriteLockManagerChangesOriginal) {
        // (a) As a first step we want to clone-copy the two maps
        // once we start working with the maps and using them to do dead lock detection
        // or simply print the state of the system we do not want the maps to continue changing as the threads referenced in the maps
        // go forward with their work
        Map<Thread, ReadLockManager> readLockManagerMapClone = cloneReadLockManagerMap(readLockManagersOriginal);
        Map<Thread, DeferredLockManager> deferredLockManagerMapClone = cloneDeferredLockManagerMap(deferredLockManagers);

        // NOTE: the wait on acquire write locks are tricky
        // we want to fuse together the threads we are tracking waiting to acquire locks
        // both the one we track in the hash map of the concurrency manager
        // as well as the ones we need to track inside of the write lock manager
        Map<Thread, Set<ConcurrencyManager>> unifiedMapOfThreadsStuckTryingToAcquireWriteLock = null;
        {
            // information from the concurrency manager state
            Map<Thread, ConcurrencyManager> mapThreadToWaitOnAcquireClone = cloneMapThreadToWaitOnAcquire(mapThreadToWaitOnAcquireOriginal);
            // info from the the write lock manager state
            Map<Thread, Set<ConcurrencyManager>> mapThreadToWaitOnAcquireInsideWriteLockManagerClone = cloneMapThreadToWaitOnAcquireInsideWriteLockManagerOriginal(
                    mapThreadToWaitOnAcquireInsideWriteLockManagerOriginal);
            // merge both maps together
            enrichMapThreadToWaitOnAcquireInsideWriteLockManagerClone(mapThreadToWaitOnAcquireInsideWriteLockManagerClone, mapThreadToWaitOnAcquireClone);
            // update the variable we want to be carrying forward to be the enriched map
            unifiedMapOfThreadsStuckTryingToAcquireWriteLock = mapThreadToWaitOnAcquireInsideWriteLockManagerClone;
        }
        Map<Thread, ConcurrencyManager> mapThreadToWaitOnAcquireReadLockClone = cloneMapThreadToWaitOnAcquire(mapThreadToWaitOnAcquireReadLockOriginal);
        Set<Thread> setThreadWaitingToReleaseDeferredLocksClone = cloneSetThreadsThatAreCurrentlyWaitingToReleaseDeferredLocks(setThreadWaitingToReleaseDeferredLocksOriginal);
        Map<Thread, Set<Object>> mapThreadToObjectIdWithWriteLockManagerChangesClone = cloneMapThreadToObjectIdWithWriteLockManagerChanges(
                mapThreadToObjectIdWithWriteLockManagerChangesOriginal);
        // (b) All of the above maps tell a story from the respective of the threads
        // very interesting as well is to be able to go over the story of the cache keys and what threads have
        // expectations for these cache keys
        Map<ConcurrencyManager, CacheKeyToThreadRelationships> mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey = new HashMap<>();
        // (i) pump information about the read locks
        enrichMapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKeyInfoAboutReadLocks(
                mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey, readLockManagerMapClone);

        // (ii) pump information about the active and deferred locks
        enrichMapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKeyInfoAboutActiveAndDeferredLocks(
                mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey, deferredLockManagerMapClone);

        // (iii) Pump information into the map about the threads that are stuck because they cannot acquire a certain
        // cache key (they want to acquire the cache key for WRITING either to become active thread or to defer)
        enrichMapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKeyInfoThreadsStuckOnAcquire(
                mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey, unifiedMapOfThreadsStuckTryingToAcquireWriteLock);

        // (iv) Pump information into the map about the threads that are stuck because they cannot acquire a certain
        // cache key (they want to acquire the cache key for READING)
        enrichMapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKeyInfoThreadsStuckOnAcquireLockForReading(
                mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey, mapThreadToWaitOnAcquireReadLockClone);

        return new ConcurrencyManagerState(
                readLockManagerMapClone, deferredLockManagerMapClone, unifiedMapOfThreadsStuckTryingToAcquireWriteLock,
                mapThreadToWaitOnAcquireReadLockClone, setThreadWaitingToReleaseDeferredLocksClone,
                mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey,
                mapThreadToObjectIdWithWriteLockManagerChangesClone);
    }

    /**
     * Create a print of the ACTIVE locks associated to the DeferredLockManager. Owning an active lock on a thread
     * implies that the thread is allowed to do write operations in relation to the object.
     */
    private String createStringWithSummaryOfActiveLocksOnThread(DeferredLockManager lockManager, String threadName) {
        // (a) Make sure the lock manager being passed is not null
        StringWriter writer = new StringWriter();
        writer.write(TraceLocalization.buildMessage("concurrency_util_header_active_locks_owned_by_thread", new Object[] {threadName}));
        writer.write(TraceLocalization.buildMessage("concurrency_util_summary_active_locks_on_thread_1", new Object[] {threadName}));
        if (lockManager == null) {
            writer.write(TraceLocalization.buildMessage("concurrency_util_summary_active_locks_on_thread_2"));
            return writer.toString();
        }
        // (b) Try to build a string that lists all of the active locks on the thread
        // Loop over all of the active locks and print them
        List<ConcurrencyManager> activeLocks = new ArrayList<>(lockManager.getActiveLocks());
        writer.write(TraceLocalization.buildMessage("concurrency_util_summary_active_locks_on_thread_3", new Object[] {activeLocks.size()}));
        for (int activeLockNumber = 0; activeLockNumber < activeLocks.size(); activeLockNumber++) {
            writer.write(TraceLocalization.buildMessage("concurrency_util_summary_active_locks_on_thread_4", new Object[] {activeLockNumber, createToStringExplainingOwnedCacheKey(activeLocks.get(activeLockNumber))}));
        }
        return writer.toString();
    }

    /**
     * The {@link org.eclipse.persistence.internal.helper.DeferredLockManager} contains two baskat of locks being
     * managed for a thread. One are active locks (granted write permission). The other deferred locks (write access or
     * read access was being held by somebody else and the thread deferred).
     *
     * @param lockManager
     *            the deferred lock manager of the current thread
     * @return
     */
    private String createStringWithSummaryOfDeferredLocksOnThread(DeferredLockManager lockManager, String threadName) {
        // (a) Make sure the lock manager being passed is not null
        StringWriter writer = new StringWriter();
        writer.write(TraceLocalization.buildMessage("concurrency_util_header_deferred_locks_owned_by_thread", new Object[] {threadName}));
        writer.write(TraceLocalization.buildMessage("concurrency_util_summary_deferred_locks_on_thread_1", new Object[] {threadName}));
        if (lockManager == null) {
            writer.write(TraceLocalization.buildMessage("concurrency_util_summary_deferred_locks_on_thread_2"));
            return writer.toString();
        }
        // (b) Try to build a string that lists all of the active locks on the thread
        // Loop over all of the deferred locks and print them
        @SuppressWarnings("unchecked")
        List<ConcurrencyManager> deferredLocks = new ArrayList<>(lockManager.getDeferredLocks());
        writer.write(TraceLocalization.buildMessage("concurrency_util_summary_deferred_locks_on_thread_3", new Object[] {deferredLocks.size()}));
        for (int deferredLockNumber = 0; deferredLockNumber < deferredLocks.size(); deferredLockNumber++) {
            writer.write(TraceLocalization.buildMessage("concurrency_util_summary_deferred_locks_on_thread_4", new Object[] {deferredLockNumber, createToStringExplainingOwnedCacheKey(deferredLocks.get(deferredLockNumber))}));
        }
        return writer.toString();
    }

    /**
     * Relates to issue. We are convinced that a read lock manager is needed for two reasons: implementing a
     * dead lock detection algorithm which are currently not doing. And also beause when the code does not go according
     * to happy path and do encounter a dead lock and forced to interrupt the thread, we need to force the thread to
     * release any acquired a read locks it may have.
     *
     * @param readLockManager
     *            this is hacky class we created to close a gap in eclipselink code whereby read access on cache keys is
     *            not tracked. The only thing that happens is incrementing the nuber of readers but that is not
     *            sufficient if we need to abort all read locks.
     * @param threadName
     *            the thread for which we are logging the read locks acquired
     * @return A big string summarizing all of the read locks the thread.
     */
    private String createStringWithSummaryOfReadLocksAcquiredByThread(ReadLockManager readLockManager, String threadName) {
        // (a) Make sure the lock manager being passed is not null
        StringWriter writer = new StringWriter();
        writer.write(TraceLocalization.buildMessage("concurrency_util_header_reader_locks_owned_by_thread", new Object[] {threadName}));
        writer.write(TraceLocalization.buildMessage("concurrency_util_summary_read_locks_on_thread_step001_1", new Object[] {threadName}));
        if (readLockManager == null) {
            writer.write(TraceLocalization.buildMessage("concurrency_util_summary_read_locks_on_thread_step001_2"));
            return writer.toString();
        }
        // (b) Try to build a string that lists all of the acitve locks on the thread
        // Loop over al of the active locks and print them
        @SuppressWarnings("unchecked")
        List<ConcurrencyManager> readLocks = readLockManager.getReadLocks();
        writer.write(TraceLocalization.buildMessage("concurrency_util_summary_read_locks_on_thread_step001_3", new Object[] {readLocks.size()}));
        for (int readLockNumber = 0; readLockNumber < readLocks.size(); readLockNumber++) {
            writer.write(TraceLocalization.buildMessage("concurrency_util_summary_read_locks_on_thread_step001_4", new Object[] {readLockNumber + 1, createToStringExplainingOwnedCacheKey(readLocks.get(readLockNumber))}));
        }
        // (c) This is the main point of candidate 007 - having a lot fatter information about when and where the read
        // locks were acquired
        // (i) If a thread has 48 read locks most likely it acquired all 48 read locks in the exact same code area
        // so we want to avoid dumping 48 stack traces to the massive dump that would be completely out of control
        // we create a map of strings in order to know if we can refer to any previously created stack trace
        Map<String, Long> stackTraceStringToStackTraceExampleNumber = new HashMap<>();
        // (ii) Let us start dumping a mini header to give indication we now will sow very fact information about the
        // read locks acquired by a thread
        writer.write(TraceLocalization.buildMessage("concurrency_util_summary_read_locks_on_thread_step002_1"));
        // (iii) let us organize the iformation we are about to dump by the creation date of the records in the map
        Map<Long, List<ReadLockAcquisitionMetadata>> mapThreadToReadLockAcquisitionMetadata = readLockManager.getMapThreadToReadLockAcquisitionMetadata();
        List<Long> sortedThreadIds = new ArrayList<>(mapThreadToReadLockAcquisitionMetadata.keySet());
        Collections.sort(sortedThreadIds);
        // (iv) commence the loop of dumping trace data LOOP OVER EACH JPA TRANSACTION ID
        for (Long currentThreadId : sortedThreadIds) {
            List<ReadLockAcquisitionMetadata> readLocksAcquiredByThread = mapThreadToReadLockAcquisitionMetadata.get(currentThreadId);
            writer.write(TraceLocalization.buildMessage("concurrency_util_summary_read_locks_on_thread_step002_2", new Object[] {threadName, currentThreadId, readLocksAcquiredByThread.size()}));
            // LOOP OVER EACH CACHE KEY ACQUIRED FORE READING BUT NEVER RELEASED FOR CURRENT THREAD ID
            int readLockNumber = 0;
            for (ReadLockAcquisitionMetadata currentReadLockAcquiredAndNeverReleased : readLocksAcquiredByThread) {
                readLockNumber++;
                writer.write(TraceLocalization.buildMessage("concurrency_util_summary_read_locks_on_thread_step002_3", new Object[] {readLockNumber,
                        SINGLETON.createToStringExplainingOwnedCacheKey(currentReadLockAcquiredAndNeverReleased.getCacheKeyWhoseNumberOfReadersThreadIsIncrementing()),
                        currentReadLockAcquiredAndNeverReleased.getDateOfReadLockAcquisition(),
                        currentReadLockAcquiredAndNeverReleased.getNumberOfReadersOnCacheKeyBeforeIncrementingByOne(),
                        currentReadLockAcquiredAndNeverReleased.getCurrentThreadStackTraceInformationCpuTimeCostMs()}));
                String stackTraceInformation = currentReadLockAcquiredAndNeverReleased.getCurrentThreadStackTraceInformation();
                if (stackTraceStringToStackTraceExampleNumber.containsKey(stackTraceInformation)) {
                    // we can spare the massive dump from being any fatter we have alreayd added a stack trace id that
                    // is identical to the stack trace were were about dump
                    // we just refer to the stack trace id.
                    writer.write(TraceLocalization.buildMessage("concurrency_util_summary_read_locks_on_thread_step002_4", new Object[] {readLockNumber, stackTraceStringToStackTraceExampleNumber.get(stackTraceInformation)}));
                } else {
                    // Since we have not see this stack trace pattern for this thread yet we will dump the stack trace
                    // into the massive dump giving it a new global id
                    long stackTraceId = stackTraceIdAtomicLong.incrementAndGet();
                    stackTraceStringToStackTraceExampleNumber.put(stackTraceInformation, stackTraceId);
                    writer.write(TraceLocalization.buildMessage("concurrency_util_summary_read_locks_on_thread_step002_5", new Object[] {readLockNumber, stackTraceId, stackTraceInformation}));
                }
                writer.write("\n\n");
            }
        }

        // (d) We have some more information to pump out namely errors we have traced each time the number of readers was decremented
        writer.write("\n\n");
        writer.write(TraceLocalization.buildMessage("concurrency_util_summary_read_locks_on_thread_step002_6", new Object[] {threadName, readLockManager.getRemoveReadLockProblemsDetected().size()}));
        for (int releaseReadLockProblemNumber = 0; releaseReadLockProblemNumber < readLockManager.getRemoveReadLockProblemsDetected().size(); releaseReadLockProblemNumber++) {
            writer.write(TraceLocalization.buildMessage("concurrency_util_summary_read_locks_on_thread_step002_7", new Object[] {releaseReadLockProblemNumber + 1, readLockManager.getRemoveReadLockProblemsDetected().get(releaseReadLockProblemNumber)}));
        }
        writer.write("\n\n");
        return writer.toString();
    }

    /**
     * This helper API is created due to the problem of the corruption of the eclipselink cache. The idea is to have a
     * tool that allows us to know specifically where the current thread was located when it acquired a READ LOCK.
     *
     * <P>
     * Cache corruption problem: <br>
     * namely the fact that when dead locks are seen to be taking place some of the threads that seem to be primary
     * culprits of the dead lock are actually idle doing nothing but they have have left the number of readers of the
     * cache corrupted (e.g. typically forever incremnted).
     *
     * @return get the stack trace of the current thread.
     */
    private String enrichGenerateThreadDumpForCurrentThread() {
        final Thread currentThread = Thread.currentThread();
        final long currentThreadId = currentThread.getId();

        try {
            // (a) search for the stack trace of the current
            final StringWriter writer = new StringWriter();
            ThreadInfo[] threadInfos = null;
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
                threadInfos = AccessController.doPrivileged(new PrivilegedGetThreadInfo(new long[] { currentThreadId }, 700));
            } else {
                final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
                threadInfos = threadMXBean.getThreadInfo(new long[] { currentThreadId }, 700);
            }
            
            for (ThreadInfo threadInfo : threadInfos) {
                enrichGenerateThreadDumpForThreadInfo(writer, threadInfo);
            }
            return writer.toString();
        } catch (Exception failToAcquireThreadDumpProgrammatically) {
            AbstractSessionLog.getLog().logThrowable(SessionLog.SEVERE, SessionLog.CACHE, failToAcquireThreadDumpProgrammatically);
            return TraceLocalization.buildMessage("concurrency_util_enrich_thread_dump", new Object[] {failToAcquireThreadDumpProgrammatically.getMessage()});
        }
    }

    /**
     * We simply copy pasted this code from the net to have some helper tool to generate thread dumps programatically
     * when the event takes place.
     *
     * <P>
     * NOTE: This approach can be easily tested in a basic unit test.
     *
     *
     * <a href="https://crunchify.com/how-to-generate-java-thread-dump-programmatically/">Original source of code</a>
     *
     */
    private String enrichGenerateThreadDump() {
        try {
            final StringWriter writer = new StringWriter();
            
            ThreadInfo[] threadInfos = null;
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
                threadInfos = AccessController.doPrivileged(new PrivilegedGetThreadInfo(700));
            } else {
                final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
                threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 700);
            }
            
            for (ThreadInfo threadInfo : threadInfos) {
                enrichGenerateThreadDumpForThreadInfo(writer, threadInfo);
            }
            return writer.toString();
        } catch (Exception failToAcquireThreadDumpProgrammatically) {
            AbstractSessionLog.getLog().logThrowable(SessionLog.SEVERE, SessionLog.CACHE, failToAcquireThreadDumpProgrammatically);
            return TraceLocalization.buildMessage("concurrency_util_enrich_thread_dump", new Object[] {failToAcquireThreadDumpProgrammatically.getMessage()});
        }
    }

    /**
     * Enrich a given string building with the the thread writer for a given thread info object.
     */
    private void enrichGenerateThreadDumpForThreadInfo(StringWriter writer, ThreadInfo threadInfo) {
        writer.write(TraceLocalization.buildMessage("concurrency_util_enrich_thread_dump_thread_info_1", new Object[] {threadInfo.getThreadName(), threadInfo.getThreadState()}));
        final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
        for (final StackTraceElement stackTraceElement : stackTraceElements) {
            writer.write(TraceLocalization.buildMessage("concurrency_util_enrich_thread_dump_thread_info_2", new Object[] {stackTraceElement}));
        }
        writer.write("\n\n");
    }

    /**
     * Write a severe log message with the current thread dump.
     */
    private String createInformationThreadDump() {
        StringWriter writer = new StringWriter();
        writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_thread_dump", new Object[] {enrichGenerateThreadDump()}));
        return writer.toString();
    }

    /**
     * In this page of log dumping information we want to give a summary to the user of threads that appear to be stuck
     * doing an acquire of the cache key.
     *
     * @param unifiedMapOfThreadsStuckTryingToAcquireWriteLock
     *            this a cloned map that has an association between thread and cache keys the thread would like to
     *            acquire but cannot because there are readers on the cache key. The thread might be stuck either on the
     *            concurrency manager or on the write lock manager.
     */
    private String createInformationAboutAllThreadsWaitingToAcquireCacheKeys(Map<Thread, Set<ConcurrencyManager>> unifiedMapOfThreadsStuckTryingToAcquireWriteLock) {
        // (a) Create a header string of information
        StringWriter writer = new StringWriter();
        writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_threads_acquire_cache_keys_1", new Object[] {unifiedMapOfThreadsStuckTryingToAcquireWriteLock.size()}));
        int currentThreadNumber = 0;
        for (Map.Entry<Thread, Set<ConcurrencyManager>> currentEntry : unifiedMapOfThreadsStuckTryingToAcquireWriteLock
                .entrySet()) {
            currentThreadNumber++;
            Thread thread = currentEntry.getKey();
            Set<ConcurrencyManager> writeLocksCurrentThreadIsTryingToAcquire = currentEntry.getValue();
            for (ConcurrencyManager cacheKey : writeLocksCurrentThreadIsTryingToAcquire) {
                writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_threads_acquire_cache_keys_2", new Object[] {currentThreadNumber, thread.getName(), createToStringExplainingOwnedCacheKey(cacheKey)}));
            }
        }
        writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_threads_acquire_cache_keys_3"));
        return writer.toString();
    }

    /**
     * In this page of log dumping information we want to give a summary to the user of threads that appear to be stuck
     * doing an acquire of the cache key.
     *
     * @param mapThreadToWaitOnAcquireReadLockClone
     *            this a cloned map that has an association between thread and cache keys the thread would like to
     *            acquire for READING but cannot because there is some active thread (other than themselves) holding the cache key (e.g. for writing)
     */
    protected String createInformationAboutAllThreadsWaitingToAcquireReadCacheKeys(
            Map<Thread, ConcurrencyManager> mapThreadToWaitOnAcquireReadLockClone) {
        // (a) Create a header string of information
        StringWriter writer = new StringWriter();
        writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_threads_acquire_read_cache_keys_1", new Object[] {mapThreadToWaitOnAcquireReadLockClone.size()}));

        int currentThreadNumber = 0;
        for(Map.Entry<Thread, ConcurrencyManager> currentEntry : mapThreadToWaitOnAcquireReadLockClone.entrySet()) {
            currentThreadNumber++;
            writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_threads_acquire_read_cache_keys_2", new Object[] {currentThreadNumber, currentEntry.getKey().getName(), createToStringExplainingOwnedCacheKey(currentEntry.getValue())}));
        }
        writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_threads_acquire_read_cache_keys_3"));
        return writer.toString();
    }

    /**
     * Log information about threads not moving forward because they are waiting for the
     * {@code isBuildObjectOnThreadComplete } to return true.
     *
     * @param setThreadWaitingToReleaseDeferredLocksClone
     *            threads waiting for the release deferred lock process to complete.
     */
    protected String createInformationAboutAllThreadsWaitingToReleaseDeferredLocks(Set<Thread> setThreadWaitingToReleaseDeferredLocksClone) {
        // (a) Create a header string of information
        StringWriter writer = new StringWriter();
        writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_threads_release_deferred_locks_1", new Object[] {setThreadWaitingToReleaseDeferredLocksClone.size()}));

        // (b) add to the string building information about each of these threads that are stuck in the
        // isBuildObjectOnThreadComplete
        int currentThreadNumber = 0;
        for (Thread currentEntry : setThreadWaitingToReleaseDeferredLocksClone) {
            currentThreadNumber++;
            writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_threads_release_deferred_locks_1", new Object[] {currentThreadNumber, currentEntry.getName()}));
        }
        writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_threads_release_deferred_locks_3"));
        return writer.toString();
    }

    /**
     * Log information about all threads tracked in the concurrency manager.
     *
     * @param concurrencyManagerState
     *            and object that represents a snaphot of the current state of the concurrency manager.
     */
    protected String createInformationAboutAllResourcesAcquiredAndDeferredByAllThreads(
            ConcurrencyManagerState concurrencyManagerState) {
        // (a) Compile a union of all threads
        Set<Thread> allRelevantThreads = new HashSet<>();
        allRelevantThreads.addAll(concurrencyManagerState.getSetThreadWaitingToReleaseDeferredLocksClone());
        allRelevantThreads.addAll(concurrencyManagerState.getUnifiedMapOfThreadsStuckTryingToAcquireWriteLock().keySet());
        allRelevantThreads.addAll(concurrencyManagerState.getDeferredLockManagerMapClone().keySet());
        allRelevantThreads.addAll(concurrencyManagerState.getReadLockManagerMapClone().keySet());

        // (b) print information about all threads
        StringWriter writer = new StringWriter();
        int currentThreadNumber = 0;
        final int totalNumberOfThreads = allRelevantThreads.size();
        for(Thread currentThread : allRelevantThreads) {
            currentThreadNumber++;
            ReadLockManager readLockManager = concurrencyManagerState.getReadLockManagerMapClone().get(currentThread);
            DeferredLockManager lockManager = concurrencyManagerState.getDeferredLockManagerMapClone().get(currentThread);
            Set<ConcurrencyManager> waitingOnAcquireCacheKeys = concurrencyManagerState
                    .getUnifiedMapOfThreadsStuckTryingToAcquireWriteLock()
                    .get(currentThread);
            ConcurrencyManager waitingOnAcquireReadCacheKey = concurrencyManagerState
                    .getMapThreadToWaitOnAcquireReadLockClone().get(currentThread);
            boolean threadWaitingToReleaseDeferredLocks = concurrencyManagerState
                    .getSetThreadWaitingToReleaseDeferredLocksClone().contains(currentThread);

            Set<Object> wirteManagerThreadPrimaryKeysWithChangesToBeMerged = concurrencyManagerState
                    .getMapThreadToObjectIdWithWriteLockManagerChangesClone()
                    .get(currentThread);
            String informationAboutCurrentThread = createInformationAboutAllResourcesAcquiredAndDeferredByThread(
                    readLockManager, lockManager, waitingOnAcquireCacheKeys, waitingOnAcquireReadCacheKey,
                    threadWaitingToReleaseDeferredLocks, currentThread, currentThreadNumber, totalNumberOfThreads,
                    wirteManagerThreadPrimaryKeysWithChangesToBeMerged);
            writer.write(informationAboutCurrentThread);
        }

        // (c) Log on the serverlog information about all the threads being tracked in the concurrency manager
        return writer.toString();
    }

    /**
     * Build a string that tries to describe in as much detail as possible the resources associated to the current
     * thread.
     *
     * @param readLockManager
     *            the read lock manager for the current thread
     * @param lockManager
     *            the lock manager for the current thread
     * @param waitingOnAcquireCacheKeys
     *            null if the current thread is not waiting to acquire a cache key otherwise the cachekey that the
     *            current thread wants to acquire and that is making it block. This field evolved to be a set and not
     *            just one cache key because when we needed to tweak the write lock manager code to report about why the
     *            write lock manager is stuck we need it to create the map
     *            {@link org.eclipse.persistence.internal.helper.WriteLockManager#THREAD_TO_FAIL_TO_ACQUIRE_CACHE_KEYS}
     *            whereby during a commit where entiteis are merged into the shared cache a thread might be trying to
     *            grab several write locks. so here we have a mix between the concurrency manager cache key a thread
     *            currently wants together with cache keys the write lock managed is not managing to grab.
     *
     * @param waitingOnAcquireReadCacheKey
     *            cache key the thread is failing to acquire in the
     *            {@link org.eclipse.persistence.internal.helper.ConcurrencyManager#acquireReadLock()}
     * @param threadWaitingToReleaseDeferredLocks
     *            true if the curren thread is now blocked waiting to confirm the locks it deferred have finished
     *            building the corresponding objects.
     * @param thread
     *            the thread eing described
     * @param currentThreadNumber
     *            just loop incremented index to help the dump log messages give the feeling of the current thread being
     *            described and how many more threads are still to be described
     * @param totalNumberOfThreads
     *            the total number of threads being described in a for loop
     * @return a string describing the thread provided. We can see the active locks, deferred locks, read locks etc...
     *         as well sa if the thread is waiting to acquire a specific cache key or waiting for build object to
     *         complete.
     * @param writeManagerThreadPrimaryKeysWithChangesToBeMerged
     *            Null for all threads excep those that are currently about to commit and merge changes to the shared
     *            cache. In this case it holds the primary keys of the objects that were changed by the transaction. The
     *            write lock manager has been tweaked to store information about objects ids that the current thread has
     *            in its hands and that will required for write locks to be acquired by a committing thread. This
     *            information is especially interesting if any thread participating in a dead lock is getting stuck in
     *            the acquisition of write locks as part of the commit process. This information might end up revealing
     *            a thread that has done too many changes and is creating a bigger risk fo dead lock. The more resources
     *            an individual thread tries to grab the worse it is for the concurrency layer. The size of the change
     *            set can be interesting.
     */
    protected String createInformationAboutAllResourcesAcquiredAndDeferredByThread(
            ReadLockManager readLockManager, DeferredLockManager lockManager,
            Set<ConcurrencyManager> waitingOnAcquireCacheKeys, ConcurrencyManager waitingOnAcquireReadCacheKey,
            boolean threadWaitingToReleaseDeferredLocks, Thread thread,
            int currentThreadNumber, int totalNumberOfThreads,
            Set<Object> writeManagerThreadPrimaryKeysWithChangesToBeMerged) {

        // (a) Build a base overview summary of the thread state
        StringWriter writer = new StringWriter();
        String threadName = thread.getName();
        // (i) A base summary about the current thread
        // is the thread waiting to acquire a lock or is it waiting to release deferred locks
        writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_resources_acquired_deferred_1", new Object[] {currentThreadNumber, totalNumberOfThreads, thread.getName(), threadWaitingToReleaseDeferredLocks}));
        // (iii) Information is this is a thread in the process of trying to acquire for writing a cache key
        if (waitingOnAcquireCacheKeys != null && !waitingOnAcquireCacheKeys.isEmpty()) {
            for (ConcurrencyManager waitingOnAcquireCacheKey : waitingOnAcquireCacheKeys) {
                writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_resources_acquired_deferred_2", new Object[] {createToStringExplainingOwnedCacheKey(waitingOnAcquireCacheKey)}));
            }
        } else {
            writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_resources_acquired_deferred_3"));
        }
        // (iv) Information is this is a thread in the process of trying to acquire for reading a cache key
        if (waitingOnAcquireReadCacheKey != null) {
            writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_resources_acquired_deferred_4", new Object[] {createToStringExplainingOwnedCacheKey(waitingOnAcquireReadCacheKey)}));
        } else {
            writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_resources_acquired_deferred_5"));
        }
        // (v) if the thread is stuck in the write lock manager trying to acquire all write locks to commit and merge
        // changes to the shared
        // cache this information might be interesting
        boolean currentThreadIsTryingCommitToSharedCacheChanges = writeManagerThreadPrimaryKeysWithChangesToBeMerged != null
                && !writeManagerThreadPrimaryKeysWithChangesToBeMerged.isEmpty();
        if (currentThreadIsTryingCommitToSharedCacheChanges) {
            writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_resources_acquired_deferred_6", new Object[] {writeManagerThreadPrimaryKeysWithChangesToBeMerged.toString()}));
        } else {
            writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_resources_acquired_deferred_7"));
        }
        // Start dumping information about the deferred lock and read lock manager of this thread
        // (b) Add information about the cache keys where the current thread was set as actively owning
        writer.write(ConcurrencyUtil.SINGLETON.createStringWithSummaryOfActiveLocksOnThread(lockManager, threadName));
        // (c) Now very interesting as well are all of the objects that current thread could not acquire the deferred locks are essential
        writer.write(createStringWithSummaryOfDeferredLocksOnThread(lockManager, threadName));
        // (d) Add information about all cache keys te current thread acquired with READ permission
        writer.write(createStringWithSummaryOfReadLocksAcquiredByThread(readLockManager, threadName));
        writer.write(TraceLocalization.buildMessage("concurrency_util_create_information_all_resources_acquired_deferred_8", new Object[] {currentThreadNumber, totalNumberOfThreads}));
        return writer.toString();
    }

    /**
     * Clone the static map of the concurrency manager that tells us about threads waiting to acquire locks.
     *
     * @param mapThreadToWaitOnAcquireOriginal
     *            the original map we want to clone
     * @return a cloned map
     */
    public static Map<Thread, ConcurrencyManager> cloneMapThreadToWaitOnAcquire(Map<Thread, ConcurrencyManager> mapThreadToWaitOnAcquireOriginal) {
        return new HashMap<>(mapThreadToWaitOnAcquireOriginal);
    }

    /**
     * Clone the static map of the concurrency manager that tells us about threads waiting to acquire locks.
     *
     * @param mapThreadToWaitOnAcquireInsideWriteLockManagerOriginal
     *            the original map we want to clone
     * @return a cloned map
     */
    public static Map<Thread, Set<ConcurrencyManager>> cloneMapThreadToWaitOnAcquireInsideWriteLockManagerOriginal(
            Map<Thread, Set<ConcurrencyManager>> mapThreadToWaitOnAcquireInsideWriteLockManagerOriginal) {
        Map<Thread, Set<ConcurrencyManager>> result = new HashMap<>();
        // this iterator is safe because the original map is a concurrent hashmap so the iterator should not blow up
        for (Map.Entry<Thread, Set<ConcurrencyManager>> entry : mapThreadToWaitOnAcquireInsideWriteLockManagerOriginal.entrySet()) {
            Set<ConcurrencyManager> clonedSet = new HashSet<>(entry.getValue());
            result.put(entry.getKey(), clonedSet);
        }
        return result;
    }


    /**
     * We have two maps we are using to trace threads that are stuck acquiring locks.
     * One map is found in the concurrency manager the other in the write lock manager.
     * When we start dumping information we only care about working with one and only one map.
     * Therefore we merge the two maps together since semantically they both mean the exact same thing:
     * a thread A wants a cachekey B for writing and is not getting it.
     *
     * @param mapThreadToWaitOnAcquireInsideWriteLockManagerClone
     *      this is the map we want o enrich
     * @param mapThreadToWaitOnAcquireClone
     *      this is the map whose entries we want to copy into the map to enrich
     */
    public static void enrichMapThreadToWaitOnAcquireInsideWriteLockManagerClone(
            Map<Thread, Set<ConcurrencyManager>> mapThreadToWaitOnAcquireInsideWriteLockManagerClone, Map<Thread, ConcurrencyManager> mapThreadToWaitOnAcquireClone ) {
        // (a) Loop over each of the threads the map of mapThreadToWaitOnAcquireClone
        // and add the cache keys threads are waiting for into the corresponding entery of the
        // mapThreadToWaitOnAcquireInsideWriteLockManagerClone
        for (Map.Entry<Thread, ConcurrencyManager> entry : mapThreadToWaitOnAcquireClone.entrySet()) {
            Thread currentThread = entry.getKey();
            if(!mapThreadToWaitOnAcquireInsideWriteLockManagerClone.containsKey(currentThread)) {
                mapThreadToWaitOnAcquireInsideWriteLockManagerClone.put(currentThread, new HashSet<>());
            }
            Set<ConcurrencyManager> cacheKeys =  mapThreadToWaitOnAcquireInsideWriteLockManagerClone.get(currentThread);
            cacheKeys.add(entry.getValue());
        }
    }

    /**
     * A set of threads that are at the end of object building and are waiting for the deferred locks to be resolved.
     *
     * @param setThreadWaitingToReleaseDeferredLocksOriginal
     *            the original set of threads that are waiting for deferred locks to be resolved.
     * @return A cloned has set of threads waiting for their deferred locks to be resolved.
     */
    public static Set<Thread> cloneSetThreadsThatAreCurrentlyWaitingToReleaseDeferredLocks(
            Set<Thread> setThreadWaitingToReleaseDeferredLocksOriginal) {
        return new HashSet<>(setThreadWaitingToReleaseDeferredLocksOriginal);
    }

    /**
     * Clone the information about threads that are in the write lock manager trying to commit and the object ids they
     * are holding with some arbitrary changes.
     *
     * @param mapThreadToObjectIdWithWriteLockManagerChangesOriginal
     *            map of thread to the primary keys of of objects changed by a transaction in the commit phase. This is
     *            the original map grabbed from the WriteLockManager.
     * @return a cloned map of thread to object id primary keys that a thread committing might have changed.
     */
    public static Map<Thread, Set<Object>> cloneMapThreadToObjectIdWithWriteLockManagerChanges(
            Map<Thread, Set<Object>> mapThreadToObjectIdWithWriteLockManagerChangesOriginal) {
        Map<Thread, Set<Object>> result = new HashMap<>();
        for (Map.Entry<Thread, Set<Object>> currentEntry : mapThreadToObjectIdWithWriteLockManagerChangesOriginal.entrySet()) {
            result.put(currentEntry.getKey(), new HashSet<>(currentEntry.getValue()));
        }
        return result;
    }

    /**
     * To facilitate algorithms that want to dump a snapshot of the current state of the concurrency manager or to start
     * a hunt for dead locks this api faciliates the boostraping logic of such algorithms by giving the algorithm a
     * stable clone of the map of read locks that we know will not change throughout the time the algorithm is running.
     *
     * @param readLockManagersOriginal
     *            This the original map of read locks referred by the concurrency manager. This is a very bad platform
     *            to work with because if for whatever reason not all threads are frozen and some are actualy managing
     *            to complete their transactions the contents of this map are systematically changing with threds being
     *            added in and removed.
     * @return A clone of the readLockManagersOriginal. Essentially the map instance returned is new and independent and
     *         the values {@link ReadLockManager} are also clones and independent. The only thing that is
     *         not cloned here - whose state could be changing - are the cache key themselves. The cache keys pointed by
     *         the vector {@link ReadLockManager#getReadLocks()} are the original values. So our clone
     *         from the read lock manager is not a perfectly stable clone. It will not be blowing up telling us
     *         concurrent access modification when we loop through the vector. But no one can guarnate the number of
     *         readers on the cache key stays the same nor that the active thread on a cache key stays the same... Those
     *         values can definitely be fluctuating (not ideal ... but it would be quite hard to get better than this).
     */
    public Map<Thread, ReadLockManager> cloneReadLockManagerMap(Map<Thread, ReadLockManager> readLockManagersOriginal) {
        // (a) Start by safeguarding the keys of the map we want to clone
        // (e.g. avoid the risk of concurrent modification exception while looping over a keyset)
        List<Thread> mapKeys = new ArrayList<>(readLockManagersOriginal.keySet());

        // (b) start the the cloning process
        Map<Thread, ReadLockManager> cloneResult = new HashMap<>();
        for (Thread currentKey : mapKeys) {
            ReadLockManager readLockManagerOriginal = readLockManagersOriginal.get(currentKey);
            if (readLockManagerOriginal != null) {
                ReadLockManager readLockManagerClone = readLockManagerOriginal.clone();
                cloneResult.put(currentKey, readLockManagerClone);
            } else {
                // most likely the current thread has just finished its work
                // and is no longer to be found in the original map
            }
        }

        // (c) The caller of this method can do with it whatever it wants because no one will be modifying this map
        // nor the contained
        return cloneResult;
    }

    /**
     * The exact same thing as the {@link #cloneReadLockManagerMap(Map)} but the map we are cloning here is the one of
     * threads to deferred locks
     *
     * @param deferredLockManagersOriginal
     *            the original map taken from the conrruency manager itself
     * @return A clone of that map that is a relatively stable data structure to work with since no new threads will
     *         register in or out in the map nor will the DeferredLockManager values be changing. As for the read lock
     *         manager we have no assurance as to what is happening with the cache keys themselves refered by the
     *         {@link DeferredLockManager} values, the cache keys are always changing their metadata as new threads come
     *         in to do work or finish doing work. So it is not a perfect snapshot of the state of the system, but it is
     *         as close as we can get.
     */
    public Map<Thread, DeferredLockManager> cloneDeferredLockManagerMap(Map<Thread, DeferredLockManager> deferredLockManagersOriginal) {
        // (a) Start by safeguarding the keys of the map we want to clone
        // (e.g. avoid the risk of concurrent modification exception while looping over a keyset)
        List<Thread> mapKeys = new ArrayList<>(deferredLockManagersOriginal.keySet());

        // (b) start the the cloning process
        Map<Thread, DeferredLockManager> cloneResult = new HashMap<>();
        for (Thread currentKey : mapKeys) {
            DeferredLockManager deferredLockManagerOriginal = deferredLockManagersOriginal.get(currentKey);
            if (deferredLockManagerOriginal != null) {
                DeferredLockManager deferredLockManagerClone = cloneDeferredLockManager(deferredLockManagerOriginal);
                cloneResult.put(currentKey, deferredLockManagerClone);
            } else {
                // most likely the current thread has just finished its work
                // and is no longer to be found in the original map
            }
        }

        // (c) The caller of this method can do with it whatever it wants because no one will be modifying this map
        // nor the contained
        return cloneResult;

    }

    /**
     * Clone an original {@link DeferredLockManager} so that our algorithms of state dump or dead lock search can safely
     * work ina stable model state that is not constantly changing.
     *
     * @param deferredLockManagerOriginal
     *            an object that is originating from the map of thread to deferred locks from the concurrency manager
     *            class. We do not want to be carrying around the original object while try to make a dump/snapshot of
     *            the current state of the concurrency manager since these objects are always mutating. Locks are being
     *            acquired and released etc... All the tiem. The only objest thta will be stable are those of threads
     *            involved ina dead lock. And those are the threads that matter the most to us anyway.
     * @return a cloned deferred lock manager. The newly created deferred lock manager will have its vectors of cache
     *         keys holding references the same cache keys as the original object. The cache keys themselves are not
     *         cloned. That measn that the DeferredLockManager will be immuatable in terms of its vectors and held
     *         references. But the objects it refers to (e.g. cache keys) can be mutating all the time if new readers or
     *         active threads arrive.
     */
    @SuppressWarnings("rawtypes")
    public DeferredLockManager cloneDeferredLockManager(DeferredLockManager deferredLockManagerOriginal) {
        // (a) Start by cloning from the original the two vectors of cache keys is administers
        Vector cloneOfActiveLocks = (Vector) deferredLockManagerOriginal.getActiveLocks().clone();
        Vector cloneOfDeferredLocks = (Vector) deferredLockManagerOriginal.getDeferredLocks().clone();

        // (b) Build our clone object
        DeferredLockManager deferredLockManagerClone = new DeferredLockManager();
        deferredLockManagerClone.setIsThreadComplete(deferredLockManagerOriginal.isThreadComplete());

        // NOTE: the DeferredLockManager follows a very bad practice
        // it gives direct acess to its internal state from outside
        // it gives us direct access to its referred lists
        // so the internal private state of the deferredLockManager can be modified directly from the outisde
        // by anybody...
        // but we use the apis we have access to.
        deferredLockManagerClone.getActiveLocks().addAll(cloneOfActiveLocks);
        deferredLockManagerClone.getDeferredLocks().addAll(cloneOfDeferredLocks);
        return deferredLockManagerClone;
    }

    /**
     * Enrich the mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey by setting on the cache keys the threads
     * that are stuck trying to acquire the cache key.
     *
     * @param mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey
     *            the map we are trying to enirhc with additional information
     * @param unifiedMapOfThreadsStuckTryingToAcquireWriteLock
     *            a map telling us about threads that at a certain point in time were not progressing anywhere because
     *            they were waiting to acquire a write lock. These are threads either stuck on the concurrency manager
     *            or in the write lock manager during a transaction commmit
     *
     *
     */
    public void enrichMapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKeyInfoThreadsStuckOnAcquire(
            Map<ConcurrencyManager, CacheKeyToThreadRelationships> mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey,
            Map<Thread, Set<ConcurrencyManager>> unifiedMapOfThreadsStuckTryingToAcquireWriteLock) {

        // (a) Loop over each thread that registered itself as being waiting to lock a cache key
        for (Map.Entry<Thread, Set<ConcurrencyManager>> currentEntry : unifiedMapOfThreadsStuckTryingToAcquireWriteLock
                .entrySet()) {
            Thread currentThread = currentEntry.getKey();
            for (ConcurrencyManager cacheKeyThreadIsWaitingToAcquire : currentEntry.getValue()) {
                CacheKeyToThreadRelationships dto = get(cacheKeyThreadIsWaitingToAcquire, mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey);
                dto.addThreadsKnownToBeStuckTryingToAcquireLock(currentThread);
            }
        }
    }

    /**
     * Enrich the mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey by setting on the cache keys the threads
     * that are stuck trying to acquire the cache key with a read lock. These are threads stuck on the
     * {@link org.eclipse.persistence.internal.helper.ConcurrencyManager#acquireReadLock()}
     *
     * @param mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey
     *            the map we are trying to enirhc with additional information
     * @param mapThreadToWaitOnAcquireReadLockClone
     *            a map telling us about threads that at a certain point in time were not progressing anywhere because
     *            they were waiting to acquire a lock.
     *
     *
     */
    public void enrichMapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKeyInfoThreadsStuckOnAcquireLockForReading(
            Map<ConcurrencyManager, CacheKeyToThreadRelationships> mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey,
            Map<Thread, ConcurrencyManager> mapThreadToWaitOnAcquireReadLockClone) {

        // (a) Loop over each thread that registered itself as being waiting to lock a cache key
        for (Map.Entry<Thread, ConcurrencyManager> currentEntry : mapThreadToWaitOnAcquireReadLockClone.entrySet()) {
            Thread currentThread = currentEntry.getKey();
            ConcurrencyManager cacheKeyThreadIsWaitingToAcquire = currentEntry.getValue();
            CacheKeyToThreadRelationships dto = get(cacheKeyThreadIsWaitingToAcquire,
                    mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey);
            dto.addThreadsKnownToBeStuckTryingToAcquireLockForReading(currentThread);
        }
    }

    /**
     * Enrich the mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey based on the read locks
     *
     * @param mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey
     *            a map whose metadata we need to enrich
     * @param readLockManagerMapClone
     *            map cloned from the original map and that gives us a snapshot of threads that acquired read locks
     */
    public void enrichMapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKeyInfoAboutReadLocks(
            Map<ConcurrencyManager, CacheKeyToThreadRelationships> mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey,
            Map<Thread, ReadLockManager> readLockManagerMapClone) {

        // (a) Loop over each thread that is regisered as having acquired read locks
        for (Map.Entry<Thread, ReadLockManager> currentEntry : readLockManagerMapClone.entrySet()) {
            Thread currentThread = currentEntry.getKey();
            ReadLockManager currentValue = currentEntry.getValue();
            // (b) loop over each read lock acquired by the current thread
            // enrich the map of cache key to thread doing something in respect to the cache key
            for (ConcurrencyManager cacheKeyAcquiredReadLock : currentValue.getReadLocks()) {
                CacheKeyToThreadRelationships dto = get(cacheKeyAcquiredReadLock,
                        mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey);
                dto.addThreadsThatAcquiredReadLock(currentThread);
            }
        }
    }

    /**
     * Enrich our map map of cache key to threads having a relationship with that object in regards to active locks on
     * the cache key and deferred locks on the cache key
     *
     * @param mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey
     *            the map we want to enrich with more information
     * @param deferredLockManagerMapClone
     *            the cloned map with information about threads and their deferred locks.
     */
    public void enrichMapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKeyInfoAboutActiveAndDeferredLocks(
            Map<ConcurrencyManager, CacheKeyToThreadRelationships> mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey,
            Map<Thread, DeferredLockManager> deferredLockManagerMapClone) {

        // (a) Loop over each thread that has a deferred lock manager
        for (Map.Entry<Thread, DeferredLockManager> currentEntry : deferredLockManagerMapClone.entrySet()) {
            Thread currentThread = currentEntry.getKey();
            DeferredLockManager currentValue = currentEntry.getValue();

            // (b) First we focus on the active locks owned by the thread
            // enrich the map of cache key to thread doing something in respect to the cache key
            for (Object activeLockObj : currentValue.getActiveLocks()) {
                ConcurrencyManager activeLock = (ConcurrencyManager) activeLockObj;
                CacheKeyToThreadRelationships dto = get(activeLock, mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey);
                dto.addThreadsThatAcquiredActiveLock(currentThread);
            }

            // (c) Now we go over the defferred locks on this thread
            // (e.g. object locks that it could not acquire because some other thread was active at the time owning the
            // lock)
            for (Object deferredLockObj : currentValue.getDeferredLocks()) {
                ConcurrencyManager deferredLock = (ConcurrencyManager) deferredLockObj;
                CacheKeyToThreadRelationships dto = get(deferredLock, mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey);
                dto.addThreadsThatAcquiredDeferredLock(currentThread);
            }
        }
    }

    /**
     * Helper method to make sure we never get null dto from the
     * mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey.
     *
     * @param cacheKey
     *            the cache key we are search for
     * @param mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey
     *            the map of cache key to concurrency manager locking metadata
     * @return never returls null. If the cache key is not yet in the map a ney entry is returned.
     */
    protected CacheKeyToThreadRelationships get(ConcurrencyManager cacheKey,
                                                                 Map<ConcurrencyManager, CacheKeyToThreadRelationships> mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey) {
        if (!mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey.containsKey(cacheKey)) {
            mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey.put(cacheKey,
                    new CacheKeyToThreadRelationships(cacheKey));
        }
        return mapOfCacheKeyToDtosExplainingThreadExpectationsOnCacheKey.get(cacheKey);
    }



    /**
     * If when we are decrement the counter of number of readers on a cache key we find ourselves lacking the read lock
     * manager at the time of the decrement we want to log a big fat error on the server log protecting that the current
     * thread is misbehaving.
     *
     * @param currentNumberOfReaders
     *            the current count of readers on the cache key about to be decremented
     * @param decrementedNumberOfReaders
     *            the number of readers of the cache key if we subtract one reader
     * @param cacheKey
     *            the cache key that is about to suffer a decrement on the number of readers
     */
    public String readLockManagerProblem01CreateLogErrorMessageToIndicateThatCurrentThreadHasNullReadLockManagerWhileDecrementingNumberOfReaders(
            final int currentNumberOfReaders, final int decrementedNumberOfReaders, ConcurrencyManager cacheKey) {

        Thread currentThread = Thread.currentThread();
        StringWriter writer = new StringWriter();
        writer.write(TraceLocalization.buildMessage("concurrency_util_read_lock_manager_problem01", new Object[] {currentThread.getName(), currentNumberOfReaders, decrementedNumberOfReaders,
                ConcurrencyUtil.SINGLETON.createToStringExplainingOwnedCacheKey(cacheKey), enrichGenerateThreadDumpForCurrentThread(), new Date()}));
        AbstractSessionLog.getLog().log(SessionLog.SEVERE, SessionLog.CACHE, writer.toString(), new Object[] {}, false);
        return writer.toString();
    }

    public String readLockManagerProblem02ReadLockManageHasNoEntriesForThread(ConcurrencyManager cacheKey, long threadId) {
        Thread currentThread = Thread.currentThread();
        StringWriter writer = new StringWriter();
        writer.write(TraceLocalization.buildMessage("concurrency_util_read_lock_manager_problem02", new Object[] {currentThread.getName(), SINGLETON.createToStringExplainingOwnedCacheKey(cacheKey),
                threadId, enrichGenerateThreadDumpForCurrentThread(), new Date()}));
        // We do log immediately the error as we spot it
        AbstractSessionLog.getLog().log(SessionLog.SEVERE, SessionLog.CACHE, writer.toString(), new Object[] {}, false);
        // we also return the error message we just logged to added it to our tracing permanently
        return writer.toString();
    }

    public String readLockManagerProblem03ReadLockManageHasNoEntriesForThread(ConcurrencyManager cacheKey, long threadId) {
        Thread currentThread = Thread.currentThread();
        StringWriter writer = new StringWriter();
        writer.write(TraceLocalization.buildMessage("concurrency_util_read_lock_manager_problem03", new Object[] {currentThread.getName(), SINGLETON.createToStringExplainingOwnedCacheKey(cacheKey),
                threadId, enrichGenerateThreadDumpForCurrentThread(), new Date()}));
        // We do log immediately the error as we spot it
        AbstractSessionLog.getLog().log(SessionLog.SEVERE, SessionLog.CACHE, writer.toString(), new Object[] {}, false);
        // we also return the error message we just logged to added it to our tracing permanently
        return writer.toString();
    }

    /**
     * The concurrency managers about to acquire a cache key. And since we have been suffering from cache corruption on
     * the acquire read locks we need to collect a lot more information about the time of acquisition of a read lock.
     *
     * @param concurrencyManager
     *            the cache key we are about to increment and acquire for reading
     * @return object that have all the context information to allow us to know when and where
     *         exactly this key acquisition took place.
     */
    public ReadLockAcquisitionMetadata createReadLockAcquisitionMetadata(ConcurrencyManager concurrencyManager) {
        final boolean isAllowTakingStackTraceDuringReadLockAcquisition = isAllowTakingStackTraceDuringReadLockAcquisition();
        String currentThreadStackTraceInformation = TraceLocalization.buildMessage("concurrency_util_read_lock_acquisition_metadata");
        long currentThreadStackTraceInformationCpuTimeCostMs = 0l;
        if (isAllowTakingStackTraceDuringReadLockAcquisition) {
            long startTimeMillis = System.currentTimeMillis();
            currentThreadStackTraceInformation = enrichGenerateThreadDumpForCurrentThread();
            long endTimeMillis = System.currentTimeMillis();
            currentThreadStackTraceInformationCpuTimeCostMs = endTimeMillis - startTimeMillis;
        }
        int numberOfReadersOnCacheKeyBeforeIncrementingByOne = concurrencyManager.getNumberOfReaders();
        // data in ReadLockAcquisitionMetadata are immutable it reflects an accurate snapshot of the time of acquisition
        return new ReadLockAcquisitionMetadata(concurrencyManager, numberOfReadersOnCacheKeyBeforeIncrementingByOne,
                currentThreadStackTraceInformation, currentThreadStackTraceInformationCpuTimeCostMs);
    }

    private long getLongProperty(final String key, final long defaultValue) {
        String value = (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) ?
                AccessController.doPrivileged(new PrivilegedGetSystemProperty(key, String.valueOf(defaultValue)))
                : System.getProperty(key, String.valueOf(defaultValue));
        if (value != null) {
            try {
                return Long.parseLong(value.trim());
            } catch (Exception ignoreE) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean getBooleanProperty(final String key, final boolean defaultValue) {
        String value = (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) ?
                AccessController.doPrivileged(new PrivilegedGetSystemProperty(key, String.valueOf(defaultValue)))
                : System.getProperty(key, String.valueOf(defaultValue));
        if (value != null) {
            try {
                return Boolean.parseBoolean(value.trim());
            } catch (Exception ignoreE) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}

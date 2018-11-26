/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.zip.cache.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.zip.cache.ZipCachingProperties;
import com.ibm.ws.artifact.zip.internal.SystemUtils;

/**
 * Reaper facility for managing ZipFiles.
 *
 * A zip file reaper provides two capabilities:
 *
 * A cache of ZipFiles is maintained.
 *
 * Delays are introduced for closing zip files.
 *
 * This implementation retains the complete collection of zip files which
 * were ever opened.  Alternatively, zip files which are fully closed
 * could be removed from the managed collections.
 *
 * TODO: This implementation does not handle zip file closures which are
 * performed by finalization.  Such closes are not linked back to the reaper,
 * and the reaper will hold on to the zip files forever.
 *
 * It would be nice if zip files could be scavenged during reap cycles.  However,
 * there doesn't seem to be a way to tell if a zip file has been closed, so that
 * doesn't seem to be doable.
 *
 * The reaper maintains a history for the zip files which it has cached.
 * Each zip file has five states: initial and final, and open, pending and closed.
 *
 * The 'initial' state is never entered.  It is implied as the state of the zip file
 * before the zip file was first entered.
 *
 * The 'final' state is never entered.  It is implied as the state of the zip file
 * after the reaper framework was shutdown.
 *
 * A zip file transitions immediately to "open" when the zip file is first opened.
 * Close requests put the zip file to a "pending" state, which is left when either
 * the zip file is opened before the close request expires, or when the zip file
 * is closed when the close request expires.  That is, "pending" can transition
 * to "open" or "closed".  The closed state is left either if the zip file is
 * re-opened, or if the final state is reached.  That is, "closed" can transition
 * to "open" or to "final".
 *
 * <ul>
 * <li>initial -&gt; open</li>
 * <li>open -%gt; pending</li>
 * <li>pending -&gt; open | pending -&gt; closed</li>
 * <li>closed -&gt; open | closed -&gt; final</li>
 * </pre>
 *
 * That divides the lifetime of each entry into several intervals:
 *
 * <ul>
 * <li>time spent in 'initial'</li>
 * <li>time spent in 'open'</li>
 * <li>time spent in 'pending'</li>
 * <li>time spent in 'closed'</li>
 * </ul>
 *
 * these times should sum to the total lifetime, which is span from the initial to
 * the final times.
 *
 * The model has counts for the number of times which each of the states is entered.
 *
 * The model also divides the 'pending' and 'closed' durations into the time before
 * each of the following transitions:
 *
 * The count and duration in 'pending' which went back to 'open'.
 *
 * The count and duration in 'pending' which went to 'closed'.
 *
 * The count and duration in 'closed' which went back to 'open'.
 *
 * The intent is to measure the effectiveness of the cache, with the time spent
 * 'pending' balanced against the proportion of 'pending' which transition to
 * 'open' instead of 'closed.
 *
 * The goal is a high proportion of transitions to 'open' instead of 'closed', but
 * not at the cost of having 'pending' wait too long.
 *
 * For example, when the pattern of opens and closes always opens each zip file
 * exactly once, the reaper provides a negative benefit, since its only effect
 * is to hold zip files open longer.
 *
 * When the pattern of opens and closes has opens of the same zip file occurring
 * within a close time of a close, the reaper provides a benefit in allowing the
 * first open to be reused.
 *
 * When re-opens occur, maximum re-use is achieved by setting an infinite close delay.
 * However, that also maximizes the amount of time zip files are open.  A balance is
 * needed that provides re-use without keeping zip files open longer than is useful.
 * 
 * The reaper monitors the size and last modified date of target zip files.  Changes
 * to either are allowed only when a zip file has no active opens.
 */
@Trivial
public class ZipFileReaper {
    static final TraceComponent tc = Tr.register(ZipFileReaper.class);

    //

    @Trivial
    private static String toCount(int count) {
        return ZipCachingProperties.toCount(count);
    }

    @Trivial
    private static String toRelSec(long baseNS, long actualNS) {
        return ZipCachingProperties.toRelSec(baseNS, actualNS);
    }

    @Trivial
    private static String toAbsSec(long durationNS) {
        return ZipCachingProperties.toAbsSec(durationNS);
    }

    //

    @Trivial
    public void validate() {
        synchronized ( reaperLock ) {
            pendingQuickStorage.validate();
            pendingSlowStorage.validate();
            if ( !debugState ) {
                completedStorage.validate();
            }
        }
    }

    //

    private static class ReaperRunnable implements Runnable {
        @Trivial
        public ReaperRunnable(ZipFileReaper reaper) {
            this.reaper = reaper;
        }

        //

        private final ZipFileReaper reaper;

        @Trivial
        public ZipFileReaper getReaper() {
            return reaper;
        }

        //

        @Trivial
        public void run() {
            String methodName = "run";
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + " Start");
            }

            synchronized ( reaper.reaperLock ) {
                long reapDelay = REAP_DELAY_INDEFINITE;

                // CAUTION CAUTION CAUTION CAUTION
                //
                // The notification which occurs when a pending close is added does not
                // necessarily resume the reaper thread before any other thread blocked
                // by the reaper lock.  In particular, any number of 'enactOpen()' and
                // 'enactClose()' may occur before 'reap()' resumes.  'reap()' cannot
                // assume that there will necessarily be a pending close when it
                // resumes.
                //
                // CAUTION CAUTION CAUTION CAUTION

                while ( true ) {
                    // Condition:
                    // Start an indefinite wait if and only if there are no pending closes.
                    // Upon waking up, at least one pending close is expected, but
                    // is not guaranteed.

                    try {
                        if ( reapDelay < 0L ) {
                            reaper.reaperLock.wait(methodName, "new pending close"); // throws InterruptedException
                        } else {
                            reaper.reaperLock.waitNS(reapDelay, methodName, "active pending close"); // throws InterruptedException
                        }
                    } catch ( InterruptedException e ) {
                        if ( tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName + " Interrupted!");
                        }
                        break;
                    }

                    ZipFileData ripestPending = reaper.getRipest();
                    if ( ripestPending == null ) {
                        if ( tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName + " No pending!");
                        }

                        // Condition:
                        // No ripest means there are no pending closes, and because
                        // this code holds the reaper lock, none can be added before
                        // the next wait.

                        reapDelay = REAP_DELAY_INDEFINITE;
                        continue;
                    }

                    long reapAt = SystemUtils.getNanoTime();

                    long lastPendAt = ripestPending.lastPendAt;
                    long consumedPend = ( reapAt - lastPendAt );
                    long pendMax = ( ripestPending.expireQuickly ? reaper.getQuickPendMin() : reaper.getSlowPendMax() );

                    if ( consumedPend < pendMax ) {
                        // The ripest still has time left before it is fully closed.
                        // That is the amount of time to wait to the next reap. 
                        reapDelay = pendMax - consumedPend;
                        if ( tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName + " Ripest [ " + ripestPending.path + " ] waited [ " + toAbsSec(consumedPend) + " (s) ] remaining [ " + toAbsSec(reapDelay) + " (s) ]");
                        }

                    } else {
                        // The ripest is ready to fully close.  Fully close the ripest, and any
                        // other pending closes which are fully ripe, and set the next reap delay
                        // according to the ripest but not fully ripe pending close. 
                        if ( tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName + " Ripest [ " + ripestPending.path + " ] waited [ " + toAbsSec(consumedPend) + " (s) ]");
                        }

                        reapDelay = reaper.reap(reapAt, ZipFileReaper.IS_NOT_SHUTDOWN_REAP);
                    }
                }
            }

            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + " Shutting down");
            }

            // Maybe, move this to the shutdown thread.

            reaper.reap(SystemUtils.getNanoTime(), ZipFileReaper.IS_SHUTDOWN_REAP);

            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + " Stop");
            }
        }
    }

    //

    @Trivial
    public ZipFileReaper(String reaperName) {
        this( reaperName, SystemUtils.getNanoTime() );
    }

    @Trivial
    public ZipFileReaper(String reaperName, long initialAt) {
        this(reaperName,
             ZipCachingProperties.ZIP_REAPER_DEBUG_STATE,
             ZipCachingProperties.ZIP_CACHE_REAPER_MAX_PENDING,
             ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN,
             ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN,
             ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MAX,
             ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MAX);
    }

    @Trivial
    public ZipFileReaper(
        String reaperName,
        boolean debugState,
        int maxCache,
        long quickPendMin, long quickPendMax,
        long slowPendMin, long slowPendMax) {

        this(reaperName,
             debugState,
             maxCache,
             quickPendMin, quickPendMax,
             slowPendMin, slowPendMax,
             SystemUtils.getNanoTime() );
    }

    private static void validate(
        int maxCache,
        long quickPendMin, long quickPendMax,
        long slowPendMin, long slowPendMax) throws IllegalArgumentException {

        if ( maxCache == 0 ) {
            throw new IllegalArgumentException("Max cache cannot be zero.");
        }

        if ( (quickPendMin == 0) || (quickPendMax == 0) ) {
            if ( (quickPendMin != 0) || (quickPendMax != 0) ) {
                throw new IllegalArgumentException("If one quick pend duration is zero, the other must be zero.");
            }
        } else {
            if ( quickPendMin < 0 ) {
                throw new IllegalArgumentException("Minimum quick pend duration [ " + quickPendMin + " ] must be positive or zero");
            } else if ( quickPendMax < 0 ) {
                throw new IllegalArgumentException("Maximum quick pend duration[ " + quickPendMax + " ] must be positive or zero");
            } else if ( quickPendMin >= quickPendMax ) {
                throw new IllegalArgumentException(
                    "Both quick durations must be zero, or, the minimum quick pend duration [ " + quickPendMin + " ]" +
                    " must be less than maximum quick pend duration [ " + quickPendMax + " ]");
            }
        }

        if ( slowPendMin <= 0 ) {
            throw new IllegalArgumentException("Minimum slow pend duration [ " + slowPendMin + " ] must be positive");
        } else if ( slowPendMax <= 0 ) {
            throw new IllegalArgumentException("Maximum slow pend duration [ " + slowPendMax + " ] must be positive");
        } else if ( slowPendMin >= slowPendMax ) {
            throw new IllegalArgumentException(
                "Minimum slow pend duration [ " + slowPendMin + " ]" +
                " must be less than maximum slow pend duration [ " + slowPendMax + " ]");
        }

        if ( slowPendMin <= quickPendMax ) {
            throw new IllegalArgumentException(
                    "Minimum slow pend duration [ " + slowPendMin + " ]" +
                    " must be greater than maximum quick pend duration [ " + quickPendMax + " ]");
        }
    }

    public static final boolean DO_DEBUG_STATE = true;
    public static final boolean DO_NOT_DEBUG_STATE = false;
    
    public ZipFileReaper(
        String reaperName, boolean debugState,
        int maxCache,
        long quickPendMin, long quickPendMax,
        long slowPendMin, long slowPendMax,
        final long initialAt) {

        // Parameters ...

        validate(maxCache,
                 quickPendMin, quickPendMax,
                 slowPendMin, slowPendMax);

        this.reaperName = reaperName;
        this.debugState = debugState;
        
        this.maxCache = maxCache;

        this.quickPendMin = quickPendMin;
        this.quickPendMax = quickPendMax;

        this.slowPendMin = slowPendMin;
        this.slowPendMax = slowPendMax;

        // Storage ...

        this.storage = new HashMap<String, ZipFileData>();
        this.pendingQuickStorage = new ZipFileDataStore("pendingQuick");
        this.pendingSlowStorage = new ZipFileDataStore("pendingSlow");
        
        if ( !debugState ) {
            this.completedStorage = new ZipFileDataStore("completed");
        } else {
            this.completedStorage = null;
        }

        // Threading ...

        // Use of the shutdown thread is optional.  Shutdown provides an
        // opportunity to complete the thread statistics, but at the cost
        // of iterating across and closing all active zip files, which is
        // very probably unnecessary since the JVM is shutting down.

        this.reaperLock = new ReaperLock();

        this.reaperRunnable = new ReaperRunnable(this);
        this.reaperThread = new Thread(this.reaperRunnable, "zip file reaper");
        this.reaperThread.setDaemon(true);

        if ( this.debugState ) {
            this.reaperShutdown = new ReaperShutdownRunnable(this.reaperThread);
            this.reaperShutdownThread = new Thread(this.reaperShutdown);
        } else {
            this.reaperShutdown = null;
            this.reaperShutdownThread = null;
        }

        // Startup ...

        this.initialAt = initialAt;
        this.finalAt = 0L;

        this.isActive = true;

        // TODO: Not sure which of the following two steps to do first.

        this.reaperThread.start();

        if ( this.debugState ) {
            SystemUtils.addShutdownHook(this.reaperShutdownThread);
        }
    }

    public void shutDown() {
        this.reaperThread.interrupt();
    }
    
    /**
     * Shutdown code for the reaper thread.
     *
     * That thread runs as a daemon, so shutdown is not entirely
     * necessary.  However, shutdown is useful for completing and
     * displaying the zip file statistics.
     */
    private static class ReaperShutdownRunnable implements Runnable {
        @Trivial
        public ReaperShutdownRunnable(Thread reaperThread) {
            this.reaperThread = reaperThread;
        }

        private final Thread reaperThread;

        @Trivial
        private Thread getReaperThread() {
            return reaperThread;
        }

        /**
         * Run the reaper shutdown thread: This interrupts the
         * reaper thread, which will force it to close all of the
         * registered zip files.
         */
        public void run() {
            Thread reaperThread = getReaperThread();

            // The reaper is shut down by being interrupted.
            reaperThread.interrupt();

            // This join is necessary to ensure that the reaper
            // thread can complete its shutdown steps.  Otherwise,
            // The exit of this shutdown thread allows the JVM
            // shutdown to complete.
            //
            // Maybe, the shutdown steps should be invoked directly
            // from here.
            try {
                reaperThread.join(); // throws InterruptedException
            } catch ( InterruptedException e ) {
                // Ignore
            }
        }
    }

    //

    private final String reaperName;

    @Trivial
    public String getReaperName() {
        return reaperName;
    }

    private final boolean debugState;

    @Trivial
    public boolean getDebugState() {
        return debugState;
    }

    //

    private final int maxCache;

    @Trivial
    public int getMaxCache() {
        return maxCache;
    }

    //

    private final long quickPendMin;
    private final long quickPendMax;

    @Trivial
    public long getQuickPendMin() {
        return quickPendMin;
    }

    @Trivial
    public long getQuickPendMax() {
        return quickPendMax;
    }

    private final long slowPendMin;
    private final long slowPendMax;

    @Trivial
    public long getSlowPendMin() {
        return slowPendMin;
    }

    @Trivial
    public long getSlowPendMax() {
        return slowPendMax;
    }

    //

    /**
     * Setting of whether the reaper is active.  The reaper starts active.
     * The reaper goes inactive upon receiving the interrupt from the
     * shutdown thread.
     *
     * {@link #open}, if attempted after the reaper is shutdown, fails
     * with an {@link IOException}.  {@link #close}, if attempted after
     * the reaper is shutdown, does nothing.
     */
    private boolean isActive;

    @Trivial
    public boolean getIsActive() {
        return isActive;
    }

    @Trivial
    private void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Setting of when when the reaper was created.
     * Used when displaying zip file statistics.
     */
    private final long initialAt;

    /**
     * Setting of when the reaper was shutdown.
     * Used when displaying zip file statistics.
     */
    private long finalAt;

    @Trivial
    public long getInitialAt() {
        return initialAt;
    }

    @Trivial
    public long getFinalAt() {
        return finalAt;
    }

    @Trivial
    private void setFinalAt(long finalAt) {
        this.finalAt = finalAt;
    }

    //

    private final Map<String, ZipFileData> storage;

    private final ZipFileDataStore pendingQuickStorage;
    private final ZipFileDataStore pendingSlowStorage;

    private final ZipFileDataStore completedStorage;

    //

    public ZipFileData.ZipFileState getState(String path) {
        synchronized ( reaperLock ) {
            ZipFileData data = storage.get(path);
            if ( data == null ) {
                if ( !debugState ) {
                    data = completedStorage.get(path);
                }
            }
            return ( (data == null) ? null : data.zipFileState );
        }
    }

    protected ZipFileData getRipest() {
        ZipFileData ripest;
        if ( pendingQuickStorage.isEmpty() ) {
            ripest = pendingSlowStorage.getFirst();
        } else if ( pendingSlowStorage.isEmpty() ) {
            ripest = pendingQuickStorage.getFirst();
        } else {
            ZipFileData ripestQuick = pendingQuickStorage.getFirst();
            long expireAtQuick = ripestQuick.lastPendAt + quickPendMin;

            ZipFileData ripestSlow = pendingQuickStorage.getFirst();
            long expireAtSlow = ripestSlow.lastPendAt + slowPendMin;

            if ( expireAtQuick <= expireAtSlow ) {
                ripest = ripestQuick;
            } else {
                ripest = ripestSlow;
            }
        }
        return ripest;
    }

    protected void fullyClose(ZipFileData data, long fullCloseAt, boolean isShutdown) {
        String methodName = "fullyClose";
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + " Path [ " + data.path + " ] at [ " + toRelSec(initialAt, fullCloseAt) + " (s) ]");
        }

        data.closeZipFile();
        data.enactFullClose(fullCloseAt);

        if ( !isShutdown && !debugState ) {
            @SuppressWarnings("unused") // Same as 'data'
            ZipFileData fullyClosedData = storage.remove(data.path);

            ZipFileData oldestCompletedClose =
                completedStorage.addLast( data, getMaxCache() );
            if ( tc.isDebugEnabled() ) {
                if ( oldestCompletedClose != null ) {
                    Tr.debug(tc, methodName + " Discard completed close [ " + oldestCompletedClose.path + " ]");
                }
            }
        }
    }

    // Reaping ...

    private final ReaperShutdownRunnable reaperShutdown;
    private final Thread reaperShutdownThread;

    @Trivial
    private ReaperShutdownRunnable getReaperShutdown() {
        return reaperShutdown;
    }

    @Trivial
    private Thread getReaperShutdownThread() {
        return reaperShutdownThread;
    }

    private final ReaperRunnable reaperRunnable;
    private final Thread reaperThread;

    @Trivial
    private ReaperRunnable getReaper() {
        return reaperRunnable;
    }

    @Trivial
    private Thread getReaperThread() {
        return reaperThread;
    }

    //

    private static class ReaperLock {
        public void notify(String methodName, String text) {
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + " " + text);
            }

            notify();
        }

        public void wait(String methodName, String text) throws InterruptedException {
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + " Waiting for [ " + text + " ]");
            }
            wait(); // throws InterruptedException
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + " Waited for [ " + text + " ]");
            }
        }

        public void waitNS(long waitNs, String methodName, String text) throws InterruptedException {
            long waitMs = waitNs / ZipCachingProperties.NANO_IN_MILLI;
            int fracWaitNs = (int) (waitNs - (waitMs * ZipCachingProperties.NANO_IN_MILLI));

            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName +
                    " Waiting [ " + Long.toString(waitMs) + " (ms) " + Integer.toString(fracWaitNs) + " (ns) ]" +
                    " for [ " + text + " ]");
            }

            // 'wait' with no duration is intended for indefinite waits; this parameterized
            // 'wait' is intended for fixed, finite, waits.
            if ( (waitMs == 0) && (fracWaitNs == 0) ) {
                throw new IllegalArgumentException(methodName + ": Invalid zero wait request for [ " + text + " ]");
            }

            wait(waitMs, fracWaitNs); // throws InterruptedException

            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName +
                    " Waited [ " + Long.toString(waitMs) + " (ms) " + Integer.toString(fracWaitNs) + " (ns) ]" +
                    " for [ " + text + " ]");
            }
        }
    }

    private final ReaperLock reaperLock;

    //

    /** Control parameter: Have {@link #reap} to do a normal reap. */
    private static final boolean IS_NOT_SHUTDOWN_REAP = false;
    /** Control parameter: Have {@link #reap} to do a shutdown reap. */
    private static final boolean IS_SHUTDOWN_REAP = true;

    /**
     * Control value: Used to specify to the reaper thread that no
     * closes are pending and the thread should wait until a pending
     * close is available.
     */
    private static final long REAP_DELAY_INDEFINITE = -1;

    /**
     * Reap the pending closes.
     *
     * Reaping is performed in two modes: Un-forced, which occurs a set delay after the first
     * pending close, and forced, which occurs when shutting down.
     *
     * An un-forced reap will see pending closes in several different configurations:
     *
     * First, the pending closes may be empty.  That indicates that all pending closes
     * were re-opened before the reaper was run.  Answer -1 in this case, which indicates
     * that the reaper should wait for a pending close.
     *
     * Second, the pending closes is not empty, and one or more pending closes is ready
     * to close.  Close each of these.  That indicates that no new open occurred on the
     * pending closes, which means they are now to be closed.
     *
     * Third, the pending closes is not empty, but none of the pending closes is ready
     * to close.  This is similar to the first: The pending close which expired was
     * re-opened.
     *
     * In the second case, after reaping, there may be un-expired pending closes.  In
     * the third case, there must be un-expired pending closes.  When there are un-expired
     * closes, answer the time to wait before the first of these expires.  That will
     * be the reap time minus the pend time plus the reap interval.
     *
     * In the second case, if there are no pending closes after reaping, answer -1, as
     * was done for the first case.
     *
     * If this is a forced reap, all zip files are closed, starting with the pending
     * closes, and completing with the un-pended zip files.  Also, the final time is
     * set as the reap time, and diagnostic information is displayed.
     *
     * Reaping is based on two intervals, a minimum delay amount, which is the
     * the threshold for allowing a close to proceed, and an maximum delay amount,
     * which is the amount of time the reaper waits before performing delayed
     * closes.  The intent is to reduce the amount of chatter of the reaper
     * waking up and reaping when there are many opens and closes in a short
     * amount of time.  That is, to prevent a "stutter" of waking the reaper every
     * few milliseconds because several closes were performed milliseconds apart.
     *
     * @param reapAt The time at which the reaping is being performed.
     * @param isShutdownReap True or false telling if to perform a shutdown reap.
     *
     * @return The next reap time.  -1 if there are no pending closes.
     */
    private long reap(long reapAt, boolean isShutdownReap) {
        String methodName = "reap";
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + " At [ " + toRelSec(initialAt, reapAt) + " (s) ] Force [ " + isShutdownReap + " ]");
            Tr.debug(tc, methodName +
                " All [ " + storage.size() + " ]" +
                " Pending Quick [ " + pendingQuickStorage.size() + " ]" +
                " Pending Slow [ " + pendingSlowStorage.size() + " ]");
        }

        // Reap the quick pending closes ...

        long nextQuickReapDelay = REAP_DELAY_INDEFINITE;
        Iterator<ZipFileData> pendingQuick = pendingQuickStorage.values();
        while ( (nextQuickReapDelay == REAP_DELAY_INDEFINITE) && pendingQuick.hasNext() ) {
            ZipFileData nextPending = pendingQuick.next();

            long nextLastPendAt = nextPending.lastPendAt;
            long nextPendDuration = reapAt - nextLastPendAt;

            if ( isShutdownReap ) {
                // Shutdown closes all pending, regardless of how long they have waited.

                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName +
                            " Path [ " + nextPending.path + " ]" +
                            " Waiting [ " + toAbsSec(nextPendDuration) + " (s) ] (Quick): Forced");
                }

                pendingQuick.remove();

                fullyClose(nextPending, reapAt, IS_SHUTDOWN_REAP);

            } else { // Normal reap.
                if ( nextPendDuration > quickPendMin) { // Reached the shortest expiration?
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName +
                            " Path [ " + nextPending.path + " ]" +
                            " Waiting [ " + toAbsSec(nextPendDuration) + " (s) ] (Quick): Expired");
                    }

                    pendingQuick.remove();

                    fullyClose(nextPending, reapAt, IS_NOT_SHUTDOWN_REAP);

                } else { // Not yet reached the shorted expiration.
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName +
                            " Path [ " + nextPending.path + " ]" +
                            " Waiting [ " + toAbsSec(nextPendDuration) + " (s) ]: Still Waiting");
                    }

                    if ( nextPendDuration < 0 ) {
                        nextPendDuration = 0; // Should never happen;
                    }
                    nextQuickReapDelay = quickPendMax - nextPendDuration;
                }
            }
        }

        // Reap the slow pending closes ...

        long nextSlowReapDelay = REAP_DELAY_INDEFINITE;
        Iterator<ZipFileData> pendingSlow = pendingSlowStorage.values();
        while ( (nextSlowReapDelay == REAP_DELAY_INDEFINITE) && pendingSlow.hasNext() ) {
            ZipFileData nextPending = pendingSlow.next();

            long nextLastPendAt = nextPending.lastPendAt;
            long nextPendDuration = reapAt - nextLastPendAt;

            if ( isShutdownReap ) {
                // Shutdown closes all pending, regardless of how long they have waited.

                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName +
                            " Path [ " + nextPending.path + " ]" +
                            " Waiting [ " + toAbsSec(nextPendDuration) + " (s) ] (Slow): Forced");
                }

                pendingSlow.remove();

                fullyClose(nextPending, reapAt, IS_SHUTDOWN_REAP);

            } else { // Normal reap.
                if ( nextPendDuration > slowPendMin ) { // Reached the shortest expiration?
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName +
                            " Path [ " + nextPending.path + " ]" +
                            " Waiting [ " + toAbsSec(nextPendDuration) + " (s) ] (Slow): Expired");
                    }

                    pendingSlow.remove();

                    fullyClose(nextPending, reapAt, IS_NOT_SHUTDOWN_REAP);

                } else { // Not yet reached the shorted expiration.
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName +
                            " Path [ " + nextPending.path + " ]" +
                            " Waiting [ " + toAbsSec(nextPendDuration) + " (s) ]: Still Waiting");
                    }

                    if ( nextPendDuration < 0 ) {
                        nextPendDuration = 0; // Should never happen;
                    }
                    nextSlowReapDelay = slowPendMax - nextPendDuration;
                }
            }
        }

        // Maybe, move this into a different method, and invoke from the
        // shutdown thread?
        //
        // Placement here seems couples normal reaping with shutdown steps,
        // which seems off.

        if ( isShutdownReap ) {
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + " De-activating reaper");
            }

            // We have the lock: There can be no activity since receiving
            // the interrupted exception and setting the reaper inactive.

            // Note: Have to set this before pending the outstanding open zip files.
            //        Remove of the eldest is not performed while shutting down.
            setIsActive(false);

            setFinalAt(reapAt);

            // Since this is a shut-down reap, all pending closes were
            // forced close, regardless of how long they were waiting.
            // There are only dangling opens to handle.

            for ( ZipFileData mustBeOpenOrClosed : storage.values() ) {
                String path = mustBeOpenOrClosed.path;

                if ( mustBeOpenOrClosed.isFullyClosed() ) {
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + " Closed [ " + path + " ]: No shutdown action");
                    }
                } else {
                    if ( mustBeOpenOrClosed.isPending() ) {
                        if ( tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName + " Unexpected Pending [ " + path + " ]: Shutdown close");
                        }
                    } else {
                        if ( tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName +
                                " Open [ " + path + " ] [ " + mustBeOpenOrClosed.getActiveOpens() + " ]:" +
                                " Shutdown pend and close");
                        }
                        mustBeOpenOrClosed.enactClose(reapAt, ZipFileData.CLOSE_ALL);
                    }

                    fullyClose(mustBeOpenOrClosed, reapAt, IS_SHUTDOWN_REAP);
                }
            }

            // Finalize the zip files, all of which should be closed.
            //
            // Display statistics for each of the zip files.

            for ( ZipFileData mustBeClosed : storage.values() ) {
                mustBeClosed.setFinal(reapAt);
                mustBeClosed.debugState();
            }
        }

        long nextReapDelay;
        boolean useQuick;
        if ( (nextQuickReapDelay < 0) && (nextSlowReapDelay < 0) ) {
            useQuick = true;
            nextReapDelay = REAP_DELAY_INDEFINITE;
        } else if ( nextQuickReapDelay < 0 ) {
            useQuick = false;
            nextReapDelay = nextSlowReapDelay;
        } else if ( nextSlowReapDelay < 0 ) {
            useQuick = true;
            nextReapDelay = nextQuickReapDelay;
        } else {
            if ( nextQuickReapDelay < nextSlowReapDelay ) {
                useQuick = true;
                nextReapDelay = nextQuickReapDelay;
            } else {
                useQuick = false;
                nextReapDelay = nextSlowReapDelay;
            }
        }

        if ( tc.isDebugEnabled() ) {
            String delayText =
                ( (nextReapDelay == REAP_DELAY_INDEFINITE) ? "Indefinite" : toAbsSec(nextReapDelay) );
            String speedText =
                ( useQuick ? "Quick" : "Slow" );
            Tr.debug(tc, methodName + " Next reap [ " + delayText + " (s) ] (" + speedText + ")");
        }
        return nextReapDelay;
    }

    @Trivial
    public ZipFile open(String path) throws IOException, ZipException {
        return open( path, SystemUtils.getNanoTime() );
    }

    @Trivial
    public ZipFile open(String path, long openAt) throws IOException, ZipException {
        String methodName = "open";
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + " Path [ " + path + " ] at [ " + toRelSec(initialAt, openAt) + " (s) ]");
        }

        // Open could try to turn off the reaper thread if the last pending close
        // is removed.  Instead, the reaper allowed to run, and is coded to handle
        // that case.

        synchronized ( reaperLock ) {
            if ( !getIsActive() ) {
                Tr.warning(tc, methodName + " Cannot open [ " + path + " ]: ZipFile cache [ " + reaperName + " ] is inactive");
                throw new IOException("Cannot open [ " + path + " ]: ZipFile cache is inactive");
            }

            ZipFileData data = storage.get(path);
            ZipFile zipFile;

            if ( data == null ) {
                if ( !debugState ) {
                    data = completedStorage.remove(path);
                }

                if ( data == null ) {
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + " New [ " + path + " ]");
                    }
                    data = new ZipFileData( path, getInitialAt() ); // throws IOException, ZipException
                } else {
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + " Recovered [ " + path + " ]");
                    }
                }

                storage.put(path, data);
            }

            if ( data.isFullyClosed() ) {
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + " Open [ " + path + " ]");
                }

                zipFile = data.openZipFile(); // throws IOException, ZipException

            } else if ( data.isPending() ) {
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + " Unpend [ " + path + " ]");
                }

                if ( data.expireQuickly ) {
                    @SuppressWarnings("unused") // same as 'data'
                    ZipFileData pendingQuickData = pendingQuickStorage.remove(path);
                } else {
                    @SuppressWarnings("unused") // same as 'data'
                    ZipFileData pendingSlowData = pendingSlowStorage.remove(path);
                }
                // Removal from pending may result in the next reap
                // discovering no expired closes.

                try {
                    zipFile = data.reacquireZipFile(); // throws IOException, ZipException

                } catch (Exception e) {
                    // The closeZipFile() or openZipFile() call failed in reacquireZipFile().
                    // Either way, the proper state should be fully closed.
                    data.enactFullClose(openAt);
                    throw e;
                }

            } else if ( data.isOpen() ) {
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + " Already open [ " + path + " ]");
                }

                try {
                    zipFile = data.reacquireZipFile(); // throws IOException, ZipException

                } catch (Exception e) {
                    // The closeZipFile() or openZipFile() call failed in reacquireZipFile().
                    // Either way, the proper state should be fully closed.
                    data.enactClose(openAt, ZipFileData.CLOSE_ALL);
                    data.enactFullClose( openAt );
                    throw e;
                }

            } else {
                throw data.unknownState();
            }

            data.enactOpen(openAt);

            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + " Path [ " + path + " ] [ " + zipFile + " ]");
            }
            return zipFile;
        }
    }

    public ZipFileData.ZipFileState close(String path) {
        return close( path, SystemUtils.getNanoTime() );
    }

    public ZipFileData.ZipFileState close(String path, long closeAt) {
        String methodName = "close";

        synchronized ( reaperLock ) {
            if ( !getIsActive() ) {
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + " Path [ " + path + " ]: Ignore: Inactive");
                }
                return null;
            }

            ZipFileData data = storage.get(path);

            if ( data == null ) {
                Tr.warning(tc, methodName + " Unregistered [ " + path + " ]: Ignore");

            } else if ( data.isFullyClosed() ) {
                Tr.warning(tc, methodName + " Fully closed [ " + path + " ]: Ignore");

            } else if ( data.isPending() ) {
                Tr.warning(tc, methodName + " Pending [ " + path + " ]: No active opens: Ignore");

            } else if ( data.isOpen() ) {
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + " Active opens [ " + path + " ] [ " + data.getActiveOpens() + " ]");
                }

                if ( data.enactClose(closeAt, ZipFileData.CLOSE_ONCE) ) {
                    boolean expireQuickly = data.setExpireQuickly(slowPendMin);

                    if ( expireQuickly && (quickPendMin == 0) ) {
                        fullyClose(data, closeAt, IS_NOT_SHUTDOWN_REAP);

                    } else {
                        boolean wasQuickEmpty = pendingQuickStorage.isEmpty();
                        boolean wasSlowEmpty = pendingSlowStorage.isEmpty();

                        ZipFileData ripestPending;
                        if ( expireQuickly ) {
                            // pendingQuickStorage.display();
                            ripestPending = pendingQuickStorage.addLast(data, getMaxCache());
                            // pendingQuickStorage.display();
                        } else {
                            // pendingSlowStorage.display();
                            ripestPending = pendingSlowStorage.addLast(data, getMaxCache());
                            // pendingSlowStorage.display();
                        }

                        if ( ripestPending != null ) {
                            fullyClose(ripestPending, closeAt, IS_NOT_SHUTDOWN_REAP);
                        }

                        String wakeReason;

                        if ( wasQuickEmpty && wasSlowEmpty ) {
                            // The reaper was in an indefinite wait, since there were
                            // no pending closes.  Since there are now pending closes,
                            // the reaper must be woken.
                            //
                            // The first reap cycle will perform no closes; the first reap
                            // will find the ripest pending close and set a definite wait
                            // based on that pending close.
                            wakeReason = "Added first pending";

                        } else if ( expireQuickly ) {
                            if ( !wasQuickEmpty ) { // expireQuickly && !wasQuickEmpty
                                // The reaper is set to a definite wait based on the ripest
                                // quick pending.
                                //
                                // If the new pending is expiring quickly, it is expiring
                                // after the ripest quick pending.
                                //
                                // Either way, the definite wait already set for the reaper
                                // remains correct.
                                wakeReason = null; // Added quick while quick are present.

                                // wasQuickEmpty ==> !wasSlowEmpty
                            } else { // expireQuickly && wasQuickEmpty && !wasSlowEmpty
                                // The reaper is set to a definite wait based on the ripest
                                // slow pending close.
                                //
                                // If the new pending was a slow pending, no update to the
                                // reaper wait would be needed, since the new pending expiration
                                // would necessarily be after the first slow pending expiration.
                                //
                                // But, the new pending is a quick pending, which might expire
                                // before or after the first slow pending.  The slow pending will
                                // expire first if it has waited long enough to put its expiration
                                // before the new quick pending's expiration. 
                                //
                                // What should be done is for the new pending to be compared with
                                // the prior ripest pending, and if the new pending expires sooner,
                                // to reset the reaper wait time to the new, earlier expiration.
                                //
                                // As a simplification, wake the reaper early and allow the reap
                                // cycle to run.  The reaper will notice that no pending closes
                                // have expired, and will recompute and set the reaper wait to a
                                // newly computed definite wait.
                                wakeReason = "Added first quick while slow are present";
                            }

                        } else { // !expireQuickly
                            // Since the new pending expires slowly, it must have an expiration which
                            // is later than the expiration of the ripest pending close.  The definite
                            // wait already set for the reaper remains correct.
                            wakeReason = null; // Added slow while quick or slow are present.
                        }

                        // CAUTION CAUTION CAUTION CAUTION
                        //
                        // This notification does not ensure that reap() is continued
                        // before any other reaper operation is performed.  This notification
                        // simply unblocks the reap() thread and puts it in the pool of threads
                        // available to be run.  This has a strong implication on how 'reap()'
                        // must work.
                        //
                        // CAUTION CAUTION CAUTION CAUTION

                        if ( wakeReason != null ) {
                            reaperLock.notify(methodName, wakeReason);
                        }
                    }
                }

            } else {
                throw data.unknownState();
            }
            
            return ( (data == null) ? null : data.zipFileState );
        }
    }
}

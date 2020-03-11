/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
import java.io.PrintWriter;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
//import java.util.function.Consumer;
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
    private static String toRelSec_s(long baseNS, long actualNS) {
        return ZipCachingProperties.toRelSec(baseNS, actualNS) + " (s)";
    }

    @Trivial
    private static String toAbsSec_s(long durationNS) {
        return ZipCachingProperties.toAbsSec(durationNS) + " (s)";
    }

    //

    @Trivial
    public void validate() {
        synchronized (pendingStorageLock) {
            pendingQuickStorage.validate();
            pendingSlowStorage.validate();
        }
        if ( !debugState ) {
            synchronized (completedStorage) {
                completedStorage.validate();
            }
        }
    }

    //

    // Locking note:
    //
    // 'Tr.warning' can cause a bundle load, which can cause a call to 'AppClassLoader.loadClass'.
    //
    // But, 'AppClassLoader.loadClass' can obtain an entry's input stream, which causes
    // a call to 'ZipFileReaper.open'.
    //
    // See below for stacks which show typical deadlocked threads.
    //
    // To avoid the deadlock, the stall warning is issued from a new thread.
    //
    // zip file reaper 55 BLOCKED
    // com.ibm.ws.classloading.internal.AppClassLoader.loadClass(AppClassLoader.java:480)
    // java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:1032)
    // java.base/java.util.ResourceBundle$Control.newBundle(ResourceBundle.java:3170)
    // java.base/java.util.ResourceBundle.loadBundle(ResourceBundle.java:1994)
    // ...
    // java.base/java.util.ResourceBundle.getBundle(ResourceBundle.java:1509)
    // ...
    // com.ibm.ws.logging.internal.impl.BaseTraceService.warning(BaseTraceService.java:512)
    // com.ibm.websphere.ras.Tr.warning(Tr.java:653)
    // com.ibm.ws.artifact.zip.cache.internal.ZipFileReaper$ReaperRunnable.run(ZipFileReaper.java:235)
    // 
    // Default Executor-thread-2 40 BLOCKED
    // com.ibm.ws.artifact.zip.cache.internal.ZipFileReaper.open(ZipFileReaper.java:1018)
    // ...
    // com.ibm.ws.artifact.zip.internal.ZipFileEntry.getInputStream(ZipFileEntry.java:176)
    // com.ibm.ws.classloading.internal.ContainerClassLoader$ArtifactEntryUniversalResource.getByteResourceInformation(ContainerClassLoader.java:517)
    // ...
    // com.ibm.ws.classloading.internal.AppClassLoader.loadClass(AppClassLoader.java:482)
    // java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:1032)
    // org.apache.derby.impl.sql.compile.SQLParser.insertColumnsAndSource(SQLParser.java:8249)
    // ...
    // com.ibm.ws.rsadapter.jdbc.WSJdbcStatement.executeUpdate(WSJdbcStatement.java:608)
    // ...
    // javax.servlet.http.HttpServlet.service(HttpServlet.java:575)

    private static class DeferredLogRecord {
        // Currently only defer warnings.  Other log record types could be
        // made asynchronous, but so far that hasn't been necessary.

        private final String msgKey;
        private final Object[] msgArgs;

        public DeferredLogRecord(String msgKey, Object... msgArgs) {
            this.msgKey = msgKey;
            this.msgArgs = msgArgs;
        }

        public void emit() {
            Tr.warning(tc, msgKey, msgArgs);
        }
    }

    private static class DeferredLogEmitter implements Runnable {
        public DeferredLogEmitter() {
            this.deferredLogQueue = new LinkedBlockingQueue<DeferredLogRecord>();
        }

        public void run() {
            try {
                while ( true ) {
                    take().emit(); // 'take' throws InterruptedException
                }
            } catch ( InterruptedException e ) {
                // Handle interruption *OUTSIDE* of the loop.  We
                // want interruption to cause an exit.
                //
                // FFDC and ignore
            }
        }

        //

        private final BlockingQueue<DeferredLogRecord> deferredLogQueue;

        public void post(String msgKey, Object... msgArgs) {
            try {
                deferredLogQueue.put( new DeferredLogRecord(msgKey, msgArgs) );
            } catch ( InterruptedException e ) {
                // Don't really care if a put is interrupted.  That means
                // that a warning was not emitted.  If that is a problem,
                // the warning would need to be emitted outside of usual
                // trace.
                //
                // FFDC and ignore
            }
        }

        public DeferredLogRecord take() throws InterruptedException {
            return deferredLogQueue.take();
        }
    }

    private static final DeferredLogEmitter logEmitter;
    private static final Thread logThread;

    // Should this initialization be done only when constructing the first
    // reaper?

    static {
        logEmitter = new DeferredLogEmitter();
        logThread = new Thread(logEmitter, "reaper logger");
        logThread.setDaemon(true);
        logThread.start();
    }

    private static void asyncWarning(final String msgKey, final Object ... msgArgs) {
        logEmitter.post(msgKey, msgArgs);
    }

    //

    private static class ReaperRunnable implements Runnable {
        @Trivial
        public ReaperRunnable(ZipFileReaper reaper) {
            this.reaper = reaper;

            // These are used only by 'run', but are stored at the
            // instance level to enable view by the introspector.

            this.didShutdownReap = false;
            this.shutdownReapAt = -1;

            this.nextReapAt = -1;
            this.lastReapAt = -1;
            this.nextReapDelay = -1;

            this.startupLatch = new CountDownLatch(1);
        }

        private boolean didShutdownReap;
        private long shutdownReapAt;

        private long nextReapAt;
        private long lastReapAt;
        private long nextReapDelay;
        private String nextReapReason;

        private final CountDownLatch startupLatch;

        void waitForStartup() throws InterruptedException {
            startupLatch.await(); // throws InterruptedException
        }

        private void signalStartup() {
            startupLatch.countDown();
        }

        // Invoked by 'ZipFileReaper.introspect(PrintWriter)'

        void introspect(PrintWriter output) {
            output.println();
            output.println("  Runner [ " + this + " ]");

            output.println("    Initial    [ " + toAbsSec_s( reaper.getInitialAt() ) + " ]");
            if ( reaper.getIsFinal() ) {
                output.println("    Final      [ " + reaper.fromInitial_s( reaper.getFinalAt() ) + " ]");
            }

            if ( didShutdownReap ) {
                output.println("    Shutdown   [ " + reaper.fromInitial_s(shutdownReapAt) + " ]");
            }

            output.println("    Last Reap  [ " + reaper.fromInitial_s(lastReapAt) + " ]");
            output.println("    Next Reap  [ " + reaper.fromInitial_s(nextReapAt) + " ]");
            String delayText = ( (nextReapDelay < 0) ? "INDEFINITE" : toAbsSec_s(nextReapDelay) );
            output.println("    Next Delay [ " + delayText + " ] (" + nextReapReason + ")");

            output.println();
            output.println("  Logger [ " + logThread + " ]");
        }

        private final ZipFileReaper reaper;

        @Trivial
        public ZipFileReaper getReaper() {
            return reaper;
        }

        //

        /**
         * Time allowed for stalled reaps: Any reap delay which exceeds the expected delay
         * by this amount causes a warning.
         */
        public static final long STALL_LIMIT = ZipCachingProperties.NANO_IN_ONE * 10;

        @Trivial
        public void run() {
            String methodName = "run";
            boolean doDebug = ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() );

            if ( doDebug ) {
                Tr.debug(tc, methodName + " Start [ " + toAbsSec_s( reaper.getInitialAt() ) + " ]");
            }

            reaper.reaperLock.acquireWriteLock();
            try {
                // Use this latch to ensure the reaper thread has started
                // and has acquired the reaper lock before allowing zip file
                // operations.  That prevents us from having to handle cases where
                // the reaper thread doesn't start immediately.

                signalStartup();

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

                nextReapDelay = REAP_DELAY_INDEFINITE;
                reaper.addWaitReason("indefinite (initial)");

                nextReapAt = reaper.getInitialAt();
                nextReapReason = "indefinite (initial)";

                while ( true ) {
                    lastReapAt = nextReapAt;

                    // Condition:
                    // Start an indefinite wait if and only if there are no pending closes.
                    // Upon waking, at least one pending close is expected, but
                    // is not guaranteed.

                    // The reaper delay logic is fragile: Put in these extra checks to guard
                    // against incorrectly putting the reaper in an indefinite wait, since that
                    // cannot be recovered.

                    String errorMessage;

                    ZipFileData testPending = reaper.getRipest();
                    if ( nextReapDelay < 0L ) {
                        if ( testPending != null ) {
                            errorMessage = "Indefinite wait with pending [ " + testPending + " ]";
                            nextReapReason = "specific (corrected from indefinite) [ " + nextReapDelay + " ]";
                            nextReapDelay = reaper.getQuickPendMin();
                            reaper.addWaitReason("specific (corrected from indefinite)");
                        } else {
                            errorMessage = null;
                            nextReapReason = "indefinite (no pending)";
                        }

                    } else if ( nextReapDelay == 0L ) {
                        errorMessage = "Incorrect explicit indefinited wait [ " + testPending + " ]";
                        if ( testPending == null ) {
                            nextReapDelay = REAP_DELAY_INDEFINITE;
                            nextReapReason = "indefinite (corrected from zero)";
                            reaper.addWaitReason("indefinite (corrected from zero)");
                        } else {
                            nextReapDelay = reaper.getQuickPendMin();
                            nextReapReason = "specific (corrected from zero)";
                            reaper.addWaitReason("specific (corrected from zero)");
                        }

                    } else {
                        errorMessage = null;
                        if ( testPending == null ) {
                            nextReapReason = "specific (no pending)";
                        } else {
                            nextReapReason = "specific [ " + nextReapDelay + " ]";
                        }
                    }

                    if ( errorMessage != null ) {
                        reaper.handleError(errorMessage);
                    }

                    try {
                        if ( nextReapDelay < 0L ) {
                            reaper.reaperLock.wait(methodName, "new pending close"); // throws InterruptedException
                        } else {
                            reaper.reaperLock.waitNS(nextReapDelay, methodName, "active pending close"); // throws InterruptedException
                        }
                    } catch ( InterruptedException e ) {
                        if ( doDebug ) {
                            Tr.debug(tc, methodName + " Interrupted!");
                        }
                        break;
                    }

                    nextReapAt = SystemUtils.getNanoTime();

                    if ( doDebug ) {
                        Tr.debug(tc, methodName + " Reap [ " + reaper.fromInitial_s(nextReapAt) + " ]");
                        
                        // Issue #7770 - Remove reaper stall warning - change to debug
                        // asyncWarning("reaper.stall", toAbsSec_s(actualDelay), toAbsSec_s(reapDelay));
                        
                        String message;
                        long actualReapDelay = nextReapAt - lastReapAt;
                        if ( nextReapDelay > 0L ) {
                            long overage = actualReapDelay - nextReapDelay;
                            if ( overage > STALL_LIMIT ) {
                                message = " Excessive reap cycle time:";
                            } else {
                                message = " Reap cycle time:";  
                            }
                        } else {
                            message = " Reap cycle time:";
                        }
                        Tr.debug(tc, methodName + message + " Actual delay [ " 
                                        + toAbsSec_s(actualReapDelay) + " ]; Requested delay [ " 
                                        + toAbsSec_s(nextReapDelay) + " ]");
                    }

                    ZipFileData ripestPending = reaper.getRipest();
                    if ( ripestPending == null ) {
                        if ( doDebug ) {
                            Tr.debug(tc, methodName + " No pending!");
                        }

                        // Condition:
                        // No ripest means there are no pending closes, and because
                        // this code holds the reaper lock, none can be added before
                        // the next wait.

                        nextReapDelay = REAP_DELAY_INDEFINITE;
                        reaper.addWaitReason("indefinite (no pending)");
                        continue;
                    }

                    long lastPendAt = ripestPending.lastPendAt;
                    long consumedPend = ( nextReapAt - lastPendAt );
                    long pendMax = ( ripestPending.expireQuickly ? reaper.getQuickPendMin() : reaper.getSlowPendMax() );

                    if ( consumedPend < pendMax ) {
                        // The ripest still has time left before it is fully closed.
                        // That is the amount of time to wait to the next reap. 

                        nextReapDelay = pendMax - consumedPend;
                        reaper.addWaitReason("specific (incomplete pend)");
                        if ( doDebug ) {
                            Tr.debug(tc, methodName + " Ripest [ " + ripestPending.path + " ] waited [ " + toAbsSec_s(consumedPend) + " ] remaining [ " + toAbsSec_s(nextReapDelay) + " ]");
                        }

                    } else {
                        // The ripest is ready to fully close.  Fully close the ripest, and any
                        // other pending closes which are fully ripe, and set the next reap delay
                        // according to the ripest but not fully ripe pending close. 
                        if ( doDebug ) {
                            Tr.debug(tc, methodName + " Ripest [ " + ripestPending.path + " ] waited [ " + toAbsSec_s(consumedPend) + " ]");
                        }

                        nextReapDelay = reaper.reap(nextReapAt, ZipFileReaper.IS_NOT_SHUTDOWN_REAP);
                        reaper.addWaitReason("specific (reap)");
                    }
                }
            } finally {
                reaper.reaperLock.releaseWriteLock();
            }

            long shutdownAt = SystemUtils.getNanoTime();
            if ( doDebug ) {
                Tr.debug(tc, methodName + " Shutting down [ " + reaper.fromInitial_s(shutdownAt) + " ]");
            }
            reaper.reaperLock.acquireWriteLock();
            try {
                reaper.reap(shutdownAt, ZipFileReaper.IS_SHUTDOWN_REAP); // Maybe, move this to the shutdown thread.
            } finally {
                reaper.reaperLock.releaseWriteLock();
            }

            long stopAt = SystemUtils.getNanoTime();
            if ( doDebug ) {
                Tr.debug(tc, methodName + " Stop [ " + reaper.fromInitial_s(stopAt) + " ]");
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
        this( reaperName,
              ZipCachingProperties.ZIP_REAPER_DEBUG_STATE,
              ZipCachingProperties.ZIP_CACHE_REAPER_MAX_PENDING,
              ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN,
              ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN,
              ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MAX,
              ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MAX,
              NULL_ERROR_HANDLER,
              initialAt );
    }

    private static final ErrorConsumer<String> NULL_ERROR_HANDLER = null;

    @Trivial
    public ZipFileReaper(
        String reaperName,
        boolean debugState,
        int maxCache,
        long quickPendMin, long quickPendMax,
        long slowPendMin, long slowPendMax,
        ErrorConsumer<String> errorHandler) {

        this( reaperName, debugState,
              maxCache,
              quickPendMin, quickPendMax,
              slowPendMin, slowPendMax,
              errorHandler,
              SystemUtils.getNanoTime() );
    }

    @Trivial
    public ZipFileReaper(
        String reaperName, boolean debugState,
        int maxCache,
        long quickPendMin, long quickPendMax,
        long slowPendMin, long slowPendMax) {

        this( reaperName, debugState,
              maxCache,
              quickPendMin, quickPendMax,
              slowPendMin, slowPendMax,
              ZipFileReaper.NULL_ERROR_HANDLER,
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
        ErrorConsumer<String> errorHandler,
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

        this.storage = new ConcurrentHashMap<String, ZipFileData>();
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

        this.waitReasons = new Reasons("Reaper Waits");

        this.errorHandler = errorHandler;

        // TODO: Not sure which of the following two steps to do first.
 
        this.reaperThread.start();

        if ( this.debugState ) {
            SystemUtils.addShutdownHook(this.reaperShutdownThread);
        }

        try {
            this.reaperRunnable.waitForStartup(); // throws InterruptedException
        } catch ( InterruptedException e ) {
            // FFDC
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
            Thread useReaperThread = getReaperThread();

            // The reaper is shut down by being interrupted.
            useReaperThread.interrupt();

            // This join is necessary to ensure that the reaper
            // thread can complete its shutdown steps.  Otherwise,
            // The exit of this shutdown thread allows the JVM
            // shutdown to complete.
            //
            // Maybe, the shutdown steps should be invoked directly
            // from here.
            try {
                useReaperThread.join(); // throws InterruptedException
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
     * Setting of whether the reaper has been shut down.
     */
    private boolean isFinal;

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
    public boolean getIsFinal() {
        return isFinal;
    }

    @Trivial
    public long getFinalAt() {
        return finalAt;
    }

    private void setFinalAt(long finalAt) {
        this.isFinal = true;
        this.finalAt = finalAt;
    }

    @Trivial
    private String fromInitial_s(long actualNS) {
        return toRelSec_s( getInitialAt(), actualNS);
    }

    //

    private final ConcurrentMap<String, ZipFileData> storage;

    private final ZipFileDataStore pendingQuickStorage;
    private final ZipFileDataStore pendingSlowStorage;
    private final Object pendingStorageLock = new Integer(0);

    private final ZipFileDataStore completedStorage;

    //

    private static final class ZipFilePathLock { }

    static ReferenceQueue<ZipFilePathLock> refQueue = new ReferenceQueue<>();
    private static ConcurrentHashMap<String, ZipFilePathLockRef> zipFilePathLockMap = new ConcurrentHashMap<>();

    private static final class ZipFilePathLockRef extends WeakReference<ZipFilePathLock> {
        final String  key;
        @Trivial
        public ZipFilePathLockRef(ZipFilePathLock referent, String keyValue) {
                super(referent, refQueue);
                key = keyValue;
        }
    }

    @Trivial
    private final ZipFilePathLock getLockForPath(String path) {
        poll();
        ZipFilePathLockRef lockRef = zipFilePathLockMap.get(path);
        ZipFilePathLock lock = lockRef != null ? lockRef.get() : null;
        if (lock != null) {
            return lock;
        }

        lock = new ZipFilePathLock();

        while (true) {
            ZipFilePathLockRef retVal = zipFilePathLockMap.putIfAbsent(path, new ZipFilePathLockRef(lock, path));
            if (retVal == null) {
                return lock;
            }

            ZipFilePathLock retLock = retVal.get();
            if (retLock != null) {
                return retLock;
            }
            zipFilePathLockMap.remove(path, retVal);
        }
    }

    @Trivial
    private final void poll() {
        ZipFilePathLockRef lockRef;
        while ((lockRef = (ZipFilePathLockRef) refQueue.poll()) != null) {
            zipFilePathLockMap.remove(lockRef.key, lockRef);
        }
    }
    
    public ZipFileData.ZipFileState getState(String path) {
        reaperLock.acquireReadLock();
        try {
            ZipFilePathLock lock = getLockForPath(path);
            synchronized (lock) {
                ZipFileData data = storage.get(path);
                if ( data == null ) {
                    if ( !debugState ) {
                        synchronized (completedStorage) {
                            data = completedStorage.get(path);
                        }
                    }
                }
                return ( (data == null) ? null : data.zipFileState );
            }
        } finally {
            reaperLock.releaseReadLock();
        }
    }

    ZipFileData getRipest() {
        ZipFileData ripestQuick;
        ZipFileData ripestSlow;

        synchronized (pendingStorageLock) {
            ripestQuick = pendingQuickStorage.getFirst();
            ripestSlow = pendingSlowStorage.getFirst();
        }

        ZipFileData ripest;
        if (ripestQuick == null) {
            ripest = ripestSlow;
        } else if (ripestSlow == null) {
            ripest = ripestQuick;
        } else {
            long expireAtQuick = ripestQuick.lastPendAt + quickPendMin;

            long expireAtSlow = ripestSlow.lastPendAt + slowPendMin;

            if ( expireAtQuick <= expireAtSlow ) {
                ripest = ripestQuick;
            } else {
                ripest = ripestSlow;
            }
        }
        return ripest;
    }

    private void fullyClose(ZipFileData data, long fullCloseAt, boolean isShutdown) {
        String methodName = "fullyClose";
        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + " Path [ " + data.path + " ] at [ " + fromInitial_s(fullCloseAt) + " ]");
        }

        data.closeZipFile();
        data.enactFullClose(fullCloseAt);

        if ( !isShutdown && !debugState ) {
            @SuppressWarnings("unused") // Same as 'data'
            ZipFileData fullyClosedData = storage.remove(data.path);

            ZipFileData oldestCompletedClose;
            synchronized (completedStorage) {
                oldestCompletedClose =
                    completedStorage.addLast( data, getMaxCache() );
            }
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
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

    private void introspectReaperThread(PrintWriter output) {
        output.println();
        output.println("  Reaper [ " + reaperThread + " ]");
        output.println("    Id          [ " + reaperThread.getId() + " ]"); 
        output.println("    Name        [ " + reaperThread.getName() + " ]"); 
        output.println("    Daemon      [ " + reaperThread.isDaemon() + " ]");
        output.println("    Priority    [ " + reaperThread.getPriority() + " ]");
        output.println("    Group       [ " + reaperThread.getThreadGroup() + " ]");

        output.println();
        output.println("    State       [ " + reaperThread.getState() + " ]");
        output.println("    Alive       [ " + reaperThread.isAlive() + " ]");
        output.println("    Interrupted [ " + reaperThread.isInterrupted() + " ]");

        output.println();
        reaperLock.introspect(output);

        output.println();
        waitReasons.introspect(output);

        output.println();
        reaperRunnable.introspect(output);
    }

    //

    public interface ErrorConsumer<T>{
        public void accept(T t);
    }

    /**
     * Error handling hook.  Used by unit tests to detect test errors.
     */
    private final ErrorConsumer<String> errorHandler;

    private void handleError(String errorMessage) {
        //System.out.println("**** ZipFileReaper ERROR [ " + errorMessage + " ] ****");

        Tr.error(tc, errorMessage);

        if ( errorHandler != null ) {
            errorHandler.accept(errorMessage);
        }

        PrintWriter writer = new PrintWriter(System.out);
        introspect(writer, System.nanoTime());
        writer.flush();
    }

    //

    static final int NUM_REASONS = 10;

    private static class Reasons {
        private final String description;
        private int nextReason;
        public final String[] reasons;

        public Reasons(String description) {
            this.description = description;
            this.reasons = new String[NUM_REASONS];
            this.nextReason = 0;
        }

        public void add(String reason) {
            synchronized (reasons) {
                reasons[nextReason] = reason;
                if ( nextReason == NUM_REASONS - 1 ) {
                    nextReason = 0;
                } else {
                    nextReason = nextReason + 1;
                }
            }
        }

        public void introspect(PrintWriter printWriter) {
            printWriter.println(description + ": ");

            synchronized(reasons)  {
                int lastReason = ( (nextReason == 0) ? (NUM_REASONS - 1) : (nextReason - 1) );
                if ( reasons[lastReason] == null ) {
                    printWriter.println("  *** NONE ***");
                } else {
                    for ( int reasonNo = 0; reasonNo > -NUM_REASONS; reasonNo-- ) {
                        int actualReasonNo = nextReason + reasonNo - 1;
                        if ( actualReasonNo < 0 ) {
                            actualReasonNo += NUM_REASONS;
                        }

                        String reason = reasons[actualReasonNo];
                        if ( reason == null ) {
                            break;
                        }

                        printWriter.println("  [ " + reasonNo + " ] [ " + reasons[actualReasonNo] + " ]");
                    }
                }
            }
        }
    }

    private final Reasons waitReasons;

    void addWaitReason(String reason) {
        waitReasons.add(reason);
    }

    private static class ReaperLock {
        final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        final Condition condition = rwLock.writeLock().newCondition();
        final Reasons lockReasons;

        public ReaperLock() {
            this.lockReasons = new Reasons("Reaper Lock Activity");
        }

        public void addReason(String reason) {
            lockReasons.add(reason);
        }

        public void introspect(PrintWriter printWriter) {
            lockReasons.introspect(printWriter);
        }

        public void skipNotify(String methodName, String text) {
            addReason(text);

            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + " " + text);
            }
        }

        @Trivial
        void acquireWriteLock() {
            rwLock.writeLock().lock();
        }

        @Trivial
        void releaseWriteLock() {
            rwLock.writeLock().unlock();
        }

        @Trivial
        void acquireReadLock() {
            rwLock.readLock().lock();
        }

        @Trivial
        void releaseReadLock() {
            rwLock.readLock().unlock();
        }

        void notify(String methodName, String text) {
            addReason(text);

            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + " " + text);
            }
            rwLock.writeLock().lock();
            try {
                condition.signal();
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        void wait(String methodName, String text) throws InterruptedException {
            addReason(text);

            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + " Waiting for [ " + text + " ]");
            }
            condition.await();  // throws InterruptedException
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + " Waited for [ " + text + " ]");
            }
        }

        void waitNS(long waitNs, String methodName, String text) throws InterruptedException {
            addReason(text);

            long waitMs = waitNs / ZipCachingProperties.NANO_IN_MILLI;
            int fracWaitNs = (int) (waitNs - (waitMs * ZipCachingProperties.NANO_IN_MILLI));

            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName +
                    " Waiting [ " + Long.toString(waitMs) + " (ms) " + Integer.toString(fracWaitNs) + " (ns) ]" +
                    " for [ " + text + " ]");
            }

            // 'wait' with no duration is intended for indefinite waits; this parameterized
            // 'wait' is intended for fixed, finite, waits.
            if ( (waitMs == 0) && (fracWaitNs == 0) ) {
                throw new IllegalArgumentException(methodName + ": Invalid zero wait request for [ " + text + " ]");
            }

            condition.await(waitNs, TimeUnit.NANOSECONDS); // throws InterruptedException

            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
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
    private static final long REAP_DELAY_INDEFINITE = -1L;

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
    long reap(long reapAt, boolean isShutdownReap) {
        String methodName = "reap";
        long nextQuickReapDelay = REAP_DELAY_INDEFINITE;
        long nextSlowReapDelay = REAP_DELAY_INDEFINITE;

        synchronized (pendingStorageLock) {
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + " At [ " + fromInitial_s(reapAt) + " ] Force [ " + isShutdownReap + " ]");
                Tr.debug(tc, methodName +
                    " All [ " + storage.size() + " ]" +
                    " Pending Quick [ " + pendingQuickStorage.size() + " ]" +
                    " Pending Slow [ " + pendingSlowStorage.size() + " ]");
            }

            // Reap the quick pending closes ...

            Iterator<ZipFileData> pendingQuick = pendingQuickStorage.values();
            while ( (nextQuickReapDelay == REAP_DELAY_INDEFINITE) && pendingQuick.hasNext() ) {
                ZipFileData nextPending = pendingQuick.next();

                long nextLastPendAt = nextPending.lastPendAt;
                long nextPendDuration = reapAt - nextLastPendAt;

                if ( isShutdownReap ) {
                    // Shutdown closes all pending, regardless of how long they have waited.

                    if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName +
                                " Path [ " + nextPending.path + " ]" +
                                " Waiting [ " + toAbsSec_s(nextPendDuration) + " ] (Quick): Forced");
                    }

                    pendingQuick.remove();

                    fullyClose(nextPending, reapAt, IS_SHUTDOWN_REAP);

                } else { // Normal reap.
                    if ( nextPendDuration > quickPendMin) { // Reached the shortest expiration?
                        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName +
                                " Path [ " + nextPending.path + " ]" +
                                " Waiting [ " + toAbsSec_s(nextPendDuration) + " ] (Quick): Expired");
                        }

                        pendingQuick.remove();

                        fullyClose(nextPending, reapAt, IS_NOT_SHUTDOWN_REAP);

                    } else { // Not yet reached the shorted expiration.
                        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName +
                                " Path [ " + nextPending.path + " ]" +
                                " Waiting [ " + toAbsSec_s(nextPendDuration) + " ]: Still Waiting");
                        }

                        if ( nextPendDuration < 0 ) {
                            nextPendDuration = 0; // Should never happen;
                        }
                        nextQuickReapDelay = quickPendMax - nextPendDuration;
                    }
                }
            }

            // Reap the slow pending closes ...

            Iterator<ZipFileData> pendingSlow = pendingSlowStorage.values();
            while ( (nextSlowReapDelay == REAP_DELAY_INDEFINITE) && pendingSlow.hasNext() ) {
                ZipFileData nextPending = pendingSlow.next();

                long nextLastPendAt = nextPending.lastPendAt;
                long nextPendDuration = reapAt - nextLastPendAt;

                if ( isShutdownReap ) {
                    // Shutdown closes all pending, regardless of how long they have waited.

                    if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName +
                                " Path [ " + nextPending.path + " ]" +
                                " Waiting [ " + toAbsSec_s(nextPendDuration) + " ] (Slow): Forced");
                    }

                    pendingSlow.remove();

                    fullyClose(nextPending, reapAt, IS_SHUTDOWN_REAP);

                } else { // Normal reap.
                    if ( nextPendDuration > slowPendMin ) { // Reached the shortest expiration?
                        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName +
                                " Path [ " + nextPending.path + " ]" +
                                " Waiting [ " + toAbsSec_s(nextPendDuration) + " ] (Slow): Expired");
                        }

                        pendingSlow.remove();

                        fullyClose(nextPending, reapAt, IS_NOT_SHUTDOWN_REAP);

                    } else { // Not yet reached the shorted expiration.
                        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName +
                                " Path [ " + nextPending.path + " ]" +
                                " Waiting [ " + toAbsSec_s(nextPendDuration) + " ]: Still Waiting");
                        }

                        if ( nextPendDuration < 0 ) {
                            nextPendDuration = 0; // Should never happen;
                        }
                        nextSlowReapDelay = slowPendMax - nextPendDuration;
                    }
                }
            }
        }

        // Maybe, move this into a different method, and invoke from the
        // shutdown thread?
        //
        // Placement here seems couples normal reaping with shutdown steps,
        // which seems off.

        if ( isShutdownReap ) {
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
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
                    if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + " Closed [ " + path + " ]: No shutdown action");
                    }
                } else {
                    if ( mustBeOpenOrClosed.isPending() ) {
                        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName + " Unexpected Pending [ " + path + " ]: Shutdown close");
                        }
                    } else {
                        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
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

        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            String delayText =
                ( (nextReapDelay == REAP_DELAY_INDEFINITE) ? "Indefinite" : toAbsSec_s(nextReapDelay) );
            String speedText =
                ( useQuick ? "Quick" : "Slow" );
            Tr.debug(tc, methodName + " Next reap [ " + delayText + " ] (" + speedText + ")");
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
        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + " Path [ " + path + " ] at [ " + toRelSec_s(initialAt, openAt) + " ]");
        }

        // Open could try to turn off the reaper thread if the last pending close
        // is removed.  Instead, the reaper allowed to run, and is coded to handle
        // that case.

        reaperLock.acquireReadLock();
        try {
            if ( !getIsActive() ) {
                asyncWarning("reaper.inactive", path, reaperName);
                // "Cannot open [ " + path + " ]: ZipFile cache [ " + reaperName + " ] is inactive"
                throw new IOException("Cannot open [ " + path + " ]: ZipFile cache is inactive");
            }

            ZipFilePathLock lock = getLockForPath(path);
            synchronized (lock) {
                ZipFileData data = storage.get(path);
                ZipFile zipFile;

                if ( data == null ) {
                    if ( !debugState ) {
                        synchronized (completedStorage) {
                            data = completedStorage.remove(path);
                        }
                    }

                    if ( data == null ) {
                        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName + " New [ " + path + " ]");
                        }
                        data = new ZipFileData( path, getInitialAt() ); // throws IOException, ZipException
                    } else {
                        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName + " Recovered [ " + path + " ]");
                        }
                    }

                    storage.put(path, data);
                }

                if ( data.isFullyClosed() ) {
                    if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + " Open [ " + path + " ]");
                    }

                    zipFile = data.openZipFile(); // throws IOException, ZipException

                } else if ( data.isPending() ) {
                    if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + " Unpend [ " + path + " ]");
                    }

                    synchronized (pendingStorageLock) {
                        if ( data.expireQuickly ) {
                            @SuppressWarnings("unused") // same as 'data'
                            ZipFileData pendingQuickData = pendingQuickStorage.remove(path);
                        } else {
                            @SuppressWarnings("unused") // same as 'data'
                            ZipFileData pendingSlowData = pendingSlowStorage.remove(path);
                        }
                        // Removal from pending may result in the next reap
                        // discovering no expired closes.
                    }

                    try {
                        zipFile = data.reacquireZipFile(); // throws IOException, ZipException

                    } catch (Exception e) {
                        // The closeZipFile() or openZipFile() call failed in reacquireZipFile().
                        // Either way, the proper state should be fully closed.
                        data.enactFullClose(openAt);
                        throw e;
                    }

                } else if ( data.isOpen() ) {
                    if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
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

                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + " Path [ " + path + " ] [ " + zipFile + " ]");
                }
                return zipFile;
            }
        } finally {
            reaperLock.releaseReadLock();
        }
    }

    public ZipFileData.ZipFileState close(String path) {
        return close( path, SystemUtils.getNanoTime() );
    }

    public ZipFileData.ZipFileState close(String path, long closeAt) {
        String methodName = "close";

        String wakeReason = null;
        String skipWakeReason = null;
        ZipFileData.ZipFileState state = null;
        ZipFileData ripestPending = null;

        reaperLock.acquireReadLock();
        try {
            if ( !getIsActive() ) {
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + " Path [ " + path + " ]: Ignore: Inactive");
                }
                return null;
            }

            ZipFilePathLock lock = getLockForPath(path);

            synchronized (lock) {
                ZipFileData data = storage.get(path);

                if ( data == null ) {
                    asyncWarning("reaper.unregistered.path", path);
                    // "Unregistered [ " + path + " ]: Ignore"

                } else if ( data.isFullyClosed() ) {
                    asyncWarning("reaper.closed.path", path);
                    // "Fully closed [ " + path + " ]: Ignore"

                } else if ( data.isPending() ) {
                    asyncWarning("reaper.pending.path", path);
                    // "Pending [ " + path + " ]: No active opens: Ignore"

                } else if ( data.isOpen() ) {
                    if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + " Active opens [ " + path + " ] [ " + data.getActiveOpens() + " ]");
                    }

                    if ( data.enactClose(closeAt, ZipFileData.CLOSE_ONCE) ) {
                        boolean expireQuickly = data.setExpireQuickly(slowPendMin);

                        if ( expireQuickly && (quickPendMin == 0) ) {
                            fullyClose(data, closeAt, IS_NOT_SHUTDOWN_REAP);

                        } else {
                            boolean wasQuickEmpty;
                            boolean wasSlowEmpty;
                            synchronized (pendingStorageLock) {
                                wasQuickEmpty = pendingQuickStorage.isEmpty();
                                wasSlowEmpty = pendingSlowStorage.isEmpty();

                                if ( expireQuickly ) {
                                    // pendingQuickStorage.display();
                                    ripestPending = pendingQuickStorage.addLast(data, getMaxCache());
                                    // pendingQuickStorage.display();
                                } else {
                                    // pendingSlowStorage.display();
                                    ripestPending = pendingSlowStorage.addLast(data, getMaxCache());
                                    // pendingSlowStorage.display();
                                }
                            }

                            if ( wasQuickEmpty && wasSlowEmpty ) {
                                // The reaper was in an indefinite wait, since there were
                                // no pending closes.  Since there are now pending closes,
                                // the reaper must be woken.
                                //
                                // The first reap cycle will perform no closes; the first reap
                                // will find the ripest pending close and set a definite wait
                                // based on that pending close.
                                wakeReason = "Added first pending";
                                skipWakeReason = null;
                             } else if ( expireQuickly ) {
                                if ( !wasQuickEmpty ) { // expireQuickly && !wasQuickEmpty
                                    // Either:
                                    //
                                    // The reaper is set to a definite wait based on the ripest
                                    // quick pending.
                                    //
                                    // If the new pending is expiring quickly, it is expiring
                                    // after the ripest quick pending.
                                    //
                                    // Either way, the definite wait already set for the reaper
                                    // remains correct.
                                    wakeReason = null; // Added quick while quick are present.
                                    skipWakeReason = "Quick added with other quick";
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
                                    //
                                    // The reaper is set to an indefinite wait, because this close
                                    // captured the notification and prevented the reaper from waking
                                    // up for a prior close request.
                                    wakeReason = "Added first quick while slow are present";
                                    skipWakeReason = null;
                                }
                             } else { // !expireQuickly
                                // Since the new pending expires slowly, it must have an expiration which
                                // is later than the expiration of the ripest pending close.  The definite
                                // wait already set for the reaper remains correct.
                                wakeReason = null; // Added slow while quick or slow are present.
                                skipWakeReason = "slow added with quick or slow";
                            }
                        }
                    }
                } else {
                    throw data.unknownState();
                }
                state = data == null ? null : data.zipFileState;
            }
            // After we release the reader lock on the current zip file to prevent a dead lock, 
            // remove the ripestPending if it is still pending closure and not re-added to 
            // the data structures.
            if ( ripestPending != null ) {
                String ripestPath = ripestPending.path;
                ZipFilePathLock lock2 = getLockForPath(ripestPath);
                synchronized (lock2) {
                    // need to check if the data was removed or if the zip file was re-opened
                    // while we were waiting for the lock.
                    ZipFileData checkData = storage.get(ripestPath);
                    if (checkData == ripestPending && checkData.isPending()) {
                        // verify that the ZipFileData was not added back into pending storage
                        ZipFileData pendingCheckData;
                        synchronized (pendingStorageLock) {
                            if ( checkData.expireQuickly ) {
                                pendingCheckData = pendingQuickStorage.get(ripestPath);
                            } else {
                                pendingCheckData = pendingSlowStorage.get(ripestPath);
                            }
                        }
                        if (pendingCheckData == null) {
                            fullyClose(ripestPending, closeAt, IS_NOT_SHUTDOWN_REAP);
                        }
                    }
                }
            }
        } finally {
            reaperLock.releaseReadLock();
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
        } else if (skipWakeReason != null){
            reaperLock.skipNotify(methodName, skipWakeReason);
        }
        
        return state;
    }

    //

    public void introspect(PrintWriter output, long introspectAt) {
        reaperLock.acquireReadLock();
        try {
            output.println();
            output.println("  IsActive [ " + Boolean.valueOf(isActive) + " ]");
            output.println("  Initial  [ " + toAbsSec_s(initialAt) + " ]" );
            output.println("  Final    [ " + toAbsSec_s(finalAt) + " ]");
            output.println("  Current  [ " + toAbsSec_s(introspectAt) + " ]");

            introspectReaperThread(output);

            output.println();
            output.println("Active and Pending Data:");
            if ( storage.isEmpty() ) {
                output.println("  ** NONE **");
            } else {
                for ( Map.Entry<String, ZipFileData> reaperEntry : storage.entrySet() ) {
                    output.println();
                    synchronized (getLockForPath(reaperEntry.getKey())) {
                        reaperEntry.getValue().introspect(output, introspectAt);
                    }
                }
            }

            synchronized (pendingStorageLock) {
                pendingQuickStorage.introspect(output, ZipFileDataStore.DISPLAY_SPARSELY, introspectAt);
                pendingSlowStorage.introspect(output, ZipFileDataStore.DISPLAY_SPARSELY, introspectAt);
            }

            if ( completedStorage == null ) {
                output.println();
                output.println("Completed zip file data is not being tracked");
            } else {
                synchronized (completedStorage) {
                    completedStorage.introspect(output, ZipFileDataStore.DISPLAY_FULLY, introspectAt);
                }
            }
        } finally {
            reaperLock.releaseReadLock();
        }
    }
}

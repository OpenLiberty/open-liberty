/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.artifact.zip.cache.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
 */
@Trivial
public class ZipFileReaper {
    static final TraceComponent tc = Tr.register(ZipFileReaper.class);

    @Trivial
    private void debug(String methodName, String text) {
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + " [ " + getReaperName() + " ] " + text);
        }
    }

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

    /**
     * Data for tracking a single zip file.
     *
     * The data has a state: Initial, Open, Pending, Closed, or Final.
     *
     * Since data is (currently) only created for an immediate open, the
     * data constructor places the data the open state.  The Initial
     * state is never reached.
     *
     * Data is moved to a final state when the reaper shuts down.  This
     * only done if shutdown processing is enabled, which is only when
     * zip state debugging is enabled.
     *
     * The states Open, Pending, and Closed are the main states:
     *
     * The Open state means that the zip file is non-null and the number
     * of active opens is greater than zero.
     *
     * The Pending state means that the zip file is non-null and the number
     * of active opens is zero.
     *
     * The Closed state means that the zip file is null and the number of
     * active opens is zero.
     *
     * There should never be other than zero active opens when the zip file
     * is null.
     *
     * State transition times are tracked: For each state, the time of the
     * first and last enty to the state are recorded.
     *
     * State entries are tracked: For each state, the number of entries to
     * that state is tracked.  For each state, the time spent in the state
     * is tracked.
     *
     * Both the pending and closed states have two exits: Pending can transition
     * directly to open, or can transition to closed.  Closed can transition to
     * open, or can transition (eventually) to the final state.
     *
     * Tracking how many times the different transitions occur is important:
     * Pending to open represents a successful cache of a zip file.  The time
     * spent pending before an open represents the overhead of keeping the zip
     * file open.  A transition from closed to open represents a failed cache
     * of a zip file.  The time spent in closed before an open represents the
     * overhead which was avoided by allowing the zip file to close.
     */
    private static class ZipFileData {
        /**
         * Main operations: Create, with the initial state as open.
         *
         * @param path The path to the zip file.
         * @param zipFile The zip file.
         * @param initialAt The time at which the reaper subsystem was initialized.
         * @param openAt When the zip file was initially opened.
         */
        public ZipFileData(String path, ZipFile zipFile, long initialAt, long openAt) {
            String methodName = "<init>";

            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName, "Create [ " + path + " ] at [ " + toRelSec(initialAt, openAt) + " ]");
            }

            this.path = path; // Fixed: The identity of the zip data.

            // activeOpens == 0; zipFile == null: simply closed
            // activeOpens == 0; zipFile != null: pending close
            // activeOpens  > 0; zipFile != null: open
            // activeOpens  > 0; zipFile == null: invalid

            this.zipFile = zipFile;
            this.activeOpens = 1; // Start open

            //

            this.fromInitialToOpen(initialAt, openAt);
        }

        /**
         * Transition from an "initial" state to an "open" state.
         *
         * This is somewhat improperly names, as both the initial assignment
         * and the transition from "initial" to "open" are done.
         *
         * @param useInitialAt The initial time for the subsystem.
         * @param openAt When the zip file was opened.
         */
        private void fromInitialToOpen(long useInitialAt, long openAt) {
            // See the comments in 'debugState' for an explanation of
            // the state model and the statistics which are gathered.

            initialAt = useInitialAt;

            firstOpenAt = openAt;
            lastOpenAt = openAt;

            openCount = 1;
            openDuration = 0L;

            firstPendAt = -1L; // Only meaningful after the first pend.
            lastPendAt = -1L; // Only meaningful after the first pend.

            pendCount = 0;
            pendDuration = 0L;

            pendBeforeOpenCount = 0;
            pendBeforeOpenDuration = 0L;

            pendBeforeCloseCount = 0;
            pendBeforeCloseDuration = 0L;

            firstCloseAt = -1L; // Only meaningful after the first close.
            lastCloseAt = -1L; // Only meaningful after the first close.

            closeCount = 0;
            closeDuration = 0L;

            closeBeforeOpenCount = 0;
            closeBeforeOpenDuration = 0L;

            finalAt = 0L;
        }

        private void fromOpenToPending(long pendAt) {
            openDuration += pendAt - lastOpenAt;

            lastPendAt = pendAt;
            if ( pendCount == 0 ) {
                firstPendAt = pendAt;
            }

            pendCount++;
        }

        private void fromPendingToOpen(long openAt) {
            lastOpenAt = openAt;
            openCount++;

            long nextPendDuration = openAt - lastPendAt;
            pendDuration += nextPendDuration;

            pendBeforeOpenCount++;
            pendBeforeOpenDuration += nextPendDuration;
        }

        private void fromPendingToClosed(long closeAt) {
            lastCloseAt = closeAt;
            if ( closeCount == 0 ) {
                firstCloseAt = closeAt;
            }
            closeCount++;

            long nextPendDuration = closeAt - lastPendAt;
            pendDuration += nextPendDuration;

            pendBeforeCloseCount++;
            pendBeforeCloseDuration += nextPendDuration;
        }

        private void fromClosedToOpen(long openAt) {
            lastOpenAt = openAt;
            openCount++;

            long nextCloseDuration = openAt - lastCloseAt;
            closeDuration += nextCloseDuration;

            closeBeforeOpenCount++;
            closeBeforeOpenDuration += nextCloseDuration;
        }

        private void fromClosedToFinal(long useFinalAt) {
            finalAt = useFinalAt;

            long nextCloseDuration = finalAt - lastCloseAt;
            closeDuration += nextCloseDuration;
        }

        // Main operations ...

        /**
         * A close is requested on the data. The data should be open.
         *
         * @param pendAt When the close is requested.
         */
        public void pendClose(long pendAt) {
            String methodName = "pendClose";
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + "On [ " + getPath() + " ] at [ " + toRelSec(initialAt, pendAt) + " ]");
            }
            if ( zipFile == null ) { // (zipFile == null) && (activeOpens == 0)
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "No zip file; ignoring!");
                }
            } else if ( activeOpens > 0 ) { // (zipFile != null) && (activeOpens > 0)
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "Active opens; ignoring!");
                }
            } else { // (zipFile != null) && (activeOpens == 0)
                fromOpenToPending(pendAt);
            }
        }

        /**
         * A close is performed on the data.  The data
         * may be open or may be pending a close.
         *
         * @param closeAt When the close is requested.
         */
        public void close(long closeAt) {
            String methodName = "close";
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + "On [ " + getPath() + " ] at [ " + toRelSec(initialAt, closeAt) + " ]");
            }
            if ( zipFile == null ) { // (zipFile == null) && (activeOpens == 0)
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "No zip file; ignoring!");
                }
                return;
            } else if ( activeOpens > 0 ) { // (zipFile != null) && (activeOpens > 0)
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "Active opens; ignoring");
                }
                return;
            }
            // else // (zipFile != null) && (activeOpens == 0)

            ZipFile useZipFile = zipFile;
            zipFile = null;

            try {
                ZipFileUtils.closeZipFile(getPath(), useZipFile); // throws IOException
            } catch ( IOException e ) {
                Tr.debug(tc, methodName + "Close failure [ " + getPath() + " ] [ " + e.getMessage() + " ]");
                // FFDC
            }

            //

            fromPendingToClosed(closeAt);
        }

        /**
         * An open is requested on the data.  The data
         * is pending a close.
         *
         * @param openAt When the open was performed.
         */
        public void unpendClose(long openAt) {
            String methodName = "unpendClose";
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + "On [ " + getPath() + " ] at [ " + toRelSec(initialAt, openAt) + " ]");
            }
            if ( zipFile == null ) { // (zipFile == null) && (activeOpens == 0)
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "No zip file; ignoring!");
                }
            } else if ( activeOpens > 0 ) { // (zipFile != null) && (activeOpens > 0)
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "Active opens; ignoring!");
                }
            } else { // (zipFile != null) && (activeOpens == 0)
                fromPendingToOpen(openAt);
            }
        }

        /**
         * An open is requested on the data.  The data is closed.
         *
         * @param useZipFile The zip file from the recent open.
         * @param openAt When the open was performed.
         */
        public void reopen(ZipFile useZipFile, long openAt) {
            String methodName = "reopen";
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + "On [ " + getPath() + " ] at [ " + toRelSec(initialAt, openAt) + " ]");
            }
            if ( zipFile != null ) { // (zipFile != null) && (activeOpens == 0) || (activeOpens > 0)
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "Open; ignoring!");
                }
            } else { // (zipFile == null) && (activeOpens == 0)
                zipFile = useZipFile;
                fromClosedToOpen(openAt);
            }
        }

        // Identity ...
        //
        // The path is used as the unique key for zip data.
        //
        // Callers must ensure that meaningful paths are provided, with
        // attention given to use paths which are the same for the same
        // actual files on disk.

        private final String path;

        @Trivial
        public String getPath() {
            return path;
        }

        // State ...

        // activeOpens == 0; zipFile == null: simply closed
        // activeOpens == 0; zipFile != null: pending close
        // activeOpens  > 0; zipFile != null: open
        // activeOpens  > 0; zipFile == null: invalid

        private ZipFile zipFile;

        @Trivial
        public ZipFile getZipFile() {
            return zipFile;
        }

        @Trivial
        private void setZipFile(ZipFile zipFile) {
            this.zipFile = zipFile;
        }

        private int activeOpens;

        @Trivial
        public int getActiveOpens() {
            return activeOpens;
        }

        @Trivial
        public int removeAllActiveOpens() {
            int oldActiveOpens = activeOpens;
            activeOpens = 0;
            return oldActiveOpens;
        }

        @Trivial
        public int removeActiveOpen() {
            return ( activeOpens -= 1 );
        }

        @Trivial
        public int addActiveOpen() {
            return ( activeOpens += 1 );
        }

        @Trivial
        public boolean getIsClosed() {
            return ( zipFile == null );
            // activeOpens == 0; zipFile == null
        }

        @Trivial
        public boolean getIsClosePending() {
            return ( (activeOpens == 0) && ( zipFile != null) );
             // activeOpens == 0; zipFile != null
        }

        @Trivial
        public boolean getIsOpen() {
            return ( activeOpens > 0 );
            // activeOpens  > 0; zipFile != null
        }

        // State tracking ...

        private long initialAt;

        // private int initialBeforeOpenCount; // Always 1
        // private long initialBeforeOpenDuration; // Always 'firstOpenAt - initialOpenAt'.

        private long firstOpenAt;
        private long lastOpenAt;

        private int openCount;
        private long openDuration;

        private long firstPendAt;
        private long lastPendAt;

        public long getLastPendAt() {
            return lastPendAt;
        }

        private int pendCount;
        private long pendDuration;

        private int pendBeforeOpenCount;
        private long pendBeforeOpenDuration;
        private int pendBeforeCloseCount;
        private long pendBeforeCloseDuration;

        private long firstCloseAt;
        private long lastCloseAt;

        private int closeCount;
        private long closeDuration;

        private int closeBeforeOpenCount;
        private long closeBeforeOpenDuration;

        // private int closeBeforeFinalCount; // Always 1
        // private long closeBeforeFinalDuration; // Always 'finalAt - lastCloseAt'.

        private long finalAt;

        //

        @Trivial
        public void debugState() {
            String methodName = "debugState";

            if ( !ZipCachingProperties.ZIP_CACHE_DEBUG_STATE || !tc.isInfoEnabled() ) {
                return;
            }

            // See the class comment for details of the state model and the
            // statistics which are gathered.

            Tr.info(tc, methodName, "ZipFile [ " + getPath() + " ]:");

            String overallText =
                    "Span: Initial [ " + toAbsSec(initialAt) + " ]" +
                    " Final [ " + toAbsSec(finalAt) + " ]" +
                    " Duration [ " + toAbsSec(finalAt - initialAt) + " ]";
            Tr.info(tc, methodName + overallText);

            String marginText =
                    "  Margin: To First Open [ " + toAbsSec(firstOpenAt - initialAt) + " ]" +
                    " From Last Close [ " + toAbsSec(finalAt - lastCloseAt) + " ]";
            Tr.info(tc, methodName + marginText);

            String openText =
                    "  Open: First [ " + toRelSec(initialAt, firstOpenAt) + " ]" +
                    " Last [ " + toRelSec(initialAt, lastOpenAt) + " ]" +
                    " Count [ " + toCount(openCount) + " ]" +
                    " Duration [ " + toAbsSec(openDuration) + " ]";
            Tr.info(tc, methodName + openText);

            String pendingText =
                    "  Pending: First [ " + toRelSec(initialAt, firstPendAt) + " ]" +
                    " Last [ " + toRelSec(initialAt, lastPendAt) + " ]" +
                    " Count [ " + toCount(pendCount) + " ]" +
                    " Duration [ " + toAbsSec(pendDuration) + " ]";
            Tr.info(tc, methodName + pendingText);

            String pendingBeforeOpenText =
                    "    Pending to Open: Count [ " + toCount(pendBeforeOpenCount) + " ]" +
                    " Duration [ " + toAbsSec(pendBeforeOpenDuration) + " ]";
            Tr.info(tc, methodName + pendingBeforeOpenText);

            String pendingBeforeCloseText =
                    "    Pending to Close: Count [ " + toCount(pendBeforeCloseCount) + " ]" +
                    " Duration [ " + toAbsSec(pendBeforeCloseDuration) + " ]";
            Tr.info(tc, methodName + pendingBeforeCloseText);

            String closeText =
                    "  Close: First [ " + toRelSec(initialAt, firstCloseAt) + " ]" +
                    " Last [ " + toRelSec(initialAt, lastCloseAt) + " ]" +
                    " Count [ " + toCount(closeCount) + " ]" +
                    " Duration [ " + toAbsSec(closeDuration) + " ]";
            Tr.info(tc, methodName + closeText);

            String closeBeforeOpenText =
                    "    Close to Open: Count [ " + toCount(closeBeforeOpenCount) + " ]" +
                    " Duration [ " + toAbsSec(closeBeforeOpenDuration) + " ]";
            Tr.info(tc, methodName + closeBeforeOpenText);
        }
    }

    private static class Reaper implements Runnable {
        @Trivial
        public Reaper(ZipFileReaper zipFileReaper) {
            this.zipFileReaper = zipFileReaper;
        }

        private final ZipFileReaper zipFileReaper;

        @Trivial
        public ZipFileReaper getZipFileReaper() {
            return zipFileReaper;
        }

        //

        @Trivial
        public void run() {
            String methodName = "run";

            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + "Start");
            }

            ZipFileReaper useReaper = getZipFileReaper();
            ReaperLock useReaperLock = useReaper.getReaperLock();

            synchronized ( useReaperLock ) {
                boolean isInterrupted = false;
                while ( !isInterrupted ) {
                    try {
                        if ( useReaper.haveNoPendingCloses() ) {
                            if ( tc.isDebugEnabled() ) {
                                Tr.debug(tc, methodName + "Waiting for new pending close");
                            }
                            useReaperLock.wait(); // 'wait' throws InterruptedException
                            if ( tc.isDebugEnabled() ) {
                                Tr.debug(tc, methodName + "Notified by new pending close");
                            }

                            // Wait for the single pending close which is known to be present.

                            long reapAt = SystemUtils.getNanoTime();

                            ZipFileData lastPendingClose = useReaper.getLastPendingClose();
                            long lastPendAt = lastPendingClose.lastPendAt;

                            long lastWaitDuration = ( reapAt - lastPendAt );
                            long useDelayUpper = useReaper.getDelayUpper();
                            if ( lastWaitDuration < useDelayUpper ) {
                                long reapDelay = useDelayUpper - lastWaitDuration;

                                if ( tc.isDebugEnabled() ) {
                                    Tr.debug(tc, methodName + "Waiting [ " + toAbsSec(reapDelay) + " ] for new pending close");
                                }
                                useReaperLock.wait(reapDelay / ZipCachingProperties.ONE_MILLI_SEC_IN_NANO_SEC); // throws InterruptedException
                                if ( tc.isDebugEnabled() ) {
                                    Tr.debug(tc, methodName + "Waited for new pending close");
                                }
                            } else {
                                // Can happen if the reaper wakes up a long time after the notification
                                // was provided from posting the close request.
                                if ( tc.isDebugEnabled() ) {
                                    Tr.debug(tc, methodName + "Already waited [ " + toAbsSec(lastWaitDuration) + " ]");
                                }
                            }

                        } else {
                            long reapAt = SystemUtils.getNanoTime();
                            if ( tc.isDebugEnabled() ) {
                                Tr.debug(tc, methodName + "Reaping at [ " + toRelSec(useReaper.initialAt, reapAt) + " ]");
                            }
                            long reapDelay = useReaper.reap(reapAt, ZipFileReaper.IS_NOT_SHUTDOWN_REAP);

                            if ( reapDelay < 0 ) {
                                if ( tc.isDebugEnabled() ) {
                                    Tr.debug(tc, methodName + "Reaped; next delay [ indefinite ]");
                                }
                            } else {
                                if ( tc.isDebugEnabled() ) {
                                    Tr.debug(tc, methodName + "Waiting [ " + toAbsSec(reapDelay) + " ] for current pending close");
                                }
                                useReaperLock.wait(reapDelay / ZipCachingProperties.ONE_MILLI_SEC_IN_NANO_SEC); // throws InterruptedException
                                if ( tc.isDebugEnabled() ) {
                                    Tr.debug(tc, methodName + "Waited for current pending close");
                                }
                            }
                        }

                    } catch ( InterruptedException e ) {
                        if ( tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName + "Interrupted!");
                        }
                        isInterrupted = true;
                    }
                }
            }

            // Maybe, move this to the shutdown thread.

            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + "Reaping (forced)");
            }
            getZipFileReaper().reap(SystemUtils.getNanoTime(), ZipFileReaper.IS_SHUTDOWN_REAP);
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + "Reaped (forced)");
            }

            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + "Stop");
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
            ZipCachingProperties.ZIP_CACHE_REAPER_MAX_PENDING,
            ZipCachingProperties.ZIP_CACHE_REAPER_SHORT_INTERVAL,
            ZipCachingProperties.ZIP_CACHE_REAPER_LONG_INTERVAL);
    }

    @Trivial
    public ZipFileReaper(
        String reaperName,
        int maxCache, long delayLower, long delayUpper) {

        this(reaperName, maxCache, delayLower, delayUpper, SystemUtils.getNanoTime() );
    }

    public ZipFileReaper(
        String reaperName,
        int maxCache, long delayLower, long delayUpper, final long initialAt) {

        if ( maxCache == 0 ) {
            throw new IllegalArgumentException("Max cache cannot be zero.");
        }

        if ( delayLower <= 0 ) {
            throw new IllegalArgumentException("Lower delay [ " + Long.toString(delayLower) + " ] must be positive");
        } else if ( delayUpper <= 0 ) {
            throw new IllegalArgumentException("Upper delay [ " + Long.toString(delayUpper) + " ] must be positive");
        } else if ( delayLower >= delayUpper ) {
            throw new IllegalArgumentException(
                "Lower delay [ " + Long.toString(delayLower) + " ]" +
                " must less than upper delay [ " + Long.toString(delayUpper) + " ]");
        }

        this.reaperName = reaperName;

        this.maxCache = maxCache;

        this.delayLower = delayLower;
        this.delayUpper = delayUpper;

        this.isActive = true;

        this.initialAt = initialAt;
        this.finalAt = 0L;

        this.zipData = new HashMap<String, ZipFileData>();

        this.pendingCloses = new LinkedHashMap<String, ZipFileData>() {
            private static final long serialVersionUID = 1L;

            @Override
            @Trivial
            protected boolean removeEldestEntry(Map.Entry<String, ZipFileData> eldestEntry) {
                String methodName = "removeEldestEntry";

                // Don't remove the eldest entry when on a shutdown reap:
                // Allow all of the open zip files to pend before doing any of
                // the closes.
                if ( !getIsActive() ) {
                    return false;
                }

                int useMaxCache = getMaxCache();
                if ( useMaxCache < 0 ) {
                    // A max cache of -1 makes the cache unbounded.
                    return false;
                } else if ( size() < useMaxCache ) {
                    return false;
                } else {
                    // Set the eldest: The caller of 'put' needs to check
                    // for this and close it.
                    //
                    // Alternatively, the close could be performed here.
                    // That is not currently done: The caller of 'put' is responsible
                    // for doing all zip data state updates.

                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + "Removed eldest [ " + eldestEntry.getKey() + " ]");
                    }
                    setEldestPendingClose( eldestEntry.getValue() );

                    return true;
                }
            }
        };

        this.youngestPendingClose = null;
        this.eldestPendingClose = null;

        this.reaperLock = new ReaperLock();

        this.reaper = new Reaper(this);
        this.reaperThread = new Thread(this.reaper, "zip file reaper");
        this.reaperThread.setDaemon(true);

        // Use of the shutdown thread is optional.  Shutdown provides an
        // opportunity to complete the thread statistics, but at the cost
        // of iterating across and closing all active zip files, which is
        // very probably unnecessary since the JVM is shutting down.

        boolean useShutdown = ZipCachingProperties.ZIP_CACHE_DEBUG_STATE;

        if ( useShutdown ) {
            this.reaperShutdown = new ReaperShutdown(this.reaperThread);
            this.reaperShutdownThread = new Thread(this.reaperShutdown);
        } else {
            this.reaperShutdown = null;
            this.reaperShutdownThread = null;
        }

        // TODO: Not sure which of the following two steps to do first.

        this.reaperThread.start();

        if ( useShutdown ) {
            SystemUtils.addShutdownHook(this.reaperShutdownThread);
        }
    }

    /**
     * Shutdown code for the reaper thread.
     *
     * That thread runs as a daemon, so shutdown is not entirely
     * necessary.  However, shutdown is useful for completing and
     * displaying the zip file statistics.
     */
    private static class ReaperShutdown implements Runnable {
        @Trivial
        public ReaperShutdown(Thread reaperThread) {
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

    public String getReaperName() {
        return reaperName;
    }

    //

    private final int maxCache;

    @Trivial
    public int getMaxCache() {
        return maxCache;
    }

    //

    /** How the minimum that the close of a zip file is delayed. */
    private final long delayLower;

    /** The maximum that the close of a zip file is delayed. */
    private final long delayUpper;

    @Trivial
    public long getDelayLower() {
        return delayLower;
    }

    @Trivial
    public long getDelayUpper() {
        return delayUpper;
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

    /** Data for all zip file ever opened using the zip file reaper. */
    private final Map<String, ZipFileData> zipData;

    @Trivial
    private Map<String, ZipFileData> getZipData() {
        return zipData;
    }

    @Trivial
    private ZipFileData get(String path) {
        return zipData.get(path);
    }

    @Trivial
    private void put(String path, ZipFileData data) {
        zipData.put(path, data);
    }

    @SuppressWarnings("unused")
    private ZipFileData remove(String path) {
        return zipData.remove(path);
    }

    //

    /**
     * All pending closes.  These are kept in the order in which close
     * requests were received.  These should always be in ascending
     * order.
     */
    private final LinkedHashMap<String, ZipFileData> pendingCloses;

    /**
     * The last close which was requested.  This is useful when
     * to transition from an empty collection of pending closes
     * to just one pending close.
     */
    private ZipFileData youngestPendingClose;

    private ZipFileData eldestPendingClose;

    @Trivial
    private LinkedHashMap<String, ZipFileData> getPendingCloses() {
        return pendingCloses;
    }

    @Trivial
    private boolean haveNoPendingCloses() {
        return pendingCloses.isEmpty();
    }

    @Trivial
    private boolean haveOnePendingClose() {
        return ( pendingCloses.size() == 1 );
    }

    @Trivial
    private boolean isPendingFull() {
        // 'maxCache == -1' means never full
        // 'maxCache == 0' is not allowed
        return ( pendingCloses.size() == getMaxCache() );
    }

    @Trivial
    private ZipFileData putPending(String path, ZipFileData data) {
        youngestPendingClose = data;

        return pendingCloses.put(path, data);
    }

    @Trivial
    private ZipFileData removePending(String path) {
        return pendingCloses.remove(path);
    }

    @Trivial
    private ZipFileData getLastPendingClose() {
        return youngestPendingClose;
    }

    @Trivial
    private void setEldestPendingClose(ZipFileData eldestPendingClose) {
        this.eldestPendingClose = eldestPendingClose;
    }

    @Trivial
    private ZipFileData getEldestPendingClose() {
         ZipFileData useEldest = eldestPendingClose;
         if ( useEldest != null ) {
             eldestPendingClose = null;
         }
         return useEldest;
    }

    // Reaping ...

    /** The runnable of the reaper shutdown thread. */
    private final ReaperShutdown reaperShutdown;
    /**
     * The reaper shutdown thread.  Used to shut down
     * the reaper thread, which includes steps to
     * close all open zip files and to display zip file
     * statistics.
     */
    private final Thread reaperShutdownThread;

    @Trivial
    private ReaperShutdown getReaperShutdown() {
        return reaperShutdown;
    }

    @Trivial
    private Thread getReaperShutdownThread() {
        return reaperShutdownThread;
    }

    /** The actual reaper. */
    private final Reaper reaper;
    /** The thread used to run the reaper. */
    private final Thread reaperThread;

    @Trivial
    private Reaper getReaper() {
        return reaper;
    }

    @Trivial
    private Thread getReaperThread() {
        return reaperThread;
    }

    //

    private static class ReaperLock {
        // EMPTY
    }
    private final ReaperLock reaperLock;

    @Trivial
    private ReaperLock getReaperLock() {
        return reaperLock;
    }

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
     * Reaping is based on two intervals, a lower delay amount, which is the
     * the threshold for allowing a close to proceed, and an upper delay amount,
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
            Tr.debug(tc, methodName + "At [ " + toRelSec(initialAt, reapAt) + " ] Force [ " + Boolean.toString(isShutdownReap) + " ]");
        }

        long nextReapDelay = REAP_DELAY_INDEFINITE;

        Map<String, ZipFileData> useStorage = getZipData();
        Map<String, ZipFileData> usePendingCloses = getPendingCloses();

        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName +
                "All [ " + Integer.toString(useStorage.size()) + " ]" +
                " Pending [ " + Integer.toString(usePendingCloses.size()) + " ]");
        }

        long useDelayLower = getDelayLower();
        long useDelayUpper = getDelayUpper();

        Iterator<ZipFileData> allPendingCloses = usePendingCloses.values().iterator();

        while ( (nextReapDelay == -1) && allPendingCloses.hasNext() ) {
            ZipFileData nextPending = allPendingCloses.next();

            long nextLastPendAt = nextPending.getLastPendAt();
            long nextPendDuration = reapAt - nextLastPendAt;

            String nextPendPath = nextPending.getPath();
            String nextPendSec = toAbsSec(nextPendDuration);

            if ( isShutdownReap ) {
                 // Shutdown closes all pending, regardless of how long they have waited.
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "Path [ " + nextPendPath + " ] Waiting [ " + nextPendSec + " ]: Forced");
                }
                nextPending.close(reapAt);
                allPendingCloses.remove();

                // Do not remove the closed data during a shutdown reap.
                // We want to keep the data around so we can display zip file statistics.

            } else {
                // Note that we check using the lower interval ...
                if ( nextPendDuration > useDelayLower ) {
                    // Otherwise, close pending which have waited the lower delay amount.
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + "Path [ " + nextPendPath + " ] Waiting [ " + nextPendSec + " ]: Expired");
                    }
                    nextPending.close(reapAt);
                    allPendingCloses.remove();

                    // If we are tracking state, keep zip data forever.
                    // That gives us complete tracking data after the shutdown reap.
                    if ( !ZipCachingProperties.ZIP_CACHE_DEBUG_STATE ) {
                        zipData.remove(nextPendPath);
                    }

                } else {
                    // Keep waiting any pending which has waited less than the lower delay
                    // amount.
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + "Path [ " + nextPendPath + " ] Waiting [ " + nextPendSec + " ]: Still Waiting");
                    }

                    if ( nextPendDuration < 0 ) {
                        nextPendDuration = 0; // Should never happen;
                    }
                    // ... but we set the delay using the upper interval.
                    nextReapDelay = useDelayUpper - nextPendDuration;
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
                Tr.debug(tc, methodName + "De-activating reaper");
            }

            // We have the lock: There can be no activity since receiving
            // the interrupted exception and setting the reaper inactive.

            // Note: Have to set this before pending the outstanding open zip files.
            //        Remove of the eldest is not performed while shutting down.
            setIsActive(false);

            setFinalAt(reapAt);

            // activeOpens == 0; zipFile == null: simply closed
            // activeOpens == 0; zipFile != null: pending close
            // activeOpens  > 0; zipFile != null: open
            // activeOpens  > 0; zipFile == null: invalid

            // Since this is a shut-down reap, all pending closes were
            // forced close, regardless of how long they were waiting.
            // There are only dangling opens to handle.

            for ( ZipFileData mustBeOpenOrClosed : useStorage.values() ) {
                String path = mustBeOpenOrClosed.getPath();

                if ( mustBeOpenOrClosed.getIsClosed() ) { // activeOpens == 0; zipFile == null
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + "Closed [ " + path + " ]: No shutdown action");
                    }
                } else {
                    if ( mustBeOpenOrClosed.getIsClosePending() ) { // activeOpens == 0; zipFile != null
                        if ( tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName + "Unexpected Pending [ " + path + " ]: Shutdown close");
                        }
                    } else {
                        // activeOpens  > 0; zipFile != null
                        int danglingOpens = mustBeOpenOrClosed.removeAllActiveOpens();
                        if ( tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName +
                                "Open [ " + path + " ] [ " + Integer.toString(danglingOpens) + " ]:" +
                                " Shutdown pend and close");
                        }
                        mustBeOpenOrClosed.pendClose(reapAt);
                    }
                    mustBeOpenOrClosed.close(reapAt);
                }
            }

            // Finalize the zip files, all of which should be closed.
            //
            // Display statistics for each of the zip files.

            for ( ZipFileData mustBeClosed : useStorage.values() ) {
                mustBeClosed.fromClosedToFinal(reapAt);
                mustBeClosed.debugState();
            }
        }

        if ( nextReapDelay < 0 ) {
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + "Next reap [ indefinite ]");
            }
        } else {
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, methodName + "Next reap [ " + toAbsSec(nextReapDelay) + " ]");
            }
        }
        return nextReapDelay;
    }

    /**
     * Open a zip file.
     *
     * Set the open time as the current time.
     *
     * See {@link #open(String)}.
     *
     * @param path The path to the zip file.
     */
    public ZipFile open(String path) throws IOException, ZipException {
        return open( path, SystemUtils.getNanoTime() );
    }

    /**
     * Open a zip file.
     *
     * What to do depends on whether the zip file was previously seen.
     *
     * If the zip file is entirely new, create data for the zip file.
     *
     * If the zip file was seen before, if the zip file is open, do nothing.
     * If the zip file is waiting to close, stop waiting to close it.  If
     * the zip file is closed, mark it as open.
     *
     * @param path The path to the zip file.
     * @param zipFile The zip file.
     * @param openAt The time at which the zip file was opened.
     */
    public ZipFile open(String path, long openAt) throws IOException, ZipException {
        String methodName = "open";

        // Open could try to turn off the reaper thread if the last pending close
        // is removed.  Instead, the reaper allowed to run, and is coded to handle
        // that case.

        synchronized ( getReaperLock() ) {
            if ( !getIsActive() ) {
                Tr.debug(tc, methodName + "Path [ " + path + " ]: Fail");
                throw new IOException("Cannot open [ " + path + " ]: ZipFile cache is inactive");
            }

            // null: Not yet seen
            //
            // activeOpens == 0; zipFile == null: simply closed
            // activeOpens == 0; zipFile != null: pending close
            // activeOpens  > 0; zipFile != null: open
            // activeOpens  > 0; zipFile == null: invalid

            ZipFile zipFile;

            ZipFileData data = get(path);

            if ( data == null ) { // Complete new zip file.
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "New [ " + path + " ]: Open and create data");
                }

                zipFile = ZipFileUtils.openZipFile(path); // throws IOException, ZipException
                data = new ZipFileData(path, zipFile, getInitialAt(), openAt);
                put(path, data);

            } else if ( data.getIsClosePending() ) { // activeOpens == 0; zipFile != null
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "Pending [ " + path + " ]: Unpend close; retrieve prior open");
                }
                removePending(path);
                data.unpendClose(openAt);
                data.addActiveOpen();
                zipFile = data.getZipFile();
                // The reaper thread might wake up early now!  That is OK:
                // The reaper tolerates changes to the pending closes collection.

            } else if ( data.getIsOpen() ) { // activeOpens  > 0; zipFile != null
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "Open [ " + path + " ]: Retrieve prior open");
                }
                data.addActiveOpen();
                zipFile = data.getZipFile();

            } else {
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "Closed [ " + path + " ]: Re-open and store");
                }
                zipFile = ZipFileUtils.openZipFile(path); // throws IOException, ZipException
                data.reopen(zipFile, openAt);
            }

            return zipFile;
        }
    }

    /**
     * A zip file is being closed.
     *
     * Don't actually close the zip file: Delay the close for a preset
     * interval.
     *
     * Set the close time as the current time.
     *
     * See {@link #close(String, long)}.
     *
     * @param path The path to the zip file.
     */
    public void close(String path) {
        close( path, SystemUtils.getNanoTime() );
    }

    /**
     * A zip file is being closed.
     *
     * Don't actually close the zip file: Delay the close for a preset
     * interval.
     *
     * @param path The path to the zip file.
     * @param closeAt The time of the close.
     */
    public void close(String path, long closeAt) {
        String methodName = "close";

        ReaperLock useReaperLock = getReaperLock();

        synchronized ( useReaperLock ) {
            if ( !getIsActive() ) {
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "Path [ " + path + " ]: Ignore: Inactive");
                }
                return;
            }

            // null: Not yet seen
            //
            // activeOpens == 0; zipFile == null: simply closed
            // activeOpens == 0; zipFile != null: pending close
            // activeOpens  > 0; zipFile != null: open
            // activeOpens  > 0; zipFile == null: invalid

            ZipFileData data = get(path);

            if ( data == null ) {
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "Unregistered [ " + path + " ]: Ignore");
                }

            } else if ( data.getIsClosed() ) { // activeOpens == 0; zipFile == null
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "Closed [ " + path + " ]: Ignore");
                }

            } else if ( data.getIsClosePending() ) { // activeOpens == 0; zipFile != null
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + "Pending [ " + path + " ]: Ignore");
                }

            } else {
                int remainingActiveOpens = data.removeActiveOpen();

                if ( remainingActiveOpens > 0 ) {
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + "Active opens [ " + path + " ] [ " + Integer.toString(remainingActiveOpens) + " ]: Leave open");
                    }

                } else {
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + "Active opens [ " + path + " ] [ 0 ]: Pend close");
                    }

                    putPending(path, data);
                    data.pendClose(closeAt);

                    // If the eldest was forced out, immediately close it.
                    // 'getEldestPendingClose' is linear: If the eldest
                    // was set, it is cleared.
                    ZipFileData useEldest = getEldestPendingClose();
                    if ( useEldest != null ) {
                        useEldest.close(closeAt);
                    }

                    // The reaper goes into an indefinite wait when the pending
                    // closes are exhausted.  Upon receiving a new first pending
                    // close, wake the reaper.

                    if ( haveOnePendingClose() ) {
                        if ( tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName + "Awaken reaper");
                        }
                        useReaperLock.notify();
                    }
                }
            }
        }
    }
}

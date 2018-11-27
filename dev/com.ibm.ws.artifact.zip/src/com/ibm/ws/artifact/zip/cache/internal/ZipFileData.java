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

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.zip.cache.ZipCachingProperties;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

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
public class ZipFileData {
    static final TraceComponent tc = Tr.register(ZipFileData.class);

    //

    private static final boolean ZIP_REAPER_COLLECT_TIMINGS =
        ZipCachingProperties.ZIP_REAPER_COLLECT_TIMINGS;

    @Trivial
    private static String toRelSec(long initialAt, long finalAt) {
        return ZipCachingProperties.toRelSec(initialAt, finalAt);
    }

    @Trivial
    private static String toAbsSec(long eventAt) {
        return ZipCachingProperties.toAbsSec(eventAt);
    }

    @Trivial
    private static String dualTiming(long eventAt, long initialAt) {
        return ZipCachingProperties.dualTiming(eventAt, initialAt);
    }

    @Trivial
    private static String dualTiming(long eventAt) {
        return ZipCachingProperties.dualTiming(eventAt);
    }

    @Trivial
    private void timing(String text) {
        System.out.println("ZFR Path [ " + path + " ] " + text);
    }

    @Trivial
    private String openState() {
        return "Opens/Closes [ " + openCount + "/" + closeCount + " ]";
    }

    @Trivial
    private static String toCount(int count) {
        return ZipCachingProperties.toCount(count);
    }

    //

    /**
     * Main operations: Create, with the initial state as open.
     *
     * @param path The path to the zip file.
     * @param initialAt The time at which the reaper subsystem was initialized.
     * @param openAt When the zip file was initially opened.
     * 
     * @throws IOException Thrown if the open fails.
     * @throws ZipException Thrown if the open fails.
     */
    public ZipFileData(String path, long initialAt)
        throws IOException, ZipException {
        String methodName = "<init>";
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName, "Create [ " + path + " ]");
        }

        this.path = path; // Fixed: The identity of the zip data.
        this.setInitial(initialAt);
    }

    //

    public static enum ZipFileState {
        OPEN, PENDING, FULLY_CLOSED;
    }

    public static enum ZipFileAction {
        OPEN, CLOSE, FULL_CLOSE;
    }

    //

    private void setInitial(long useInitialAt) {
        zipFileState = ZipFileState.FULLY_CLOSED;

        initialAt = useInitialAt;
        finalAt = -1L;

        openCount = 0;
        closeCount = 0;
        openToPendCount = 0;
        pendToOpenCount = 0;
        pendToFullCloseCount = 0;
        fullCloseToOpenCount = 0;

        firstOpenAt = -1L;
        lastLastOpenAt = -1L;
        lastOpenAt = -1L;

        firstPendAt = -1L;
        lastPendAt = -1L;

        firstFullCloseAt = -1L;
        lastFullCloseAt = -1L;

        openDuration = 0L;

        pendToOpenDuration = 0L;
        pendToFullCloseDuration = 0L;

        fullCloseToOpenDuration = 0L;

        if ( ZIP_REAPER_COLLECT_TIMINGS ) {
            timing(" Initial " + dualTiming(useInitialAt));
        }
    }

    public void setFinal(long useFinalAt) {
        finalAt = useFinalAt;

        if ( ZIP_REAPER_COLLECT_TIMINGS ) {
            timing(" Final " + dualTiming(useFinalAt));
        }
    }

    // State transitions:
    //
    // FULLY_CLOSED -> OPEN
    // PENDING -> OPEN
    // OPEN -> OPEN
    //
    // OPEN -> PENDING
    //
    // PENDING -> FULLY_CLOSED

    @Trivial
    public void enactOpen(long openAt) {
        String methodName = "enactOpen";
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + " On [ " + path + " ] at [ " + toRelSec(initialAt, openAt) + " (s) ]");
        }

        if ( zipFileState == ZipFileState.OPEN ) { // OPEN -> OPEN
            openDuration += openAt - lastOpenAt;

            lastLastOpenAt = lastOpenAt;
            lastOpenAt = openAt;

            openCount++;

        } else if ( zipFileState == ZipFileState.PENDING ) { // PENDING -> OPEN
            long lastPendDuration = openAt - lastPendAt;
            pendToOpenDuration += lastPendDuration;

            pendToOpenCount++;

            lastLastOpenAt = lastOpenAt;
            lastOpenAt = openAt;

            openCount++;

            zipFileState = ZipFileState.OPEN;

            if ( ZIP_REAPER_COLLECT_TIMINGS ) {
                timing(" Pend Success [ " + toAbsSec(lastPendDuration) + " (s) ]");
            }

        } else if ( zipFileState == ZipFileState.FULLY_CLOSED ) { // FULLY_CLOSED -> OPEN
            if ( firstOpenAt == -1L ) {
                firstOpenAt = openAt;

            } else {
                long lastFullCloseDuration = openAt - lastFullCloseAt;
                fullCloseToOpenDuration += lastFullCloseDuration;

                if ( ZIP_REAPER_COLLECT_TIMINGS ) {
                    long lastPendDuration = ( (lastPendAt == -1L) ? 0 : (lastFullCloseAt - lastPendAt) );
                    timing(" Reopen; Pend [ " + toAbsSec(lastPendDuration) + " (s) ] " +
                           " Close [ " + toAbsSec(lastFullCloseDuration) + " (s) ]");
                }
            }

            fullCloseToOpenCount++;

            lastLastOpenAt = lastOpenAt;
            lastOpenAt = openAt;

            openCount++;

            zipFileState = ZipFileState.OPEN;

        } else {
            throw unknownState();
        }
        

        if ( ZIP_REAPER_COLLECT_TIMINGS ) {
            timing(" Open " + dualTiming(openAt, initialAt) + " " + openState());
        }
    }

    public static final boolean CLOSE_ONCE = false;
    public static final boolean CLOSE_ALL = true;

    @Trivial
    public boolean enactClose(long closeAt, boolean closeAll) {
        String methodName = "enactClose";
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + " On [ " + path + " ] at [ " + toRelSec(initialAt, closeAt) + " (s) ]");
        }

        if ( zipFileState == ZipFileState.OPEN ) { // OPEN -> OPEN or OPEN -> PENDING
            if ( closeAll ) {
                closeCount = openCount;
            } else {
                closeCount++;
            }

            boolean isLastClose;

            if ( closeCount == openCount ) { // OPEN -> PENDING
                openDuration += closeAt - lastOpenAt;

                if ( firstPendAt == -1L ) {
                    firstPendAt = closeAt;
                }
                lastPendAt = closeAt;

                openToPendCount++;

                zipFileState = ZipFileState.PENDING;

                isLastClose = true;

            } else {
                isLastClose = false;
            }

            if ( ZIP_REAPER_COLLECT_TIMINGS ) {
                timing(" Close " + dualTiming(closeAt, initialAt) + " " + openState());
            }

            return isLastClose;

        } else if ( zipFileState == ZipFileState.PENDING ) {
            throw illegalTransition(ZipFileAction.CLOSE); 
        } else if ( zipFileState == ZipFileState.FULLY_CLOSED ) {
            throw illegalTransition(ZipFileAction.CLOSE);

        } else {
            throw unknownState();
        }
    }
    
    @Trivial
    public void enactFullClose(long fullCloseAt) {
        String methodName = "enactFullClose";
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + " On [ " + path + " ] at [ " + toRelSec(initialAt, fullCloseAt) + " (s) ]");
        }

        if ( zipFileState == ZipFileState.OPEN ) {
            throw illegalTransition(ZipFileAction.FULL_CLOSE);

        } else if ( zipFileState == ZipFileState.PENDING ) { // PENDING -> FULLY_CLOSED
            long lastPendDuration = fullCloseAt - lastPendAt;
            pendToFullCloseDuration += lastPendDuration;
            pendToFullCloseCount++;

            if ( firstFullCloseAt == -1L ) {
                firstFullCloseAt = fullCloseAt;
            }
            lastFullCloseAt = fullCloseAt;

            zipFileState = ZipFileState.FULLY_CLOSED;

            if ( ZIP_REAPER_COLLECT_TIMINGS ) {
                timing(" Failed Pend [ " + toAbsSec(lastPendDuration) + " (s) ]");
                timing(" Full Close [ " + dualTiming(fullCloseAt, initialAt) + " (s) ]");
            }

        } else if ( zipFileState == ZipFileState.FULLY_CLOSED ) {
            throw illegalTransition(ZipFileAction.FULL_CLOSE);

        } else {
            throw unknownState();
        }
    }

    @Trivial
    public void displayData() {
        if ( !ZIP_REAPER_COLLECT_TIMINGS ) {
            return;
        }

        // See the class comment for details of the state model and the
        // statistics which are gathered.

        System.out.println("ZFR ZipFile [ " + path + " ] Statistics:");

        String openText;
        if ( lastLastOpenAt == -1L ) {
            openText =
                "   Open: First [ " + toRelSec(initialAt, firstOpenAt) + " (s) ]" +
                " Last [ " + toRelSec(initialAt, lastOpenAt) + " (s) ]" +
                " Count [ " + toCount(openCount) + " ]" +
                " Duration [ " + toAbsSec(openDuration) + " (s) ]";
        } else {
            openText =
                "   Open: First [ " + toRelSec(initialAt, firstOpenAt) + " (s) ]" +
                " Last [ " + toRelSec(initialAt, lastOpenAt) + " (s) ]" +
                " Next Last [ " + toRelSec(initialAt, lastLastOpenAt) + " (s) ]" +
                " Count [ " + toCount(openCount) + " ]" +
                " Duration [ " + toAbsSec(openDuration) + " (s) ]";
        }
        System.out.println("ZFR " + openText);

        String pendingText =
                "   Pending: First [ " + toRelSec(initialAt, firstPendAt) + " (s) ]" +
                " Last [ " + toRelSec(initialAt, lastPendAt) + " (s) ]" +
                " Count [ " + toCount(openToPendCount) + " ]";
        System.out.println("ZFR " + pendingText);

        String pendingBeforeOpenText =
                "     Pending to Open: Count [ " + toCount(pendToOpenCount) + " ]" +
                " Duration [ " + toAbsSec(pendToOpenDuration) + " (s) ]";
        System.out.println("ZFR " + pendingBeforeOpenText);

        String pendingBeforeCloseText =
                "     Pending to Full Close: Count [ " + toCount(pendToFullCloseCount) + " ]" +
                " Duration [ " + toAbsSec(pendToFullCloseDuration) + " (s) ]";
        System.out.println("ZFR " + pendingBeforeCloseText);

        String closeText =
                "   Full Close: First [ " + toRelSec(initialAt, firstFullCloseAt) + " (s) ]" +
                " Last [ " + toRelSec(initialAt, lastFullCloseAt) + " (s) ]";
        System.out.println("ZFR " + closeText);

        String closeBeforeOpenText =
                "     Full Close to Open: Count [ " + toCount(fullCloseToOpenCount) + " ]" +
                " Duration [ " + toAbsSec(fullCloseToOpenDuration) + " (s) ]";
        System.out.println(closeBeforeOpenText);
    }

    @Trivial
    protected IllegalStateException unknownState() {
        return new IllegalStateException("Unknown zip file state [ " + path + " ] [ " + zipFileState + " ]");
    }

    @Trivial
    protected IllegalStateException illegalTransition(ZipFileAction zipFileAction) {
        return new IllegalStateException("Action [ " + zipFileAction + " ] is not valid from zip file state [ " + path + " ] [ " + zipFileState + " ]");
    }

    // Main operations ...

    // enactOpen(openAt);
    // enactClose(closeAt);
    //
    // closeZipFile();
    // enactFullClose((fullCloseAt);

    // openZipFile(UNKNOWN_ZIP_LENGTH, UNUSED_ZIP_LAST_MODIFIED); // throws IOException, ZipException
    // enactOpen(openAt);

    // Identity ...
    //
    // The path is used as the unique key for zip data.
    //
    // Callers must ensure that meaningful paths are provided, with
    // attention given to use paths which are the same for the same
    // actual files on disk.

    protected final String path;

    // State ...

    // activeOpens == 0; zipFile == null: simply closed
    // activeOpens == 0; zipFile != null: pending close
    // activeOpens  > 0; zipFile != null: open
    // activeOpens  > 0; zipFile == null: invalid

    private ZipFile zipFile;
    private long zipLength;
    private long zipLastModified;

    /**
     * Re-acquire the ZIP file.
     * 
     * If either the zip file length or last modified times changed
     * since the zip file was set, re-open the zip file and update
     * the length and last modified times.
     * 
     * A warning is displayed if changes are detected and there are
     * active opens.  If there are no active opens, the data will be
     * in a pending close state.  Changes between a pending close
     * and a re-open are expected.
     * 
     * @return The zip file of the data.
     * 
     * @throws IOException Thrown if the re-open of the zip file failed.
     * @throws ZipException Thrown if the re-open of the zip file failed.
     */
    @Trivial
    protected ZipFile reacquireZipFile() throws IOException, ZipException {
        String methodName = "reacquireZipFile";

        File rawZipFile = new File(path);
        long newZipLength = FileUtils.fileLength(rawZipFile);
        long newZipLastModified = FileUtils.fileLastModified(rawZipFile);

        boolean zipFileChanged = false;

        if ( newZipLength != zipLength ) {
            zipFileChanged = true;
            
            if ( openCount > closeCount ) {
                Tr.warning(tc, methodName +
                    " Zip [ " + path + " ]:" +
                    " Update length from [ " + Long.valueOf(zipLength) + " ]" +
                    " to [ " + Long.valueOf(newZipLength) + " ]");
            } else {
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName +
                        " Zip [ " + path + " ]:" +
                        " Update length from [ " + Long.valueOf(zipLength) + " ]" +
                        " to [ " + Long.valueOf(newZipLength) + " ]");
                }
            }
        }

        if ( newZipLastModified != zipLastModified ) {
            zipFileChanged = true;

            if ( openCount > closeCount ) {
                Tr.warning(tc, methodName +
                    " Zip [ " + path + " ]:" +
                    " Update last modified from [ " + Long.valueOf(zipLastModified) + " ]" +
                    " to [ " + Long.valueOf(newZipLastModified) + " ]");
            } else {
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName +
                        " Zip [ " + path + " ]:" +
                        " Update last modified from [ " + Long.valueOf(zipLastModified) + " ]" +
                        " to [ " + Long.valueOf(newZipLastModified) + " ]");
                }
            }
        }

        if ( zipFileChanged ) {
            if ( openCount > closeCount ) {
                Tr.warning(tc, methodName + " Reopen [ " + path + " ]");
            } else {
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, methodName + " Reopen [ " + path + " ]");
                }
            }

            @SuppressWarnings("unused")
            ZipFile oldZipFile = closeZipFile();
            @SuppressWarnings("unused")
            ZipFile newZipFile = openZipFile(newZipLength, newZipLastModified); // throws IOException, ZipException
        }

        return zipFile;
    }

    private static final long UNKNOWN_ZIP_LENGTH = -1L;
    private static final long UNUSED_ZIP_LAST_MODIFIED = -1L;

    @Trivial
    protected ZipFile openZipFile() throws IOException, ZipException {
        return openZipFile(ZipFileData.UNKNOWN_ZIP_LENGTH, ZipFileData.UNUSED_ZIP_LAST_MODIFIED);
        // throws IOException, ZipException
    }

    @Trivial
    protected ZipFile openZipFile(long useZipLength, long useZipLastModified) throws IOException, ZipException {
        String methodName = "openZipFile";

        zipFile = ZipFileUtils.openZipFile(path); // throws IOException, ZipException

        if ( useZipLength == UNKNOWN_ZIP_LENGTH ) {
            File rawZipFile = new File(path);
            useZipLength = FileUtils.fileLength(rawZipFile);
            useZipLastModified = FileUtils.fileLastModified(rawZipFile);
        }

        zipLength = useZipLength;
        zipLastModified = useZipLastModified;

        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc,
                methodName +
                " Path [ " + path + " ]" +
                " Length [ " + Long.valueOf(zipLength) + " ]" +
                " Last modified [ " + Long.valueOf(zipLastModified) + " ]");
        }
        return zipFile;
    }

    @Trivial
    protected ZipFile closeZipFile() {
        String methodName = "closeZipFile";
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + " Path [ " + path + " ]");
        }

        if ( zipFile == null ) {
            throw new IllegalStateException("Null zip file [ " + path + " ]");
        }

        ZipFile useZipFile = zipFile;
        zipFile = null;
        zipLength = -1L;
        zipLastModified = -1L;

        try {
            ZipFileUtils.closeZipFile(path, useZipFile); // throws IOException
        } catch ( IOException e ) {
            Tr.debug(tc, methodName + " Close failure [ " + path + " ] [ " + e.getMessage() + " ]");
            // FFDC
        }
        
        return useZipFile;
    }

    // State tracking ...

    protected ZipFileState zipFileState;

    @Trivial
    public boolean isOpen() {
        return zipFileState == ZipFileState.OPEN;
    }

    @Trivial
    public boolean isPending() {
        return zipFileState == ZipFileState.PENDING;
    }

    @Trivial
    public boolean isFullyClosed() {
        return zipFileState == ZipFileState.FULLY_CLOSED;
    }        

    protected long initialAt;
    protected long finalAt;

    protected int openCount;
    protected int closeCount;
    
    @Trivial
    public int getActiveOpens() {
        return openCount - closeCount;
    }
    
    protected int openToPendCount;
    protected int pendToOpenCount;
    protected int pendToFullCloseCount;
    protected int fullCloseToOpenCount;

    protected long firstOpenAt;
    protected long lastLastOpenAt;
    protected long lastOpenAt;

    protected long firstPendAt;
    protected long lastPendAt;
    
    protected long firstFullCloseAt;
    protected long lastFullCloseAt;

    protected long openDuration;
    protected long pendToOpenDuration;
    protected long pendToFullCloseDuration;
    protected long fullCloseToOpenDuration;

    protected boolean expireQuickly;

    protected long expireAt(long firstDelay, long hitDelay) {
        return ( lastPendAt + (expireQuickly ? firstDelay : hitDelay) );
    }

    protected boolean setExpireQuickly(long slowPendMin) {
        boolean useExpireQuickly;
        if ( lastLastOpenAt == -1L ) { // only opened once
            useExpireQuickly = true;
        } else if ( (lastOpenAt - lastLastOpenAt) < slowPendMin ) { // time between opens is not too long
            useExpireQuickly = true;
        } else { // time between opens was too long
            useExpireQuickly = false;
        }

        expireQuickly = useExpireQuickly;

        return useExpireQuickly;
    }

    //

    @Trivial
    public void debugState() {
        String methodName = "debugState";

        if ( !ZipCachingProperties.ZIP_REAPER_DEBUG_STATE || !tc.isInfoEnabled() ) {
            return;
        }

        // See the class comment for details of the state model and the
        // statistics which are gathered.

        Tr.info(tc, methodName + " ZipFile [ " + path + " ]:");

        String spanText =
                " Span: Initial [ " + toAbsSec(initialAt) + " (s) ]" +
                " Final [ " + toAbsSec(finalAt) + " (s) ]" +
                " Duration [ " + toAbsSec(finalAt - initialAt) + " (s) ]";
        Tr.info(tc, methodName + spanText);

        String marginsText =
                "   Margin: To First Open [ " + toAbsSec(firstOpenAt - initialAt) + " (s) ]" +
                " From Last Close [ " + toAbsSec(finalAt - lastFullCloseAt) + " (s) ]";
        Tr.info(tc, methodName + marginsText);

        String openText;
        if ( lastLastOpenAt == -1L ) {
            openText =
                "   Open: First [ " + toRelSec(initialAt, firstOpenAt) + " (s) ]" +
                " Last [ " + toRelSec(initialAt, lastOpenAt) + " (s) ]" +
                " Count [ " + toCount(openCount) + " ]" +
                " Duration [ " + toAbsSec(openDuration) + " (s) ]";
        } else {
            openText =
                "   Open: First [ " + toRelSec(initialAt, firstOpenAt) + " (s) ]" +
                " Last [ " + toRelSec(initialAt, lastOpenAt) + " (s) ]" +
                " Next Last [ " + toRelSec(initialAt, lastLastOpenAt) + " (s) ]" +
                " Count [ " + toCount(openCount) + " ]" +
                " Duration [ " + toAbsSec(openDuration) + " (s) ]";
        }
        Tr.info(tc, methodName + openText);

        String pendingText =
                "   Pending: First [ " + toRelSec(initialAt, firstPendAt) + " (s) ]" +
                " Last [ " + toRelSec(initialAt, lastPendAt) + " (s) ]" +
                " Count [ " + toCount(openToPendCount) + " ]";
        Tr.info(tc, methodName + pendingText);

        String pendingBeforeOpenText =
                "     Pending to Open: Count [ " + toCount(pendToOpenCount) + " ]" +
                " Duration [ " + toAbsSec(pendToOpenDuration) + " (s) ]";
        Tr.info(tc, methodName + pendingBeforeOpenText);

        String pendingBeforeCloseText =
                "     Pending to Full Close: Count [ " + toCount(pendToFullCloseCount) + " ]" +
                " Duration [ " + toAbsSec(pendToFullCloseDuration) + " (s) ]";
        Tr.info(tc, methodName + pendingBeforeCloseText);

        String closeText =
                "   Full Close: First [ " + toRelSec(initialAt, firstFullCloseAt) + " (s) ]" +
                " Last [ " + toRelSec(initialAt, lastFullCloseAt) + " (s) ]";
        Tr.info(tc, methodName + closeText);

        String closeBeforeOpenText =
                "     Full Close to Open: Count [ " + toCount(fullCloseToOpenCount) + " ]" +
                " Duration [ " + toAbsSec(fullCloseToOpenDuration) + " (s) ]";
        Tr.info(tc, methodName + closeBeforeOpenText);
    }
}
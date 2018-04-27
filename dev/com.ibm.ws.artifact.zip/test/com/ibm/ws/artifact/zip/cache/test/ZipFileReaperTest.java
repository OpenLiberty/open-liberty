/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.artifact.zip.cache.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.Test;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.zip.cache.ZipCachingProperties;
import com.ibm.ws.artifact.zip.cache.internal.ZipFileData;
import com.ibm.ws.artifact.zip.cache.internal.ZipFileReaper;

public class ZipFileReaperTest {

    //

    @Test
    public void testDefaultReaper() throws Exception {
        DEFAULT_PROFILE.runTest();
    }
    
    @Test
    public void testNoQuickReaper() throws Exception {
        NO_QUICK_PROFILE.runTest();
    }

    //

    public static class TestProfile {
        public TestProfile(
            String profileName,
            boolean debugState,
            int maxCache,
            long quickPendMin, long quickPendMax,
            long slowPendMin, long slowPendMax,
            long shortOpen, long mediumOpen, long longOpen) {

            this.profileName = profileName;
            
            this.debugState = debugState;
            
            this.maxCache = maxCache;

            this.quickPendMin = quickPendMin;
            this.quickPendMax = quickPendMax;

            this.slowPendMin = slowPendMin;
            this.slowPendMax = slowPendMax;

            this.shortOpen = shortOpen;
            this.mediumOpen = mediumOpen;
            this.longOpen = longOpen;

            this.zipPaths = createZipPaths();
            this.workerData = createWorkerData();

            // A consequence of the particular worker data
            // which is created.
            this.overallDuration = mediumOpen * 120;
        }

        // Profile name ...

        public final String profileName;

        // Debug enablement ...
        
        public final boolean debugState;

        // Reaper parms ...

        public final int maxCache;

        public final long quickPendMin;
        public final long quickPendMax;

        public final long slowPendMin;
        public final long slowPendMax;

        // Worker parms ...

        public final long shortOpen;
        public final long mediumOpen;
        public final long longOpen;

        public final long overallDuration;

        // Path data ...

        public final String[] zipPaths;

        // Worker data ...

        public final ZipTestOp[][] workerData;

        //

        private static final String ZIP_PATH_ROOT = "./build/testdata";
        private static final String ZIP_PATH_PREFIX = "test";
        private static final String ZIP_PATH_EXT = ".zip";

        private static final String DUMMY_ENTRY_NAME = "dummy";
        private static final String DUMMY_ENTRY_EXT = ".txt";

        // The zip path count must match the worker data.
        private static final int ZIP_PATH_COUNT = 10;

        private String[] createZipPaths() {
             String[] zipPaths = new String[ZIP_PATH_COUNT];

             for ( int pathNo = 0; pathNo < ZIP_PATH_COUNT; pathNo++ ) {
                 zipPaths[pathNo] = ZIP_PATH_ROOT + "/" + ZIP_PATH_PREFIX + pad0(pathNo, 2) + ZIP_PATH_EXT;
             }

             return zipPaths;
        }

        public void ensureZipFiles() throws IOException{
            for ( int pathNo = 0; pathNo < ZIP_PATH_COUNT; pathNo++ ) {
                ensureZipFile(pathNo, zipPaths[pathNo]); // throws IOException
            }
        }

        protected void ensureZipFile(int pathNo, String zipPath) throws IOException {
            File zipFile = new File(zipPath);

            File zipFileParent = zipFile.getParentFile();

            if ( !zipFileParent.exists() ) {
            	zipFileParent.mkdirs();
            }

            if ( !zipFile.exists() ) {
                createZipFile(pathNo, zipPath); // throws IOException
            }
        }

        protected void createZipFile(int pathNo, String zipPath) throws IOException {
            FileOutputStream rawZipOutput = new FileOutputStream(zipPath); // throws FileNotFoundException
            ZipOutputStream zipOutput = new ZipOutputStream(rawZipOutput);

            String dummyEntryName = DUMMY_ENTRY_NAME + pad0(pathNo, 2) + DUMMY_ENTRY_EXT;
            byte[] dummyEntryBytes = dummyEntryName.getBytes();

            ZipEntry zipEntry = new ZipEntry(dummyEntryName);
            zipOutput.putNextEntry(zipEntry); // throws IOException
            zipOutput.write(dummyEntryBytes); // throws IOException
            zipOutput.closeEntry(); // throws IOException

            zipOutput.finish(); // throws IOException

            zipOutput.close(); // throws IOException
        }

        //

        protected ZipTestOp[][] createWorkerData() {
            // Medium spaced opens.
            // 20 times: Every 5 * <medium> seconds, open for <medium> seconds.
            // Duration: (20 * (5 * medium)) + medium == 101 * medium
            int pairCount0 = 20;
            ZipTestOp[] testOps0 = new ZipTestOp[pairCount0 * 2];
            for ( int pairNo0 = 0; pairNo0 < pairCount0; pairNo0++ ) {
                long openAt0 = pairNo0 * (5 * mediumOpen);
                long closeAt0 = openAt0 + mediumOpen;
                testOps0[pairNo0 * 2] = new ZipTestOp(zipPaths[0], openAt0, DO_OPEN);
                testOps0[(pairNo0 * 2) + 1] = new ZipTestOp(zipPaths[0], closeAt0, DO_CLOSE);
            }

            // Medium-to-widely spaced opens.
            // Ten times: Every 2 * <long> seconds, open for <medium> seconds.
            // Duration: (10 * (2 * long)) + medium == 20 * long + medium
            int pairCount1 = 10;
            ZipTestOp[] testOps1 = new ZipTestOp[pairCount1 * 2];
            for ( int pairNo1 = 0; pairNo1 < pairCount1; pairNo1++ ) {
                long openAt1 = pairNo1 * (2 * longOpen);
                long closeAt1 = openAt1 + mediumOpen;            
                testOps1[pairNo1 * 2] = new ZipTestOp(zipPaths[1], openAt1, DO_OPEN);
                testOps1[(pairNo1 * 2) + 1] = new ZipTestOp(zipPaths[1], closeAt1, DO_CLOSE);
            }

            // Quick opens.
            // 50 times: Every 2 * <medium> seconds, open for <medium> seconds.
            // Duration: (50 * (2 * medium)) + medium == 101 medium
            int pairCount2 = 50;
            ZipTestOp[] testOps2 = new ZipTestOp[pairCount2 * 2];
            for ( int pairNo2 = 0; pairNo2 < pairCount2; pairNo2++ ) {
                long openAt1 = pairNo2 * (2 * mediumOpen);
                long closeAt1 = openAt1 + mediumOpen;            
                testOps2[pairNo2 * 2] = new ZipTestOp(zipPaths[2], openAt1, DO_OPEN);
                testOps2[(pairNo2 * 2) + 1] = new ZipTestOp(zipPaths[2], closeAt1, DO_CLOSE);
            }

            // Very quick opens:
            // 100 times: Every <medium> seconds, open for <short> seconds.
            // Duration: (100 * medium) + short
            int pairCount3 = 100;
            ZipTestOp[] testOps3 = new ZipTestOp[pairCount3 * 2];
            for ( int pairNo3 = 0; pairNo3 < pairCount3; pairNo3++ ) {
                long openAt3 = pairNo3 * mediumOpen;
                long closeAt3 = openAt3 + shortOpen;            
                testOps3[pairNo3 * 2] = new ZipTestOp(zipPaths[3], openAt3, DO_OPEN);
                testOps3[(pairNo3 * 2) + 1] = new ZipTestOp(zipPaths[3], closeAt3, DO_CLOSE);
            }

            // Test of very widely spaced opens.
            // Three times: Every 5 * <long> seconds, open for <long> seconds.
            // Duration: (3 * (5 * long)) + long == 16 * long
            int pairCount4 = 3;
            ZipTestOp[] testOps4 = new ZipTestOp[pairCount4 * 2];
            for ( int pairNo4 = 0; pairNo4 < pairCount4; pairNo4++ ) {
                long openAt4 = pairNo4 * 5 * longOpen;
                long closeAt4 = openAt4 + longOpen;
                testOps4[pairNo4 * 2] = new ZipTestOp(zipPaths[4], openAt4, DO_OPEN);
                testOps4[(pairNo4 * 2) + 1] = new ZipTestOp(zipPaths[4], closeAt4, DO_CLOSE);
            }

            // Test of indefinite opens.
            // Five times: Every <long> seconds, open indefinitely.
            // Use a different zip for each open.
            // Duration: 5 * long
            int pairCount5 = 5;
            ZipTestOp[] testOps5 = new ZipTestOp[pairCount5];
            for ( int pairNo5 = 0; pairNo5 < pairCount5; pairNo5++ ) {
                long openAt5 = pairNo5 * longOpen;
                testOps5[pairNo5] = new ZipTestOp(zipPaths[5 + pairNo5], openAt5, DO_OPEN);
            }

            //

            return new ZipTestOp[][] {
                testOps0,  testOps1,  testOps2,  testOps3, testOps4,
                testOps5 };  
        }

        public void displayParameters() {
            String methodName = "displayParameters";

            debug(methodName, "Max Cache [ " + toCount(maxCache) + " ]");
            
            debug(methodName, "Quick Pend Min [ " + toAbsSec(quickPendMin) + " ]");
            debug(methodName, "Quick Pend Max [ " + toAbsSec(quickPendMin) + " ]");
            
            debug(methodName, "Slow Pend Min [ " + toAbsSec(slowPendMin) + " ]");
            debug(methodName, "Slow Pend Max [ " + toAbsSec(slowPendMin) + " ]");

            debug(methodName, "Test Duration [ " + toAbsSec(overallDuration) + " ]");

            debug(methodName, "Short Open Duration [ " + toAbsSec(shortOpen) + " ]");
            debug(methodName, "Medium Open Duration [ " + toAbsSec(mediumOpen) + " ]");
            debug(methodName, "Long Open Duration [ " + toAbsSec(longOpen) + " ]");
        }

        public void runTest() throws IOException {
            String methodName = "run";
            debug(methodName, "Begin [ " + profileName + " ]");
            
            displayParameters();

            ensureZipFiles(); // throws IOException

            ZipFileReaper reaper = startReaper();

            Thread[] testThreads = launchThreads(reaper);

            waitForThreads(testThreads);

            finishReaper(reaper);

            debug(methodName, "End [ " + profileName + " ]");
        }

        public ZipFileReaper startReaper() {
            return new ZipFileReaper(
                profileName,
                debugState,
                maxCache,
                quickPendMin, quickPendMax,
                slowPendMin, slowPendMax); 
        }

        public void finishReaper(ZipFileReaper reaper) {
            reaper.shutDown();
        }

        public Thread[] launchThreads(ZipFileReaper reaper) {
            String methodName = "launchThreads";

            int numThreads = workerData.length;
            debug(methodName, "Threads [ " + Integer.toString(numThreads) + " ] ...");

            Thread[] testThreads = new Thread[ numThreads ];
            for ( int threadNo = 0; threadNo < numThreads; threadNo++ ) {
                debug(methodName,
                    "  Thread [ " + Integer.toString(threadNo) + " ]" +
                    " Operations [ " + Integer.toString(workerData[threadNo].length) + " ]");
                Runnable zipTestWorker = new ZipTestWorker(reaper, workerData[threadNo]);
                testThreads[threadNo] = new Thread(zipTestWorker, "zip test thread " + Integer.toString(threadNo));
            }

            for ( int threadNo = 0; threadNo < numThreads; threadNo++ ) {
                testThreads[threadNo].start();
            }

            debug(methodName, "Threads [ " + Integer.toString( numThreads ) + " ] ... launched");
            
            return testThreads;
        }

        public void waitForThreads(Thread[] testThreads) {
            for ( Thread testThread : testThreads ) {
                try {
                    testThread.join(); // throws InterruptedException
                    } catch ( InterruptedException e ) {
                        // IGNORE
                    }
            }
        }
    }

    public static final TestProfile DEFAULT_PROFILE =
        new TestProfile(
            "default reaper",
            ZipFileReaper.DO_NOT_DEBUG_STATE,
            
            ZipCachingProperties.ZIP_CACHE_REAPER_MAX_PENDING_DEFAULT_VALUE,

            ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN,
            ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MAX,

            ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MIN,
            ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MAX,

            ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN / 4,
            (ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN / 4) * 10,
            (ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN / 4) * 50 );

    public static final TestProfile NO_QUICK_PROFILE =
        new TestProfile(
            "no quick reaper",
            ZipFileReaper.DO_NOT_DEBUG_STATE,

            ZipCachingProperties.ZIP_CACHE_REAPER_MAX_PENDING_DEFAULT_VALUE,

            0,
            0,

            ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MIN,
            ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MAX,

            ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN / 4,
            (ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN / 4) * 10,
            (ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN / 4) * 50 );

    //

    private static final boolean DO_OPEN = true;
    private static final boolean DO_CLOSE = false;

    //
    
    private static class ZipTestOp {
        public final String path;
        public final long actAt;
        public final boolean isOpen;
        
        public ZipTestOp(String path, long actAt, boolean isOpen) {
            this.path = path;
            this.actAt = actAt;
            this.isOpen = isOpen;
        }

        public void perform(ZipFileReaper reaper) {
            String methodName = "operate";
            
            long actualActAt = getNanoTime();

            // The state following an open should be 'OPEN', since the tester
            // balances opens and closes, and the balancing close has not yet
            // been performed.  There may be *additional* opens performed between
            // the open and the state retrieval, but the state of the zip file
            // should still be 'OPEN'.

            // To obtain the zip file state immediately following the close, the
            // state must be obtained from the 'close' invocation.  Obtaining the
            // state by an independent call to 'getState' allows addition operations
            // to be performed, changing the state.

            if ( isOpen ) {
                try {
                    debug(methodName, "Open [ " + path + " ] [ " + reaper.getState(path) + " ]" +
                        " at [ " + toRelSec(reaper.getInitialAt(), actualActAt) + " ]");
                    @SuppressWarnings("unused")
                    ZipFile zipFile = reaper.open(path, actualActAt);
                    debug(methodName, "Opened [ " + path + " ] [ " + reaper.getState(path) + " ]");

                    reaper.validate();

                } catch ( Exception e ) {
                    debug(methodName, "Failed to open [ " + path + " ] at [ " + toAbsSec(actAt) + " ]");
                    e.printStackTrace(System.out);
                }

            } else {
                debug(methodName, "Close [ " + path + " ] [ " + reaper.getState(path) + " ]" +
                    " at [ " + toRelSec(reaper.getInitialAt(), actualActAt) + " ]");
                ZipFileData.ZipFileState immediateState = reaper.close(path, actualActAt);
                debug(methodName, "Closed [ " + path + " ] [ " + immediateState + " ]");

                reaper.validate();
            }
        }

        private static final String INNER_CLASS_NAME = ZipTestOp.class.getSimpleName();

        @Trivial    
        private static final void innerDebug(String methodName, String text) {
            debug(INNER_CLASS_NAME, methodName, text);
        }
    }

    private static class ZipTestWorker implements Runnable {
        public ZipTestWorker(ZipFileReaper reaper, ZipTestOp[] operations) {
            this.reaper = reaper;
            this.operations = operations;
        }

        private final ZipFileReaper reaper;
        private final ZipTestOp[] operations;

        //

        public void run() {
            long lastActAt = 0L;

            for ( ZipTestOp nextOp : operations ) {
                long nextActAt = nextOp.actAt;
                try {
                    Thread.sleep((nextActAt - lastActAt) / ZipCachingProperties.ONE_MILLI_SEC_IN_NANO_SEC);
                } catch ( InterruptedException e ) {
                    // Ignore
                }
                nextOp.perform(reaper);

                lastActAt = nextActAt;
            }
        }

        //

        private static final String INNER_CLASS_NAME = ZipTestWorker.class.getSimpleName();

        @Trivial    
        private static final void innerDebug(String methodName, String text) {
            debug(INNER_CLASS_NAME, methodName, text);
        }
    }

    //

    @Trivial    
    private static long getNanoTime() {
        return System.nanoTime();
    }

    //

    public static final String MAX_PAD = "0000000000000000";
    public static final int MAX_PAD_WIDTH = 16; // Do *NOT* use 'MAX_PAD.length()': That inlines as '0'! 

    public static String pad0(int num, int targetWidth) {
        if ( targetWidth > MAX_PAD_WIDTH ) {
            targetWidth = MAX_PAD_WIDTH;
        }

        String actualText = Integer.toString(num);
        int actualWidth = actualText.length();
        if ( actualWidth >= targetWidth ) {
            return actualText;
        }

        int padWidth = targetWidth - actualWidth;
        String padText = MAX_PAD.substring(0, padWidth) + actualText;

        return padText;
    }

    //

    private static final String CLASS_NAME = ZipFileReaperTest.class.getSimpleName();

    @Trivial    
    private static final void debug(String methodName, String text) {
        debug(CLASS_NAME, methodName, text);
    }

    @Trivial
    private static final void debug(String className, String methodName, String text) {
        System.out.println(className +": " + methodName + ": " + text);
    }

    @Trivial    
    public static String toAbsSec(long duration) {
        return ZipCachingProperties.toAbsSec(duration);
    }

    @Trivial    
    public static String toRelSec(long base, long actual) {
        return ZipCachingProperties.toRelSec(base, actual);
    }

    @Trivial
    public static String toCount(int count) {
        return ZipCachingProperties.toCount(count);
    }
}

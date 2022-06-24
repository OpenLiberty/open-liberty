/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.zip.cache.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.zip.cache.ZipCachingProperties;
import com.ibm.ws.artifact.zip.cache.internal.ZipFileData;
import com.ibm.ws.artifact.zip.cache.internal.ZipFileReaper;

public class ZipFileReaperTest {

    public static void sleep(long nanoSec) {
        long milliSec = nanoSec / ZipCachingProperties.NANO_IN_MILLI;
        int remainingNanoSec = (int) (nanoSec % ZipCachingProperties.NANO_IN_MILLI);

        try {
            Thread.sleep(milliSec, remainingNanoSec);
        } catch ( InterruptedException e ) {
            // IGNORE
        }
    }

    // String[] zipPaths, ZipTestOps[] allTestOps, long testDuration) {

    // Reaper behavior is parameterized on four values:
    //
    // ZIP_CACHE_REAPER_QUICK_PEND_MIN: 0.01 s
    // ZIP_CACHE_REAPER_QUICK_PEND_MAX: 0.02 s
    //
    // ZIP_CACHE_REAPER_SLOW_PEND_MIN: 0.1 s
    // ZIP_CACHE_REAPER_SLOW_PEND_MAX: 0.2 s    
    //
    // These durations determine the open times used by testing.
    //
    // TRIVIAL: 0.0 s                   : 0.0 s
    // SHORT:   (0.01 / 4) s            : 0.0025 s
    // MEDIUM:  ((0.01 / 4) * 10) s     : 0.025 s
    // LONG:    (0.1 * 3) s             : 0.3 s

    public static final long TRIVIAL_OPEN = 0L;
    public static final long TINY_OPEN = ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN / 100;
    public static final long SHORT_OPEN = ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN / 4;
    public static final long MEDIUM_OPEN = (ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN / 4) * 10;
    public static final long LONG_OPEN = (ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MIN) * 3;

    public ZipFileReaperTest() throws IOException {
        this.zipPaths = createZipPaths();
        this.ensureZipFiles(); // throws IOException
        this.ensureArqFiles(); // throws IOException

        this.trivialOpen = TRIVIAL_OPEN;
        this.tinyOpen = TINY_OPEN;
        this.shortOpen = SHORT_OPEN;
        this.mediumOpen = MEDIUM_OPEN;
        this.longOpen = LONG_OPEN;

        this.maxTestDuration = mediumOpen * 120; // Based on the worker data and open times
        this.tinyTestDuration = tinyOpen * 100 + (ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MAX * 2);

        this.simpleWorkerData = createSimpleWorkerData();

        this.burstWorkerData = new ZipTestOps[] { createBurstWorkerData() };
        this.overflowWorkerData = new ZipTestOps[] { createOverflowWorkerData() };

        this.scatterWorkerData_One_Short = new ZipTestOps[] {
            createScatterWorkerData(10, 1)
        };
        this.scatterWorkerData_One_Long = new ZipTestOps[] {
            createScatterWorkerData(11, 100)
        };

        this.scatterWorkerData_Many_Short = new ZipTestOps[] {
            createScatterWorkerData(20, 1),
            createScatterWorkerData(30, 1),
            createScatterWorkerData(40, 1)
        };
        this.scatterWorkerData_Many_Long = new ZipTestOps[] {
            createScatterWorkerData(21, 100),
            createScatterWorkerData(31, 100),
            createScatterWorkerData(41, 100)
        };        

        this.dribbleWorkerData_One = new ZipTestOps[] {
            createDribbleWorkerData(12)
        };
        this.dribbleWorkerData_Many = new ZipTestOps[] {
            createDribbleWorkerData(21),
            createDribbleWorkerData(31),
            createDribbleWorkerData(41)
        };

        this.tinyWorkerData_One = new ZipTestOps[] {
            createTinyWorkerData(12, 8)
        };
        this.tinyWorkerData_Many = new ZipTestOps[] {
            createTinyWorkerData(21, 8),
            createTinyWorkerData(31, 8),
            createTinyWorkerData(41, 8)
        };

        this.arqData = new ZipTestOps[] {
            createArqData()
        };

        // '15' is 5 less than the number of test paths
        this.defaultProfile = createDefaultProfile(15);
        this.noQuickProfile = createNoQuickProfile(15);
    }

    //

    public void runProfile(
        int iterations,
        long testDuration,
        TestProfile profile, ZipTestOps[] allTestOps) throws Exception {

        System.out.println("Test iterations [ " + iterations + " ]");

        for ( int iteration = 0; iteration < iterations; iteration++ ) {
            try {
                profile.runTest(
                    iteration,
                    testDuration, trivialOpen, tinyOpen, shortOpen, mediumOpen, longOpen,
                    zipPaths,
                    allTestOps); // throws Exception

            } catch ( Exception e ) {
                System.out.println("Failure on iteration [ " + iteration + " ] of [ " + iterations + " ]");
                throw e;
            }
        }
    }

    public void runProfile(int iterations, TestProfile profile, ZipTestOps[] allTestOps)
        throws Exception {

        runProfile(iterations, maxTestDuration, profile, allTestOps); // throws Exception
    }

    public static final int TEST_ITERATIONS = 2;

    @Test
    public void testDefaultReaper_Simple() throws Exception {
        runProfile(TEST_ITERATIONS, defaultProfile, simpleWorkerData); // throws Exception
    }

    @Test
    public void testNoQuick_Simple() throws Exception {
        runProfile(TEST_ITERATIONS, noQuickProfile, simpleWorkerData); // throws Exception
    }

    @Test
    public void testDefaultReaper_Burst() throws Exception {
        runProfile(TEST_ITERATIONS, defaultProfile, burstWorkerData); // throws Exception
    }

    @Test
    public void testNoQuick_Burst() throws Exception {
        runProfile(TEST_ITERATIONS, noQuickProfile, burstWorkerData); // throws Exception
    }

    @Test
    public void testDefaultReaper_Overflow() throws Exception {
        runProfile(TEST_ITERATIONS, defaultProfile, overflowWorkerData); // throws Exception
    }

    @Test
    public void testNoQuick_Overflow() throws Exception {
        runProfile(TEST_ITERATIONS, noQuickProfile, overflowWorkerData); // throws Exception
    }

    //

    @Test
    public void testDefaultReaper_Scatter_One_Short() throws Exception {
        runProfile(TEST_ITERATIONS, defaultProfile, scatterWorkerData_One_Short); // throws Exception
    }

    @Test
    public void testNoQuick_Scatter_One_Short() throws Exception {
        runProfile(TEST_ITERATIONS, noQuickProfile, scatterWorkerData_One_Short); // throws Exception
    }

    //@Test
    public void testDefaultReaper_Scatter_One_Long() throws Exception {
        runProfile(TEST_ITERATIONS, defaultProfile, scatterWorkerData_One_Long); // throws Exception
    }

    //@Test
    public void testNoQuick_Scatter_One_Long() throws Exception {
        runProfile(TEST_ITERATIONS, noQuickProfile, scatterWorkerData_One_Long); // throws Exception
    }

    @Test
    public void testDefaultReaper_Scatter_Many_Short() throws Exception {
        runProfile(TEST_ITERATIONS, defaultProfile, scatterWorkerData_Many_Short); // throws Exception
    }

    @Test
    public void testNoQuick_Scatter_Many_Short() throws Exception {
        runProfile(TEST_ITERATIONS, noQuickProfile, scatterWorkerData_Many_Short); // throws Exception
    }

    //@Test
    public void testDefaultReaper_Scatter_Many_Long() throws Exception {
        runProfile(TEST_ITERATIONS, defaultProfile, scatterWorkerData_Many_Long); // throws Exception
    }

    //@Test
    public void testNoQuick_Scatter_Many_Long() throws Exception {
        runProfile(TEST_ITERATIONS, noQuickProfile, scatterWorkerData_Many_Long); // throws Exception
    }

    //

    @Test
    public void testDefaultReaper_Dribble_One() throws Exception {
        runProfile(TEST_ITERATIONS, defaultProfile, dribbleWorkerData_One); // throws Exception
    }

    @Test
    public void testNoQuick_Dribble_One() throws Exception {
        runProfile(TEST_ITERATIONS, noQuickProfile, dribbleWorkerData_One); // throws Exception
    }    

    @Test
    public void testDefaultReaper_Dribble_Many() throws Exception {
        runProfile(TEST_ITERATIONS, defaultProfile, dribbleWorkerData_Many); // throws Exception
    }

    @Test
    public void testNoQuick_Dribble_Many() throws Exception {
        runProfile(TEST_ITERATIONS, noQuickProfile, dribbleWorkerData_Many); // throws Exception
    }

    @Test
    public void testDefault_Arq() throws Exception {
        runProfile(TEST_ITERATIONS, defaultProfile, arqData); // throws Exception
    }

    @Test
    public void testNoQuick_Arq() throws Exception {
        runProfile(TEST_ITERATIONS, noQuickProfile, arqData); // throws Exception
    }

    //

    @Test
    public void testDefault_Tiny_One() throws Exception {
        runProfile(TEST_ITERATIONS, tinyTestDuration, defaultProfile, tinyWorkerData_One); // throws Exception
    }

    @Test
    public void testNoQuick_Tiny_One() throws Exception {
        runProfile(TEST_ITERATIONS, tinyTestDuration, noQuickProfile, tinyWorkerData_One); // throws Exception
    }

    @Test
    public void testDefault_Tiny_Many() throws Exception {
        runProfile(TEST_ITERATIONS, tinyTestDuration, defaultProfile, tinyWorkerData_Many); // throws Exception
    }

    @Test
    public void testNoQuick_Tiny_Many() throws Exception {
        runProfile(TEST_ITERATIONS, tinyTestDuration, noQuickProfile, tinyWorkerData_Many); // throws Exception
    }

    //

    public static final String ZIP_PATH_ROOT = "./build/testdata";
    public static final String ZIP_NAME_PREFIX = "test";
    public static final int ZIP_NAME_SUFFIX_PAD = 2;
    public static final String ZIP_EXT = ".zip";

    public static final String DUMMY_ENTRY_NAME_PREFIX = "dummy";
    public static final int DUMMY_ENTRY_NAME_SUFFIX_PAD = 2;
    public static final String DUMMY_ENTRY_EXT = ".txt";

    // The zip path count must match the worker data.
    public static final int ZIP_PATH_COUNT = 20;

    private String[] createZipPaths() {
         String[] useZipPaths = new String[ZIP_PATH_COUNT];

         for ( int pathNo = 0; pathNo < ZIP_PATH_COUNT; pathNo++ ) {
             useZipPaths[pathNo] = ZIP_PATH_ROOT + "/" + ZIP_NAME_PREFIX + pad0(pathNo, ZIP_NAME_SUFFIX_PAD) + ZIP_EXT;
         }

         return useZipPaths;
    }

    public void ensureZipFiles() throws IOException {
        for ( int pathNo = 0; pathNo < ZIP_PATH_COUNT; pathNo++ ) {
            ensureZipFile(pathNo, zipPaths[pathNo]); // throws IOException
        }
    }

    public void ensureArqFiles() throws IOException {
        int pathNo = 0;
        for ( SampleData sampleData : SAMPLE_DATA ) {
            ensureZipFile( pathNo++, sampleData.name );
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

        String dummyEntryName = DUMMY_ENTRY_NAME_PREFIX + pad0(pathNo, DUMMY_ENTRY_NAME_SUFFIX_PAD) + DUMMY_ENTRY_EXT;
        byte[] dummyEntryBytes = dummyEntryName.getBytes();

        ZipEntry zipEntry = new ZipEntry(dummyEntryName);
        zipOutput.putNextEntry(zipEntry); // throws IOException
        zipOutput.write(dummyEntryBytes); // throws IOException
        zipOutput.closeEntry(); // throws IOException

        zipOutput.finish(); // throws IOException

        zipOutput.close(); // throws IOException
    }

    protected String[] zipPaths;

    //

    public static class ZipTestOpComparator implements Comparator<ZipTestOp> {
        public int compare(ZipTestOp op1, ZipTestOp op2) {
            long at1 = op1.actAt;
            long at2 = op2.actAt;
            if ( at1 > at2 ) {
                return +1;
            } else if ( at1 < at2 ) {
                return -1;

            } else {
                String p1 = op1.path;
                String p2 = op2.path;
                int pCmp = p1.compareTo(p2);
                if ( pCmp != 0 ) {
                    return pCmp;

                } else {
                    int seqNo1 = op1.seqNo;
                    int seqNo2 = op2.seqNo;
                    if ( seqNo1 > seqNo2 ) {
                        return +1;
                    } else if ( seqNo1 < seqNo2 ) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            }
        }
    }

    public static final ZipTestOpComparator ZIP_TEST_OP_COMPARATOR = new ZipTestOpComparator();

    //

    protected ZipTestOps[] createSimpleWorkerData() {
        // Medium spaced opens.
        // 20 times: Every 5 * <medium> seconds, open for <medium> seconds.
        // Duration: (20 * (5 * medium)) + medium == 101 * medium
        int pairCount0 = 20;
        int seqNo0 = 0;
        String path0 = zipPaths[0];
        ZipTestOp[] basicPattern0 = new ZipTestOp[pairCount0 * 2];
        for ( int pairNo0 = 0; pairNo0 < pairCount0; pairNo0++ ) {
            long openAt0 = pairNo0 * (5 * mediumOpen);
            long closeAt0 = openAt0 + mediumOpen;
            basicPattern0[pairNo0 * 2] = new ZipTestOp(seqNo0++, path0, openAt0, DO_OPEN);
            basicPattern0[(pairNo0 * 2) + 1] = new ZipTestOp(seqNo0++, path0, closeAt0, DO_CLOSE);
        }

        // Medium-to-widely spaced opens.
        // Ten times: Every 2 * <long> seconds, open for <medium> seconds.
        // Duration: (10 * (2 * long)) + medium == 20 * long + medium
        int pairCount1 = 10;
        int seqNo1 = 0;
        String path1 = zipPaths[1];
        ZipTestOp[] basicPattern1 = new ZipTestOp[pairCount1 * 2];
        for ( int pairNo1 = 0; pairNo1 < pairCount1; pairNo1++ ) {
            long openAt1 = pairNo1 * (2 * longOpen);
            long closeAt1 = openAt1 + mediumOpen;
            basicPattern1[pairNo1 * 2] = new ZipTestOp(seqNo1++, path1, openAt1, DO_OPEN);
            basicPattern1[(pairNo1 * 2) + 1] = new ZipTestOp(seqNo1++, path1, closeAt1, DO_CLOSE);
        }

        // Quick opens.
        // 50 times: Every 2 * <medium> seconds, open for <medium> seconds.
        // Duration: (50 * (2 * medium)) + medium == 101 medium
        int pairCount2 = 50;
        int seqNo2 = 0;
        String path2 = zipPaths[2];
        ZipTestOp[] basicPattern2 = new ZipTestOp[pairCount2 * 2];
        for ( int pairNo2 = 0; pairNo2 < pairCount2; pairNo2++ ) {
            long openAt1 = pairNo2 * (2 * mediumOpen);
            long closeAt1 = openAt1 + mediumOpen;
            basicPattern2[pairNo2 * 2] = new ZipTestOp(seqNo2++, path2, openAt1, DO_OPEN);
            basicPattern2[(pairNo2 * 2) + 1] = new ZipTestOp(seqNo2++, path2, closeAt1, DO_CLOSE);
        }

        // Very quick opens:
        // 100 times: Every <medium> seconds, open for <short> seconds.
        // Duration: (100 * medium) + short
        int pairCount3 = 100;
        int seqNo3 = 0;
        String path3 = zipPaths[3];
        ZipTestOp[] basicPattern3 = new ZipTestOp[pairCount3 * 2];
        for ( int pairNo3 = 0; pairNo3 < pairCount3; pairNo3++ ) {
            long openAt3 = pairNo3 * mediumOpen;
            long closeAt3 = openAt3 + shortOpen;
            basicPattern3[pairNo3 * 2] = new ZipTestOp(seqNo3++, path3, openAt3, DO_OPEN);
            basicPattern3[(pairNo3 * 2) + 1] = new ZipTestOp(seqNo3++, path3, closeAt3, DO_CLOSE);
        }

        // Test of very widely spaced opens.
        // Three times: Every 5 * <long> seconds, open for <long> seconds.
        // Duration: (3 * (5 * long)) + long == 16 * long
        int pairCount4 = 3;
        int seqNo4 = 0;
        String path4 = zipPaths[4];
        ZipTestOp[] basicPattern4 = new ZipTestOp[pairCount4 * 2];
        for ( int pairNo4 = 0; pairNo4 < pairCount4; pairNo4++ ) {
            long openAt4 = pairNo4 * 5 * longOpen;
            long closeAt4 = openAt4 + longOpen;
            basicPattern4[pairNo4 * 2] = new ZipTestOp(seqNo4++, path4, openAt4, DO_OPEN);
            basicPattern4[(pairNo4 * 2) + 1] = new ZipTestOp(seqNo4++, path4, closeAt4, DO_CLOSE);
        }

        // Test of indefinite opens.
        // Five times: Every <long> seconds, open indefinitely.
        // Use a different zip for each open.
        // Duration: 5 * long
        int pairCount5 = 5;
        int seqNo5 = 0;
        ZipTestOp[] indefiniteOpenPattern = new ZipTestOp[pairCount5];
        for ( int pairNo5 = 0; pairNo5 < pairCount5; pairNo5++ ) {
            long openAt5 = pairNo5 * longOpen;
            indefiniteOpenPattern[pairNo5] = new ZipTestOp(seqNo5++, zipPaths[5 + pairNo5], openAt5, DO_OPEN);
        }
        
        return new ZipTestOps[] {
            new ZipTestOps("basic 0", basicPattern0),
            new ZipTestOps("basic 1", basicPattern1),
            new ZipTestOps("basic 2", basicPattern2),
            new ZipTestOps("basic 3", basicPattern3),
            new ZipTestOps("basic 4", basicPattern4),
            new ZipTestOps("indefinite open", indefiniteOpenPattern)
        };
    }
    
    protected ZipTestOps createBurstWorkerData() {
        // Burst operations exercise the reaper synchronization
        // very differently than spaced operations.  Burst operations
        // typically do not allow the reaper to run after posting a
        // pending close.
        //
        // See the CAUTION comments on ZipFileReaper.ReaperRunnable.run()
        // and on ZipFileReaper.close().

        // Test of burst operations.
        // Five times:
        // Every <long> seconds:
        //   Burst (no-delay) open/close of the same zip.
        // Every <long> seconds + <medium> seconds:
        //   Burst open/close of different zips.
        // Duration: 5 * (long + medium)

        int packetCount = 5;

        int sameBurstCount = 5;
        int diffBurstCount = 5;

        int samePairs = (sameBurstCount * 2);
        int diffPairs = (diffBurstCount * 2);
        int burstPairs = (samePairs + diffPairs);

        int burstSeqNo = 0;
        String burstPath0 = zipPaths[0];

        ZipTestOp[] burstPattern = new ZipTestOp[packetCount * burstPairs];

        for ( int packetNo = 0; packetNo < packetCount; packetNo++ ) {
            long sameAt = packetNo * longOpen;

            int sameBase = packetNo * burstPairs;
            int sameCap = sameBase + samePairs;

            for ( int pairNo = 0; pairNo < sameBurstCount; pairNo++ ) {
                burstPattern[sameBase + (pairNo * 2)] =
                    new ZipTestOp(burstSeqNo++, burstPath0, sameAt, DO_OPEN);
                burstPattern[sameBase + (pairNo * 2) + 1] =
                    new ZipTestOp(burstSeqNo++, burstPath0, sameAt, DO_CLOSE);
            }

            long diffAt = sameAt + mediumOpen;

            int diffBase = sameCap;
            @SuppressWarnings("unused")
            int diffCap = diffBase + diffPairs;

            for ( int pairNo = 0; pairNo < diffBurstCount; pairNo++ ) {
                burstPattern[diffBase + (pairNo * 2)] =
                    new ZipTestOp(burstSeqNo++, zipPaths[packetNo], diffAt, DO_OPEN);
                burstPattern[diffBase + (pairNo * 2) + 1] =
                    new ZipTestOp(burstSeqNo++, zipPaths[packetNo], diffAt, DO_CLOSE);
            }
        }

        return new ZipTestOps("burst", burstPattern); 
    }

    protected ZipTestOps createOverflowWorkerData() {
        // Burst with overflow:
        //
        // Five times:
        // Every <long> seconds:
        //   Burst open/close of all zips for <medium> seconds
        // Duration: (5 * long) + medium

        int packetCount7 = 5;

        int burstCount7 = ZIP_PATH_COUNT; // Set to be larger than cache maximum size.

        int overflowSeqNo = 0;

        ZipTestOp[] burstOverflowPattern = new ZipTestOp[packetCount7 * burstCount7 * 2];

        for ( int packetNo = 0; packetNo < packetCount7; packetNo++ ) {
            int burstOpenBase = packetNo * burstCount7 * 2;
            long burstOpenAt = packetNo * longOpen;

            for ( int pairNo = 0; pairNo < burstCount7; pairNo++ ) {
                burstOverflowPattern[burstOpenBase + pairNo] =
                    new ZipTestOp(overflowSeqNo++, zipPaths[pairNo], burstOpenAt, DO_OPEN);
            }

            int burstCloseBase = burstOpenBase + burstCount7;
            long burstCloseAt = burstOpenAt + mediumOpen;

            for ( int pairNo = 0; pairNo < burstCount7; pairNo++ ) {
                burstOverflowPattern[burstCloseBase + pairNo] =
                    new ZipTestOp(overflowSeqNo++, zipPaths[pairNo], burstCloseAt, DO_CLOSE);
            }
        }

        return new ZipTestOps("burst", burstOverflowPattern); 
    }

    protected ZipTestOps createScatterWorkerData(int seed, int scale) {
        return new ZipTestOps("scatter[" + seed + "] [ " + scale + " ]", scatterOperations(seed, scale));
    }

    protected ZipTestOps createDribbleWorkerData(int seed) {
        return new ZipTestOps("dribble[" + seed + "]", dribbleOperations(seed));
    }

    protected ZipTestOps createTinyWorkerData(int seed, int divisions) {
        return new ZipTestOps("tiny[" + seed + "]", tinyOperations(seed, divisions));
    }

    // Random pattern:
    //
    // Duration:  Weight:
    // TRIVIAL    4
    // SHORT      4
    // MEDIUM     2
    // LONG       1
    //
    // Positions:
    // 0..3   : TRIVIAL
    // 4..7   : SHORT
    // 8..9   : MEDIUM
    // 10     : LONG

    protected long[] standardDurations;

    /**
     * Answer standard durations.  This is a distribution of open durations
     * which mixes four categories of opens: Four very short ({@link #trivialOpen})
     * opens, four short ({@link #shortOpen}) opens, two medium ({@link #mediumOpen})
     * opens, and one long ({@link #longOpen}) open.
     * 
     * @return An array of standard open durations.
     */
    protected long[] getStandardDurations() {
        if ( standardDurations == null ) {
            standardDurations = new long[] {
                trivialOpen, trivialOpen, trivialOpen, trivialOpen, 
                shortOpen, shortOpen, shortOpen, shortOpen,
                mediumOpen, mediumOpen,
                longOpen
            };
        }
        return standardDurations;
    }

    protected long selectDuration(Random randomizer, long[] durations) {
        return durations[ randomizer.nextInt(durations.length) ];
    }

    protected ZipTestOp[] scatterOperations(long seed, int scale) {
        Random randomizer = new Random(seed);
        long[] durations = getStandardDurations();

        long testUnits = 120 * scale;
        long testDuration = mediumOpen * testUnits;

        int pairCounts = 40 * scale;

        int scatterSeqNo = 0;
        List<ZipTestOp> randomPatternBuilder = new ArrayList<ZipTestOp>();

        for ( int pathNo = 0; pathNo < zipPaths.length; pathNo++ ) {
            String nextPath = zipPaths[pathNo];
            for ( int pairNo = 0; pairNo < pairCounts; pairNo++ ) {
                long nextOpenAt = randomizer.nextInt(95) * mediumOpen;
                long nextDuration = selectDuration(randomizer, durations);
                long nextCloseAt = nextOpenAt + nextDuration;

                randomPatternBuilder.add( new ZipTestOp(scatterSeqNo++, nextPath, nextOpenAt, DO_OPEN) );

                if ( nextCloseAt < testDuration ) {
                    randomPatternBuilder.add( new ZipTestOp(scatterSeqNo++, nextPath, nextCloseAt, DO_CLOSE) );
                }
            }
        }

        ZipTestOp[] randomPattern = randomPatternBuilder.toArray( new ZipTestOp[ randomPatternBuilder.size() ]);
        Arrays.sort(randomPattern, ZIP_TEST_OP_COMPARATOR);

        return randomPattern;
    }

    protected ZipTestOp[] dribbleOperations(long seed) {
        Random randomizer = new Random(seed);
        long[] durations = getStandardDurations();
        long testDuration = maxTestDuration;

        int dribbleSeqNo = 0;

        List<ZipTestOp> randomPatternBuilder = new ArrayList<ZipTestOp>();

        for ( int pathNo = 0; pathNo < zipPaths.length; pathNo++ ) {
            String nextPath = zipPaths[pathNo];
            long nextAt = 0L;
            while ( nextAt < testDuration ) {
                randomPatternBuilder.add( new ZipTestOp(dribbleSeqNo++, nextPath, nextAt, DO_OPEN) );
                nextAt += selectDuration(randomizer, durations);

                if ( nextAt < testDuration ) {
                    randomPatternBuilder.add( new ZipTestOp(dribbleSeqNo++, nextPath, nextAt, DO_CLOSE) );
                    nextAt += selectDuration(randomizer, durations);
                }
            }
        }

        ZipTestOp[] randomPattern = randomPatternBuilder.toArray( new ZipTestOp[ randomPatternBuilder.size() ]);
        Arrays.sort(randomPattern, ZIP_TEST_OP_COMPARATOR);

        return randomPattern;
    }

    protected ZipTestOp[] tinyOperations(long seed, int divisions) {
        // The tiny test duration includes an extra amount for slow pending
        // closes.  Take this off the duration used to schedule operations.
        long testDuration = tinyTestDuration - (ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MAX * 2);

        // +1 to fit the last close within the overall duration
        long burstSeparation = testDuration / (divisions + 1);

        int tinySeqNo = 0;

        List<ZipTestOp> tinyOps= new ArrayList<ZipTestOp>();

        for ( int divNo = 0; divNo < divisions; divNo++ ) {
            long openAt = burstSeparation * divNo;
            long closeAt = openAt + TINY_OPEN;

            for ( int pathNo = 0; pathNo < zipPaths.length; pathNo++ ) {
                String nextPath = zipPaths[pathNo];
                tinyOps.add( new ZipTestOp(tinySeqNo++, nextPath, openAt, DO_OPEN) );
                tinyOps.add( new ZipTestOp(tinySeqNo++, nextPath, closeAt, DO_CLOSE) );
            }
        }

        return tinyOps.toArray( new ZipTestOp[ tinyOps.size() ]);
    }

    public static class SampleData {
        public final String name;
        public final int openCount;
        public final long firstOpen, lastOpen;
        public final long firstClose, lastClose;
        
        public final long openDur, pendDur;

        public SampleData(
            String name,
            int openCount,
            long firstOpen, long lastOpen,
            long firstClose, long lastClose,
            long openDur, long pendDur) {

            this.name = ZIP_PATH_ROOT + "/" + name;

            this.openCount = openCount;
        
            this.firstOpen = firstOpen;
            this.lastOpen = lastOpen;
        
            this.firstClose = firstClose;
            this.lastClose = lastClose;
            
            this.openDur = openDur;
            this.pendDur = pendDur;
        }

        public void createOperations(List<ZipTestOp> ops) {
            long openDelta = (lastOpen - firstOpen) / openCount;
            // long closeDelta = (lastClose - firstClose) / openCount;
            long closeDelta = openDur / openCount;

            for ( int openNo = 0; openNo < openCount; openNo++ ) {
                long openAt = firstOpen + (openNo * openDelta);
                ops.add( new ZipTestOp( ops.size(), name, openAt, DO_OPEN) );

                // long closeAt = firstClose + (openNo * closeDelta);
                long closeAt = openAt + closeDelta;
                ops.add( new ZipTestOp( ops.size(), name, closeAt, DO_CLOSE) );
            }
        }
    }

    public static final SampleData[] SAMPLE_DATA = new SampleData[] {
        new SampleData( "arquillian-junit.jar", 8, 35439000L, 1676142000L, 37283000L, 1677358000L, 474086000L, 1167833000L ),
        new SampleData( "arquillian-core.jar", 9, 13421000L, 1711152000L, 30729000L, 1711383000L, 536070000L, 1161890000L ),
        new SampleData( "arquillian-testenricher-initialcontext.jar", 8, 52564000L, 1676372000L, 52796000L, 1677402000L, 377287000L, 1247550000L ),
        new SampleData( "ef97c071-6901-4626-a774-2c34360ba9d8.war", 1, 70147000L, 7017000L, 70396000L, 70396000L, 249000L, 0L ),
        new SampleData( "arquillian-protocol.jar", 10, 40817000L, 1680390000L, 41229000L, 1680534000L, 402384000L, 1237333000L ),
        new SampleData( "arquillian-testenricher-cdi.jar", 7, 46458000L, 1676257000L, 46868000L, 1677384000L, 376613000L, 1254312000L ),
        new SampleData( "arquillian-testenricher-resource.jar", 8, 56072000L, 1676433000L, 56282000L, 1677410000L, 378085000L, 1243252000L ),
        new SampleData( "arquillian-testenricher-ejb.jar", 8, 49938000L, 1676315000L, 50161000L, 1677394000L, 378917000L, 1248537000L ),
        new SampleData( "arquillian-chameleon-runner.jar", 8, 2788000L, 1675987000L, 9647000L, 1676989000L, 386646000L, 1287554000L ),
        new SampleData( "arquillian-testenricher-cdi.jar", 3, 1165627000L, 1720516000L, 1166296000L, 172069100L, 997000L, 554066000L ),
    };

    protected ZipTestOps createArqData() {
        List<ZipTestOp> arqOps = new ArrayList<ZipTestOp>();
        for ( SampleData sampleData : SAMPLE_DATA ) {
            sampleData.createOperations(arqOps);
        }
        ZipTestOp[] arqOpsArray = arqOps.toArray( new ZipTestOp[ arqOps.size() ]);

        Arrays.sort(arqOpsArray, ZIP_TEST_OP_COMPARATOR);

        return new ZipTestOps("arq", arqOpsArray);
    }

    protected long trivialOpen;
    protected long tinyOpen;
    protected long shortOpen;
    protected long mediumOpen;
    protected long longOpen;

    protected long tinyTestDuration;
    protected long maxTestDuration;

    protected ZipTestOps[] simpleWorkerData;

    protected ZipTestOps[] burstWorkerData;
    protected ZipTestOps[] overflowWorkerData;

    protected ZipTestOps[] scatterWorkerData_One_Short;
    protected ZipTestOps[] scatterWorkerData_One_Long;

    protected ZipTestOps[] scatterWorkerData_Many_Short;
    protected ZipTestOps[] scatterWorkerData_Many_Long;

    protected ZipTestOps[] dribbleWorkerData_One;
    protected ZipTestOps[] dribbleWorkerData_Many;

    protected ZipTestOps[] tinyWorkerData_One;
    protected ZipTestOps[] tinyWorkerData_Many;

    protected ZipTestOps[] arqData;

    //

    public static class TestProfile {
        public TestProfile(
            String profileName,
            boolean debugState,
            int maxCache,
            long quickPendMin, long quickPendMax,
            long slowPendMin, long slowPendMax) {

            this.profileName = profileName;

            this.debugState = debugState;

            this.maxCache = maxCache;

            this.quickPendMin = quickPendMin;
            this.quickPendMax = quickPendMax;

            this.slowPendMin = slowPendMin;
            this.slowPendMax = slowPendMax;
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
        private class TestErrorConsumer implements ZipFileReaper.ErrorConsumer<String>{

            private List<String> collection;

            public TestErrorConsumer(List<String> collection){
                this.collection = collection;
            }

            @Override
            public void accept(String t) {
                collection.add(t);
            }
            
        }

        public void displayProfile() {
            String methodName = "displayProfile";

            debug(methodName, "Max Cache [ " + toCount(maxCache) + " ]");

            debug(methodName, "Quick Pend" +
                " Min [ " + toAbsSec(quickPendMin) + "s ]" +
                " Max [ " + toAbsSec(quickPendMin) + "s ]");
            debug(methodName, "Slow Pend" +
                " Min [ " + toAbsSec(slowPendMin) + "s ]" +
                " Max [ " + toAbsSec(slowPendMin) + "s ]");
        }

        public void displayParameters(
            long maxTestDuration,
            long trivialOpen, long tinyOpen, long shortOpen, long mediumOpen, long longOpen,
            String[] zipPaths, ZipTestOps[] allTestOps) {

            String methodName = "displayParameters";

            debug(methodName, "Test Duration [ " + toAbsSec(maxTestDuration) + "s ]");

            debug(methodName, "Open Durations:" +
                " [ " + toAbsSec(trivialOpen) + "s ]" +
                " [ " + toAbsSec(tinyOpen) + "s ]" +
                " [ " + toAbsSec(shortOpen) + "s ]" +
                " [ " + toAbsSec(mediumOpen) + "s ]" +
                " [ " + toAbsSec(longOpen) + "s ]");

            debug(methodName, "Zip paths [ " + Integer.toString(zipPaths.length) + " ]");
            debug(methodName, "Test Threads [ " + Integer.toString(allTestOps.length) + " ]");
        }

        public void runTest(
            int iteration,
            long maxTestDuration,
            long trivialOpen, long tinyOpen, long shortOpen, long mediumOpen, long longOpen,
            String[] zipPaths, ZipTestOps[] allTestOps) throws IOException, Exception {

            String methodName = "run";
            debug(methodName, "Begin [ " + profileName + " ] [ " + iteration + " ]");

            displayProfile();
            displayParameters(
                maxTestDuration, trivialOpen, tinyOpen, shortOpen, mediumOpen, longOpen,
                zipPaths, allTestOps);

            List<String> reaperErrors = new ArrayList<>();
            
            TestErrorConsumer errorHandler = new TestErrorConsumer(reaperErrors);

            //Consumer<String> errorHandler =
            //    (String errorMessage) -> { reaperErrors.add(errorMessage); };

            ZipFileReaper reaper = startReaper(errorHandler);

            ZipTestWorker[] testWorkers = createTestWorkers(reaper, allTestOps);
            Thread[] testThreads = createTestThreads(reaper, allTestOps, testWorkers);

            long startAt = launchThreads(reaper, allTestOps, testWorkers, testThreads);
            @SuppressWarnings("unused")
            long endAt = waitForThreads(reaper, testWorkers, testThreads, startAt, maxTestDuration);

            int errorCount = examineResults(reaper, testWorkers);

            // PrintWriter reaperWriter = new PrintWriter(System.out);
            // reaper.introspect( reaperWriter, System.nanoTime() );
            // reaperWriter.flush();
            // System.out.println();

            finishReaper(reaper);

            errorCount += reaperErrors.size();

            debug(methodName, "End [ " + profileName + " ]");

            if ( errorCount > 0 ) {
                throw new Exception("Failed: Exception count [ " + Integer.valueOf(errorCount) + " ]");
            }
        }

        public ZipFileReaper startReaper(TestErrorConsumer errorHandler) {
            return new ZipFileReaper(
                profileName,
                debugState,
                maxCache,
                quickPendMin, quickPendMax,
                slowPendMin, slowPendMax,
                errorHandler); 
        }

        public void finishReaper(ZipFileReaper reaper) {
            reaper.shutDown();
        }

        public ZipTestWorker[] createTestWorkers(
            ZipFileReaper reaper, ZipTestOps[] allTestOps) {
            String methodName = "createTestWorkers";

            int numThreads = allTestOps.length;
            debug(methodName, "Threads [ " + pad0(numThreads, THREAD_PAD) + " ] ...");

            ZipTestWorker[] testWorkers = new ZipTestWorker[numThreads];

            for ( int threadNo = 0; threadNo < numThreads; threadNo++ ) {
                debug(methodName,
                    "  Thread [ " + pad0(threadNo, THREAD_PAD) + " ]" +
                    " Operations [ " + pad0(allTestOps[threadNo].testOps.length, OP_PAD) + " ]");

                ZipTestWorker zipTestWorker = new ZipTestWorker(reaper, allTestOps[threadNo]);
                testWorkers[threadNo] = zipTestWorker;
            }

            return testWorkers;
        }

        public Thread[] createTestThreads(
            ZipFileReaper reaper,
            ZipTestOps[] allTestOps,
            ZipTestWorker[] testWorkers) {

            int numThreads = allTestOps.length;

            Thread[] testThreads = new Thread[numThreads];

            for ( int threadNo = 0; threadNo < numThreads; threadNo++ ) {
                ZipTestWorker testWorker = testWorkers[threadNo];

                Thread testThread = new Thread(testWorker, "zip test thread " + pad0(threadNo, THREAD_PAD));
                testThread.setUncaughtExceptionHandler( testWorker.getErrorHandler() );

                testThreads[threadNo] = testThread;
            }

            return testThreads;
        }

        public long launchThreads(
            ZipFileReaper reaper,
            ZipTestOps[] allTestOps,
            ZipTestWorker[] testWorkers,
            Thread[] testThreads) {

            String methodName = "launchThreads";

            long startAt = System.nanoTime();

            int numThreads = allTestOps.length;

            for ( int threadNo = 0; threadNo < numThreads; threadNo++ ) {
                testThreads[threadNo].start();
            }

            debug(methodName, "Threads [ " + Integer.toString( numThreads ) + " ] ... launched");

            return startAt;
        }

        public long waitForThreads(
            ZipFileReaper reaper,
            ZipTestWorker[] testWorkers,
            Thread[] testThreads,
            long startAt, long maxDuration) {

            System.out.println("Waiting for test worker threads [ " + testThreads.length + " ]");

            for ( Thread testThread : testThreads ) {
                try {
                    testThread.join(); // throws InterruptedException
                } catch ( InterruptedException e ) {
                    // IGNORE
                }
            }

            long endAt = System.nanoTime();
            long threadDuration = endAt - startAt;
            System.out.println("Worker duration [ " + toAbsSec(threadDuration) + " ]");

            long remainingDuration = maxDuration - threadDuration;
            if ( remainingDuration > 0L ) {
                System.out.println("Remaining duration [ " + toAbsSec(remainingDuration) + " ]");

                sleep(remainingDuration);

                endAt = System.nanoTime();
                threadDuration = endAt - startAt;

            } else {
                System.out.println("Extra duration [ " + toAbsSec(-1 * remainingDuration) + " ]");
            }

            System.out.println("Completed test duration [ " + toAbsSec(threadDuration) + " ]; expected [ " + toAbsSec(maxDuration) + " ]");

            return endAt;
        }

        public int examineResults(ZipFileReaper reaper, ZipTestWorker[] testWorkers) {
            int numErrors = 0;

            for ( ZipTestWorker testWorker : testWorkers ) {
                Throwable caughtError = testWorker.getError();
                if ( caughtError != null ) {
                    numErrors++;
                    caughtError.printStackTrace();
                }
            }

            return numErrors;
        }
    }

    //

    private static final boolean DO_OPEN = true;
    private static final boolean DO_CLOSE = false;

    //

    public static class ZipTestOps {
        public final String testDesc;
        public final ZipTestOp[] testOps;

        public ZipTestOps(String testDesc, ZipTestOp[] testOps) {
            this.testDesc = testDesc;
            this.testOps = testOps;
        }

        public void display() {
            System.out.println("Test Operations [ " + testDesc + " ]");
            for ( ZipTestOp testOp : testOps ) {
                System.out.println("  " + testOp.actionText());
            }
        }
    }

    public static class ZipTestOp {
        public final int seqNo;
        public final String path;
        public final long actAt;
        public final boolean isOpen;

        public ZipTestOp(int seqNo, String path, long actAt, boolean isOpen) {
            this.seqNo = seqNo;
            this.path = path;
            this.actAt = actAt;
            this.isOpen = isOpen;
        }

        private String actionText() {
            return "[ " + pad0(seqNo, SEQ_PAD) + " ]" +
                   " [ " + path + " ] [ " + (isOpen ? "OPEN" : "CLOSE") + " ]" +
                   " at [ " + toAbsSec(actAt) + "s ]";
        }

        private String actionText(ZipFileReaper reaper, long actualActAt) {
            return "[ " + pad0(seqNo, SEQ_PAD) + " ]" +
                   " [ " + path + " ] [ " + reaper.getState(path) + " ]" +
                   " at [ " + toAbsSec(actAt) + "s ]" +
                   " [ " + toRelSec(reaper.getInitialAt(), actualActAt) + "s ]";
        }

        public void perform(ZipFileReaper reaper) {
            String methodName = "perform";

            long actualActAt = getNanoTime();

            if ( isOpen ) {
                try {
                    // debug(methodName, "Open " + actionText(reaper, actualActAt));
                    @SuppressWarnings("unused")
                    ZipFile zipFile = reaper.open(path, actualActAt); // throws IOException, ZipException
                    // debug(methodName, "Opened " + actionText(reaper, actualActAt));

                } catch ( Exception e ) {
                    debug(methodName, "Failed to open " + actionText(reaper, actualActAt));
                    throw new RuntimeException(e);
                }

                reaper.validate();

            } else {
                // debug(methodName, "Close " + actionText(reaper, actualActAt));
                @SuppressWarnings("unused")
                ZipFileData.ZipFileState immediateState = reaper.close(path, actualActAt);
                // debug(methodName, "Closed [ " + immediateState + " ] " + actionText(reaper, actualActAt));

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
        public ZipTestWorker(ZipFileReaper reaper, ZipTestOps operations) {
            this.reaper = reaper;
            this.operations = operations;

            this.errorHandler = this.createHandler();
        }

        private final ZipFileReaper reaper;
        private final ZipTestOps operations;

        //

        private Thread.UncaughtExceptionHandler createHandler() {
            return new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    setError(throwable);
                }
            };
        }

        private final Thread.UncaughtExceptionHandler errorHandler;

        public Thread.UncaughtExceptionHandler getErrorHandler() {
            return errorHandler;
        }

        //

        private Throwable caughtError;
        
        public void setError(Throwable caughtError) {
            this.caughtError = caughtError;
        }

        public Throwable getError() {
            return caughtError;
        }

        //

        public void run() {
            long lastActAt = 0L;

            for ( ZipTestOp nextOp : operations.testOps ) {
                long nextActAt = nextOp.actAt;
                long nextDelay = nextActAt - lastActAt;
                if ( nextDelay != 0 ) {
                    try {
                        Thread.sleep((nextDelay) / ZipCachingProperties.NANO_IN_MILLI);
                    } catch ( InterruptedException e ) {
                        // Ignore
                    }
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

    public TestProfile createDefaultProfile(int reaperMax) {
        return new TestProfile(
            "default reaper",
            ZipFileReaper.DO_NOT_DEBUG_STATE,

            reaperMax,

            ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN,
            ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MAX,

            ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MIN,
            ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MAX);
    }

    public final TestProfile createNoQuickProfile(int reaperMax) {
        return new TestProfile(
            "no quick reaper",
            ZipFileReaper.DO_NOT_DEBUG_STATE,

            reaperMax, // Related to operation data value 'ZIP_PATH_COUNT'

            0,
            0,

            ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MIN,
            ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MAX);
    }

    protected final TestProfile defaultProfile;
    protected final TestProfile noQuickProfile;

    //

    @Trivial    
    private static long getNanoTime() {
        return System.nanoTime();
    }

    //

    public static final int THREAD_PAD = 3;
    public static final int OP_PAD = 4;
    public static final int SEQ_PAD = 4;

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

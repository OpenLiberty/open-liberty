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
package com.ibm.ws.annocache.test.jandex;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.annocache.jandex.internal.SparseIndex;

import org.jboss.jandex.Index;
import org.junit.AfterClass;
import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(Parameterized.class)
public class SparseIndexPerfTest_Samples {

    @Parameters
    public static Collection<Object[]> data() {
        return JandexTestData.data();
    }

    //

    private static class TestData {
        public final String indexPath;
        public final int indexSize;
        public final byte[] indexBytes;

        public TestData(String indexPath, int indexSize) {
            this.indexPath = indexPath;
            this.indexSize = indexSize;

            long readStart = System.nanoTime();
            this.indexBytes = JandexTestUtils.asBytes(indexPath);
            long readDuration = System.nanoTime() - readStart;

            System.out.println("Raw read [ " + Long.valueOf(readDuration / JandexTestUtils.NANOS_IN_MICRO) + " (micro) ]");
        }
    }

    private List<List<SparseIndex>> sparseData =  new ArrayList<List<SparseIndex>>();
    private List<List<Index>> fullData = new ArrayList<List<Index>>();

    //

    private static final Map<Integer, TestData> allTestData = new HashMap<Integer, TestData>();
    private final TestData testData;

    public SparseIndexPerfTest_Samples(Object testNoObj, Object indexPathObj, Object indexSizeObj) {
        System.out.println("Test [ " + testNoObj + " ] [ " + indexPathObj + " ] [ " + indexSizeObj + " (classes) ]");

        Integer testNo = (Integer) testNoObj;
        String indexPath = (String) indexPathObj;
        int indexSize = ((Integer) indexSizeObj).intValue();

        TestData useTestData = allTestData.get(testNoObj);
        if ( useTestData == null ) {
            useTestData = new TestData(indexPath, indexSize);
            allTestData.put(testNo, useTestData);
        }

        testData = useTestData;
    }

    public static class HeapStats {
        public HeapStats() {
            this.maxSize = Runtime.getRuntime().maxMemory();
            this.currentSize = Runtime.getRuntime().totalMemory(); 
            this.freeSize = Runtime.getRuntime().freeMemory(); 
        }

        public final long maxSize;
        public final long currentSize;
        public final long freeSize;
    }

    public static class TimeStats {
        public final String indexPath;
        public final long numClasses;

        public final int iterations;
        public final long totalDuration;
        public final long minDuration;
        public final long maxDuration;

        public TimeStats(
            String indexPath, long numClasses,
            int iterations,
            long totalDuration, long minDuration, long maxDuration) {

            this.indexPath = indexPath;
            this.numClasses = numClasses;

            this.iterations = iterations;
            this.totalDuration = totalDuration;
            this.minDuration = minDuration;
            this.maxDuration = maxDuration;
        }
    }

    public static TimeStats fullTimeStats;
    public static TimeStats sparseTimeStats;

    @AfterClass
    public static void summarizeTimes() {
        if ( fullTimeStats == null ) {
            System.out.println("Full times were not collected");
        } else {
            System.out.println("Full times");
            System.out.println("  Path [ " + fullTimeStats.indexPath + " ]");
            System.out.println("  Classes [ " + fullTimeStats.numClasses + " ]");
        }

        if ( sparseTimeStats == null ) {
            System.out.println("Sparse times were not collected");
        } else {
            System.out.println("Sparse times");
            System.out.println("  Path [ " + sparseTimeStats.indexPath + " ]");
            System.out.println("  Classes [ " + sparseTimeStats.numClasses + " ]");
        }
    }

    @Test
    public void testFullReads() {
        int iterations = JandexTestData.ITERATIONS;

        List<Long> readTimes = new ArrayList<Long>(iterations);
        List<Index> fullIndexes = new ArrayList<Index>(iterations);
        fullData.add(fullIndexes);

        List<HeapStats> heapStats = new ArrayList<HeapStats>(iterations + 1);

        System.gc();
        heapStats.add( new HeapStats() );

        long minDuration = -1L;
        long maxDuration = 0L;
        long totalDuration = 0L;

        for ( int iterationNo = 0; iterationNo < iterations; iterationNo++ ) {
            long readStart = System.nanoTime();

            Index fullIndex = JandexTestUtils.readFullIndex(
                testData.indexPath, new ByteArrayInputStream(testData.indexBytes));
            fullIndexes.add(fullIndex);

            long readDuration = System.nanoTime() - readStart;

            readTimes.add(Long.valueOf(readDuration));
            if ( (minDuration == -1L) || (readDuration < minDuration) ) {
                minDuration = readDuration;
            }
            if ( readDuration > maxDuration ) {
                maxDuration = readDuration;
            }
            totalDuration += readDuration;

            heapStats.add( new HeapStats() );

            Assert.assertEquals("Expected count of classes", testData.indexSize, fullIndex.getKnownClasses().size());
        }

        displayTimes(
            "Full Index Read",
            iterations,
            readTimes,
            minDuration, maxDuration, totalDuration);

        displayHeaps(
            "Full Index Read",
            iterations,
            heapStats);

        fullTimeStats = new TimeStats(
            testData.indexPath, testData.indexSize,
            iterations,
            totalDuration, minDuration, maxDuration);
    }

    @Test
    public void testSparseReads() {
        int iterations = JandexTestData.ITERATIONS;

        List<Long> readTimes = new ArrayList<Long>(iterations);
        List<SparseIndex> sparseIndexes = new ArrayList<SparseIndex>(iterations);
        sparseData.add(sparseIndexes);

        List<HeapStats> heapStats = new ArrayList<HeapStats>(iterations + 1);
        
        long minDuration = -1L;
        long maxDuration = 0L;
        long totalDuration = 0L;

        heapStats.add( new HeapStats() );

        for ( int iterationNo = 0; iterationNo < iterations; iterationNo++ ) {
            long readStart = System.nanoTime();

            SparseIndex sparseIndex = JandexTestUtils.readSparseIndex(
                testData.indexPath, new ByteArrayInputStream(testData.indexBytes));
            sparseIndexes.add(sparseIndex);

            long readDuration = System.nanoTime() - readStart;

            readTimes.add(Long.valueOf(readDuration));
            if ( (minDuration == -1L) || (readDuration < minDuration) ) {
                minDuration = readDuration;
            }
            if ( readDuration > maxDuration ) {
                maxDuration = readDuration;
            }
            totalDuration += readDuration;

            heapStats.add( new HeapStats() );

            Assert.assertEquals("Expected count of classes", testData.indexSize, sparseIndex.getKnownClasses().size());
        }

        displayTimes(
            "Sparse Index Read",
            iterations,
            readTimes,
            minDuration, maxDuration, totalDuration);

        displayHeaps(
            "Sparse Index Read",
            iterations,
            heapStats);
        

        sparseTimeStats = new TimeStats(
            testData.indexPath, testData.indexSize,
            iterations,
            totalDuration, minDuration, maxDuration);
    }

    public void displayTimes(
        String description,
        int iterations,
        List<Long> readTimes,
        long minDuration, long maxDuration, long totalDuration) {

        System.out.println(description);
        System.out.println("  Read [ " + testData.indexPath + " (path) ] [ " + Integer.valueOf(testData.indexSize) + " (classes) ]");
        System.out.println("  [ " + Integer.valueOf(iterations) + " (iterations) ]");

        minDuration /= JandexTestUtils.NANOS_IN_MICRO;
        maxDuration /= JandexTestUtils.NANOS_IN_MICRO;
        totalDuration /= JandexTestUtils.NANOS_IN_MICRO;
        long avgDuration = totalDuration / iterations;

        System.out.println(
            "    Avg [ " + Long.valueOf(avgDuration) + " (micro) ]" +
            " Min [ " + Long.valueOf(minDuration) + " (micro) ]" +
            " Max [ " + Long.valueOf(maxDuration) + " (micro) ]" +
            " Tot [ " + Long.valueOf(totalDuration) + " (micro) ]");
    }

    private void displayHeaps(
        String description,
        int iterations,
        List<HeapStats> heapStats) {

        // System.out.println(description);;
        // System.out.println("  Read [ " + path + " (path) ] [ " + Integer.valueOf(numClasses) + " (classes) ]");
        // System.out.println("  [ " + Integer.valueOf(iterations) + " (iterations) ]");

        int heapSize = heapStats.size();
        for ( int heapNo = 0; heapNo < heapSize; heapNo += (heapSize - 1)) {
            HeapStats nextHeapStats = heapStats.get(heapNo);

            System.out.println(
                "  [ " + Integer.valueOf(heapNo) + " ]" +
                " Max [ " + Long.valueOf(nextHeapStats.maxSize / JandexTestUtils.BYTES_IN_MEGABYTE) + " (MB) ]" +
                " Cur [ " + Long.valueOf(nextHeapStats.currentSize / JandexTestUtils.BYTES_IN_MEGABYTE) + " (MB) ]" +
                " Tot [ " + Long.valueOf(nextHeapStats.freeSize / JandexTestUtils.BYTES_IN_MEGABYTE) + " (MB) ]");
        }
    }
}

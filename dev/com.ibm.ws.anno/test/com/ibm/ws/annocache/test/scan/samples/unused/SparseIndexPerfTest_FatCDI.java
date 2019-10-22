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
package com.ibm.ws.annocache.test.scan.samples.unused;

import org.jboss.jandex.Index;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.annocache.jandex.internal.SparseIndex;
import com.ibm.ws.annocache.test.jandex.JandexTestData;
import com.ibm.ws.annocache.test.jandex.JandexTestUtils;
import com.ibm.ws.annocache.test.utils.TestLocalization;

public class SparseIndexPerfTest_FatCDI {

	public static void main(String[] args) throws Exception {
		System.out.println("Jandex Index Read Performance Suite");

		System.out.println("Pre-loading indexes ...");
		setUp(); // throws Exception

		System.out.println("Pause [ 2 ] ...");
		Thread.sleep(2000L);

		System.out.println("Loading using full reader ...");
		(new SparseIndexPerfTest_FatCDI()).testFullReads();

		System.out.println("Pause [ 2 ] ...");
		Thread.sleep(2000L);

		System.out.println("Loading using sparse reader ...");
		(new SparseIndexPerfTest_FatCDI()).testSparseReads();
	}

    private static String rootIndexPath;

    private static String[] indexPaths;
    private static byte[][] indexBytes;

    //

    private static List<List<Index>> fullData;
    private static List<List<SparseIndex>> sparseData;


	@BeforeClass
    public static void setUp() throws Exception {
        rootIndexPath = TestLocalization.putIntoProject(
            JandexTestData.FAT_CDI_JANDEX_ROOT_RELATIVE_PATH);

        System.out.println("Root [ " + rootIndexPath + " (path) ]");

        File rootIndexFile = new File(rootIndexPath);
        String[] indexNames = rootIndexFile.list();
        indexPaths = TestLocalization.putInto(rootIndexPath, indexNames);

        System.out.println("Indexes [ " + Integer.valueOf(indexNames.length) + " (files) ]");

        indexBytes = new byte[indexPaths.length][];

        long totalIndexBytes = 0L;

        System.gc();
        HeapStats initialHeap = new HeapStats();

        long readStart = System.nanoTime();
        for ( int pathNo = 0; pathNo < indexPaths.length; pathNo++ ) {
            indexBytes[pathNo] = JandexTestUtils.asBytes(indexPaths[pathNo]);
            totalIndexBytes += indexBytes[pathNo].length;
        }
        long readDuration = System.nanoTime() - readStart;

        HeapStats readPreGCHeap = new HeapStats();
        System.gc();
        HeapStats readPostGCHeap = new HeapStats();

        System.out.println("Read Time [ " + Long.valueOf(readDuration / JandexTestUtils.NANOS_IN_MILLI) + " (micro) ]");
        System.out.println("Read Size [ " + Long.valueOf(totalIndexBytes) + " (bytes) ]");

        System.out.println(
            "   [ Pre-Read, Post-GC ]" +
            " Max [ " + Long.valueOf(initialHeap.maxSize / JandexTestUtils.BYTES_IN_MEGABYTE) + " (MB) ]" +
            " Cur [ " + Long.valueOf(initialHeap.currentSize / JandexTestUtils.BYTES_IN_MEGABYTE) + " (MB) ]" +
            " Tot [ " + Long.valueOf(initialHeap.freeSize / JandexTestUtils.BYTES_IN_MEGABYTE) + " (MB) ]");
        System.out.println(
            "   [ Post-Read, Pre-GC ]" +
            " Max [ " + Long.valueOf(readPreGCHeap.maxSize / JandexTestUtils.BYTES_IN_MEGABYTE) + " (MB) ]" +
            " Cur [ " + Long.valueOf(readPreGCHeap.currentSize / JandexTestUtils.BYTES_IN_MEGABYTE) + " (MB) ]" +
            " Tot [ " + Long.valueOf(readPreGCHeap.freeSize / JandexTestUtils.BYTES_IN_MEGABYTE) + " (MB) ]");
        System.out.println(
            "   [ Post-Read, Post-GC ]" +
            " Max [ " + Long.valueOf(readPostGCHeap.maxSize / JandexTestUtils.BYTES_IN_MEGABYTE) + " (MB) ]" +
            " Cur [ " + Long.valueOf(readPostGCHeap.currentSize / JandexTestUtils.BYTES_IN_MEGABYTE) + " (MB) ]" +
            " Tot [ " + Long.valueOf(readPostGCHeap.freeSize / JandexTestUtils.BYTES_IN_MEGABYTE) + " (MB) ]");

        fullData = new ArrayList<List<Index>>();
        sparseData =  new ArrayList<List<SparseIndex>>();
    }

    //

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

    //

    // @Test
    public void testFullReads() {
        int iterations = JandexTestData.FAT_CDI_ITERATIONS;

        List<Long> readTimes = new ArrayList<Long>(iterations);
        List<HeapStats> heapStats = new ArrayList<HeapStats>(iterations + 1);

        long minDuration = -1L;
        long maxDuration = 0L;
        long totalDuration = 0L;

        heapStats.add( new HeapStats() );

        int totalClasses = 0;

        for ( int iterationNo = 0; iterationNo < iterations; iterationNo++ ) {
            List<Index> fullIndexes = new ArrayList<Index>(indexPaths.length);
            fullData.add(fullIndexes);

            long readStart = System.nanoTime();

            for ( int pathNo = 0; pathNo < indexPaths.length; pathNo++ ) {
                Index fullIndex = JandexTestUtils.readFullIndex(
                    indexPaths[pathNo],
                    new ByteArrayInputStream(indexBytes[pathNo]));
                fullIndexes.add(fullIndex);

                if ( iterationNo == 0 ) {
                    totalClasses += fullIndex.getKnownClasses().size();
                }
            }

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

            try {
            	Thread.sleep(1000L);
            } catch ( InterruptedException e ) {
            	// Ignore
            }
        }

        displayTimes(
            "Full Index Read",
            indexPaths, totalClasses,
            iterations,
            readTimes, minDuration, maxDuration, totalDuration);

        displayHeaps(
            "Full Index Read",
            indexPaths, totalClasses,
            iterations, heapStats);
    }

    // @Test
    public void testSparseReads() {
        int iterations = JandexTestData.FAT_CDI_ITERATIONS;

        List<Long> readTimes = new ArrayList<Long>(iterations);
        List<HeapStats> heapStats = new ArrayList<HeapStats>(iterations + 1);

        long minDuration = -1L;
        long maxDuration = 0L;
        long totalDuration = 0L;

        heapStats.add( new HeapStats() );

        int totalClasses = 0;

        for ( int iterationNo = 0; iterationNo < iterations; iterationNo++ ) {
            List<SparseIndex> sparseIndexes = new ArrayList<SparseIndex>(indexPaths.length);
            sparseData.add(sparseIndexes);

            long readStart = System.nanoTime();

            for ( int pathNo = 0; pathNo < indexPaths.length; pathNo++ ) {
                SparseIndex sparseIndex = JandexTestUtils.readSparseIndex(
                    indexPaths[pathNo],
                    new ByteArrayInputStream(indexBytes[pathNo]));
                sparseIndexes.add(sparseIndex);

                if ( iterationNo == 0 ) {
                    totalClasses += sparseIndex.getKnownClasses().size();
                }
            }

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

            try {
            	Thread.sleep(1000L);
            } catch ( InterruptedException e ) {
            	// Ignore
            }
        }

        displayTimes(
            "Sparse Index Read",
            indexPaths, totalClasses,
            iterations,
            readTimes, minDuration, maxDuration, totalDuration);

        displayHeaps(
            "Sparse Index Read",
            indexPaths, totalClasses,
            iterations,
            heapStats);
    }

    public void displayTimes(
        String description,
        String[] paths, int totalClasses,
        int iterations,
        List<Long> readTimes, long minDuration, long maxDuration, long totalDuration) {

        System.out.println(description);
        System.out.println("Read [ " + Integer.valueOf(paths.length) + " (paths) ]");
        System.out.println("     [ " + Integer.valueOf(totalClasses) + " (classes) ]");
        System.out.println("     [ " + Integer.valueOf(iterations) + " (iterations) ]");

        minDuration /= JandexTestUtils.NANOS_IN_MILLI;
        maxDuration /= JandexTestUtils.NANOS_IN_MILLI;
        totalDuration /= JandexTestUtils.NANOS_IN_MILLI;
        long avgDuration = totalDuration / iterations;

        System.out.println(
            "   Avg [ " + Long.valueOf(avgDuration) + " (milli) ]" +
            " Min [ " + Long.valueOf(minDuration) + " (milli) ]" +
            " Max [ " + Long.valueOf(maxDuration) + " (milli) ]" +
            " Tot [ " + Long.valueOf(totalDuration) + " (milli) ]");
    }

    private void displayHeaps(
        String description,
        String[] paths, int numClasses,
        int iterations,
        List<HeapStats> heapStats) {

        // System.out.println(description);
        // System.out.println("  Read [ " + path + " (path) ] [ " + Integer.valueOf(numClasses) + " (classes) ]");
        // System.out.println("  [ " + Integer.valueOf(iterations) + " (iterations) ]");

        int heapSize = heapStats.size();
        for ( int heapNo = 0; heapNo < heapSize; heapNo++ ) {
            HeapStats nextHeapStats = heapStats.get(heapNo);

            System.out.println(
                "     [ " + Integer.valueOf(heapNo) + " ]" +
                " Max [ " + Long.valueOf(nextHeapStats.maxSize / JandexTestUtils.BYTES_IN_MEGABYTE) + " (MB) ]" +
                " Cur [ " + Long.valueOf(nextHeapStats.currentSize / JandexTestUtils.BYTES_IN_MEGABYTE) + " (MB) ]" +
                " Tot [ " + Long.valueOf(nextHeapStats.freeSize / JandexTestUtils.BYTES_IN_MEGABYTE) + " (MB) ]");
        }
    }
}

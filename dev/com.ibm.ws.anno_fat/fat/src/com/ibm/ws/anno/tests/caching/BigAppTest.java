/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.anno.tests.caching;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;

/**
 * 
 * These tests to verify that updates to the <absolute-ordering> element in the web.xml
 * are noticed after server restarts.
 *
 */
public class BigAppTest extends CachingTest {
    private static final Logger LOG = Logger.getLogger(BigAppTest.class.getName());
    
    private static final int REPEATS = 3;
    
    private static long timedCleanStarts_avg = 0;
    private static long timedCleanStarts_max = 0;
    private static long timedCleanStarts_min = 0;
    
    private static long timedCleanStarts_CacheDisabled_avg = 0;
    private static long timedCleanStarts_CacheDisabled_max = 0;
    private static long timedCleanStarts_CacheDisabled_min = 0;
    
    private static long timedDirtyStarts_CacheDisabled_avg = 0;
    private static long timedDirtyStarts_CacheDisabled_max = 0;
    private static long timedDirtyStarts_CacheDisabled_min = 0;
    
    private static long timedDirtyStarts_avg = 0;
    private static long timedDirtyStarts_max = 0;
    private static long timedDirtyStarts_min = 0;
      
    private static long timedAppUpdate_CacheDisabled_avg = 0;
    private static long timedAppUpdate_CacheDisabled_max = 0;
    private static long timedAppUpdate_CacheDisabled_min = 0;
    
    private static long timedAppUpdate_avg = 0;
    private static long timedAppUpdate_max = 0;
    private static long timedAppUpdate_min = 0;

    private static long oneWriteThreadCacheDisabled_avg = 0;
    private static long oneWriteThreadCacheDisabled_max = 0;
    private static long oneWriteThreadCacheDisabled_min = 0;
    
    private static long oneWriteThread_avg = 0;
    private static long oneWriteThread_max = 0;
    private static long oneWriteThread_min = 0;
    
    private static long unlimitedWriteThreads_CacheDisabled_avg = 0;
    private static long unlimitedWriteThreads_CacheDisabled_max = 0;
    private static long unlimitedWriteThreads_CacheDisabled_min = 0;
    
    private static long unlimitedWriteThreads_avg = 0;
    private static long unlimitedWriteThreads_max = 0;
    private static long unlimitedWriteThreads_min = 0;
    
    private static long oneScanThread_CacheDisabled_avg = 0;
    private static long oneScanThread_CacheDisabled_max = 0;
    private static long oneScanThread_CacheDisabled_min = 0;
    
    private static long oneScanThread_avg = 0;
    private static long oneScanThread_max = 0;
    private static long oneScanThread_min = 0;
    
    private static long unlimitedScanThreads_CacheDisabled_avg = 0;
    private static long unlimitedScanThreads_CacheDisabled_max = 0;
    private static long unlimitedScanThreads_CacheDisabled_min = 0;
    
    private static long unlimitedScanThreads_avg = 0;
    private static long unlimitedScanThreads_max = 0;
    private static long unlimitedScanThreads_min = 0;
    
    enum ServerStartType 
    { 
        NONE, CLEAN, DIRTY; 
    }
    //

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("setUp BigAppTest");
         
        setEarName("big-cdi-meetings.ear");
        setSharedServer();
        File destinationDir = getInstalledAppDir();
        String destinationDirName = "apps";
        String sourceEarDirName = "test-applications";
        
        LOG.info("Copying:\nFrom: " + sourceEarDirName + "\nTo: " + destinationDir.getAbsolutePath());
        libertyServer.copyFileToLibertyServerRoot(sourceEarDirName, destinationDirName, earFileName);
                
        installServerXml("big-cdi-meetings_server.xml"); 
        
        //timedStartServerClean("setup - Initial Clean Start - no Cache");  // no cache

        LOG.info("setUp exit");
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        LOG.info("cleanUp");

        LOG.info("Iterations for each test = " + REPEATS + "\n");
               
        LOG.info("timedCleanStarts (Cache Disabled) MAX = " + TimeUnit.NANOSECONDS.toMillis(timedCleanStarts_CacheDisabled_max));
        LOG.info("timedCleanStarts (Cache Disabled) MIN = " + TimeUnit.NANOSECONDS.toMillis(timedCleanStarts_CacheDisabled_min));
        LOG.info("timedCleanStarts (Cache Disabled) AVG = " + TimeUnit.NANOSECONDS.toMillis(timedCleanStarts_CacheDisabled_avg) + "\n");  
        
        LOG.info("timedCleanStarts (with Cache) MAX = " + TimeUnit.NANOSECONDS.toMillis(timedCleanStarts_max));
        LOG.info("timedCleanStarts (with Cache) MIN = " + TimeUnit.NANOSECONDS.toMillis(timedCleanStarts_min));
        LOG.info("timedCleanStarts (with Cache) AVG = " + TimeUnit.NANOSECONDS.toMillis(timedCleanStarts_avg) + "\n");
            
        LOG.info("timedDirtyStarts (Cache Disabled) MAX = " + TimeUnit.NANOSECONDS.toMillis(timedDirtyStarts_CacheDisabled_max));
        LOG.info("timedDirtyStarts (Cache Disabled) MIN = " + TimeUnit.NANOSECONDS.toMillis(timedDirtyStarts_CacheDisabled_min));
        LOG.info("timedDirtyStarts (Cache Disabled) AVG = " + TimeUnit.NANOSECONDS.toMillis(timedDirtyStarts_CacheDisabled_avg) + "\n");        
        
        LOG.info("timedDirtyStarts (with Cache) MAX = " + TimeUnit.NANOSECONDS.toMillis(timedDirtyStarts_max));
        LOG.info("timedDirtyStarts (with Cache) MIN = " + TimeUnit.NANOSECONDS.toMillis(timedDirtyStarts_min));
        LOG.info("timedDirtyStarts (with Cache) AVG = " + TimeUnit.NANOSECONDS.toMillis(timedDirtyStarts_avg));
        
        logPerformanceChangePercent(timedDirtyStarts_CacheDisabled_avg, timedDirtyStarts_avg, "Dirty Start (Cache Disabled)", "Dirty Start (With Cache)");
        
        LOG.info("timedAppUpdate_CacheDisabled MAX = " + TimeUnit.NANOSECONDS.toMillis(timedAppUpdate_CacheDisabled_max));
        LOG.info("timedAppUpdate_CacheDisabled MIN = " + TimeUnit.NANOSECONDS.toMillis(timedAppUpdate_CacheDisabled_min));
        LOG.info("timedAppUpdate_CacheDisabled AVG = " + TimeUnit.NANOSECONDS.toMillis(timedAppUpdate_CacheDisabled_avg) + "\n");
        
        LOG.info("timedAppUpdate (with Cache) MAX = " + TimeUnit.NANOSECONDS.toMillis(timedAppUpdate_max));
        LOG.info("timedAppUpdate (with Cache) MIN = " + TimeUnit.NANOSECONDS.toMillis(timedAppUpdate_min));
        LOG.info("timedAppUpdate (with Cache) AVG = " + TimeUnit.NANOSECONDS.toMillis(timedAppUpdate_avg));
        
        logPerformanceChangePercent(timedAppUpdate_CacheDisabled_avg, timedAppUpdate_avg, "App Update (Cache Disabled)", "App Update (With Cache)");       
        
        LOG.info("oneWriteThread_CacheDisabled MAX = " + TimeUnit.NANOSECONDS.toMillis(oneWriteThreadCacheDisabled_max));
        LOG.info("oneWriteThread_CacheDisabled MIN = " + TimeUnit.NANOSECONDS.toMillis(oneWriteThreadCacheDisabled_min));
        LOG.info("oneWriteThread_CacheDisabled AVG = " + TimeUnit.NANOSECONDS.toMillis(oneWriteThreadCacheDisabled_avg) + "\n");
        
        LOG.info("oneWriteThread (with Cache) MAX = " + TimeUnit.NANOSECONDS.toMillis(oneWriteThread_max));
        LOG.info("oneWriteThread (with Cache) MIN = " + TimeUnit.NANOSECONDS.toMillis(oneWriteThread_min));
        LOG.info("oneWriteThread (with Cache) AVG = " + TimeUnit.NANOSECONDS.toMillis(oneWriteThread_avg));
        
        logPerformanceChangePercent(oneWriteThreadCacheDisabled_avg, oneWriteThread_avg, "One Write Thread (Cache Disabled)", "One Write Thread (With Cache)");
        
        LOG.info("unlimitedWriteThreads_CacheDisabled MAX = " + TimeUnit.NANOSECONDS.toMillis(unlimitedWriteThreads_CacheDisabled_max));
        LOG.info("unlimitedWriteThreads_CacheDisabled MIN = " + TimeUnit.NANOSECONDS.toMillis(unlimitedWriteThreads_CacheDisabled_min));
        LOG.info("unlimitedWriteThreads_CacheDisabled AVG = " + TimeUnit.NANOSECONDS.toMillis(unlimitedWriteThreads_CacheDisabled_avg) + "\n");
        
        LOG.info("unlimitedWriteThreads (with Cache) MAX = " + TimeUnit.NANOSECONDS.toMillis(unlimitedWriteThreads_max));
        LOG.info("unlimitedWriteThreads (with Cache) MIN = " + TimeUnit.NANOSECONDS.toMillis(unlimitedWriteThreads_min));
        LOG.info("unlimitedWriteThreads (with Cache) AVG = " + TimeUnit.NANOSECONDS.toMillis(unlimitedWriteThreads_avg));
        
        logPerformanceChangePercent(unlimitedWriteThreads_CacheDisabled_avg, unlimitedWriteThreads_avg, "Unlimited Write Threads (Cache Disabled)", "Unlimited Write Threads (With Cache)");      
              
        LOG.info("oneScanThread_CacheDisabled MAX = " + TimeUnit.NANOSECONDS.toMillis(oneScanThread_CacheDisabled_max));
        LOG.info("oneScanThread_CacheDisabled MIN = " + TimeUnit.NANOSECONDS.toMillis(oneScanThread_CacheDisabled_min));
        LOG.info("oneScanThread_CacheDisabled AVG = " + TimeUnit.NANOSECONDS.toMillis(oneScanThread_CacheDisabled_avg) + "\n");
        
        LOG.info("oneScanThread (with Cache) MAX = " + TimeUnit.NANOSECONDS.toMillis(oneScanThread_max));
        LOG.info("oneScanThread (with Cache) MIN = " + TimeUnit.NANOSECONDS.toMillis(oneScanThread_min));
        LOG.info("oneScanThread (with Cache) AVG = " + TimeUnit.NANOSECONDS.toMillis(oneScanThread_avg));
        
        logPerformanceChangePercent(oneScanThread_CacheDisabled_avg, oneScanThread_avg, "One Scan Thread (Cache Disabled)", "One Scan Thread (With Cache)");        
        
        LOG.info("unlimitedScanThreads_CacheDisabled MAX = " + TimeUnit.NANOSECONDS.toMillis(unlimitedScanThreads_CacheDisabled_max));
        LOG.info("unlimitedScanThreads_CacheDisabled MIN = " + TimeUnit.NANOSECONDS.toMillis(unlimitedScanThreads_CacheDisabled_min));
        LOG.info("unlimitedScanThreads_CacheDisabled AVG = " + TimeUnit.NANOSECONDS.toMillis(unlimitedScanThreads_CacheDisabled_avg) + "\n");
        
        LOG.info("unlimitedScanThreads (with Cache) MAX = " + TimeUnit.NANOSECONDS.toMillis(unlimitedScanThreads_max));
        LOG.info("unlimitedScanThreads (with Cache) MIN = " + TimeUnit.NANOSECONDS.toMillis(unlimitedScanThreads_min));
        LOG.info("unlimitedScanThreads (with Cache) AVG = " + TimeUnit.NANOSECONDS.toMillis(unlimitedScanThreads_avg));
        
        logPerformanceChangePercent(unlimitedScanThreads_CacheDisabled_avg, unlimitedScanThreads_avg, "Unlimited Scan Threads ((Cache Disabled)", "Unlimited Scan Threads (With Cache)");     
        
        stopServer();
        LOG.info("cleanUp complete");
    }
    
    /**
     * 
     * @param cacheDisabledAvg  - Average time for test with cached disabled
     * @param avg  -  Average time for test with cache
     * @param desc  - Description for test with cache
     * @param cacheDisabledDesc - Description for test with cache disabled
     */
    private static void logPerformanceChangePercent(double cacheDisabledAvg, 
                                                    double avg, 
                                                    String cacheDisabledDesc,
                                                    String desc) {
        
        String cacheResult = cacheDisabledAvg > avg ? "improvement" : "DEGRADATION";
        double diff = Math.abs(cacheDisabledAvg - avg);
        double percent = (diff / Math.max(cacheDisabledAvg, avg)) * 100;
        LOG.info("Percent " + cacheResult + " [ " + desc + " vs " + cacheDisabledDesc + " ]: "+  String.format("%.2f", percent)  + "\n");
    }
    
    
    /**
     * Clean start.  Wipes out any existing cache.  Creates a new cache.
     * @param message
     * @return
     * @throws Exception
     */
    private static long timedStartServerClean(String message) throws Exception {
        
        long lStartTime = System.nanoTime();
        startServerClean();
        long lEndTime = System.nanoTime();        
        long elapsed = lEndTime - lStartTime;
        
        logBlock(message); 
        LOG.info("Server started in milliseconds: " + TimeUnit.NANOSECONDS.toMillis(elapsed)  + " - Clean start");        

        stopServer();
        return elapsed;
    }
    
    /**
     * Dirty start.  If there is an existing cache, it is NOT deleted, but could be updated during server start.
     * @param message
     * @return
     * @throws Exception
     */
    private static long timedStartServerDirty(String message) throws Exception{
        
        long lStartTime = System.nanoTime();
        startServerDirty();
        long lEndTime = System.nanoTime();        
        long elapsed = lEndTime - lStartTime;
        
        logBlock(message); 
        LOG.info("Server started in milliseconds: " + TimeUnit.NANOSECONDS.toMillis(elapsed) + " - Dirty start");        

        stopServer();
        return elapsed;
    }
    
    /**
     * Clean start.  NEW CACHE WILL BE WRITTEN.
     * @throws Exception
     */
    @Test
    public void testCleanStart() throws Exception {
        
        installJvmOptions(null);  // no jvm.options
        
        long time, max, min, sum;
        time = max = min = sum = 0;
        
        for (int i = 1; i <= REPEATS; i++) {
            time = timedStartServerClean("Test CLEAN server start - clear cache, iteration [" + i + "]");
            sum += time;
            
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time : min;        
        }
        timedCleanStarts_max = max;
        timedCleanStarts_min = min;
        timedCleanStarts_avg = sum / REPEATS;
    }
    
    /**
     * Clean start.  Wipes out any existing cache.  DOES NOT CREATE NEW CACHE.
     * This is as close as we can get to the behavior before caching function was added to Liberty.
     */
    @Test
    public void testCleanStart_cachingDisabled() throws Exception {
        
        installJvmOptions("JvmOptions_AnnoCacheDisabled_True.txt");
        
        long time, max, min, sum;
        time = max = min = sum = 0;
        
        for (int i = 1; i <= REPEATS; i++) {
            time = timedStartServerClean("Test CLEAN server start - Cache DISABLED, iteration [" + i + "]");
            sum += time;
            
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time : min;        
        }
        timedCleanStarts_CacheDisabled_max = max;
        timedCleanStarts_CacheDisabled_min = min;
        timedCleanStarts_CacheDisabled_avg = sum / REPEATS;
        
        installJvmOptions(null);
    }
    
    @Test
    public void testDirtyStart_cachingDisabled() throws Exception {
        
        installJvmOptions("JvmOptions_AnnoCacheDisabled_True.txt");
        
        long time, max, min, sum;
        time = max = min = sum = 0;
        
        for (int i = 1; i <= REPEATS; i++) {
            time = timedStartServerDirty("Test DIRTY server start - Cache DISABLED, iteration [" + i + "]");
            sum += time;
            
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time : min;        
        }
        timedDirtyStarts_CacheDisabled_max = max;
        timedDirtyStarts_CacheDisabled_min = min;
        timedDirtyStarts_CacheDisabled_avg = sum / REPEATS;
        
        installJvmOptions(null);
    }
    
    /**
     * Will first create a cache by doing a clean start.  The repeatedly do dirty server starts.
     * The idea is to test the start time when a cache exists.
     */
    @Test
    public void testDirtyStart_Cache() throws Exception {
        
        // Ensure there is an existing cache by creating one.
        installJvmOptions(null);  // no jvm.options       
        startServerClean();  // create cache
        stopServer();
        
        long time, max, min, sum;
        time = max = min = sum = 0;    
        
        for (int i = 1; i <= REPEATS; i++) {
            time = timedStartServerDirty("Test DIRTY server start with existing cache, iteration [" + i + "]");     
            
            sum += time;
            
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time :min; 
        }
        timedDirtyStarts_max = max;
        timedDirtyStarts_min = min;
        timedDirtyStarts_avg = sum / REPEATS;
    }
    
    /**
     * Test is to remove a JAR from an app by renaming it to something other than .jar.  Time how long it takes for 
     * the app update to get noticed by the server.  Then restore the JAR to the app again by renaming, and time 
     * the update again.
     * 
     */
    @Test
    public void testAppUpdate_CacheDisabled() throws Exception { 
        
        installJvmOptions("JvmOptions_AnnoCacheDisabled_True.txt");
        startServerClean();  // Start server outside the loop - rely on automatic app updates inside the loop
        
        long time, max, min, sum;
        time = max = min = sum = 0;
            
        for (int i = 1; i <= REPEATS; i++) {

            // Remove a jar and time how long it takes for the app update to complete.
            time = timedRemoveJar("pmt.jar", (new Integer(i)).toString());
            sum += time;
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time : min;   
            
            // Add a jar and time how long it takes for the app update to complete.
            time = timedAddJar("pmt.jar", (new Integer(i)).toString());
            sum += time;
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time : min; 
 
        } 
        stopServer();
        
        timedAppUpdate_CacheDisabled_max = max;
        timedAppUpdate_CacheDisabled_min = min;
        timedAppUpdate_CacheDisabled_avg = sum / (REPEATS * 2);
    }
    
    @Test
    public void testAppUpdate_Cache() throws Exception { 
        installJvmOptions(null);
        startServerClean();  // Start server outside the loop - rely on automatic app updates inside the loop
        
        long time, max, min, sum;
        time = max = min = sum = 0;
        
        for (int i = 1; i <= REPEATS; i++) {
            // Remove a jar and time how long it takes for the app update to complete.
            time = timedRemoveJar("pmt.jar", (new Integer(i)).toString());
            sum += time;
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time : min;   
            
            // Add a jar and time how long it takes for the app update to complete.
            time = timedAddJar("pmt.jar", (new Integer(i)).toString());
            sum += time;
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time : min; 
        } 
        stopServer();
        
        timedAppUpdate_max = max;
        timedAppUpdate_min = min;
        timedAppUpdate_avg = sum / (REPEATS * 2);
    }
    
    public long timedRemoveJar(String jarName, String iteration) throws Exception {
        
        logBlock("Removing Jar - Waiting for Update to be noticed, iteration [" + iteration + "]");

        // Renaming to something other than JAR is same as removing the JAR.
        renameJarFileInApplication("big-cdi-meetings.war", jarName, jarName + "_backup");

        long elapsedTime = waitForAppUpdateToBeNoticed();       

        return elapsedTime;
    }
    
    /**
     * This method is meant to restore the JAR "removed" by timedRemoveJar, and assumes the
     * jar name ends with "_backup".
     * 
     * @param jarName
     * @param iteration
     * @return
     * @throws Exception
     */
    public long timedAddJar(String jarName, String iteration) throws Exception {
        logBlock("Adding Jar - Waiting for Update to be noticed, iteration [" + iteration + "]");

        renameJarFileInApplication("big-cdi-meetings.war", jarName + "_backup", jarName);
        
        long elapsedTime = waitForAppUpdateToBeNoticed();

        return elapsedTime;
    }
    
    @Test
    public void testOneWriteThread() throws Exception {
              
        installJvmOptions("JvmOptions_AnnoCacheDisabled_WriteThreads_1.txt");  // DISABLE CACHE, 1 write thread
        
        long time, max, min, sum;
        time = max = min = sum = 0;
         
        for (int i = 1; i <= REPEATS; i++) {
            time = timedStartServerDirty("1 Write Thread - No cache, [" + i + "]");
            sum += time;
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time : min;  
        }
        oneWriteThreadCacheDisabled_max = max; 
        oneWriteThreadCacheDisabled_min = min;  
        oneWriteThreadCacheDisabled_avg = sum / REPEATS;

        // ---
        
        installJvmOptions(null);   // ENABLE CACHE
        startServerClean();        // create cache
        stopServer();
        
        time = max = min = sum = 0;
        
        for (int i = 1; i <= REPEATS; i++) {
            time = timedStartServerDirty("1 Write Thread - Cache, [" + i + "]");
            sum += time;
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time : min; 
        }
        oneWriteThread_max = max;
        oneWriteThread_min = min;
        oneWriteThread_avg = sum / REPEATS;
    }
           
    @Test
    public void testUnlimitedWriteThreads() throws Exception {
        installJvmOptions("JvmOptions_AnnoCacheDisabled_WriteThreads_-1.txt");  // DISABLE CACHE, unlimited write threads
        
        long time, max, min, sum;
        time = max = min = sum = 0;
        
        for (int i = 1; i <= REPEATS; i++) {
            time = timedStartServerDirty("Unlimited Write Threads - No Cache, [" + i + "]");
            sum += time;
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time : min; 
        }
        unlimitedWriteThreads_CacheDisabled_max = max; 
        unlimitedWriteThreads_CacheDisabled_min = min;  
        unlimitedWriteThreads_CacheDisabled_avg = sum / REPEATS;
        
        // ---
        
        installJvmOptions(null);   // ENABLE CACHE
        startServerClean();        // create cache
        stopServer();

        time = max = min = sum = 0;
        
        for (int i = 1; i <= REPEATS; i++) {
            time = timedStartServerDirty("Unlimited Write Threads  - existing cache, [" + i + "]");
            sum += time;
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time : min;
        }
        unlimitedWriteThreads_max = max;
        unlimitedWriteThreads_min = min;
        unlimitedWriteThreads_avg = sum / REPEATS;
    }
    
    @Test
    public void testOneScanThread() throws Exception {
        installJvmOptions("JvmOptions_AnnoCacheDisabled_ScanThreads_1.txt");  // DISABLE CACHE, one scan threads
        
        long time, max, min, sum;
        time = max = min = sum = 0;
        
        for (int i = 1; i <= REPEATS; i++) {
            time = timedStartServerDirty("1 Scan Thread - No cache, [" + i + "]");
            sum += time;
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time : min;
        }
        oneScanThread_CacheDisabled_max = max;
        oneScanThread_CacheDisabled_min = min;
        oneScanThread_CacheDisabled_avg = sum / REPEATS;
        
        // ---
        
        installJvmOptions(null);   // ENABLE CACHE
        startServerClean();        // create cache
        stopServer();
        
        time = max = min = sum = 0;
        
        for (int i = 1; i <= REPEATS; i++) {
            time = timedStartServerDirty("1 Scan Thread - Cache, [" + i + "]");
            sum += time;
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time : min;
        }
        oneScanThread_max = max;
        oneScanThread_min = min;
        oneScanThread_avg = sum / REPEATS;
    }
    
    @Test
    public void testUnlimitedScanThreads() throws Exception {
        installJvmOptions("JvmOptions_AnnoCacheDisabled_ScanThreads_-1.txt");  // DISABLE CACHE, unlimited scan threads
        
        long time, max, min, sum;
        time = max = min = sum = 0;
        
        for (int i = 1; i <= REPEATS; i++) {
            time = timedStartServerDirty("Unlimited Scan Threads - No Cache, [" + i + "]");
            sum += time;
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time : min;
        }
        unlimitedScanThreads_CacheDisabled_max = max;
        unlimitedScanThreads_CacheDisabled_min = min;
        unlimitedScanThreads_CacheDisabled_avg = sum / REPEATS;
        
        // ---
        
        installJvmOptions(null);   // ENABLE CACHE
        startServerClean();        // create cache
        stopServer();
        
        time = max = min = sum = 0;
        
        for (int i = 1; i <= REPEATS; i++) {
            time = timedStartServerDirty("Unlimited Scan Threads  - existing cache, [" + i + "]");
            sum += time;
            max = (time > max) ?  time : max;
            min = (min == 0 || time < min) ? time : min;
        }
        unlimitedScanThreads_max = max;
        unlimitedScanThreads_min = min;
        unlimitedScanThreads_avg = sum / REPEATS;
    }
}



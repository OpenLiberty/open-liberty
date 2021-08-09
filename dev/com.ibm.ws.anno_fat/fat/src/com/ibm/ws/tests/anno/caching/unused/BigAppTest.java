/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.tests.anno.caching.unused;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.tests.anno.caching.AnnoCachingTest;

import componenttest.custom.junit.runner.Mode;

/**
 * Comprehensive test of caching options, including collection of
 * timing data.
 */
@Mode(Mode.TestMode.FULL)
public class BigAppTest extends AnnoCachingTest {
    private static final int ITERATIONS = 3;

    public static class TimedStart {
        public int iterations;
        public long sum, min, max, avg;

        public void add(long newTime) {
            if ( iterations == 0 ) {
                min = newTime;
                max = newTime;
            } else if ( newTime < min ) {
                min = newTime;
            } else if ( newTime > max ) {
                max = newTime;
            }

            iterations++;
            sum += newTime;
            avg = sum / iterations;
        }
    }

    public static enum CacheSetting {
        PRE_BETA,
        POST_BETA_DISABLED,
        POST_BETA_ENABLED
    }

    public static enum StartCase {
        DISABLED_PRE_BETA_SCRUBBING(
            CacheSetting.PRE_BETA,
            ServerStartType.DO_SCRUB,
            "Pre-Cache Scrubbing"),
        DISABLED_PRE_BETA(CacheSetting.PRE_BETA, "Pre-Cache"),

        DISABLED_SCRUBBING(
            CacheSetting.POST_BETA_DISABLED,
            ServerStartType.DO_SCRUB,
            "Disabled Scrubbing"),
        DISABLED(CacheSetting.POST_BETA_DISABLED, "Disabled"),

        // The remaining starts are unscrubbed.
        // The remaining starts are unpopulated unless indicated otherwise.

        ENABLED(CacheSetting.POST_BETA_ENABLED, "Enabled"),
        ENABLED_BINARY(CacheSetting.POST_BETA_ENABLED, "Enabled Binary Format"),
        ENABLED_JANDEX(CacheSetting.POST_BETA_ENABLED, "Enabled Using Jandex Format"),

        ENABLED_POPULATED(CacheSetting.POST_BETA_ENABLED, "Enabled Populated"),
        ENABLED_POPULATED_BINARY(CacheSetting.POST_BETA_ENABLED, "Enabled Populated Binary Format"),
        ENABLED_POPULATED_JANDEX(CacheSetting.POST_BETA_ENABLED, "Enabled Populated Using Jandex Format"),
        ENABLED_POPULATED_BINARY_JANDEX(CacheSetting.POST_BETA_ENABLED, "Enabled Populated Binary and Jandex Format"),
        // ENABLED_POPULATED_BINARY_JANDEX_FULL(CacheSetting.POST_BETA_ENABLED, "Enabled Populated Binary and Jandex Full Format"),        
        
        ENABLED_POPULATED_VALID(CacheSetting.POST_BETA_ENABLED, "Enabled Populated Assumed Valid"),
        ENABLED_POPULATED_BINARY_VALID(CacheSetting.POST_BETA_ENABLED, "Enabled Populated Binary Format Assumed Valid"),
        ENABLED_POPULATED_BINARY_JANDEX_VALID(CacheSetting.POST_BETA_ENABLED, "Enabled Populated Binary and Jandex Format Assumed Valid"),

        //

        DISABLED_MULTI_SCAN(CacheSetting.POST_BETA_DISABLED, "Disabled Multiple Scan Threads"),
        DISABLED_UNLIMITED_SCAN(CacheSetting.POST_BETA_DISABLED, "Disabled Unlimited Scan Threads"),

        ENABLED_MULTI_SCAN(CacheSetting.POST_BETA_DISABLED, "Enabled Multiple Scan Threads"),
        ENABLED_UNLIMITED_SCAN(CacheSetting.POST_BETA_DISABLED, "Enabled Unlimited Scan Threads"),

        ENABLED_MULTI_WRITE(CacheSetting.POST_BETA_ENABLED, "Enabled Multiple Write Thread"),
        ENABLED_UNLIMITED_WRITE(CacheSetting.POST_BETA_ENABLED, "Enabled Unlimited Write Threads"),

        //

        DISABLED_UPDATE(CacheSetting.POST_BETA_DISABLED, "Disabled Application Update"),
        ENABLED_UPDATE(CacheSetting.POST_BETA_ENABLED, "Enabled Application Update"),
        ENABLED_UPDATE_BINARY(CacheSetting.POST_BETA_ENABLED, "Enabled Application Update Binary Format"),
        ENABLED_UPDATE_JANDEX(CacheSetting.POST_BETA_ENABLED, "Enabled Application Update Jandex Format");

        private StartCase(CacheSetting cacheSetting, String description) {
            this(cacheSetting, ServerStartType.DO_NOT_SCRUB, description);
        }

        private StartCase(CacheSetting cacheSetting, ServerStartType startType, String description) {
            this.cacheSetting = cacheSetting;
            this.startType = startType;
            this.description = description;
        }

        public final CacheSetting cacheSetting;

        public CacheSetting getCacheSetting() {
            return cacheSetting;
        }

        public final ServerStartType startType;
        
        public ServerStartType getStartType() {
            return startType;
        }

        public final String description;

        public String getDescription() {
            return description;
        }
    }

    private static EnumMap<StartCase, TimedStart> timingData =
        new EnumMap<StartCase, TimedStart>(StartCase.class);

    static {
        for ( StartCase startCase : StartCase.values() ) {
            timingData.put( startCase, new TimedStart() );
        }
    }

    public static void add(StartCase startCase, long time) {
        timingData.get(startCase).add(time);

        info(startCase.description + ": Time: " + nsToMs(time) + "");
    }

    public static void log(StartCase startCase, TimedStart timedStart) {
        info(startCase.description);
        info("  Max " + format( nsToMs(timedStart.max) ) + " (ms)");
        info("  Min " + format( nsToMs(timedStart.min) ) + " (ms)");
        info("  Avg " + format( nsToMs(timedStart.avg) ) + " (ms)");
    }

    private static void logDifference(StartCase initialCase, StartCase finalCase) {
        TimedStart initialData = timingData.get(initialCase);
        TimedStart finalData = timingData.get(finalCase);

        long initialAvg = nsToMs(initialData.avg);
        long finalAvg = nsToMs(finalData.avg);

        long diff = finalAvg - initialAvg; 
        long pct = ( (initialAvg == 0) ? 0 : ((diff * 100) / initialAvg) );

        info("Comparing:");
        info("  " + format(initialAvg) + " (ms) : " + initialCase.description);
        info("  " + format(finalAvg)   + " (ms) : " + finalCase.description);
        info("  " + format(diff)       + " (ms) : " + "(difference)");
        info("  " + format(pct)        + " (%)");
    }

    /**
     * Collect timing data for server starts.  The server is not
     * cleared between starts.
     *
     * @param startCase The case to which to record the timing data.
     *
     * @throws Exception Thrown if the server start fails.
     */
    private static void collect(StartCase startCase) throws Exception {
        info("Collect: " + startCase + ": iterations: " + Integer.toString(ITERATIONS));

        for ( int startNo = 0; startNo < ITERATIONS; startNo++ ) {
            long time = cycleServer(startCase);
            add(startCase, time);
        }
    }

    /**
     * Collect timing data for server starts.  Do a clean start with
     * the cache disabled before each timed server start.
     * 
     * @param startCase The case to which to record the timing data.
     * @param jvmOptions JVM options file to use for the timed server start.
     * 
     * @throws Exception Thrown if the server start fails.
     */
    private static void collect(StartCase startCase, String jvmOptions) throws Exception {
        info("Collect: " + startCase + ": with: " + jvmOptions + ": iterations: " + Integer.toString(ITERATIONS));

        for ( int startNo = 0; startNo < ITERATIONS; startNo++ ) {
            scrubServer(startCase);

            installJvmOptions(jvmOptions);
            long time = cycleServer(startCase);
            add(startCase, time);
        }
    }

    //

    public static final String EAR_NAME = "big-cdi-meetings.ear";

    @BeforeClass
    public static void setUp() throws Exception {
        info("setUp ENTER BigAppTest");

        setSharedServer();
        info("setup: Server: " + getServerName());

        installJvmOptions("JvmOptions_Enabled.txt");

        String sourceAppsDirName = "test-applications";

        File destAppsDir = getInstalledAppsDir();
        String destAppsDirName = destAppsDir.getName();

        info("setUp: Copy: " + EAR_NAME + " from: " + sourceAppsDirName + " to: " + destAppsDirName); 

        getLibertyServer().copyFileToLibertyServerRoot(sourceAppsDirName, destAppsDirName, EAR_NAME);

        setEarName(EAR_NAME);
        installServerXml("big-cdi-meetings_server.xml"); 

        info("setUp RETURN BigAppTest");
    }

    public static final String THICK_BANNER = "==================================================";
    public static final String THIN_BANNER  = "--------------------------------------------------";

    // DISABLED_SCRUBBED - DISABLED
    //
    // DISABLED - ENABLED
    // ENABLED - ENABLED_BINARY
    // ENABLED - ENABLED_JANDEX
    //
    // DISABLED - ENABLED_POPULATED
    // ENABLED_POPULATED - ENABLED_POPULATED_BINARY
    // ENABLED_POPULATED - ENABLED_POPULATED_JANDEX
    //
    // ENABLED_POPULATED - ENABLED_POPULATED_VALID
    // ENABLED_POPULATED_BINARY - ENABLED_POPULATED_BINARY_VALID
    //
    // DISABLED - DISABLED_MULTI_SCAN
    // DISABLED - DISABLED_UNLIMITED_SCAN
    // DISABLED - ENABLED_ONE_WRITE
    // DISABLED - ENABLED_UNLIMITED_WRITE
    //
    // ENABLED - ENABLED_MULTI_SCAN
    // ENABLED - ENABLED_UNLIMITED_SCAN
    //
    // DISABLED - DISABLED_UPDATE
    // DISABLED_UPDATE - ENABLED_UPDATE
    // ENABLED_UPDATE - ENABLED_UPDATE_BINARY
    // ENABLED_UPDATE - ENABLED_UPDATE_JANDEX

    @AfterClass
    public static void cleanUp() throws Exception {
        info("cleanUp ENTER");

        info(THICK_BANNER);

        info(THIN_BANNER);
        info("Iterations: " + ITERATIONS);
        info(THIN_BANNER);
        for ( Map.Entry<StartCase, TimedStart> startData : timingData.entrySet() ) {
            log( startData.getKey(), startData.getValue() );
        }
        info(THIN_BANNER);

        info(THICK_BANNER);

        info("Differences:");

        info(THIN_BANNER);
        logDifference(StartCase.DISABLED_PRE_BETA_SCRUBBING, StartCase.DISABLED_SCRUBBING);
        logDifference(StartCase.DISABLED_PRE_BETA, StartCase.DISABLED_SCRUBBING); 
        
        info(THIN_BANNER);
        logDifference(StartCase.DISABLED_SCRUBBING, StartCase.DISABLED);
        logDifference(StartCase.DISABLED, StartCase.ENABLED);
        logDifference(StartCase.ENABLED, StartCase.ENABLED_BINARY);
        logDifference(StartCase.ENABLED, StartCase.ENABLED_JANDEX);

        info(THIN_BANNER);
        logDifference(StartCase.DISABLED, StartCase.ENABLED_POPULATED);
        logDifference(StartCase.ENABLED_POPULATED, StartCase.ENABLED_POPULATED_BINARY);
        logDifference(StartCase.ENABLED_POPULATED, StartCase.ENABLED_POPULATED_JANDEX);
        logDifference(StartCase.ENABLED_POPULATED, StartCase.ENABLED_POPULATED_BINARY_JANDEX);
        // logDifference(StartCase.ENABLED_POPULATED, StartCase.ENABLED_POPULATED_BINARY_JANDEX_FULL);

        info(THIN_BANNER);
        logDifference(StartCase.ENABLED_POPULATED, StartCase.ENABLED_POPULATED_VALID);
        logDifference(StartCase.ENABLED_POPULATED, StartCase.ENABLED_POPULATED_BINARY_VALID);

        // info(THIN_BANNER);
        // logDifference(StartCase.DISABLED, StartCase.DISABLED_MULTI_SCAN);
        // logDifference(StartCase.DISABLED, StartCase.DISABLED_UNLIMITED_SCAN);
        // logDifference(StartCase.DISABLED, StartCase.ENABLED_MULTI_WRITE);
        // logDifference(StartCase.DISABLED, StartCase.ENABLED_UNLIMITED_WRITE);


        // DISABLED - DISABLED_UPDATE
        // DISABLED_UPDATE - ENABLED_UPDATE
        // ENABLED_UPDATE - ENABLED_UPDATE_BINARY
        // ENABLED_UPDATE - ENABLED_UPDATE_JANDEX

        // info(THIN_BANNER);
        // logDifference( StartCase.DISABLED, StartCase.DISABLED_UPDATE );
        // logDifference( StartCase.DISABLED_UPDATE, StartCase.ENABLED_UPDATE );
        // logDifference( StartCase.ENABLED_UPDATE, StartCase.ENABLED_UPDATE_BINARY );
        // logDifference( StartCase.ENABLED_UPDATE, StartCase.ENABLED_UPDATE_JANDEX );

        info(THIN_BANNER);
        info(THICK_BANNER);

        info("cleanUp RETURN");
    }

    private static long cycleServer(StartCase startCase)
        throws Exception {

        long lStartTime = System.nanoTime();

        if ( startCase.startType == ServerStartType.DO_SCRUB ) {
            startServerScrub();
        } else {
            startServer();
        }

        long lEndTime = System.nanoTime();
        long elapsed = lEndTime - lStartTime;

        logBlock(startCase.description); 
        info("Server started: " + nsToMs(elapsed) + " (ms)");

        stopServer();

        return elapsed;
    }

    private static void scrubServer(StartCase startCase) throws Exception {
        if ( startCase.getCacheSetting() == CacheSetting.PRE_BETA ) {
            installJvmOptions("JvmOptions_PreBeta.txt");
        } else {
            installJvmOptions("JvmOptions_Disabled.txt");
        }

        startServerScrub();
        stopServer();
    }

    public static enum PopulateCase {
        TEXT("JvmOptions_Enabled.txt"),
        BINARY("JvmOptions_Enabled_Binary.txt"),
        JANDEX("JvmOptions_Enabled_Jandex.txt"),
        BINARY_JANDEX("JvmOptions_Enabled_Binary_Jandex.txt");

        private PopulateCase(String jvmOptions) {
            this.jvmOptions = jvmOptions;
        }

        private final String jvmOptions;

        public String getJvmOptions() {
            return jvmOptions;
        }
    }

    private static void populateServer(PopulateCase populateCase) throws Exception {
        installJvmOptions( populateCase.getJvmOptions() );
        startServerScrub();
        stopServer();
    }

    //

    // @Test
    public void bigApp_testPreBetaScrubbing() throws Exception {
        StartCase startCase = StartCase.DISABLED_PRE_BETA_SCRUBBING;
        scrubServer(startCase);
        collect(startCase);
    }

    // @Test
    public void bigApp_testPreBeta() throws Exception {
        StartCase startCase = StartCase.DISABLED_PRE_BETA;
        scrubServer(startCase);
        collect(startCase);
    }

    // @Test
    public void bigApp_testDisabledScrubbing() throws Exception {
        StartCase startCase = StartCase.DISABLED_SCRUBBING;
        scrubServer(startCase);
        installJvmOptions("JvmOptions_Disabled.txt");
        collect(startCase);
    }

    // @Test
    public void bigApp_testDisabled() throws Exception {
        StartCase startCase = StartCase.DISABLED;
        scrubServer(startCase);
        installJvmOptions("JvmOptions_Disabled.txt");
        collect(startCase);
    }

    //

    // @Test
    public void bigApp_testEnabled() throws Exception {
        collect(StartCase.ENABLED, "JvmOptions_Enabled.txt"); 
    }

    // @Test
    public void bigApp_testEnabledBinary() throws Exception {
        StartCase startCase = StartCase.ENABLED_BINARY;
        scrubServer(startCase);
        installJvmOptions("JvmOptions_Enabled_Binary.txt");
        collect(startCase);
    }

    // @Test
    public void bigApp_testEnabledJandex() throws Exception {
        StartCase startCase = StartCase.ENABLED_JANDEX;
        scrubServer(startCase);
        installJvmOptions("JvmOptions_Enabled_Jandex.txt");
        collect(startCase);
    }

    // @Test
    public void bigApp_testEnabledPopulated() throws Exception {
        populateServer(PopulateCase.TEXT);
        collect(StartCase.ENABLED_POPULATED);
    }

    // @Test
    public void bigApp_testEnabledPopulatedBinary() throws Exception {
        populateServer(PopulateCase.BINARY);
        collect(StartCase.ENABLED_POPULATED_BINARY);
    }

    // @Test
    public void bigApp_testEnabledPopulatedJandex() throws Exception {
        populateServer(PopulateCase.JANDEX);
        collect(StartCase.ENABLED_POPULATED_JANDEX);
    }

    // @Test
    public void bigApp_testEnabledPopulatedBinaryJandex() throws Exception {
        populateServer(PopulateCase.BINARY_JANDEX);
        collect(StartCase.ENABLED_POPULATED_BINARY_JANDEX);
    }

//    @Test
//    public void bigApp_testEnabledPopulatedBinaryJandexFull() throws Exception {
//        populateServer(PopulateCase.BINARY_JANDEX);
//        installJvmOptions("JvmOptions_Enabled_Binary_Jandex_Full.txt");
//        collect(StartCase.ENABLED_POPULATED_BINARY_JANDEX_FULL);
//    }

    //

    // @Test
    public void bigApp_testEnabledPopulatedValid() throws Exception {
        populateServer(PopulateCase.TEXT);
        installJvmOptions("JvmOptions_Enabled_AlwaysValid.txt");
        collect(StartCase.ENABLED_POPULATED_VALID);
    }

    // @Test
    public void bigApp_testEnabledPopulatedBinaryValid() throws Exception {
        populateServer(PopulateCase.BINARY);
        installJvmOptions("JvmOptions_Enabled_Binary_AlwaysValid.txt");
        collect(StartCase.ENABLED_POPULATED_BINARY_VALID);
    }

    //

    // @Test
    public void bigApp_testDisabledMultiScanThread() throws Exception {
        StartCase startCase = StartCase.DISABLED_MULTI_SCAN;

        scrubServer(startCase);
        installJvmOptions("JvmOptions_Disabled_ScanMulti.txt");
        collect(startCase);
    }

    // @Test
    public void bigApp_testEnabledOneScanThread() throws Exception {
        collect(StartCase.ENABLED_MULTI_SCAN, "JvmOptions_Enabled_ScanMulti.txt");
    }

    // @Test
    public void bigApp_testDisabledUnlimitedScanThreads() throws Exception {
        StartCase startCase = StartCase.DISABLED_UNLIMITED_SCAN;

        scrubServer(startCase);
        installJvmOptions("JvmOptions_Disabled_ScanUnlimited.txt");
        collect(startCase);
    }

    // @Test
    public void bigApp_testEnabledOneWriteThread() throws Exception {
        collect(StartCase.ENABLED_MULTI_WRITE, "JvmOptions_Enabled_WriteMulti.txt"); 
    }

    // @Test
    public void bigApp_testEnabledUnlimitedWriteThreads() throws Exception {
        collect(StartCase.ENABLED_UNLIMITED_WRITE, "JvmOptions_Enabled_WriteUnlimited.txt");
    }

    // @Test
    public void bigApp_testEnabledUnlimitedScanThreads() throws Exception {
        collect(StartCase.ENABLED_UNLIMITED_SCAN, "JvmOptions_Enabled_ScanUnlimited.txt");
    }

    //

    // @Test
    public void bigApp_testDisabledAppUpdate() throws Exception { 
        installJvmOptions("JvmOptions_Disabled.txt");
        startServerScrub();

        StartCase startCase = StartCase.DISABLED_UPDATE;

        for ( int startNo = 0; startNo < ITERATIONS; startNo++ ) {
            long removeTime = timedRemoveJar("pmt.jar");
            add(startCase, removeTime);

            long addTime = timedAddJar("pmt.jar");
            add(startCase, addTime);
        }

        stopServer();
    }

    // @Test
    public void bigApp_testEnabledAppUpdate() throws Exception { 
        installJvmOptions("JvmOptions_Enabled.txt");
        startServerScrub();

        StartCase startCase = StartCase.ENABLED_UPDATE;

        for ( int startNo = 0; startNo < ITERATIONS; startNo++ ) {
            long removeTime = timedRemoveJar("pmt.jar");
            add(startCase, removeTime);

            long addTime = timedAddJar("pmt.jar");
            add(startCase, addTime);
        }

        stopServer();
    }
    
    // @Test
    public void bigApp_testEnabledAppUpdateBinary() throws Exception { 
        installJvmOptions("JvmOptions_Enabled_Binary.txt");
        startServerScrub();

        StartCase startCase = StartCase.ENABLED_UPDATE_BINARY;

        for ( int startNo = 0; startNo < ITERATIONS; startNo++ ) {
            long removeTime = timedRemoveJar("pmt.jar");
            add(startCase, removeTime);

            long addTime = timedAddJar("pmt.jar");
            add(startCase, addTime);
        }

        stopServer();
    }

    // @Test
    public void bigApp_testEnabledAppUpdateJandex() throws Exception { 
        installJvmOptions("JvmOptions_Enabled_Jandex.txt");
        startServerScrub();

        StartCase startCase = StartCase.ENABLED_UPDATE_JANDEX;

        for ( int startNo = 0; startNo < ITERATIONS; startNo++ ) {
            long removeTime = timedRemoveJar("pmt.jar");
            add(startCase, removeTime);

            long addTime = timedAddJar("pmt.jar");
            add(startCase, addTime);
        }

        stopServer();
    }

    public long timedRemoveJar(String jarName) throws Exception {
        logBlock("Removing jar: " + jarName);
        renameJarFileInApplication("big-cdi-meetings.war", jarName, jarName + "_backup");
        return waitForAppUpdate();
    }

    public long timedAddJar(String jarName) throws Exception {
        logBlock("Adding jar: " + jarName);
        renameJarFileInApplication("big-cdi-meetings.war", jarName + "_backup", jarName);
        return waitForAppUpdate();
    }
}

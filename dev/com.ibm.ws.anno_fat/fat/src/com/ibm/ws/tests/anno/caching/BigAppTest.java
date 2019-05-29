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
package com.ibm.ws.tests.anno.caching;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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

    public static final boolean CACHE_DISABLED = false;
    public static final boolean CACHE_ENABLED = true;

    public static enum StartCase {
        DISABLED_SCRUBBED(CACHE_DISABLED, ServerStartType.SCRUBBED, "Disabled Scrubbing"),
        DISABLED(CACHE_DISABLED, "Disabled"),

        // The remaining starts are unscrubbed.
        // The remaining starts are unpopulated unless indicated otherwise.

        ENABLED(CACHE_ENABLED, "Enabled"),
        ENABLED_BINARY(CACHE_ENABLED, "Enabled Binary Format"),
        ENABLED_JANDEX(CACHE_ENABLED, "Enabled Using Jandex Format"),

        ENABLED_POPULATED(CACHE_ENABLED, "Enabled Populated"),
        ENABLED_POPULATED_BINARY(CACHE_ENABLED, "Enabled Populated Binary Format"),
        ENABLED_POPULATED_JANDEX(CACHE_ENABLED, "Enabled Populated Using Jandex Format"),
        ENABLED_POPULATED_BINARY_JANDEX(CACHE_ENABLED, "Enabled Populated Binary and Jandex Format"),
        
        ENABLED_POPULATED_VALID(CACHE_ENABLED, "Enabled Populated Assumed Valid"),
        ENABLED_POPULATED_BINARY_VALID(CACHE_ENABLED, "Enabled Populated Binary Format Assumed Valid"),
        ENABLED_POPULATED_BINARY_JANDEX_VALID(CACHE_ENABLED, "Enabled Populated Binary and Jandex Format Assumed Valid"),

        //

        DISABLED_MULTI_SCAN(CACHE_DISABLED, "Disabled Multiple Scan Threads"),
        DISABLED_UNLIMITED_SCAN(CACHE_DISABLED, "Disabled Unlimited Scan Threads"),

        ENABLED_MULTI_SCAN(CACHE_DISABLED, "Enabled Multiple Scan Threads"),
        ENABLED_UNLIMITED_SCAN(CACHE_DISABLED, "Enabled Unlimited Scan Threads"),

        ENABLED_MULTI_WRITE(CACHE_ENABLED, "Enabled Multiple Write Thread"),
        ENABLED_UNLIMITED_WRITE(CACHE_ENABLED, "Enabled Unlimited Write Threads"),

        //

        DISABLED_UPDATE(CACHE_DISABLED, "Disabled Application Update"),
        ENABLED_UPDATE(CACHE_ENABLED, "Enabled Application Update"),
        ENABLED_UPDATE_BINARY(CACHE_ENABLED, "Enabled Application Update Binary Format"),
        ENABLED_UPDATE_JANDEX(CACHE_ENABLED, "Enabled Application Update Jandex Format");

        private StartCase(boolean enabled, String description) {
        	this(enabled, ServerStartType.UNSCRUBBED, description);
        }

        private StartCase(boolean enabled, ServerStartType startType, String description) {
            this.enabled = enabled;
            this.startType = startType;
            this.description = description;
        }

        public final boolean enabled;

        public boolean getEnabled() {
            return enabled;
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

    public enum ServerStartType { 
        NONE, SCRUBBED, UNSCRUBBED; 
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
            scrubServer();

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
        logDifference(StartCase.DISABLED_SCRUBBED, StartCase.DISABLED);
        logDifference(StartCase.DISABLED, StartCase.ENABLED);
        logDifference(StartCase.ENABLED, StartCase.ENABLED_BINARY);
        logDifference(StartCase.ENABLED, StartCase.ENABLED_JANDEX);

        info(THIN_BANNER);
        logDifference(StartCase.DISABLED, StartCase.ENABLED_POPULATED);
        logDifference(StartCase.ENABLED_POPULATED, StartCase.ENABLED_POPULATED_BINARY);
        logDifference(StartCase.ENABLED_POPULATED, StartCase.ENABLED_POPULATED_JANDEX);
        logDifference(StartCase.ENABLED_POPULATED, StartCase.ENABLED_POPULATED_BINARY_JANDEX);

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

        if ( startCase.startType == ServerStartType.SCRUBBED ) {
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

    private static void scrubServer() throws Exception {
        installJvmOptions("JvmOptions_Disabled.txt");
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

    @Test
    public void bigApp_testDisabledScrubbed() throws Exception {
        scrubServer();

        installJvmOptions("JvmOptions_Disabled.txt");
        collect(StartCase.DISABLED_SCRUBBED);
    }

    @Test
    public void bigApp_testDisabled() throws Exception {
        scrubServer();

        installJvmOptions("JvmOptions_Disabled.txt");
        collect(StartCase.DISABLED);
    }

    //

    @Test
    public void bigApp_testEnabled() throws Exception {
        collect(StartCase.ENABLED, "JvmOptions_Enabled.txt"); 
    }

    @Test
    public void bigApp_testEnabledBinary() throws Exception { 
        scrubServer();

        installJvmOptions("JvmOptions_Enabled_Binary.txt");
        collect(StartCase.ENABLED_BINARY);
    }

    @Test
    public void bigApp_testEnabledJandex() throws Exception { 
        scrubServer();

        installJvmOptions("JvmOptions_Enabled_Jandex.txt");
        collect(StartCase.ENABLED_JANDEX);
    }

    @Test
    public void bigApp_testEnabledPopulated() throws Exception {
        populateServer(PopulateCase.TEXT);
        collect(StartCase.ENABLED_POPULATED);
    }

    @Test
    public void bigApp_testEnabledPopulatedBinary() throws Exception {
        populateServer(PopulateCase.BINARY);
        collect(StartCase.ENABLED_POPULATED_BINARY);
    }

    @Test
    public void bigApp_testEnabledPopulatedJandex() throws Exception {
        populateServer(PopulateCase.JANDEX);
        collect(StartCase.ENABLED_POPULATED_JANDEX);
    }

    @Test
    public void bigApp_testEnabledPopulatedBinaryJandex() throws Exception {
        populateServer(PopulateCase.BINARY_JANDEX);
        collect(StartCase.ENABLED_POPULATED_BINARY_JANDEX);
    }

    @Test
    public void bigApp_testEnabledPopulatedValid() throws Exception {
        populateServer(PopulateCase.TEXT);
        installJvmOptions("JvmOptions_Enabled_AlwaysValid.txt");
        collect(StartCase.ENABLED_POPULATED_VALID);
    }

    @Test
    public void bigApp_testEnabledPopulatedBinaryValid() throws Exception {
        populateServer(PopulateCase.BINARY);
        installJvmOptions("JvmOptions_Enabled_Binary_AlwaysValid.txt");
        collect(StartCase.ENABLED_POPULATED_BINARY_VALID);
    }

    //

    // @Test
    public void bigApp_testDisabledMultiScanThread() throws Exception {
        scrubServer();

        installJvmOptions("JvmOptions_Disabled_ScanMulti.txt");
        collect(StartCase.DISABLED_MULTI_SCAN);
    }

    // @Test
    public void bigApp_testEnabledOneScanThread() throws Exception {
        collect(StartCase.ENABLED_MULTI_SCAN, "JvmOptions_Enabled_ScanMulti.txt");
    }

    // @Test
    public void bigApp_testDisabledUnlimitedScanThreads() throws Exception {
        scrubServer();

        installJvmOptions("JvmOptions_Disabled_ScanUnlimited.txt");
        collect(StartCase.DISABLED_UNLIMITED_SCAN);
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

/*******************************************************************************
 * Copyright (c) 2019,2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.featurestart;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.test.featurestart.FeaturesStartTiming.TimingResult;
import com.ibm.ws.test.featurestart.FeaturesStartResults.StartupResult;

/**
 * Test to verify that Open Liberty can start with every valid
 * single feature.
 *
 * Split into buckets to enable shorter builds. Notably, Windows on FYRE hardware
 * does not run in under two hours, which is the maximum allowed time for a FAT
 * bucket.
 *
 * Currently split into four buckets. The last time too much time was taken the
 * number of buckets was two. The number has been increased to four to give us
 * extra running room before a new split is necessary.
 */
public class FeaturesStartTestBase {
    // Raw test APIs ...

    /**
     * The concrete test class which is being run. This is provided as a parameter
     * and means that logging is shown relative to that concrete class.
     */
    protected static Class<?> testClass;

    protected static void logInfo(String m, String msg) {
        Log.info(testClass, m, msg);
    }

    protected static void logError(String m, String msg) {
        Log.error(testClass, m, null, msg);
    }

    protected static void logError(String m, String msg, Throwable th) {
        Log.error(testClass, m, th, msg);
    }

    // Display utilities ...
    
    protected static void display(
        String m,
        String head, int width,
        Collection<String> featureShortNames,
        StringBuilder builder) {
            
        FeaturesStartReporting.display(m, head, width, featureShortNames, builder);
    }

    protected static void display(String m,
            String prefix, String nestedPrefix, int length,
            Map<String, ? extends Collection<String>> values,
            StringBuilder builder) {
        
        FeaturesStartReporting.display(m, prefix, nestedPrefix, length, values, builder);
    }

    //
    
    public static List<Object[]> getParameters() {
        return parameters.getParameters();
    }
    
    /**
     * Set test bucket parameters: This test class will be used to perform a number of
     * server startups. Set the overall parameters of performing the startups.
     *
     * This method performs basic server setup steps. Feature setup steps are
     * performed by {@link #setUp()}.
     *
     * @param testClass  The class which is performing the startup tests.
     * @param serverName The name of the server to use to run the tests.
     * @param bucketNo   The number of the bucket which will be performed by the test class.
     */
    public static void setParameters(Class<?> testClass, String serverName, int bucketNo) throws Exception {
        FeaturesStartTestBase.testClass = testClass;
        
        FeaturesStartTestBase.server = new FeaturesStartServer(serverName);
        FeaturesStartTestBase.features = new FeaturesStartFeatures(server);
        FeaturesStartTestBase.parameters = new FeaturesStartParameters(server, features, bucketNo);
        FeaturesStartTestBase.results = new FeaturesStartResults(server, features, parameters);
    }

    // The open-liberty bucket sub-classes have been updated to use the new API.
    // WS-CD bucket subclasses require a coordinated update, which is mildly painful.
    // Temporarily, the WS-CD subclasses still use the deprecated APIs.

    /**
     * Deprecated API invoked by subclasses. Use instead {@link #setParameters(Class, String, int)}.
     * The number of buckets and sparse setting are not provided by the root class.
     */
    @Deprecated
    public static void setParameters(
        Class<?> testClass, String serverName, int numBuckets, int bucketNo, int sparsity) throws Exception {

        FeaturesStartTestBase.setParameters(testClass, serverName, bucketNo);
    }

    /**
     * Deprecated API invoked by subclasses. This method no longer needs to be invoked.
     * (This implementation does nothing.) Feature setup now occurs within the standard base
     * class initialization steps.
     */
    @Deprecated    
    public static void setupFeatures() {
        // NO-OP
    }
    
    // Primary data structures:
    //
    // Tests are run against a named liberty server.
    //
    // Feature information is read from two locations: From the server, and from static
    // data files.
    //
    // Parameters are provided by the concrete test classes. A selection is made of the
    // feature information based on the bucket parameter and other parameters such as
    // the server java level, whether the server is running on ZOS, whether the test
    // mode is FULL or LITE, and based on a sparseness setting.

    // Marking these as volatile to address review / analysis concerns.
    //
    // The expectation is that in practice there will be no problem of concurrent
    // updates.

    // These *MUST* be static, because of how parameterized tests are run.
    // They could at best be put into a data structure, but that would still need
    // to be held as a static value.

    protected static volatile FeaturesStartServer server;
    private static volatile FeaturesStartFeatures features;    
    protected static volatile FeaturesStartParameters parameters;
    protected static volatile FeaturesStartResults results;
    
    // Server APIs ...

    public static String serverGetName() {
        return server.serverGetName();
    }

    public static String serverGetInstallRoot() {
        return server.getInstallRoot();
    }
    
    public static boolean serverIsZOS() {
        return server.isZOS();
    }

    public static int serverGetJavaLevel() {
        return server.getJavaLevel();
    }

    public static String serverGetConfigPath() {
        return server.getConfigPath();
    }
    
    public static File serverGetFeaturesDir() {
        return server.getFeaturesDir();
    }

    public static String serverGetPid() throws Exception {
        return server.getPid();
    }

    public static void updateServerFeature(String lastShortName, String nextShortName) throws Exception {
        server.updateFeature(lastShortName, nextShortName);
    }
    
    public static boolean serverIsStarted() {
        return server.serverIsStarted();
    }
    
    public static void serverStart(String featureShortName) throws Exception {
        server.serverStart(featureShortName);
    }

    public static void serverStop(String[] featureAllowedErrors) throws Exception {
        server.stop(featureAllowedErrors);
    }
    
    public static void serverKillProcess(String pid) throws Exception {
        server.killProcess(pid);
    }
       
    //

    // Instance state ...
    //
    // The pattern is for the test class to be setup for an element of the partition of
    // the overall feature set, then for test class instances to be created and run
    // relative to the feature short names which are within the partition element.

    /** The short name of the feature which is being tested. */
    private final String shortName;

    public String getShortName() {
        return shortName;
    }

    /**
     * Create a test instance. Each instance is used to perform a test
     * start of a single feature.
     *
     * @param shortName The short name of the feature which is to be tested.
     */
    public FeaturesStartTestBase(String shortName) {
        this.shortName = shortName;
    }

    /**
     * Run a single start server test.
     * 
     * The feature which is to be tests is provided as a test parameter.
     * See, for example {@link com.ibm.ws.test.featurestart.FeaturesStartTest1#data()}.
     * 
     * If the feature was the last feature of the current bucket, do completion
     * step.
     */
    public void testStartFeature() {
        String useShortName = getShortName();

        try {
            basicTestStartFeature();

        } finally {
            // @Parameterized.AfterParam invocation is better,
            // but we don't have that API yet.
            if ( parameters.isLastFeature(useShortName) ) {
                afterLastTest();
            }
        }
    }

    /**
     * Main test: Attempt to start the server with the single
     * named feature provisioned.
     * 
     * Complete testing by verifying the test result. A start failure
     * is handled as an assertion failure. 
     */
    public void basicTestStartFeature() {
        String m = "testStartFeature";

        String featureShortName = getShortName();

        results.updateFeatureName(featureShortName);

        TimingResult timingResult = results.addTiming();

        StartupResult testResult = results.cycleFeature(timingResult);

        timingResult.display(m);

        if ( !testResult.attempted ) {
            Assert.fail("Did not attempt [ " + featureShortName + " ]");
        } else if ( !testResult.started ) {
            Assert.fail("Failed to start [ " + featureShortName + " ]");

        } else if ( results.failures.contains(featureShortName) ) {
            String failureCase;
            if ( results.failuresAbsent.contains(featureShortName) ) {
                if ( ((failureCase = reportFirst("Missing feature specified error", results.failuresAbsentFeatureSpecified, featureShortName)) == null) &&                    
                     ((failureCase = reportFirst("Missing out-of-level error", results.failuresAbsentOutOfLevel, featureShortName)) == null) ) {                    
                    failureCase = "Strange mis-reported missing error";                    
                }
            } else if ( results.failuresPresent.contains(featureShortName) ) {
                if ( ((failureCase = reportFirst("Missing bundle", results.failuresPresentMissingBundle, featureShortName)) == null) &&                    
                     ((failureCase = reportFirst("Missing module", results.failuresPresentMissingModule, featureShortName)) == null) &&                    
                     ((failureCase = reportFirst("Other", results.failuresPresentOther, featureShortName)) == null) ) {                                        
                    failureCase = "Strange mis-reported present error";
                }
            } else {
                failureCase = "Strange mis-reported error";                
            }
            Assert.fail(failureCase + " [ " + featureShortName + " ]");

        } else if ( !testResult.stopped ) {
            Assert.fail("Failed to stop [ " + featureShortName + " ]");

        } else if ( !results.successes.contains(featureShortName) ) {
            Assert.fail("Strange: Neither success nor failure recorded for [ " + featureShortName + " ]");            
        }
    }

    private static String reportFirst(String prefix, Map<String, ? extends Collection<String>> storage, String bucketKey) {
        if ( storage.containsKey(bucketKey) ) {
            return prefix + "; first: " + firstElement(storage, bucketKey);
        } else {
            return null;
        }
    }
    
    private static String firstElement(Map<String, ? extends Collection<String>> storage, String bucketKey) {
        Collection<String> bucket = storage.get(bucketKey);
        if ( (bucket == null) || bucket.isEmpty() ) {
            return null;
        } else {
            return bucket.iterator().next();
        }
    }
    
    public static void afterLastTest() {
        String m = "afterLastTest";

        if (!parameters.skipFeatureNames.isEmpty()) {
            logInfo(m, "Skipped [ " + parameters.skipFeatureNames.size() + " ]");
            display(m, "    ", 80, parameters.skipFeatureNames, new StringBuilder());
        }

        results.displayTestResults();
        results.displayTimingResults();
    }    
}

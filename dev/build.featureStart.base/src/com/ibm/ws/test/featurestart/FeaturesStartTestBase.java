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
        return parameters.getRawParameters();
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

    protected static FeaturesStartServer server;
    private static FeaturesStartFeatures features;    
    protected static FeaturesStartParameters parameters;
    protected static FeaturesStartResults results;
    
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
            Assert.assertTrue("Did not attempt [ " + featureShortName + " ]", false);
        } else if ( !testResult.started ) {
            Assert.assertTrue("Failed to start [ " + featureShortName + " ]", false);

        } else if ( results.failures.contains(featureShortName) ) {
            String failureCase;
            if ( results.failuresAbsent.contains(featureShortName) ) {
                if ( results.failuresAbsentFeatureSpecified.containsKey(featureShortName) ) {
                    failureCase = "Missing feature specified error";
                } else if ( results.failuresAbsentOutOfLevel.containsKey(featureShortName) ) {
                    failureCase = "Missing out-of-level error";
                } else {
                    failureCase = "Strange mis-reported missing error";                    
                }
            } else if ( results.failuresPresent.contains(featureShortName) ) {
                if ( results.failuresPresentMissingBundle.containsKey(featureShortName) ) {
                    failureCase = "Missing bundle";
                } else if ( results.failuresPresentMissingModule.containsKey(featureShortName) ) {
                    failureCase = "Missing module";                    
                } else if ( results.failuresPresentOther.containsKey(featureShortName) ) {
                    failureCase = "Unexpected error";                    
                } else {
                    failureCase = "Strange mis-reported present error";
                }
            } else {
                failureCase = "Strange mis-reported error";                
            }
            Assert.assertTrue(failureCase + " [ " + featureShortName + " ]", false);

        } else if ( !testResult.stopped ) {
            Assert.assertTrue("Failed to stop [ " + featureShortName + " ]", false);

        } else if ( !results.successes.contains(featureShortName) ) {
            Assert.assertTrue("Strange: Neither success nor failure recorded for [ " + featureShortName + " ]", false);            
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

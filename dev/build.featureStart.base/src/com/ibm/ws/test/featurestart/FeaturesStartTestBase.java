/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.lang.Thread;

import org.junit.Assert;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.test.featurestart.features.FeatureData;
import com.ibm.ws.test.featurestart.features.FeatureErrors;
import com.ibm.ws.test.featurestart.features.FeatureFilter;
import com.ibm.ws.test.featurestart.features.FeatureLevels;
import com.ibm.ws.test.featurestart.features.FeatureReports;
import com.ibm.ws.test.featurestart.features.FeatureStability;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

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
    /**
     * Set test bucket parameters: This test class will be used to perform a number of
     * server startups. Set the overall parameters of performing the startups.
     *
     * This method performs basic server setup steps. Feature setup steps are
     * performed by {@link #setUp()}.
     *
     * @param c          The class which is peforming the startup tests.
     * @param serverName The name of the server to use to run the tests.
     * @param numBuckets The number of buckets of startups which will be performed.
     * @param bucketNo   The number of the bucket which will be performed by the test class.
     * @param sparsity   The sparsity of the features which will be started. For usual
     *                       testing, this should be 1, indicating that all features of the bucket should
     *                       be tested. A sparsity greater than 1 indicates that a subset of features
     *                       should be tests. This is set for tests of the framework itself.
     */
    public static void setParameters(Class<?> c,
                                     String serverName,
                                     int numBuckets, int bucketNo, int sparsity) throws Exception {
        FeaturesStartTestBase.c = c;

        FeaturesStartTestBase.serverName = serverName;

        FeaturesStartTestBase.server = LibertyServerFactory.getLibertyServer(serverName);
        FeaturesStartTestBase.serverConfigPath = server.getServerConfigurationPath();
        // Disabling unexpected FFDC checking for now because there is no good way for this FAT
        // to know what feature artifacts it should wait for before the server is stopped.
        FeaturesStartTestBase.server.setFFDCChecking(false);

        FeaturesStartTestBase.serverIsZOS = isServerZOS(FeaturesStartTestBase.server);
        FeaturesStartTestBase.serverJavaLevel = serverJavaLevel(FeaturesStartTestBase.server);

        FeaturesStartTestBase.NUM_BUCKETS = numBuckets;
        FeaturesStartTestBase.BUCKET_NO = bucketNo;
        FeaturesStartTestBase.SPARSITY = sparsity;
    }

    //

    private static Class<?> c;

    private static void logInfo(String m, String msg) {
        Log.info(c, m, msg);
    }

    private static void logError(String m, String msg) {
        Log.error(c, m, null, msg);
    }

    private static void logError(String m, String msg, Throwable th) {
        Log.error(c, m, th, msg);
    }

    // Test features within a bucket, with an assigned range within
    // the features list.
    //
    // Tests run by this class are conditioned entirely on the number
    // of buckets and the bucket number.
    //
    // The server name must be updated to match the bucket parameters.

    public static int NUM_BUCKETS;
    public static int BUCKET_NO;

    // Control parameter: Must be 0 or greater.
    // If greater than 0, test a subset of features, 1 of every SPARSITY.
    //
    // For example, setting '10' means run every 10'th test.
    // (At least 1 test is always run.)
    //
    // Use this when testing to limit the number of features which are
    // started.

    public static int SPARSITY;

    //

    protected static boolean isServerZOS(LibertyServer server) throws Exception {
        return server.getMachine().getOperatingSystem().equals(OperatingSystem.ZOS);
    }

    public static int serverJavaLevel(LibertyServer server) throws Exception {
        return JavaInfo.forServer(server).majorVersion();
    }

    //

    public static String serverName;

    public String getServerName() {
        return serverName;
    }

    public static LibertyServer server;

    public static String serverConfigPath;

    public String getServerConfigPath() {
        return serverConfigPath;
    }

    /** Relative path from the server home to the server features directory. */
    public static final String SERVER_FEATURES_PATH = "/lib/features/";

    public static File getServerFeaturesDir() {
        return new File(server.getInstallRoot() + SERVER_FEATURES_PATH);
    }

    public static boolean serverIsZOS;
    public static int serverJavaLevel;

    public static boolean isServerZOS() {
        return serverIsZOS;
    }

    public static int serverJavaLevel() {
        return serverJavaLevel;
    }

    /**
     * Kill the server which has the specified PID.
     *
     * @throws Exception Thrown if the attempt to kill the server failed.
     */
    private static void killProcess(String pid) throws Exception {
        server.getMachine().killProcess(Integer.parseInt(pid));
    }

    /**
     * Answer the PID of the server. Answer null if a running
     * server is not detected.
     *
     * @return The PID of the running server. Null if the server
     *         is not running.
     */
    private static String getPid() {
        String m = "getPid";
        try {
            return server.getPid();
        } catch (Exception e) {
            logError(m, "Failed to obtain PID", e);
            return null;
        }
    }

    /**
     * Set the server configuration to have exactly the one specified feature.
     *
     * @param lastShortName The previously configured feature.
     * @param shortName     The feature to set in the server configuration.
     * @param timingResult  Storage for timing data. (Not currently used.)
     *
     * @throws Exception Thrown if the update failed.
     */
    public static void setFeature(String lastShortName, String shortName, TimingResult timingResult) throws Exception {
        String m = "setFeature";

        logInfo(m, "Configuring server [ " + serverName + " ] for feature [ " + shortName + " ]");
        if (lastShortName != null) {
            logInfo(m, "Prior feature [ " + lastShortName + " ]");
        }

        // Performance notes: The original, unfixed 'updateServerConfiguration' took
        // just over 10s on Windows to perform an update.  That is because the
        // implementation attempted to rename the newly written configuration onto the
        // server configuration.  That failed, but was performed with full retries.
        //
        // With a fix to 'updateServerConfiguration' the update time is reduced to just
        // over 1s.  Much better, but not as good as possible.
        //
        // With either direct rewrite implementation ('changeFeatures' or 'FileRewriter')
        // the update time plummets to about 0.03s.

        // A direct rewrite API is already present in LibertyServer!  Since the server is
        // stopped for this update, a direct rewrite is usable.

        // This implementation mixes the read and write steps too tightly to
        // collect separate timings.
        server.changeFeatures(Collections.singletonList(shortName));

        // Alternate, rewrite implementation.  Used before 'changeFeatures' was discovered.

        // Initially, the server configuration has an empty feature manager
        // element.
        //
        // The first update adds a feature into that element.
        //
        // Subsequent updates replace the feature.

        // String matchLine;
        // Set<Integer> additions;
        // if (lastShortName == null) {
        //     matchLine = "<featureManager>";
        //     additions = Collections.singleton(Integer.valueOf(0));
        // } else {
        //     matchLine = "<feature>" + lastShortName + "</feature>";
        //     additions = Collections.emptySet();
        // }
        // String featureLine = "<feature>" + shortName + "</feature>";
        // String[] matchLines = new String[] { matchLine };
        // String[] updateLines = new String[] { featureLine };
        // FileRewriter.update(serverConfigPath, matchLines, updateLines, additions);

        // The original implementation, which uses XML serialization.

        // ServerConfiguration config = server.getServerConfiguration();
        // Set<String> features = config.getFeatureManager().getFeatures();
        // features.clear();
        // features.add(shortName);
        // server.updateServerConfiguration(config);
    }

    /**
     * Forcibly stop the server.
     *
     * First, if the server is set as having been started,
     * use {@link LibertyServer#stopServer} to stop the server.
     *
     * Second, if the server PID is available, attempt to kill
     * the server process.
     *
     * @param shortName     The short name of the feature being tested.
     * @param outOfLevel    True or false telling if the startup used
     *                          an unsupported java level.
     * @param pid           The PID of the running server.
     * @param allowedErrors Errors allowed in the stop server command.
     * @param failures      Storage for recording failures.
     * @param timingResult  Storage for timing data.
     *
     * @return True or false telling if the stop was successful.
     */
    public static boolean forceStopServer(String shortName,
                                          boolean outOfLevel,
                                          String pid,
                                          String[] allowedErrors,
                                          List<String> levelFailures, Map<String, String> failures,
                                          TimingResult timingResult,
                                          StartupResult startupResult) {
        String m = "forceStopServer";

        String description = "Server [ " + serverName + " ] PID [ " + pid + " ] Feature [ " + shortName + " ]";

        boolean noStopError = true;
        boolean noKillError = true;

        try {
            if (server.isStarted()) {
                if (shortName.equals("logstashCollector-1.0")) {
                    try {
                        Thread.sleep(10000); //wait 10 seconds for logstashCollector
                    }
                    catch (Exception e) {
                        //ignore and continue;
                    }
                }
                logInfo(m, "Stopping: " + description);
                if (allowedErrors != null) {
                    logInfo(m, "Allowed errors [ " + Arrays.toString(allowedErrors) + " ]");
                }

                long initialStopNs = timingResult.getTimeNs();
                Exception boundException;
                try {
                    server.stopServer(allowedErrors);
                    boundException = null;
                } catch (Exception e) {
                    boundException = e;
                } finally {
                    timingResult.setStopNsFromInitial(initialStopNs);
                }

                // Handle the result *outside* of the timing block.
                if (boundException == null) {
                    logInfo(m, "Stopped: " + description);
                } else {
                    if (outOfLevel) {
                        if (startupResult.started) {
                            // Level failures is a list; a startup failure will
                            // have already added the short name.
                            levelFailures.add(shortName);
                        }
                        logInfo(m, "Failed to stop feature (expected: out-of-level) [ " + shortName + " ]: " + boundException);
                    } else {
                        noStopError = false;
                        failures.put(shortName, "Stop Exception");
                        logError(m, "Failed to stop feature [ " + shortName + " ]: " + description, boundException);
                    }
                }
            } else {
                logInfo(m, "Not started: " + description);
            }

        } finally {
            if (pid != null) {
                logInfo(m, "Killing: " + description);
                long initialKillNs = timingResult.getTimeNs();
                Exception boundException;
                try {
                    killProcess(pid);
                    boundException = null;
                } catch (Exception e) {
                    boundException = e;
                } finally {
                    timingResult.setKillNsFromInitial(initialKillNs);
                }

                // Handle the result *outside* of the timing block.
                if (boundException == null) {
                    logInfo(m, "Killed: " + description);
                } else {
                    if (outOfLevel) {
                        levelFailures.add(shortName);
                        logInfo(m, "Failed to kill feature (expected: out-of-level) [ " + shortName + " ]: " + description + ": " + boundException);
                    } else {
                        noKillError = false;
                        failures.put(shortName, "Kill Exception");
                        logError(m, "Failed to kill feature [ " + shortName + " ]: " + description, boundException);
                    }
                }
            } else {
                logInfo(m, "Null PID: " + description);
            }
        }

        return (noStopError && noKillError);
    }

    //

    /**
     * Do feature related setup steps Read all feature data and select the features
     * which are to be tested for the previously specified test bucket.
     *
     * This method performs feature related setup steps. Basic parameter steps
     * and server related steps are performed by {@link #setParameters}.
     */
    public static void setupFeatures() throws Exception {
        String m = "setupFeatures";

        featureData = FeatureData.readFeatures(getServerFeaturesDir());

        stableFeatures = FeatureStability.readStableFeatures();

        requiredLevels = FeatureLevels.getRequiredLevels();

        featureFilter = (name) -> FeatureFilter.skipFeature(name);
        featureZOSFilter = (name) -> FeatureFilter.zosSkip(name, isServerZOS());

        allowedErrors = FeatureErrors.getAllowedErrors();

        //

        BiFunction<String, Boolean, String> zosFilter = (name, isZOS) -> FeatureFilter.zosSkip(name, isZOS.booleanValue());

        Log.info(c, m, "Read [ " + featureData.size() + " ] features for server [ " + serverName + " ] at [ " + server.getInstallRoot() + " ]");
        Log.info(c, m, "");

        (new FeatureReports(featureData, stableFeatures, requiredLevels, featureFilter, zosFilter, allowedErrors)).display();

        //

        initFeatures();
    }

    //

    public static Map<String, FeatureData> featureData;

    public static Set<String> getFeatureNames() {
        return featureData.keySet();
    }

    public static String[] sortFeatureNames() {
        Set<String> featureNamesSet = getFeatureNames();
        String[] featureNamesArray = featureNamesSet.toArray(new String[featureNamesSet.size()]);
        Arrays.sort(featureNamesArray);
        return featureNamesArray;
    }

    public static FeatureData getFeatureData(String name) {
        return featureData.get(name);
    }

    public static FeatureStability stableFeatures;

    public static FeatureStability getStableFeatures() {
        return stableFeatures;
    }

    public static boolean isStable(String name) {
        return stableFeatures.isStable(name);
    }

    public static Map<String, Integer> requiredLevels;

    public static Integer getRequiredLevel(String name) {
        return requiredLevels.get(name);
    }

    public static String isLevelFiltered(String name) {
        return isLevelFiltered(name, serverJavaLevel);
    }

    public static String isLevelFiltered(String name, int currentLevel) {
        Integer requiredLevel = getRequiredLevel(name);
        if ((requiredLevel == null) || (currentLevel >= requiredLevel.intValue())) {
            return null;
        } else {
            return "Required level [ " + requiredLevel + " ] greater than the current level [ " + currentLevel + " ]";
        }
    }

    public static Map<String, String[]> allowedErrors;

    public static String[] getAllowedErrors(String name) {
        return allowedErrors.get(name);
    }

    public static Function<String, String> featureFilter;

    public static String isFiltered(String name) {
        return featureFilter.apply(name);
    }

    public static Function<String, String> featureZOSFilter;

    public static String isZOSFiltered(String name) {
        return featureZOSFilter.apply(name);
    }

    //

    public static List<String> runnableFeatureNames;
    public static Map<String, FeatureData> runnableFeatures;

    public static Set<String> outOfLevelFeatureNames;

    public static int firstFeatureNo;
    public static int lastFeatureNo; // One past the last test to be run.

    public static Set<String> bucketOutOfLevelFeatureNames;

    //

    /**
     * Initialize the features which are to be tested.
     *
     * These are read from the features folder of the test server.
     *
     * This initialization step requires that the test server already be
     * injected by the FAT runner.
     *
     * @throws IOException Thrown if the feature directory could not be read,
     *                         or if no features are available.
     */
    public static void initFeatures() throws IOException {
        String m = "initFeatures";

        Set<String> featureNamesSet = getFeatureNames();
        String[] featureNamesArray = featureNamesSet.toArray(new String[featureNamesSet.size()]);
        Arrays.sort(featureNamesArray);

        // Generic filters: Client, test, and non-public features are never tested.
        // Stable features are not tested in LITE mode.

        List<String> clientFeatures = new ArrayList<>();
        List<String> testFeatures = new ArrayList<>();
        List<String> nonPublicFeatures = new ArrayList<>();
        List<String> stableFeatures = new ArrayList<>();

        // Features may be skipped for feature specific reasons.
        // For example, some features cannot be started by themselves.

        List<String> filteredNames = new ArrayList<>();
        Map<String, String> filterReasons = new HashMap<>();
        List<String> zosFilteredNames = new ArrayList<>();
        Map<String, String> zosFilterReasons = new HashMap<>();

        // What is left are the features features.

        List<String> selectedNames = new ArrayList<>(featureNamesArray.length);
        Map<String, FeatureData> selectedFeatures = new HashMap<>(featureNamesArray.length);

        // Of the selected features, some may be out-of-level.  Those are still
        // tested, but the expected server startup result changes.

        List<String> outOfLevelNames = new ArrayList<>();
        Map<String, String> outOfLevelReasons = new HashMap<>();

        for (String name : featureNamesArray) {
            FeatureData featureData = getFeatureData(name);
            if (featureData.isClientOnly()) {
                clientFeatures.add(name);
                continue;
            } else if (featureData.isTest()) {
                testFeatures.add(name);
                continue;
            } else if (!featureData.isPublic()) {
                nonPublicFeatures.add(name);
                continue;

            } else if ((TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.LITE) && isStable(name)) {
                stableFeatures.add(name);
                continue;
            }

            String filterReason = isFiltered(name);
            if (filterReason != null) {
                filteredNames.add(name);
                filterReasons.put(name, filterReason);
                continue;
            }
            String zosFilterReason = isZOSFiltered(name);
            if (zosFilterReason != null) {
                zosFilteredNames.add(name);
                zosFilterReasons.put(name, zosFilterReason);
                continue;
            }

            selectedNames.add(name);
            selectedFeatures.put(name, featureData);

            String outOfLevelReason = isLevelFiltered(name);
            if (outOfLevelReason != null) {
                outOfLevelNames.add(name);
                outOfLevelReasons.put(name, outOfLevelReason);
                // Do NOT skip level filtered features.  An attempt
                // is made to start these, with failure as the expected
                // result.
            }
        }

        if (!clientFeatures.isEmpty()) {
            logInfo(m, "Skip client-only features [ " + clientFeatures.size() + " ]:");
            display(m, "    ", 80, clientFeatures);
        }
        if (!nonPublicFeatures.isEmpty()) {
            logInfo(m, "Skip non-public features [ " + nonPublicFeatures.size() + " ]:");
            display(m, "    ", 80, nonPublicFeatures);
        }
        if (!testFeatures.isEmpty()) {
            logInfo(m, "Skip test features [ " + testFeatures.size() + " ]:");
            display(m, "    ", 80, testFeatures);
        }

        if (!stableFeatures.isEmpty()) {
            logInfo(m, "LITE mode: Skip stable features [ " + stableFeatures.size() + " ]:");
            display(m, "    ", 80, stableFeatures);
        }

        if (!filterReasons.isEmpty()) {
            logInfo(m, "Skip filtered features [ " + filterReasons.size() + " ]:");
            display(m, "    ", ": ", "", filteredNames, filterReasons);
        }
        if (!zosFilterReasons.isEmpty()) {
            logInfo(m, "Skip ZOS filtered features [ " + zosFilterReasons.size() + " ]:");
            display(m, "    ", ": ", "", zosFilteredNames, zosFilterReasons);
        }

        if (selectedFeatures.isEmpty()) {
            throw new IllegalArgumentException("No testable features are present.");
        }
        logInfo(m, "Run features [ " + selectedFeatures.size() + " ]:");
        display(m, "    ", 80, selectedFeatures.keySet());

        if (!outOfLevelReasons.isEmpty()) {
            logInfo(m, "Out-of-level features [ " + outOfLevelReasons.size() + " ]:");
            display(m, "    ", ": ", "", outOfLevelNames, outOfLevelReasons);
        }

        runnableFeatureNames = selectedNames;
        runnableFeatures = selectedFeatures;

        outOfLevelFeatureNames = new HashSet<>(outOfLevelNames);

        // Limit the features to the current bucket.
        int[] range = getRange(selectedFeatures.size(), NUM_BUCKETS, BUCKET_NO);

        firstFeatureNo = range[0];
        lastFeatureNo = range[1];

        bucketOutOfLevelFeatureNames = new HashSet<>(outOfLevelFeatureNames.size());

        for (int featureNo = firstFeatureNo; featureNo < lastFeatureNo; featureNo++) {
            String shortName = runnableFeatureNames.get(featureNo);
            if (outOfLevelFeatureNames.contains(shortName)) {
                bucketOutOfLevelFeatureNames.add(shortName);
            }
        }
    }

    /**
     * Compute a range for a bucket within a larger range, dividing the
     * larger range as evenly as possible. Any leftover elements are
     * allocated to the initial buckets.
     *
     * @param numElements The number of elements of the overal range.
     * @param numBuckets  The number of buckets.
     * @param bucketNo    The bucket number (one based).
     *
     * @return The range as a half open interval: The first offset of the range,
     *         then the last offset of the range plus one.
     */
    public static int[] getRange(int numElements, int numBuckets, int bucketNo) {
        // A zero based bucket number is easier to compute with.
        int useBucketNo = bucketNo - 1;

        int bucketSize = numElements / numBuckets;
        int residue = numElements % numBuckets;

        // Assign the range assuming an even split (residue == 0).

        int firstFeatureNo = useBucketNo * bucketSize;
        int lastFeatureNo = firstFeatureNo + bucketSize;

        // But there may be leftover features.
        // Allocate these one per bucket, starting with the first bucket.

        // When there is a residue, 'bucketSize' is imprecise:
        //   Bucket numbers [ 0 .. residue - 1 ] have a bucket size one greater.
        //   Bucket numbers [ residue .. bucketNo - 1 ] have the computed bucket size

        if (residue != 0) {
            if (useBucketNo < residue) {
                // In effect, add one to the bucket size.
                firstFeatureNo += useBucketNo;
                lastFeatureNo += useBucketNo + 1;
            } else {
                // In effect, add one to the bucket size **for preceeding buckets**
                firstFeatureNo += residue;
                lastFeatureNo += residue;
            }
        }

        return new int[] { firstFeatureNo, lastFeatureNo };
    }

    // Set the range for this bucket ...
    // Distribute features as evenly as possible:
    // 12 features with 4 buckets: 0..3, 3..6, 6..9. 9..12: 3, 3, 3, 3: 12
    // 11 features with 4 buckets: 0..3, 3..6, 6..9, 9..11: 3, 3, 3, 2: 11
    //  9 features with 4 buckets: 0..3, 3..5, 5..7, 7..9 : 3, 2, 2, 2: 9
    //
    // static final int[] NUM_FEATURES_RANGE = { 12, 11, 10, 9, 8 };
    // static final int[] NUM_BUCKETS_RANGE = { 2, 3, 4 };
    //
    // public static void main(String[] args) {
    //     for (int numFeatures : NUM_FEATURES_RANGE) {
    //         for (int numBuckets : NUM_BUCKETS_RANGE) {
    //             for (int bucketNo = 1; bucketNo <= numBuckets; bucketNo++) {
    //                 int[] range = getRange(numFeatures, numBuckets, bucketNo);
    //             }
    //         }
    //     }
    // }

    public static class StartupResult {
        public final boolean attempted;
        public final boolean started;
        public final boolean hadForbiddenErrors;
        public final boolean missingRequiredErrors;
        public final String pid;
        public boolean didStop;

        public static final boolean DID_ATTEMPT = true;
        public static final boolean DID_START = true;
        public static final boolean HAD_FORBIDDEN_ERRORS = true;
        public static final boolean MISSING_REQUIRED_ERRORS = true;
        public static final boolean DID_STOP = true;

        public static StartupResult notAttemptedResult() {
            return new StartupResult(!DID_ATTEMPT, !DID_START, !HAD_FORBIDDEN_ERRORS, !MISSING_REQUIRED_ERRORS, null);
        }

        /**
         * Fully parameterized factory method.
         *
         * 'started' should not be true if 'attempted' is false.
         *
         * 'pid' should be null if 'attempted' is false. 'pid' may be null
         * if 'started' is false. That indicates a startup attempt which left
         * a dangling server process, but which reported failure.
         *
         * @param attempted             True or false, telling if the startup was attempted.
         * @param started               True or false telling if the server was started.
         * @param hadForbiddenErrors    True or false telling if unexpected errors were present.
         * @param missingRequiredErrors True or false telling if required errors were missing.
         * @param pid                   The PID of the server process.
         */
        public StartupResult(boolean attempted, boolean started,
                             boolean hadForbiddenErrors, boolean missingRequiredErrors,
                             String pid) {
            this.attempted = attempted;
            this.started = started;
            this.hadForbiddenErrors = hadForbiddenErrors;
            this.missingRequiredErrors = missingRequiredErrors;
            this.pid = pid;
            this.didStop = !DID_STOP;
        }

        public void stopped() {
            this.didStop = DID_STOP;
        }
    }

    // There was a discussion of time-limiting the essential steps.
    // However, on some of the very slow FYRE hardware, these can take
    // several minutes or more.
    //
    // Also, the entire FAT is time-limited.

    public static class TimingResult {
        public final String shortName;

        public long updateNs = UNSET_NS;
        // public long readNs = UNSET_NS; // Subset of 'update'; no longer used
        // public long writeNs = UNSET_NS; // Subset of 'update'; no longer used
        public long startNs = UNSET_NS;
        public long pidNs = UNSET_NS;
        public long verifyNs = UNSET_NS;
        public long stopNs = UNSET_NS;
        public long killNs = UNSET_NS;

        public TimingResult(String shortName) {
            this.shortName = shortName;
        }

        public long getTimeNs() {
            return System.nanoTime();
        }

        public long getTimeNs(long initialNs) {
            return getTimeNs() - initialNs;
        }

        public void setUpdateNsFromInitial(long initialNs) {
            updateNs = getTimeNs(initialNs);
        }

        // public void setReadNsFromInitial(long initialNs) {
        //     readNs = getTimeNs(initialNs);
        // }

        // public void setWriteNsFromInitial(long initialNs) {
        //     writeNs = getTimeNs(initialNs);
        // }

        public void setStartNsFromInitial(long initialNs) {
            startNs = getTimeNs(initialNs);
        }

        public void setPidNsFromInitial(long initialNs) {
            pidNs = getTimeNs(initialNs);
        }

        public void setVerifyNsFromInitial(long initialNs) {
            verifyNs = getTimeNs(initialNs);
        }

        public void setStopNsFromInitial(long initialNs) {
            stopNs = getTimeNs(initialNs);
        }

        public void setKillNsFromInitial(long initialNs) {
            killNs = getTimeNs(initialNs);
        }

        public long totalNs() {
            return sum(updateNs, startNs, pidNs, verifyNs, stopNs, killNs);
        }
    }

    //

    private static List<Object[]> parameters;
    private static List<String> runFeatureNames;
    private static List<String> skipFeatureNames;

    private static TestState testState;

    public static List<Object[]> getParameters() {
        // @Parameterized.BeforeParam invocation is better,
        // but we don't have that API yet.

        if (parameters == null) {
            setupParameters();
        }
        return parameters;
    }

    public static List<String> getRunFeatureNames() {
        return runFeatureNames;
    }

    public static boolean isFirstFeature(String shortName) {
        return (!runFeatureNames.isEmpty() && runFeatureNames.get(0).equals(shortName));
    }

    public static boolean isLastFeature(String shortName) {
        return (!runFeatureNames.isEmpty() &&
                runFeatureNames.get(runFeatureNames.size() - 1).equals(shortName));
    }

    public static List<String> getSkipFeatureNames() {
        return skipFeatureNames;
    }

    /**
     * Initial processing: Display test bucket information, including information
     * in regards to the selected features. Do sparsity related feature selection.
     * Setup for running tests, including the gathering of test result information,
     * including timing information.
     */
    public static void setupParameters() {
        String m = "beforeFirstTest";

        logInfo(m, "Test class [ " + c + " ]");

        logInfo(m, "Test server [ " + serverName + " ]");
        logInfo(m, "  Server java [ " + serverJavaLevel + " ]");
        logInfo(m, "  Server isZOS [ " + serverIsZOS + " ]");
        banner(m);

        logInfo(m, "Features [ " + runnableFeatures.size() + " ]");
        logInfo(m, "  Out-of-level [ " + outOfLevelFeatureNames.size() + " ]");

        logInfo(m, "Bucket [ " + BUCKET_NO + " ] of [ " + NUM_BUCKETS + " ]");
        logInfo(m, "  Count [ " + (lastFeatureNo - firstFeatureNo) + " ] Out-of-level [ " + bucketOutOfLevelFeatureNames.size() + " ]");
        logInfo(m, "  First [ " + firstFeatureNo + " ]: [ " + runnableFeatureNames.get(firstFeatureNo) + " ]");
        logInfo(m, "  Last  [ " + (lastFeatureNo - 1) + " ]: [ " + runnableFeatureNames.get(lastFeatureNo - 1) + " ]");
        if (SPARSITY > 0) {
            logInfo(m, "  Sparsity [ " + SPARSITY + " ]");
        }
        banner(m);

        int numFeatures = lastFeatureNo - firstFeatureNo;
        if (SPARSITY > 0) {
            numFeatures /= SPARSITY;
            if ((numFeatures % SPARSITY) > 0) {
                numFeatures++;
            }
        }

        List<Object[]> useParameters = new ArrayList<>(numFeatures);
        List<String> useRunFeatureNames = new ArrayList<>((SPARSITY > 0) ? ((numFeatures / SPARSITY) + 1) : numFeatures);
        List<String> useSkipFeatureNames = new ArrayList<>((SPARSITY > 0) ? numFeatures : 0);

        for (int featureNo = firstFeatureNo; featureNo < lastFeatureNo; featureNo++) {
            String shortName = runnableFeatureNames.get(featureNo);
            if ((SPARSITY > 0) && (((featureNo - firstFeatureNo) % SPARSITY) != 0)) {
                logInfo(m, "Skipping [ " + shortName + " ]: Filtered by SPARSITY");
                useSkipFeatureNames.add(shortName);
            } else {
                useRunFeatureNames.add(shortName);
                useParameters.add(new Object[] { shortName });
            }
        }

        FeaturesStartTestBase.parameters = useParameters;
        FeaturesStartTestBase.skipFeatureNames = useSkipFeatureNames;
        FeaturesStartTestBase.runFeatureNames = useRunFeatureNames;

        FeaturesStartTestBase.testState = new TestState(firstFeatureNo, lastFeatureNo, numFeatures);
    }

    public static class TestState {
        public final int firstFeatureNo;
        public final int lastFeatureNo;
        public final int numFeatures;

        public final List<String> successes;
        public final Map<String, String> failures;
        public final List<String> levelExpectedFailures;
        public final List<String> levelUnexpectedSuccesses;

        public final Map<String, TimingResult> timingResults;

        public String lastShortName;
        public String nextShortName;

        public TestState(int firstFeatureNo, int lastFeatureNo, int numFeatures) {
            this.firstFeatureNo = firstFeatureNo;
            this.lastFeatureNo = lastFeatureNo;
            this.numFeatures = numFeatures;

            this.successes = new ArrayList<>();
            this.failures = new LinkedHashMap<>();
            this.levelExpectedFailures = new ArrayList<>();
            this.levelUnexpectedSuccesses = new ArrayList<>();

            this.timingResults = new HashMap<>(numFeatures);

            this.lastShortName = null;
            this.nextShortName = null;
        }

        public TimingResult addTiming(String shortName) {
            TimingResult timingResult = new TimingResult(nextShortName);
            timingResults.put(nextShortName, timingResult);
            return timingResult;
        }

        public void displayResults() {
            String m = "displayResults";

            logInfo(m, "Successes [ " + successes.size() + " ]");
            if (!successes.isEmpty()) {
                display(m, "    ", 80, successes);
            }
            logInfo(m, "Expected failures [ " + levelExpectedFailures.size() + " ]");
            if (!levelExpectedFailures.isEmpty()) {
                display(m, "    ", 80, levelExpectedFailures);
            }
            logInfo(m, "Failures [ " + failures.size() + " ]");
            if (!failures.isEmpty()) {
                display(m, "    ", 80, failures.keySet());
            }

            logInfo(m, "Unexpected successes [ " + levelUnexpectedSuccesses.size() + " ]");
            if (!levelUnexpectedSuccesses.isEmpty()) {
                display(m, "    ", 80, levelUnexpectedSuccesses);
            }
            if (!testState.levelUnexpectedSuccesses.isEmpty()) {
                logInfo(m, "Features [ " + testState.levelUnexpectedSuccesses + " ] started on java [ " + serverJavaLevel + " ].");
                logInfo(m, "");
                logInfo(m, "If these are test-only features, add 'IBM-Test-Feature: true' to the feature manifests. ");
                logInfo(m, "Feature required java levels are specified in resource [ " + FeatureLevels.REQUIRED_LEVELS_NAME + " ]");
            }

            display(m, timingResults);
        }

        /**
         * Attempt to start a feature. Set the feature as the single configured feature
         * then start the server.
         *
         * If the feature was started, verify that no unexpected errors were logged.
         *
         * Do not stop the feature.
         *
         * If an attempt was made to start the server, answer the PID of the started server.
         * (This can be null if the attempt was made but failed.)
         *
         * @param isOutOfLevel The java level is not sufficient to run the feature. A
         *                         failure is expected.
         * @param timingResult Storage for time recording.
         *
         * @return The PID of the started server. Null if the feature could not be
         *         configured, or if the server startup was attempted but failed.
         */
        public StartupResult startFeature(boolean isOutOfLevel, TimingResult timingResult) {
            String m = "startFeature";

            long initialUpdateNs = timingResult.getTimeNs();

            try {
                setFeature(lastShortName, nextShortName, timingResult);

            } catch (Exception e) {
                logError(m, "Failed to set feature [ " + nextShortName + " ]", e);
                failures.put(nextShortName, "Failed to set feature");

                // Complete failure: The start was not attempted.
                return StartupResult.notAttemptedResult();

            } finally {
                timingResult.setUpdateNsFromInitial(initialUpdateNs);
            }

            // 'attempted' is now true

            long initialStartNs = timingResult.getTimeNs();
            boolean started;
            boolean extraErrors = false;
            boolean missingErrors = false;

            try {
                // Default start: Pre-clean and clean the server.
                server.setConsoleLogName(nextShortName + ".log");

                // When a feature is out-of-level, the server starts with
                // logged errors.  The  messages log is expected to have the
                // following pattern of messages:
                //
                // [2/7/23 23:08:18:611 EST] 00000001 com.ibm.ws.kernel.launch.internal.FrameworkManager
                //   A CWWKE0001I: The server features.start.1.server has been launched.
                // [2/7/23 23:08:21:254 EST] 00000001 com.ibm.ws.kernel.launch.internal.FrameworkManager
                //   I CWWKE0002I: The kernel started after 3.066 seconds
                //
                // [2/7/23 23:08:21:809 EST] 00000033 com.ibm.ws.kernel.feature.internal.FeatureManager
                //   I CWWKF0007I: Feature update started.
                //
                // [2/7/23 23:08:24:907 EST] 00000033 com.ibm.ws.kernel.feature.internal.FeatureManager
                //   E CWWKF0032E: The io.openliberty.servlet.api-6.0 feature requires a minimum Java
                //   runtime environment version of JavaSE 11.
                // [2/7/23 23:08:24:907 EST] 00000033 com.ibm.ws.kernel.feature.internal.FeatureManager
                //   E CWWKF0032E: The io.openliberty.jakarta.expressionLanguage-5.0 feature requires
                //   a minimum Java runtime environment version of JavaSE 11.
                // [2/7/23 23:08:24:907 EST] 00000033 com.ibm.ws.kernel.feature.internal.FeatureManager
                //   E CWWKF0032E: The io.openliberty.jsonpImpl-2.1.1 feature requires a minimum Java
                //   runtime environment version of JavaSE 11.
                //
                // [2/7/23 23:08:31:101 EST] 00000033 com.ibm.ws.kernel.feature.internal.FeatureManager
                //   A CWWKF0012I: The server installed the following features: [appAuthentication-3.0,
                //   distributedMap-1.0, jndi-1.0, jsonp-2.1, servlet-6.0, ssl-1.0, timedexit-1.0,
                //   transportSecurity-1.0].
                // [2/7/23 23:08:31:101 EST] 00000033 com.ibm.ws.kernel.feature.internal.FeatureManager
                //   I CWWKF0008I: Feature update completed in 9.858 seconds.
                //
                // [2/7/23 23:08:31:101 EST] 00000033 com.ibm.ws.kernel.feature.internal.FeatureManager
                //   A CWWKF0011I: The features.start.1.server server is ready to run a smarter planet.
                //   The features.start.1.server server started in 12.922 seconds.

                boolean preClean = true;
                boolean cleanStart = true;
                boolean validateApps = false;
                boolean expectStartFailure = false;
                boolean validateTimedExit = false;

                server.startServerAndValidate(preClean, cleanStart,
                                              validateApps,
                                              expectStartFailure,
                                              validateTimedExit);
                started = true;

            } catch (Exception e) {
                started = false;
                failures.put(nextShortName, "Start failure");

                logError(m, "Failed to start feature [ " + nextShortName + " ]", e);

            } finally {
                timingResult.setStartNsFromInitial(initialStartNs);
            }

            // The PID may or may not be available:
            // PID retrieval is performed even if 'started' is false, so to handle
            // the case of an apparently failed startup which left a dangling process.

            long initialPidNs = timingResult.getTimeNs();

            String pid = getPid();
            logInfo(m, "Server PID: " + pid);

            timingResult.setPidNsFromInitial(initialPidNs);

            // There is no point to doing feature startup verification if the
            // server did not start cleanly.

            // Verify we DO get CWWKF0032E
            // boolean featureStarted = server.waitForStringInLog("CWWKF0032E", 5 * 1000) == null;

            if (started) {
                long initialVerifyNs = timingResult.getTimeNs();
                try {
                    List<String> errors = server.findStringsInLogs("CWWKF0032E");
                    if (!errors.isEmpty()) {
                        if (isOutOfLevel) {
                            levelExpectedFailures.add(nextShortName);
                            logInfo(m, "Found expected out-of-level errors [ " + nextShortName + " ]");

                        } else {
                            extraErrors = true;
                            failures.put(nextShortName, "Unexpected errors");

                            logError(m, "Found unexpected errors [ " + nextShortName + " ]");
                            for (String error : errors) {
                                logError(m, "  [ " + error + " ]");
                            }
                        }
                    } else {
                        if (isOutOfLevel) {
                            missingErrors = true;
                            levelUnexpectedSuccesses.add(nextShortName);

                            logError(m, "Missing expected out-of-level errors [ " + nextShortName + " ]");
                        }
                    }
                } catch (Exception e) {
                    extraErrors = true;
                    failures.put(nextShortName, "Verify exception");

                    logError(m, "Verify exception [ " + nextShortName + " ]", e);

                } finally {
                    timingResult.setVerifyNsFromInitial(initialVerifyNs);
                }
            }

            return new StartupResult(StartupResult.DID_ATTEMPT, started, extraErrors, missingErrors, pid);
        }

        /**
         * The allowed errors for when the feature start uses a dis-allowed java level.
         *
         * These allowed errors are used in place of any other allowed errors.
         */
        private static String[] JAVA_LEVEL_ALLOWED_ERRORS = { "CWWKF0032E", "CWWKE0702E" };

        // Both 32E and 702E errors are expected:
        //
        // [2/7/23 23:08:24:907 EST] 00000033 com.ibm.ws.kernel.feature.internal.FeatureManager
        //   E CWWKF0032E: The io.openliberty.jakarta.expressionLanguage-5.0 feature requires
        //   a minimum Java runtime environment version of JavaSE 11.
        //
        // [2/8/23 12:22:13:451 EST] 00000024 LogService-25-io.openliberty.java11.internal
        //   E CWWKE0702E: Could not resolve module: io.openliberty.java11.internal [25]

        public void forceStopFeature(boolean isOutOfLevel,
                                     TimingResult timingResult,
                                     StartupResult startupResult) {

            String[] allowedErrors = (isOutOfLevel ? JAVA_LEVEL_ALLOWED_ERRORS : getAllowedErrors(nextShortName));

            if (forceStopServer(nextShortName, isOutOfLevel,
                                startupResult.pid, allowedErrors,
                                levelExpectedFailures, failures,
                                timingResult, startupResult)) {

                startupResult.stopped();

                if (!failures.containsKey(nextShortName)) {
                    successes.add(nextShortName);
                }
            }
        }
    }

    /**
     * After processing: Display the overall results.
     */
    public static void afterLastTest() {
        String m = "afterLastTest";

        if (!skipFeatureNames.isEmpty()) {
            logInfo(m, "Skipped [ " + skipFeatureNames.size() + " ]");
            display(m, "    ", 80, skipFeatureNames);
        }

        testState.displayResults();
    }

    //

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

    public void testStartFeature() throws Exception {
        String useShortName = getShortName();

        try {
            basicTestStartFeature();

        } finally {
            // @Parameterized.AfterParam invocation is better,
            // but we don't have that API yet.
            if (isLastFeature(useShortName)) {
                afterLastTest();
            }
        }
    }

    /**
     * Main test: Attempt to start the server with the single
     * named feature provisioned.
     */
    public void basicTestStartFeature() throws Exception {
        String m = "testStartFeature";

        String useShortName = getShortName();

        testState.lastShortName = testState.nextShortName;
        testState.nextShortName = useShortName;

        boolean isOutOfLevel = outOfLevelFeatureNames.contains(useShortName);

        TimingResult timingResult = testState.addTiming(useShortName);

        StartupResult startupResult = null;

        try {
            try {
                startupResult = testState.startFeature(isOutOfLevel, timingResult);

            } finally {
                // A null result is only possible if 'startFeature' failed with a throwable.
                // In this case, do our best to stop the server.
                // Make a dummy result that looks like an attempted startup.
                if (startupResult == null) {
                    startupResult = new StartupResult(StartupResult.DID_ATTEMPT, !StartupResult.DID_START, !StartupResult.HAD_FORBIDDEN_ERRORS, !StartupResult.MISSING_REQUIRED_ERRORS, getPid());
                }

                if (startupResult.attempted) {
                    testState.forceStopFeature(isOutOfLevel, timingResult, startupResult);
                }
            }

        } finally {
            display(m, timingResult);

            if (!startupResult.attempted) {
                Assert.assertTrue("Did not attempt [ " + useShortName + " ]", false);
            } else if (!startupResult.started) {
                Assert.assertTrue("Failed to start [ " + useShortName + " ]", false);
            } else if (startupResult.hadForbiddenErrors) {
                Assert.assertFalse("Start had forbidden errors [ " + useShortName + " ]", true);
            } else if (startupResult.missingRequiredErrors) {
                Assert.assertFalse("Start missing required errors [ " + useShortName + " ]", true);
            } else if (!startupResult.didStop) {
                Assert.assertTrue("Failed to stop [ " + useShortName + " ]", false);
            }
        }
    }

    //

    public static void banner(String m) {
        logInfo(m, "**************************************************************************");
    }

    public static void display(String m, TimingResult timingResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("Feature [ " + timingResult.shortName + " ]: ");

        builder.append(format("Update", timingResult.updateNs));
        builder.append(", ");

        builder.append(format("Start", timingResult.startNs));
        builder.append(", ");

        builder.append(format("PID", timingResult.pidNs));
        builder.append(", ");

        builder.append(format("Verify", timingResult.verifyNs));
        builder.append(',');

        logInfo(m, builder.toString());
        builder.setLength(0);

        builder.append("    ");

        builder.append(format("Stop", timingResult.stopNs));
        builder.append(", ");

        builder.append(format("Kill", timingResult.killNs));
        builder.append(", ");

        builder.append(format("Total", timingResult.totalNs()));

        logInfo(m, builder.toString());
        builder.setLength(0);
    }

    public static void display(String m, Map<String, TimingResult> timingResults) {
        Map<String, TimingSummary> summaries = new LinkedHashMap<>();

        summaries.put("Update", statistics("Update", timingResults, (TimingResult result) -> result.updateNs));
        // summaries.put("Read", statistics("Read", timingResults, (TimingResult result) -> result.readNs));
        // summaries.put("Write", statistics("Write", timingResults, (TimingResult result) -> result.writeNs));
        summaries.put("Start", statistics("Start", timingResults, (TimingResult result) -> result.startNs));
        summaries.put("PID", statistics("PID", timingResults, (TimingResult result) -> result.pidNs));
        summaries.put("Verify", statistics("Verify", timingResults, (TimingResult result) -> result.verifyNs));
        summaries.put("Stop", statistics("Stop", timingResults, (TimingResult result) -> result.stopNs));
        summaries.put("Kill", statistics("Kill", timingResults, (TimingResult result) -> result.killNs));
        summaries.put("Total", statistics("Total", timingResults, (TimingResult result) -> result.totalNs()));

        logInfo(m, "Timing Summary:");

        StringBuilder builder = new StringBuilder();
        summaries.forEach((description, summary) -> {
            builder.append("[ ");
            builder.append(description);
            builder.append(" ]: ");

            if (summary.count == 0) {
                builder.append("** NONE **");
                logInfo(m, builder.toString());
                builder.setLength(0);

            } else {
                builder.append(format("Avg", summary.avg) + " ( " + summary.count + " ): ");
                builder.append(format("Total", summary.sum));
                logInfo(m, builder.toString());
                builder.setLength(0);

                logInfo(m, "  " + formatStat("Min", summary.min, summary.minShort));
                logInfo(m, "  " + formatStat("Max", summary.max, summary.maxShort));
            }
        });
    }

    public static class TimingSummary {
        public final String description;

        public TimingSummary(String description) {
            this.description = description;
        }

        public int count = 0;
        public long sum = 0L;
        public long avg = UNSET_NS;

        public long min = UNSET_NS;
        public String minShort = null;

        public long max = UNSET_NS;
        public String maxShort = null;
    }

    public static TimingSummary statistics(String description, Map<String, TimingResult> timingResults, ToLongFunction<TimingResult> producer) {
        TimingSummary summary = new TimingSummary(description);

        for (Map.Entry<String, TimingResult> resultEntry : timingResults.entrySet()) {
            String shortName = resultEntry.getKey();
            TimingResult result = resultEntry.getValue();
            long stat = producer.applyAsLong(result);

            if (stat == UNSET_NS) {
                continue;
            }

            summary.count++;
            summary.sum += stat;

            if ((summary.min == UNSET_NS) || (stat < summary.min)) {
                summary.min = stat;
                summary.minShort = shortName;
            }
            if ((summary.max == UNSET_NS) || (stat > summary.max)) {
                summary.max = stat;
                summary.maxShort = shortName;
            }
        }

        if (summary.count != 0) {
            summary.avg = summary.sum / summary.count;
        }

        return summary;
    }

    public static final long NS_IN_SEC = 1000000000;
    public static final long UNSET_NS = -1L;

    public static long sum(long... toAdd) {
        long sum = 0L;
        for (long nextToAdd : toAdd) {
            if (nextToAdd != UNSET_NS) {
                sum += nextToAdd;
            }
        }
        return sum;
    }

    public static final String nsAsSec(long ns) {
        return String.format("%.4f", Float.valueOf(((float) ns) / NS_IN_SEC));
    }

    public static String format(String description, long ns) {
        String nsText;
        if (ns == UNSET_NS) {
            nsText = "**UNSET**";
        } else {
            nsText = nsAsSec(ns);
        }

        return description + " [ " + nsText + " ]";
    }

    public static String formatStat(String description, long stat, String shortName) {
        if (stat == UNSET_NS) {
            return description + " [ **UNSET** ]";
        } else {
            return description + " [ " + nsAsSec(stat) + " ] ( " + shortName + " )";
        }
    }

    /**
     * Display values as a comma-delimited list, with values split across lines
     * at the specified length.
     *
     * @param m      The method requesting the display.
     * @param prefix A prefix to display on each line.
     * @param length The length at which to wrap the emitted lines.
     * @param values The values which are to be displayed.
     */
    public static void display(String m, String prefix, int length, Collection<String> values) {
        StringBuilder builder = new StringBuilder();
        int valuesOnLine = 0;

        int numValues = values.size();
        int valueNo = 0;

        for (String value : values) {
            if (valuesOnLine > 0) {
                builder.append(','); // Always need a comma.

                int spaceNeeded = 1; // Room for a space.
                spaceNeeded += value.length(); // Room for the value.
                if (valueNo < numValues - 1) {
                    spaceNeeded++; // Room for a comma after the value.
                }

                if ((builder.length() + spaceNeeded) > length) {
                    logInfo(m, builder.toString());
                    builder.setLength(0);
                    valuesOnLine = 0;
                } else {
                    builder.append(' ');
                }
            }

            if (valuesOnLine == 0) {
                builder.append(prefix);
            }

            // The first value on each line is added without
            // checking the length.  That allows over-size values
            // to be displayed.

            builder.append(value);
            valuesOnLine++;

            valueNo++;
        }

        if (valuesOnLine > 0) {
            logInfo(m, builder.toString());
            builder.setLength(0);
        }
    }

    public static void display(String m,
                               String head, String middle, String tail,
                               List<String> keys, Map<String, String> values) {
        keys.forEach((name) -> {
            logInfo(m, head + name + middle + values.get(name) + tail);
        });
    }
}

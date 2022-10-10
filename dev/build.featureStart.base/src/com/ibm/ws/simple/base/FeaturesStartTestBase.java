/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.simple.base;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.function.ToLongFunction;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

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
    // TODO: A cleaner implementation would have the test base have
    //       instance state.  Keeping the use of static / class state
    //       since the test implementation evolved from a single FAT
    //       test class which had a static injected test server,
    //       and which relies on the static before/after class API.

    public static void setParameters(Class<?> c,
                                     LibertyServer server, String serverName,
                                     int numBuckets, int bucketNo, int sparsity) {
        FeaturesStartTestBase.c = c;

        FeaturesStartTestBase.server = server;
        FeaturesStartTestBase.serverName = serverName;
        FeaturesStartTestBase.serverConfigPath = server.getServerConfigurationPath();

        // TODO: Not sure if having 'NUM_BUCKETS' be set is best.
        //       All of the buckets should set the same value.
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

    //

    static int JAVA_LEVEL;

    static boolean isHealthCenterAvailable() {
        // TODO: Is this the correct implementation of this test?
        // How is the test specific to the java which is running the test
        // server?  The test seems to be specific to the java running the
        // FAT.

        return JavaInfo.isSystemClassAvailable("com.ibm.java.diagnostics.healthcenter.agent.mbean.HealthCenter");
    }

    // Test features within a bucket, with an assigned range within
    // the features list.
    //
    // Tests run by this class are conditioned entirely on the number
    // of buckets and the bucket number.
    //
    // The server name must be updated to match the bucket parameters.

    static int NUM_BUCKETS;
    static int BUCKET_NO;

    // Control parameter: Must be 0 or greater.
    // If greater than 0, test a subset of features, 1 of every SPARSITY.
    //
    // For example, setting '10' means run every 10'th test.
    // (At least 1 test is always run.)
    //
    // Use this when testing to limit the number of features which are
    // started.

    static int SPARSITY;

    static int FIRST_FEATURE_NO; // The first test to be run.
    static int LAST_FEATURE_NO; // One past the last test to be run.

    static final Map<String, Integer> requiredLevels = new TreeMap<>();
    static final List<String> features = new ArrayList<>();
    static final Map<String, String[]> allowedErrors = new HashMap<>();

    // The server name is set according to the bucket parameters.
    public static LibertyServer server;
    public static String serverName;
    public static String serverConfigPath;

    // TODO: Should this be cached?  There is currently only
    //       one use, but that might change.

    public static boolean isServerZOS() {
        String m = "isServerZOS";
        try {
            return server.getMachine().getOperatingSystem().equals(OperatingSystem.ZOS);
        } catch (Exception e) {
            // This should never happen; if it does, we will run possibly one extra
            // test.
            logError(m, "Unexpected failure", e);
            return false;
        }
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
     * @param pid           The PID of the running server.
     * @param allowedErrors Errors allowed in the stop server command.
     * @param failures      Storage for recording failures.
     * @param timingResult  Storage for timing data.
     *
     * @return True or false telling if the stop was successful.
     */
    public static boolean forceStopServer(String shortName,
                                          String pid,
                                          String[] allowedErrors,
                                          Map<String, String> failures,
                                          TimingResult timingResult) {
        String m = "forceStopServer";

        String description = "Server [ " + serverName + " ] PID [ " + pid + " ] Feature [ " + shortName + " ]";

        boolean didStop;
        boolean didKill;

        try {
            if (server.isStarted()) {
                logInfo(m, "Stopping: " + description);
                if (allowedErrors != null) {
                    logInfo(m, "Allowed errors [ " + Arrays.toString(allowedErrors) + " ]");
                }

                long initialStopNs = timingResult.getTimeNs();
                Exception boundException;
                try {
                    server.stopServer(allowedErrors);
                    didStop = true;
                    boundException = null;
                } catch (Exception e) {
                    didStop = false;
                    boundException = e;
                } finally {
                    timingResult.setStopNsFromInitial(initialStopNs);
                }
                // Handle the result *outside* of the timing block.
                if (boundException == null) {
                    logInfo(m, "Stopped: " + description);
                } else {
                    addFailure(m, failures, shortName, "Stop Exception", boundException);
                }
            } else {
                logInfo(m, "Not started: " + description);
                didStop = true;
            }

        } finally {
            if (pid != null) {
                logInfo(m, "Killing: " + description);
                long initialKillNs = timingResult.getTimeNs();
                Exception boundException;
                try {
                    killProcess(pid);
                    didKill = true;
                    boundException = null;
                } catch (Exception e) {
                    didKill = false;
                    boundException = e;
                } finally {
                    timingResult.setKillNsFromInitial(initialKillNs);
                }
                // Handle the result *outside* of the timing block.
                if (boundException == null) {
                    logInfo(m, "Killed: " + description);
                } else {
                    addFailure(m, failures, shortName, "Kill Exception", boundException);
                }
            } else {
                logInfo(m, "Null PID: " + description);
                didKill = true;
            }
        }

        return (didStop && didKill);
    }

    //

    /**
     * Perform class level initialization:
     *
     * The FAT runner must have already injected the test server.
     *
     * Read the required java levels for features from the test server.
     *
     * Determine the java level used by the test server.
     *
     * Read and filter features from the test server.
     *
     * Setup the table of allowed errors, per feature short name.
     *
     * @throws Exception Thrown if the class level initialization failed. This
     *                       would be most likely because of a failure to read feature data, or
     *                       because the build produced no testable features.
     */
    public static void setUp() throws Exception {
        initRequiredLevels(); // This is a static table read.
        initJavaInfo();
        initFeatures(); // Uses the java level as one of the feature filters.
        initAllowedErrors(); // Static table of known allowable errors.
    }

    //

    /**
     * Determine the major version of the java which is being used by the server.
     *
     * This relies on the FAT running having already injected the server.
     */
    public static void initJavaInfo() throws Exception {
        JAVA_LEVEL = JavaInfo.forServer(server).majorVersion();
    }

    // TODO: Should this perhaps be initialized from java code?  Having the
    // external properties file doesn't seem to add anything.

    public static final String REQUIRED_LEVELS_NAME = "/feature-java-levels.properties";

    /**
     * Read the table of minimum required java levels for features.
     *
     * Keys are feature names, all lower case. Values are integer values
     * representing a minimum java level.
     *
     * @throws IOException Thrown if the properties file could not be read.
     */
    public static void initRequiredLevels() throws IOException {
        Properties props = new Properties();
        try (InputStream input = FeaturesStartTestBase.class.getResourceAsStream(REQUIRED_LEVELS_NAME)) {
            props.load(input);
        }

        props.forEach((sName, reqLevel) -> {
            String shortName = ((String) sName).toLowerCase();
            Integer requiredLevel = Integer.valueOf((String) reqLevel);
            requiredLevels.put(shortName, requiredLevel);
        });
    }

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

        File featuresDir = new File(server.getInstallRoot() + "/lib/features/");
        String featuresPath = featuresDir.getAbsolutePath();

        if (!featuresDir.exists()) {
            throw new IOException("Folder [ " + featuresPath + " ] does not exist");
        }

        File[] featureManifests = featuresDir.listFiles();
        if (featureManifests == null) {
            throw new IOException("Folder [ " + featuresPath + " ] could not be listed");
        } else if (featureManifests.length == 0) {
            throw new IOException("Folder [ " + featuresPath + " ] is empty");
        }

        Arrays.sort(featureManifests, (f1, f2) -> f1.getName().compareTo(f2.getName()));

        // 'parseShortName' filters on OSGI meta-information:
        // client features, test features, and non-public features are filtered.
        //
        // 'skipFeature' filters on feature name and external information, for
        // example, required java level, required platform, disallowed as singleton.

        List<String> testFeatures = new ArrayList<>();
        List<String> nonPublicFeatures = new ArrayList<>();

        for (File featureManifest : featureManifests) {
            String shortName = parseShortName(featureManifest, nonPublicFeatures, testFeatures);
            if (shortName == null) {
                continue; // Not readable, or filtered.
            }

            String skipReason = skipFeature(shortName);
            if (skipReason != null) {
                logInfo(m, "Cannot test feature [ " + shortName + " ]: " + skipReason);
                continue;
            }

            features.add(shortName);
        }

        if (!nonPublicFeatures.isEmpty()) {
            logInfo(m, "Cannot test features [ " + nonPublicFeatures.size() + " ]: non-public");
            display(m, "    ", 80, nonPublicFeatures);
        }

        if (!testFeatures.isEmpty()) {
            logInfo(m, "Cannot test features [ " + testFeatures.size() + " ]: test");
            display(m, "    ", 80, testFeatures);
        }

        if (features.isEmpty()) {
            throw new IOException("Folder [ " + featuresPath + " ] has no testable features");
        }

        // All tests *MUST* use the same features, in the same order.
        features.sort((n1, n2) -> n1.compareTo(n2));

        // Limit the features to the current bucket.
        int[] range = getRange(features.size(), NUM_BUCKETS, BUCKET_NO);
        FIRST_FEATURE_NO = range[0];
        LAST_FEATURE_NO = range[1];
    }

    /**
     * Parse the short name of a feature manifest.
     *
     * Answer null if the feature manifest is a directory, or is not actually a manifest.
     * Answer null if the feature manifest is missing a short name value, or is a client
     * feature, a test feature, or a non-public feature.
     *
     * @param featureFile The feature manifest file which is to be parsed.
     * @param nonPublic   Storage for non-public feature names.
     * @param test        Storage for test feature names.
     *
     * @return The short name from the feature file.
     *
     * @throws IOException Thrown if an error occurs while reading the feature file.
     */
    public static String parseShortName(File featureFile, List<String> nonPublic, List<String> test) throws IOException {
        String m = "parseShortName";

        // The features directory has additional, non-manifest files.
        // Ignore them.
        if (featureFile.isDirectory() || !featureFile.getName().endsWith(".mf")) {
            return null;
        }

        try (Scanner scanner = new Scanner(featureFile)) {
            String symbolicName = null;
            String shortName = null;
            String unusableReason = null;

            boolean isNonPublic = false;
            boolean isTest = false;

            // Keep looping after setting the reason, so to have the
            // short name to display.
            while (((unusableReason == null) || (shortName == null) || (symbolicName == null)) &&
                   scanner.hasNextLine()) {

                String line = scanner.nextLine();

                // TODO: This checking is approximate!
                // Metadata may be split across multiple lines.

                if ((shortName == null) && line.startsWith("IBM-ShortName:")) {
                    shortName = line.substring("IBM-ShortName:".length()).trim();
                    String upperShortName = shortName.toUpperCase();
                    if (upperShortName.contains("EECLIENT") || upperShortName.contains("SECURITYCLIENT")) {
                        unusableReason = "client-only";
                    }

                } else if (line.contains("IBM-Test-Feature:") && line.contains("true")) {
                    isTest = true;
                    unusableReason = "test";

                } else if (line.startsWith("Subsystem-SymbolicName:")) {
                    // A short name is not available for most non-public features.
                    // Read the symbolic name as an alternative name.

                    String tail = line.substring("Subsystem-SymbolicName:".length());
                    int semiPos = tail.indexOf(';');
                    if (semiPos == -1) {
                        symbolicName = tail.trim();
                    } else {
                        symbolicName = tail.substring(0, semiPos).trim();
                    }

                    if (!tail.contains("visibility:=public")) {
                        isNonPublic = true;
                        unusableReason = "non-public";
                    }
                }
            }

            // TODO: Do all public features have a short name?
            // At least one feature, 'configfatlibertyinternals-1.0', has no short name,
            // and is not filtered by any of the tests.

            if (unusableReason == null) {
                if (shortName == null) {
                    unusableReason = "No 'IBM-ShortName'";
                }
            }

            if (unusableReason != null) {
                if (isNonPublic || isTest) {
                    String useName;
                    if (shortName != null) {
                        useName = shortName;
                    } else if (symbolicName != null) {
                        useName = symbolicName;
                    } else {
                        useName = featureFile.getName();
                        logInfo(m, "Strange: [ " + featureFile.getAbsolutePath() + " ] has no symbolic name");
                    }
                    if (isNonPublic) {
                        nonPublic.add(useName);
                    } else {
                        test.add(useName);
                    }
                    // Log non-public and test features all together.  Otherwise,
                    // these bloat the log.
                } else {
                    logInfo(m, "Cannot test feature [ " + symbolicName + " ] [ " + shortName + " ]: " + unusableReason);
                }
                return null;
            } else {
                return shortName;
            }
        }
    }

    /**
     * Tell if a feature is to be skipped.
     *
     * Feature testing attempts to start the server using every
     * configured feature.
     *
     * However, for a variaty of reasons, a feature may not be
     * testable. The most common reasons are because of a JDK or System
     * dependencies, or a dependency on a configuration value which is
     * not set by these simple tests.
     *
     * These tests are for all buckets, not just the current bucket.
     *
     * @param shortName The short name of the feature which is to be tested.
     * @return Null if the feature is to be tested. A string message if the
     *         feature is not to be tested.
     */
    public static String skipFeature(String shortName) {
        // TODO: We don't check if the server is running on z/OS,
        //       although we do check later in this method.
        // TODO: Condition this on z/OS.
        // z/OS Connect is NOT a z/OS only feature.  Every other
        // "zos" prefix feature is for z/OS.
        if (((shortName.startsWith("zos") && !shortName.startsWith("zosconnect-")) ||
             shortName.equalsIgnoreCase("batchSMFLogging-1.0"))) {
            return "z/OS only";
        }

        // Don't test this feature if environment is using Java level below minimum required
        // specified in properties file. Not every feature has a mapping in that file.
        Integer javaLevel = requiredLevels.get(shortName.toLowerCase());
        if ((javaLevel != null) && (JAVA_LEVEL < javaLevel)) {
            return "Requires java " + javaLevel;
        }

        // This feature is grand-fathered in on not starting cleanly on its own.
        // Fixing it could potentially break existing configurations
        if (shortName.equalsIgnoreCase("wsSecurity-1.1")) {
            return "Cannot start by itself";
        } else if (shortName.equalsIgnoreCase("constrainedDelegation-1.0")) {
            return "Requires spnego-1.0 or OIDC";
        }

        // Only IBM JDK includes Health Center and IBM JDK 11+ (Semeru)
        // is based on Adopt JDK 11+, which does not include Health Center.
        if (shortName.equalsIgnoreCase("logstashCollector-1.0")) {
            if (!isHealthCenterAvailable()) {
                return "Requires Health Center";
            } else if (isServerZOS()) {
                return "Requires the attach API, which is disabled on z/OS";
            }
        }

        // WMQ features require a RAR location variable to be set.
        // These simple tests do not have a RAR and have not set the
        // location variable.
        if (shortName.equalsIgnoreCase("wmqMessagingClient-3.0") ||
            shortName.equalsIgnoreCase("wmqJmsClient-2.0") ||
            shortName.equalsIgnoreCase("wmqJmsClient-1.1")) {
            return "Required variable 'wmqJmsClient.rar.location' is not set";
        }

        return null;
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

    /**
     * Initialize the allowed failure messages.
     *
     * This is a table of error message codes, specified per feature short name.
     *
     * The table spans all buckets.
     */
    public static void initAllowedErrors() {
        // TODO: OpenAPI code needs to be reworked so that it
        // doesn't leave threads around when the server stops
        // before it has finished initializing. Once OpenAPI is
        // fixed, these QUISCE_FAILURES should be removed.
        // TODO: This might be fixed by now.
        String[] QUIESCE_FAILURES = new String[] { "CWWKE1102W", "CWWKE1107W" };
        allowedErrors.put("openapi-3.0", QUIESCE_FAILURES);
        allowedErrors.put("openapi-3.1", QUIESCE_FAILURES);
        allowedErrors.put("mpOpenApi-1.0", QUIESCE_FAILURES);

        allowedErrors.put("batchSMFLogging-1.0", new String[] { "CWWKE0702E: .* com.ibm.ws.jbatch.smflogging" });

        // requires binaryLogging-1.0 to be enabled via bootstrap.properties
        allowedErrors.put("logAnalysis-1.0", new String[] { "CWWKE0702E: .* com.ibm.ws.loganalysis" });

        // The Rtcomm service is not able to connect to tcp://localhost:1883.
        allowedErrors.put("rtcomm-1.0", new String[] { "CWRTC0002E" });
        // The Rtcomm service is not able to connect to tcp://localhost:1883.
        // The Rtcomm service - The following virtual hosts could not be found or are not correctly configured: [abcdefg].
        allowedErrors.put("rtcommGateway-1.0", new String[] { "CWRTC0002E", "SRVE9956W" });

        // lets the user now certain config attributes will be ignored depending on whether or not 'inboundPropagation' is configured
        allowedErrors.put("samlWeb-2.0", new String[] { "CWWKS5207W: .* inboundPropagation" });
        // pulls in the samlWeb-2.0 feature
        allowedErrors.put("wsSecuritySaml-1.1", new String[] { "CWWKS5207W: .* inboundPropagation" });

        // Ignore required config warnings for the 'collectiveMember-1.0' feature, and all features that include it
        String[] COLLECTIVE_MEMBER_WARNINGS = new String[] { "CWWKG0033W: .*collectiveTrust", "CWWKG0033W: .*serverIdentity" };
        allowedErrors.put("collectiveMember-1.0", COLLECTIVE_MEMBER_WARNINGS);
        allowedErrors.put("collectiveController-1.0", COLLECTIVE_MEMBER_WARNINGS);
        allowedErrors.put("clusterMember-1.0", COLLECTIVE_MEMBER_WARNINGS);
        allowedErrors.put("dynamicRouting-1.0", COLLECTIVE_MEMBER_WARNINGS);
        allowedErrors.put("healthAnalyzer-1.0", COLLECTIVE_MEMBER_WARNINGS);
        allowedErrors.put("healthManager-1.0", COLLECTIVE_MEMBER_WARNINGS);
        allowedErrors.put("scalingController-1.0", COLLECTIVE_MEMBER_WARNINGS);
        allowedErrors.put("scalingMember-1.0", COLLECTIVE_MEMBER_WARNINGS);
    }

    //

    public static class StartupResult {
        public final boolean attempted;
        public final boolean started;
        public final String pid;

        /**
         * Factory method: The startup was not even attempted.
         */
        public StartupResult() {
            this.attempted = false;
            this.started = false;
            this.pid = null;
        }

        public static final boolean DID_ATTEMPT = true;
        public static final boolean DID_START = true;

        /**
         * Fully parameterized factory method.
         *
         * 'started' should not be true if 'attempted' is false.
         *
         * 'pid' should be null if 'attempted' is false. 'pid' may be null
         * if 'started' is false. That indicates a startup attempt which left
         * a dangling server process, but which reported failure.
         *
         * @param attempted True or false, telling if the startup was attempted.
         * @param started   True or false telling if the started was successful.
         * @param pid       The PID of the server process.
         */
        public StartupResult(boolean attempted, boolean started, String pid) {
            this.attempted = attempted;
            this.started = started;
            this.pid = pid;
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

    /**
     * Main test: Attempt to start each of the features in the specified bucket.
     *
     * @throws Exception Currently unused. A thrown exception will be logged and
     *                       will cause the test to fail.
     */
    public static void testStartFeatures() throws Exception {
        String m = "testStartFeatures";

        logInfo(m, "Test server: " + serverName);
        logInfo(m, "Test server java: " + JAVA_LEVEL);

        if (features.isEmpty()) {
            banner(m);
            logInfo(m, "No features were selected");
            banner(m);
            return;
        }

        banner(m);
        logInfo(m, "Features [ " + features.size() + " ]");
        logInfo(m, "Bucket [ " + BUCKET_NO + " ] of [ " + NUM_BUCKETS + " ]");
        logInfo(m, "  Count [ " + (LAST_FEATURE_NO - FIRST_FEATURE_NO) + " ]");
        logInfo(m, "  First [ " + FIRST_FEATURE_NO + " ]: [ " + features.get(FIRST_FEATURE_NO) + " ]");
        logInfo(m, "  Last  [ " + (LAST_FEATURE_NO - 1) + " ]: [ " + features.get(LAST_FEATURE_NO - 1) + " ]");
        if (SPARSITY > 0) {
            logInfo(m, "  Sparsity [ " + SPARSITY + " ]");
        }
        banner(m);

        int numFeatures = LAST_FEATURE_NO - FIRST_FEATURE_NO;
        if (SPARSITY > 0) {
            numFeatures /= SPARSITY;
            if ((numFeatures % SPARSITY) > 0) {
                numFeatures++;
            }
        }

        List<String> successes = new ArrayList<>();
        Map<String, String> failures = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();

        Map<String, TimingResult> timingResults = new HashMap<>(numFeatures);

        String lastShortName;
        String nextShortName = null;

        for (int featureNo = FIRST_FEATURE_NO; featureNo < LAST_FEATURE_NO; featureNo++) {
            String shortName = features.get(featureNo);

            if ((SPARSITY > 0) && (((featureNo - FIRST_FEATURE_NO) % SPARSITY) != 0)) {
                skipped.add(shortName);
                logInfo(m, "Skipping [ " + shortName + " ]: Filtered by SPARSITY");
                continue;
            }

            lastShortName = nextShortName;
            nextShortName = shortName;

            TimingResult timingResult = new TimingResult(nextShortName);
            timingResults.put(nextShortName, timingResult);

            try {
                StartupResult startupResult = null;
                try {
                    startupResult = startFeature(lastShortName, nextShortName, failures, timingResult);

                } finally {
                    // A null result is only possible if 'startFeature' failed with a throwable.
                    // In this case, do our best to stop the server.
                    // Make a dummy result that looks like an attempted startup.

                    if (startupResult == null) {
                        startupResult = new StartupResult(StartupResult.DID_ATTEMPT, !StartupResult.DID_START, getPid());
                    }

                    if (startupResult.attempted) {
                        if (forceStopServer(nextShortName, startupResult.pid, allowedErrors.get(nextShortName), failures, timingResult)) {
                            if (!failures.containsKey(nextShortName)) {
                                successes.add(nextShortName);
                            }
                        }
                    }
                }

            } finally {
                display(m, timingResult);
            }
        }

        logInfo(m, "Successes [ " + successes.size() + " ]");
        if (!successes.isEmpty()) {
            display(m, "    ", 80, successes);
        }

        logInfo(m, "Failures [ " + failures.size() + " ]");
        if (!failures.isEmpty()) {
            display(m, "    ", 80, failures.keySet());
        }

        if (!skipped.isEmpty()) {
            logInfo(m, "Skipped [ " + skipped.size() + " ]");
            display(m, "    ", 80, skipped);
        }

        display(m, timingResults);

        if (!failures.isEmpty()) {
            assertTrue("Features [ " + failures.keySet() + " ] should have started.", false);
        }
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
     * @param lastShortName The last configured short name. May be null.
     * @param shortName     The short name of the feature which is to be started.
     * @param failures      Storage for features which failed to start.
     * @param timingResult  Storage for time recording.
     *
     * @return The PID of the started server. Null if the feature could not be
     *         configured, or if the server startup was attempted but failed.
     */
    public static StartupResult startFeature(String lastShortName, String shortName, Map<String, String> failures, TimingResult timingResult) {
        String m = "startFeature";

        long initialUpdateNs = timingResult.getTimeNs();

        try {
            setFeature(lastShortName, shortName, timingResult);

        } catch (Exception e) {
            addFailure(m, failures, shortName, "Failed to set feature", e);

            // Complete failure: The start was not attempted.
            return new StartupResult(!StartupResult.DID_ATTEMPT, !StartupResult.DID_START, null);

        } finally {
            timingResult.setUpdateNsFromInitial(initialUpdateNs);
        }

        // 'attempted' is now true

        long initialStartNs = timingResult.getTimeNs();
        boolean started;
        try {
            // Default start: Pre-clean and clean the server.
            server.startServer(shortName + ".log");
            started = true;
        } catch (Exception e) {
            started = false;
            addFailure(m, failures, shortName, "Start Exception", e);
        } finally {
            timingResult.setStartNsFromInitial(initialStartNs);
        }

        // The PID may or may not be available:
        // PID retrieval is performed even if 'started' is false, so to handle
        // the case of an apparently failed startup which left a dangline process.

        long initialPidNs = timingResult.getTimeNs();

        String pid = getPid();
        logInfo(m, "Server PID: " + pid);

        timingResult.setPidNsFromInitial(initialPidNs);

        // There is no point to doing feature startup verification if the
        // server did not start cleanly.

        if (started) {
            long initialVerifyNs = timingResult.getTimeNs();
            try {
                List<String> errors = server.findStringsInLogs("CWWKF0032E");
                if (!errors.isEmpty()) {
                    addFailure(m, failures, shortName, "Verification Failure", null);
                    for (String error : errors) {
                        logError(m, "Server failure message [ " + error + " ]");
                    }
                }
            } catch (Exception e) {
                addFailure(m, failures, shortName, "Verify Exception", e);
            } finally {
                timingResult.setVerifyNsFromInitial(initialVerifyNs);
            }
        }

        return new StartupResult(StartupResult.DID_ATTEMPT, started, pid);
    }

    public static void addFailure(String m, Map<String, String> failures, String shortName, String description, Throwable th) {
        logError(m, "Failed to start feature [ " + shortName + " ]: " + description, th);
        failures.put(shortName, shortName);
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
}

package com.ibm.ws.test.featurestart;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;

import com.ibm.ws.test.featurestart.FeaturesStartTiming.TimingResult;
import com.ibm.ws.test.featurestart.FeaturesStartTiming.TimingSummary;
import com.ibm.ws.test.featurestart.features.FeatureLevels;

import componenttest.topology.impl.LibertyServer;

public class FeaturesStartResults {
    public static void logInfo(String m, String msg) {
        FeaturesStartTestBase.logInfo(m, msg);
    }

    public static void logError(String m, String msg) {
        FeaturesStartTestBase.logError(m, msg);
    }
    
    public static void logError(String m, String msg, Exception e) {
        FeaturesStartTestBase.logError(m, msg, e);
    }

    // Timing utilities ...
    
    protected static String format(String description, long ns) {
        return FeaturesStartTiming.format(description, ns);
    }
    
    protected static String formatStat(String description, long stat, String shortName) {
        return FeaturesStartTiming.formatStat(description, stat, shortName);
    }
    
    protected static TimingSummary statistics(
        String description,
        Map<String, TimingResult> timingResults,
        ToLongFunction<TimingResult> producer) {
        
        return FeaturesStartTiming.statistics(description, timingResults, producer);
    }

    // Display utilities ...
    
    protected static void display(
        String m,
        String head, int width,
        Collection<String> featureShortNames,
        StringBuilder builder) {
            
        FeaturesStartReporting.display(m, head, width, featureShortNames, builder);
    }

    public static void display(String m,
            String prefix, String nestedPrefix, int length,
            Map<String, ? extends Collection<String>> values,
            StringBuilder builder) {
        
        FeaturesStartReporting.display(m, prefix, nestedPrefix, length, values, builder);
    }

    //
    
    public FeaturesStartResults(
            FeaturesStartServer server,
            FeaturesStartFeatures features,
            FeaturesStartParameters parameters) {

        // Context parameters:

        this.server = server;
        this.features = features;
        this.parameters = parameters;

        // Test state:

        this.lastShortName = null;
        this.nextShortName = null;

        // Test results:
            
        // Linked maps and sets are used so to keep the results in test order.

        this.successes = new LinkedHashSet<>();
        this.failures = new LinkedHashSet<>();
            
        this.failuresUnexpectedLevelSuccesses = new LinkedHashSet<>();
            
        this.failuresMissingModules = new LinkedHashMap<>();            
        this.failuresMissingBundles = new LinkedHashMap<>();
        this.failuresOther = new LinkedHashMap<>();

        // Timing records:

        this.timingResults = new HashMap<>(parameters.numFeatures);            
    }

    //
    
    protected final FeaturesStartServer server;
    protected final FeaturesStartFeatures features;
    protected final FeaturesStartParameters parameters;

    // Test state

    public String lastShortName;
    public String nextShortName;

    /**
     * Set the short name of the next feature which is to be tested.
     * 
     * Keep a reference to the current feature.
     * 
     * Set the allowed errors data.
     * 
     * @param featureShortName The short name of the next feature which is
     *     to be tested.
     */
    protected void updateFeatureName(String featureShortName) {
        lastShortName = nextShortName;
        nextShortName = featureShortName;

        setErrors();
    }

    // Java runtime error:
    //
    // [2/7/23 23:08:24:907 EST] 00000033 com.ibm.ws.kernel.feature.internal.FeatureManager
    //   E CWWKF0032E: The io.openliberty.jakarta.expressionLanguage-5.0 feature requires
    //   a minimum Java runtime environment version of JavaSE 11.

    protected static final String JAVA_LEVEL_ERROR = "CWWKF0032E";

    // The missing module error occurs specifically when a java runtime error occurs:

    // [2/8/23 12:22:13:451 EST] 00000024 LogService-25-io.openliberty.java11.internal
    //   E CWWKE0702E: Could not resolve module: io.openliberty.java11.internal [25]
            
    protected static final String MISSING_MODULE_ERROR = "CWWKE0702E: Could not resolve module: ";
    protected static final String MISSING_BUNDLE_ERROR = "CWWKF0002E: A bundle could not be found for ";

    protected static final String[] OUT_OF_LEVEL_ERRORS = { JAVA_LEVEL_ERROR, MISSING_MODULE_ERROR, MISSING_BUNDLE_ERROR };
    protected static final String OUT_OF_LEVEL_ERRORS_TEXT = asString(OUT_OF_LEVEL_ERRORS);
    protected static final String OUT_OF_LEVEL_ERRORS_REGEX = asRegEx(OUT_OF_LEVEL_ERRORS);

    protected static final String[] MISSING_ERRORS = { MISSING_MODULE_ERROR, MISSING_BUNDLE_ERROR };
    protected static final String MISSING_ERRORS_TEXT = asString(MISSING_ERRORS);
    protected static final String MISSING_ERRORS_REGEX = asRegEx(MISSING_ERRORS);

    //

    public String[] nextIgnoredErrors;
    public String nextIgnoredErrorsRegEx;
    public boolean nextIsOutOfLevel;
    public boolean nextIsOther;

    protected void setErrors() {
        String m = "setErrors";
        
        boolean isOutOfLevel = features.outOfLevelFeatureNames.contains(nextShortName);
        boolean isOther;
        
        String[] errors;
        String errorsList;
        String errorsRegEx;

        String errorsCase;

        if ( isOutOfLevel ) {
            isOther = false;

            errors = OUT_OF_LEVEL_ERRORS;
            errorsList = OUT_OF_LEVEL_ERRORS_TEXT;
            errorsRegEx = OUT_OF_LEVEL_ERRORS_REGEX;

            errorsCase = "Out-of-level (expected)";

        } else {
            String[] featureAllowedErrors = features.getAllowedErrors(nextShortName);
            if ( featureAllowedErrors != null ) {
                isOther = true;

                errors = asArray(featureAllowedErrors, MISSING_ERRORS);
                errorsList = asString(errors);
                errorsRegEx = asRegEx(errors);

                errorsCase = "Feature specified (expected)";

            } else {
                isOther = false;
                
                errors = MISSING_ERRORS;
                errorsList = MISSING_ERRORS_TEXT;
                errorsRegEx = MISSING_ERRORS_REGEX;

                errorsCase = "Clean";                
            }
        }

        nextIgnoredErrors = errors;
        nextIgnoredErrorsRegEx = errorsRegEx;
        nextIsOutOfLevel = isOutOfLevel;
        nextIsOther = isOther;

        logInfo(m, "Feature errors: " + errorsCase);
        logInfo(m, "    " + errorsList);
    }

    // Error detection utility:

    // Issue 35843 Need improvement to FeatureStart reporting of missing feature errors:
    //
    // https://github.com/OpenLiberty/open-liberty/issues/35843
    //
    // For example, the following "could not resolve module errors" should be reported as "missing bundle" type errors.
    // The "Failed to stop feature" message is misleading.

    // [09/21/2026 00:30:18:689 UTC] 002 FeaturesStartTest3             forceStopServer                S
    //   Failed to stop feature [ mpContextPropagation-1.2 ]: Server [ features.start.3.server ] PID [ null ] Feature [ mpContextPropagation-1.2 ]
    //
    // java.lang.Exception: Errors/warnings were found in server features.start.3.server logs:
    //  [9/21/26, 0:30:17:218 UTC] 0000002b com.ibm.ws.kernel.feature.internal.Provisioner               E
    //    CWWKF0002E: A bundle could not be found for io.openliberty.org.eclipse.microprofile.contextpropagation.1.2/[1.0.0,1.1.0).
    //  [9/21/26, 0:30:17:647 UTC] 0000001e LogService-56-com.ibm.ws.concurrent                          E
    //    CWWKE0702E: Could not resolve module: com.ibm.ws.concurrent [56]
    //  [9/21/26, 0:30:17:649 UTC] 0000001e ogService-57-io.openliberty.concurrent.internal.basictrigger E
    //    CWWKE0702E: Could not resolve module: io.openliberty.concurrent.internal.basictrigger [57]
    //  [9/21/26, 0:30:17:683 UTC] 0000001e LogService-59-com.ibm.ws.microprofile.contextpropagation.1.0 E
    //    CWWKE0702E: Could not resolve module: com.ibm.ws.microprofile.contextpropagation.1.0 [59]
    //  [9/21/26, 0:30:17:684 UTC] 0000001e vice-60-io.openliberty.microprofile.context.cleared.internal E
    //    CWWKE0702E: Could not resolve module: io.openliberty.microprofile.context.cleared.internal [60]
    //
    //  at componenttest.topology.impl.LibertyServer.checkLogsForErrorsAndWarnings(LibertyServer.java:4006)
    //  at componenttest.topology.impl.LibertyServer.stopServer(LibertyServer.java:3811)
    //  at componenttest.topology.impl.LibertyServer.stopServer(LibertyServer.java:3646)
    //  at componenttest.topology.impl.LibertyServer.stopServer(LibertyServer.java:3640)
    //  at componenttest.topology.impl.LibertyServer.stopServer(LibertyServer.java:3616)
    //  at componenttest.topology.impl.LibertyServer.stopServer(LibertyServer.java:3523)
    //  at componenttest.topology.impl.LibertyServer.stopServer(LibertyServer.java:3498)
    //  at com.ibm.ws.test.featurestart.FeaturesStartTestBase.forceStopServer(FeaturesStartTestBase.java:325)
    //  at com.ibm.ws.test.featurestart.FeaturesStartTestBase$TestState.forceStopFeature(FeaturesStartTestBase.java:1176)
    //  at com.ibm.ws.test.featurestart.FeaturesStartTestBase.basicTestStartFeature(FeaturesStartTestBase.java:1268)
    //  at com.ibm.ws.test.featurestart.FeaturesStartTestBase.testStartFeature(FeaturesStartTestBase.java:1226)
    //  at com.ibm.ws.test.featurestart.FeaturesStartTest3.test(FeaturesStartTest3.java:71)

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
    
    // Missing bundles appear eventually as a stop failure.
    // Reporting just the stop failure is confusing. The real failures
    // are the missing bundles.
    //
    // For example:
    // [09/21/2026 00:30:18:689 UTC] 002 FeaturesStartTest3 forceStopServer S
    //   Failed to stop feature [ mpContextPropagation-1.2 ]: Server [ features.start.3.server ] PID [ null ] Feature [ mpContextPropagation-1.2 ]
    // java.lang.Exception: Errors/warnings were found in server features.start.3.server logs:
    // [9/21/26, 0:30:17:218 UTC] 0000002b com.ibm.ws.kernel.feature.internal.Provisioner E
    //   CWWKF0002E: A bundle could not be found for io.openliberty.org.eclipse.microprofile.contextpropagation.1.2/[1.0.0,1.1.0).
    // [9/21/26, 0:30:17:647 UTC] 0000001e LogService-56-com.ibm.ws.concurrent E
    //   CWWKE0702E: Could not resolve module: com.ibm.ws.concurrent [56]
    // [9/21/26, 0:30:17:649 UTC] 0000001e ogService-57-io.openliberty.concurrent.internal.basictrigger E
    //   CWWKE0702E: Could not resolve module: io.openliberty.concurrent.internal.basictrigger [57]
    // [9/21/26, 0:30:17:683 UTC] 0000001e LogService-59-com.ibm.ws.microprofile.contextpropagation.1.0 E
    //   CWWKE0702E: Could not resolve module: com.ibm.ws.microprofile.contextpropagation.1.0 [59]
    // [9/21/26, 0:30:17:684 UTC] 0000001e vice-60-io.openliberty.microprofile.context.cleared.internal E
    //   CWWKE0702E: Could not resolve module: io.openliberty.microprofile.context.cleared.internal [60]        
    //
    // Per issue 35843, collected errors are examined and missing bundles are specifically reported.
    // See: https://github.com/OpenLiberty/open-liberty/issues/35843
    
    // Missing content errors 0702E and 0002E.

    // [9/21/26, 0:30:17:684 UTC] 0000001e vice-60-io.openliberty.microprofile.context.cleared.internal E
    //   CWWKE0702E: Could not resolve module: io.openliberty.microprofile.context.cleared.internal [60]
    //
    // [9/21/26, 0:30:17:218 UTC] 0000002b com.ibm.ws.kernel.feature.internal.Provisioner E
    //   CWWKF0002E: A bundle could not be found for io.openliberty.org.eclipse.microprofile.contextpropagation.1.2/[1.0.0,1.1.0).
    
    // Result tables:

    public final Set<String> successes;
    public final Set<String> failures;

    public final Set<String> failuresUnexpectedLevelSuccesses; 
    public final Map<String, Set<String>> failuresMissingModules;
    public final Map<String, Set<String>> failuresMissingBundles;
    public final Map<String, Set<String>> failuresOther;

    // Result tables ...

    public boolean didSucceed() {
        return successes.contains(nextShortName); 
    }

    private void recordSuccess(String m) {
        successes.add(nextShortName);
        logInfo(m, "Success [ " + nextShortName + " ]");
    }

    public boolean didFail() {
        return failures.contains(nextShortName); 
    }

    protected void recordFailure(String m) {
        if ( failures.add(nextShortName) ) {
            logInfo(m, "Failure [ " + nextShortName + " ]");
        }
    }

    protected void recordMissingModule(String m, String moduleName) {
        recordFailure(m);

        Set<String> featureMissingModules = 
                failuresMissingModules.computeIfAbsent(nextShortName, (useShortName -> new LinkedHashSet<String>()));
        featureMissingModules.add(moduleName);

        logError(m, "Feature [ " + nextShortName + " ]: Missing module [ " + moduleName + " ]");
    }

    protected void recordMissingBundle(String m, String bundleName) {
        recordFailure(m);

        Set<String> featureMissingBundles = 
                failuresMissingBundles.computeIfAbsent(nextShortName, (useShortName -> new LinkedHashSet<String>()));
        featureMissingBundles.add(bundleName);

        logError(m, "Feature [ " + nextShortName + " ]: Missing bundle [ " + bundleName + " ]");            
    }

    private void rawRecordOtherFailure(String m, String failure) {
        Set<String> featureOtherFailures = 
                failuresOther.computeIfAbsent(nextShortName, (useShortName -> new LinkedHashSet<String>()));
        featureOtherFailures.add(failure);
    }

    protected void recordOtherFailure(String m, String failure) {
        recordFailure(m);            
        rawRecordOtherFailure(m, failure);
        logError(m, "Feature failure [ " + nextShortName + " ]: " + failure);
    }

    protected void recordOtherFailure(String m, String failure, Exception e) {
        recordFailure(m);
        failure = "Feature failure [ " + nextShortName + " ]: " + failure;
        rawRecordOtherFailure(m, failure + ": " + e);
        logError(m, failure, e);
    }
    
    protected void recordUnexpectedSuccess(String m) {
        recordFailure(m);
        failuresUnexpectedLevelSuccesses.add(nextShortName);
        logError(m, "Unexpected success [ " + nextShortName + " ]");
    }

    
    
    // Timing results ...

    public final Map<String, TimingResult> timingResults;

    public TimingResult addTiming() {
        TimingResult timingResult = new TimingResult(nextShortName);
        timingResults.put(nextShortName, timingResult);
        return timingResult;
    }

    // Main test loop ...

    public static class StartupResult {
        public static final boolean DID_ATTEMPT = true;
        public static final boolean DID_START = true;
        public static final boolean DID_STOP = true;

        public final boolean attempted;
        public final boolean started;
        public final String pid;
        public boolean stopped;

        public static StartupResult notAttemptedResult() {
            return new StartupResult(!DID_ATTEMPT, !DID_START, null);
        }
        
        public static StartupResult notAttemptedResult(String pid) {
            return new StartupResult(!DID_ATTEMPT, !DID_START, pid);
        }        

        public static StartupResult attemptedResult(String pid) {
            return new StartupResult(DID_ATTEMPT, !DID_START, pid);
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
         * @param pid                   The PID of the server process.
         */
        public StartupResult(boolean attempted, boolean started, String pid) {
            this.attempted = attempted;
            this.started = started;
            this.stopped = !DID_STOP;

            this.pid = pid;
        }

        public void stopped() {
            this.stopped = DID_STOP;
        }
    }      
    
    /**
     * Test the start of a single feature by starting then stopping the server. Record the
     * result, including timing results.
     * 
     * @param timingResult Storage for timing results.
     * 
     * @return The result of the feature startup test. Never null.
     */
    public StartupResult cycleFeature(TimingResult timingResult) {
        String m = "cycleFeature";

        StartupResult result = startFeature(timingResult);
        if ( result.attempted ) {
            stopFeature(timingResult, result);
            scanServerMessages(timingResult, result);
        }

        if ( !didFail() ) {
            recordSuccess(m);
        }

        return result;
    }

    /**
     * Attempt to start a server with a single configured feature.
     * Record the PID of the server. Do not stop the server.
     *
     * @param timingResult Storage for time recording.
     *
     * @return A new startup result. A startup result is always returned, even
     *     if the startup attempt failed.
     */
    public StartupResult startFeature(TimingResult timingResult) {
        String m = "startFeature";

        Exception updateException =
            timingResult.runUpdate( () -> server.updateFeature(lastShortName, nextShortName) );
        if ( updateException != null ) {
            recordOtherFailure(m, "Feature update failure", updateException);
            return StartupResult.notAttemptedResult();
        }

        boolean didStart;
        Exception startException = timingResult.runStart( () -> server.serverStart(nextShortName) );
        if ( startException == null ) {
            didStart = true;
        } else {
            didStart = false;
            recordOtherFailure(m, "Start failure", startException);
        }

        // The PID may or may not be available:
        // PID retrieval is performed even if 'started' is false, so to handle
        // the case of an apparently failed startup which left a dangling process.

        String pid;
        try {
            pid = timingResult.runPid( () -> server.getPid() );
            logInfo(m, "Server PID: " + pid);
        } catch ( Exception pidException ) {
            pid = null;
            logError(m, "Failed to obtain PID", pidException);
        }

        return new StartupResult(StartupResult.DID_ATTEMPT, didStart, pid);
    }

    public void stopFeature(TimingResult timingResult, StartupResult startupResult) {
        if ( stopServer(timingResult, startupResult) ) {
            startupResult.stopped();
        }
    }

    /**
     * Forcibly stop the server.
     *
     * First, if the server is set as having been started, use
     * {@link LibertyServer#stopServer} to stop the server.
     *
     * Second, if the server PID is available, attempt to kill the server process.
     *
     * @param timingResult  Storage for timing data.
     *
     * @return True or false telling if the stop was successful.
     */
    public boolean stopServer(TimingResult timingResult, StartupResult startupResult) {
        String m = "stopServer";

        String description = "Server [ " + server.serverGetName() + " ] PID [ " + startupResult.pid + " ] Feature [ " + nextShortName + " ]";

        boolean stopFailed = false;
        boolean killFailed = false;

        try {
            if ( server.serverIsStarted() ) {
                if ( nextShortName.equals("logstashCollector-1.0") ) {
                    try {
                        Thread.sleep(10000); // wait 10 seconds for logstashCollector
                    } catch ( Exception e ) {
                        // ignore and continue;
                    }
                }

                logInfo(m, "Stopping: " + description);
                Exception stopException = timingResult.runStop( () -> server.stop(nextIgnoredErrorsRegEx) );
                if ( stopException == null ) {
                    logInfo(m, "Stopped: " + description);
                } else {
                    stopFailed = true;
                    recordOtherFailure(m, "Stop Exception [ " + stopException + " ]");
                }

            } else {
                logInfo(m, "Not started: " + description);
            }

        } finally {
            if ( startupResult.pid != null ) {
                logInfo(m, "Killing: " + description);
                Exception killException = timingResult.runKill( () -> server.killProcess(startupResult.pid) );
                if ( killException == null ) {
                    logInfo(m, "Killed: " + description);
                } else {
                    killFailed = true;                        
                    recordOtherFailure(m, "Kill exception", killException);
                }
            } else {
                logInfo(m, "Server not killed: null PID: " + description);
            }
        }

        return ( !stopFailed && !killFailed);
    }

    protected void scanServerMessages(TimingResult timingResult, StartupResult startupResult) {
        String m = "scanServerMessages";
        
        timingResult.runVerify( () -> {
            List<String> errors;
            try {
                errors = server.findMessages(nextIgnoredErrorsRegEx);
            } catch ( Exception e ) {
                recordOtherFailure(m, "Verify exception", e);
                return;
            }                    

            if ( errors.isEmpty() ) {
                if ( nextIsOutOfLevel ) {
                    recordUnexpectedSuccess(m);
                } else if ( nextIsOther ) {
                    recordOtherFailure(m, "Unexpected success of feature");
                } else {
                    // Nothing to do ... possible success!
                }

            } else {
                String missingModule;
                String missingBundle;
                String javaError;

                for ( String error : errors ) {
                    if ( (missingModule = extractMissingModule(error)) != null ) {
                        if ( nextIsOutOfLevel ) {
                            logInfo(m, "Feature [ " + nextShortName + " ] has expected missing module [ " + missingModule + " ]");
                        } else {
                            recordMissingModule(m, missingModule);
                        }

                    } else if ( (missingBundle = extractMissingBundle(error)) != null ) {
                        recordMissingBundle(m, missingBundle);

                    } else if ( (javaError = extractJavaError(error)) != null ) {
                        if ( nextIsOutOfLevel ) {
                            logInfo(m, "Feature [ " + nextShortName + " ] has expected java error: " + javaError);
                        } else {
                            recordOtherFailure(m, "Unexpected java error: " + javaError);
                        }

                    } else {
                        recordOtherFailure(m, "Unexpected error: " + error);
                    }
                }
            }
        } );
    }

    protected String extractMissingModule(String error) {
        String missingModule = extractTail(error, MISSING_MODULE_ERROR, !INCLUDE_PREFIX);
        if ( missingModule != null ) {
            return stripModuleNumber(missingModule);
        } else {
            return null;
        }
    }

    protected String extractMissingBundle(String error) {
        String missingBundle = extractTail(error, MISSING_BUNDLE_ERROR, !INCLUDE_PREFIX);
        if (missingBundle != null) {
            return stripVersionDependency(missingBundle);
        } else {
            return null;
        }
    }
    
    protected String extractJavaError(String error) {
        return extractTail(error, JAVA_LEVEL_ERROR, INCLUDE_PREFIX);
    }

    //

    public void displayTestResults() {
        String m = "displayTestResults";

        StringBuilder builder = new StringBuilder();

        logInfo(m, "Successes [ " + successes.size() + " ]");
        if ( !successes.isEmpty() ) {
            display(m, "    ", 80, successes, builder);
        }

        logInfo(m, "Failures [ " + failures.size() + " ]");
        if ( !failures.isEmpty() ) {
            display(m, "    ", 80, failures, builder);
        }

        logInfo(m, "Other failures [ " + failuresOther.size() + " ]");
        if ( !failuresOther.isEmpty() ) {
            display(m, "    > ", "      > ", 80, failuresOther, builder);
        }                        

        logInfo(m, "Missing modules [ " + failuresMissingModules.size() + " ]");
        if ( !failuresMissingModules.isEmpty() ) {
            display(m, "    > ", "      > ", 80, failuresMissingModules, builder);
        }                        
        logInfo(m, "Missing bundles [ " + failuresMissingBundles.size() + " ]");
        if ( !failuresMissingBundles.isEmpty() ) {
            display(m, "    > ", "      > ", 80, failuresMissingBundles, builder);
        }            
        if ( !failuresMissingModules.isEmpty() || !failuresMissingBundles.isEmpty() ) {
            logInfo(m, "Missing modules and/or bundles have three common causes:");
            logInfo(m, "(1) A bundle dependency is incorrectly specified.");
            logInfo(m, "    Fix this by correcting the bundle dependency.");
            logInfo(m, "(2) The liberty server package ZIP is missing a bundle jar.");
            logInfo(m, "    Fix this by updating the packaging steps to include the missing jar.");
            logInfo(m, "(3) The list of features which are present in the server package is incorrect.");
            logInfo(m, "    Fix this by correcting the features list.");
            logInfo(m, "    Possibly, the features list from a different server package is being used.");
            logInfo(m, "    Fix this by making sure the features list is specific to the server package.");
            logInfo(m, "");                
            logInfo(m, "Server features are located relative to the liberty home directory:");
            logInfo(m, "    LIBERTY_HOME/lib/features/*.mf");
        }

        logInfo( m, "Unexpected java level successes [ " + failuresUnexpectedLevelSuccesses.size() + " ]" );
        if ( !failuresUnexpectedLevelSuccesses.isEmpty() ) {
            display(m, "    ", 80, failuresUnexpectedLevelSuccesses, builder);

            logInfo(m, "Features [ " + failuresUnexpectedLevelSuccesses + " ] started on java [ " + server.getJavaLevel() + " ].");
            logInfo(m, "");
            logInfo(m, "If these are test-only features, add 'IBM-Test-Feature: true' to the feature manifests. ");
            logInfo(m, "Feature required java levels are specified in resource [ " + FeatureLevels.REQUIRED_LEVELS_NAME + " ]");
        }
    }

    public void displayTimingResults() {
        String m = "displayTimingResults";

        Map<String, TimingSummary> summaries = new LinkedHashMap<>();

        summaries.put("Update", statistics("Update", timingResults, (TimingResult result) -> result.getUpdateNs()));
        summaries.put("Start", statistics("Start", timingResults, (TimingResult result) -> result.getStartNs()));
        summaries.put("PID", statistics("PID", timingResults, (TimingResult result) -> result.getPidNs()));
        summaries.put("Verify", statistics("Verify", timingResults, (TimingResult result) -> result.getVerifyNs()));
        summaries.put("Stop", statistics("Stop", timingResults, (TimingResult result) -> result.getStopNs()));
        summaries.put("Kill", statistics("Kill", timingResults, (TimingResult result) -> result.getKillNs()));
        summaries.put("Total", statistics("Total", timingResults, (TimingResult result) -> result.getTotalNs()));

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
    
    // "io.openliberty.java11.internal [25]" ==> "io.openliberty.java11.internal"
    
    protected static String stripModuleNumber(String missingModule) {
        int delimiterOffset = missingModule.indexOf(' ');
        if ( delimiterOffset == 0 ) {
            return missingModule;
        } else {
            return missingModule.substring(0, delimiterOffset);
        }
    }

    // io.openliberty.org.eclipse.microprofile.contextpropagation.1.2/[1.0.0,1.1.0).
    // ==> io.openliberty.org.eclipse.microprofile.contextpropagation.1.2

    protected static String stripVersionDependency(String missingBundle) {
        int delimiterOffset = missingBundle.indexOf('/');
        if ( delimiterOffset == 0 ) {
            return missingBundle;
        } else {
            return missingBundle.substring(0, delimiterOffset);
        }
    }
    
    protected static final boolean INCLUDE_PREFIX = true;
    
    protected static String extractTail(String error, String prefix, boolean includePrefix) {
        int prefixOffset = error.indexOf(prefix);
        if (prefixOffset == -1) {
            return null;
        }

        int tailOffset;
        if ( !includePrefix ) {
            tailOffset = prefixOffset + prefix.length();
        } else {
            tailOffset = prefixOffset;
        }

        String tail = error.substring(tailOffset);
        return tail;
    }
    
    protected static String[] asArray(String... head) {
        String[] allElements = new String[head.length ];
        for ( int elementNo = 0; elementNo < head.length; elementNo++ ) {
            allElements[elementNo] = head[elementNo];
        }
        return allElements;
    }

    protected static String[] asArray(String[] tail, String... head) {
        int allLength = tail.length + head.length;
        
        String[] allElements = new String[allLength];
        
        int elementNo = 0;
        for ( String tailElement : tail ) {
            allElements[ elementNo++ ] = tailElement;
        }
        for ( String headElement : head ) {
            allElements[ elementNo++ ] = headElement;
        }        
        
        return allElements;
    }

    protected static String asRegEx(String[] elements) {
        if ( (elements == null) || (elements.length == 0) ) {
            return "";
        } else if ( elements.length == 1 ) {
            return elements[0];
        } else if ( elements.length == 2 ) {
            return elements[0] + "|" + elements[1];            
        } else {
            StringBuilder builder = new StringBuilder();
            for ( int elementNo = 0; elementNo < elements.length; elementNo++ ) {
                if ( elementNo > 0 ) {
                    builder.append("|");
                }
                builder.append(elements[elementNo]);
            }
            return builder.toString();
        }
    }

    protected static String asString(String[] elements) {
        if ( (elements == null) || (elements.length == 0) ) {
            return "{ }";
        } else if ( elements.length == 1 ) {
            return "{ " + elements[0] + " }";
        } else if ( elements.length == 2 ) {
            return "{ " + elements[0] + ", " + elements[1] + " }";            
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("{ ");
            for ( int elementNo = 0; elementNo < elements.length; elementNo++ ) {
                if ( elementNo > 0 ) {
                    builder.append(", ");
                }
                builder.append(elements[elementNo]);
            }
            builder.append(" }");
            return builder.toString();
        }
    }
}

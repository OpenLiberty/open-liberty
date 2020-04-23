/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.custom.junit.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.IncludeElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.processor.TestServletProcessor;
import componenttest.exception.TopologyException;
import componenttest.logging.ffdc.IgnoredFFDCs;
import componenttest.logging.ffdc.IgnoredFFDCs.IgnoredFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.impl.LibertyServerWrapper;
import junit.framework.AssertionFailedError;

public class FATRunner extends BlockJUnit4ClassRunner {
    private static final Class<?> c = FATRunner.class;

    // Used to reduce timeouts to a sensible level when FATs are running locally
    public static final boolean FAT_TEST_LOCALRUN = Boolean.getBoolean("fat.test.localrun");

    private static final int MAX_FFDC_LINES = 1000;
    private static final boolean DISABLE_FFDC_CHECKING = Boolean.getBoolean("disable.ffdc.checking");

    private static final boolean ENABLE_TMP_DIR_CHECKING = Boolean.getBoolean("enable.tmpdir.checking");
    private static final long TMP_DIR_SIZE_THRESHOLD = 20 * 1024; // 20k

    //list of filters to apply
    private static final Filter[] testFiltersToApply = new Filter[] {
                                                                      new TestModeFilter(),
                                                                      new TestNameFilter(),
                                                                      new FeatureFilter(),
                                                                      new SystemPropertyFilter(),
                                                                      new JavaLevelFilter()
    };

    private static final Set<String> classesUsingFATRunner = new HashSet<String>();

    static {
        Log.info(c, "<clinit>", "Is this FAT running locally?  fat.test.localrun=" + FAT_TEST_LOCALRUN);
        Log.info(c, "<clinit>", "Using filters " + Arrays.toString(testFiltersToApply));
    }

    public static void requireFATRunner(String className) {
        if (!classesUsingFATRunner.contains(className))
            throw new IllegalStateException("The class " + className + " is attempting to use functionality " +
                                            "that requires @RunWith(FATRunner.class) to be specified at the " +
                                            "class level.");
    }

    @Override
    protected String testName(FrameworkMethod method) {
        String testName = super.testName(method);
        if (RepeatTestFilter.CURRENT_REPEAT_ACTION != null && !RepeatTestFilter.CURRENT_REPEAT_ACTION.equals("NO_MODIFICATION_ACTION")) {
            testName = testName + "_" + RepeatTestFilter.CURRENT_REPEAT_ACTION;
        }
        return testName;
    }

    private boolean hasTests = true;

    class FFDCInfo {
        int count;
        final String ffdcFile;
        final Machine machine;
        String ffdcHeader;

        FFDCInfo(Machine machine, String file, int count) {
            this.machine = machine;
            this.ffdcFile = file;
            this.count = count;
        }

        FFDCInfo(FFDCInfo copy, int newCount) {
            this(copy.machine, copy.ffdcFile, newCount);
        }

        @Override
        public String toString() {
            return "[count=" + count + ", file=" + ffdcFile + ", machine=" + machine + "]";
        }
    }

    public FATRunner(Class<?> tc) throws Exception {
        super(tc);
        classesUsingFATRunner.add(tc.getName());
        try {
            //filter any tests, using our list of filters
            filter(new CompoundFilter(testFiltersToApply));
        } catch (NoTestsRemainException e) {
            //swallow this exception, because we might have Test classes that contain only tests that
            // run in a mode we aren't currently in log a warning so we know
            Log.warning(this.getClass(), "All tests were filtered out for class " + getTestClass().getName());
            //set the flag so we can shortcut and avoid wasting time on @BeforeClass etc for stuff we aren't going to run any tests for
            hasTests = false;
        }
    }

    /*
     * We only get one chance to add a child, because the runner determines what tests to run using getFilteredChildren(),
     * and that method caches the results of getChildren().
     */
    @Override
    public List<FrameworkMethod> getChildren() {
        List<FrameworkMethod> unfilteredChildren = super.getChildren();
        List<FrameworkMethod> servletTests = TestServletProcessor.getServletTests(getTestClass());

        if (servletTests != null && servletTests.size() > 0) {
            unfilteredChildren.addAll(servletTests);
        }

        return unfilteredChildren;
    }

    @Override
    public void run(RunNotifier notifier) {
        if (hasTests) {
            injectLibertyServers();
            super.run(notifier);
        }
    }

    /**
     * Intercepting and over-riding at this point seems to be the cleanest way
     * of generating a test failure and having it assigned to the right test method.
     * A TestRule would be more elegant, but we seem to have to annotate
     * it to each Test class, which defeats the point.
     */
    @Override
    public Statement methodBlock(final FrameworkMethod method) {
        final Statement superStatement = super.methodBlock(method);

        Statement statement = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (!RepeatTestFilter.shouldRun(method)) {
                    return;
                }
                Map<String, Long> tmpDirFilesBeforeTest = createDirectorySnapshot("/tmp");
                try {
                    Log.info(c, "evaluate", "entering " + getTestClass().getName() + "." + method.getName());

                    Map<String, FFDCInfo> ffdcBeforeTest = retrieveFFDCCounts();

                    superStatement.evaluate();

                    // If we got to here without error, do a final check that
                    // any FFDCs were expected
                    Map<String, FFDCInfo> ffdcAfterTest = retrieveFFDCCounts();
                    Map<String, FFDCInfo> unexpectedFFDCs = filterOutPreexistingFFDCs(ffdcBeforeTest, ffdcAfterTest);

                    ArrayList<String> errors = new ArrayList<String>();

                    List<String> expectedFFDCs = getExpectedFFDCAnnotationFromTest(method);
                    // check for expectedFFDCs
                    for (String ffdcException : expectedFFDCs) {
                        FFDCInfo info = unexpectedFFDCs.remove(ffdcException);
                        if (info == null) {
                            errors.add("An FFDC reporting " + ffdcException + " was expected but none was found.");
                        }
                    }

                    Set<String> allowedFFDCs = getAllowedFFDCAnnotationFromTest(method);
                    // remove allowedFFDCs
                    for (String ffdcException : allowedFFDCs) {
                        if (ffdcException.equals(AllowedFFDC.ALL_FFDC))
                            unexpectedFFDCs.clear();
                        else
                            unexpectedFFDCs.remove(ffdcException);
                    }

                    for (FFDCInfo ffdcInfo : unexpectedFFDCs.values()) {
                        ffdcInfo.ffdcHeader = getFFDCHeader(new RemoteFile(ffdcInfo.machine, ffdcInfo.ffdcFile));
                    }

                    for (IgnoredFFDC ffdcToIgnore : IgnoredFFDCs.FFDCs) {
                        FFDCInfo ffdcInfo = unexpectedFFDCs.get(ffdcToIgnore.exception);
                        if (ffdcInfo != null && ffdcToIgnore.ignore(ffdcInfo.ffdcHeader)) {
                            unexpectedFFDCs.remove(ffdcToIgnore.exception);
                        }
                    }

                    // anything remaining is an error
                    for (Map.Entry<String, FFDCInfo> unexpected : unexpectedFFDCs.entrySet()) {
                        FFDCInfo ffdcInfo = unexpected.getValue();
                        int count = ffdcInfo.count;
                        if (count > 0) {
                            String error = "Unexpected FFDC reporting " + unexpected.getKey() + " was found (count = " + count + ")";
                            if (ffdcInfo.ffdcFile != null) {
                                error += ": " + ffdcInfo.ffdcFile + "\n" + ffdcInfo.ffdcHeader;
                            }

                            errors.add(error);
                        }
                    }

                    if (errors.size() > 0) {
                        blowup(errors.toString());
                    }
                } catch (Throwable t) {
                    if (t instanceof AssumptionViolatedException) {
                        Log.info(c, "evaluate", "assumption violated: " + t);
                    } else {
                        Log.error(c, "evaluate", t);
                        if (t instanceof MultipleFailureException) {
                            Log.info(c, "evaluate", "Multiple failure");
                            MultipleFailureException e = (MultipleFailureException) t;
                            for (Throwable t2 : e.getFailures()) {
                                Log.error(c, "evaluate", t2, "Specific failure:");
                            }
                        }
                    }
                    throw newThrowableWithTimeStamp(t);
                } finally {
                    Map<String, Long> tmpDirFilesAfterTest = createDirectorySnapshot("/tmp");
                    compareDirectorySnapshots("/tmp", tmpDirFilesBeforeTest, tmpDirFilesAfterTest);
                    Log.info(c, "evaluate", "exiting " + getTestClass().getName() + "." + method.getName());
                }
            }

        };

        return statement;
    }

    private static Throwable newThrowableWithTimeStamp(Throwable orig) throws Throwable {
        // Create a new throwable that includes the current timestamp to help with the
        // investigation of test failures.  We want to create the same type of exception
        // as the original in order to distinguish between a test failure (AssertionFailedError)
        // and an error (RuntimeException, IOException, etc.).
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss:SSS");
        String newMsg = sdf.format(new Date()) + " " + orig.getMessage();
        Throwable newThrowable;

        try {
            Constructor<? extends Throwable> ctor = orig.getClass().getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            newThrowable = ctor.newInstance(newMsg);
            newThrowable.setStackTrace(orig.getStackTrace());
        } catch (Throwable t) {
            newThrowable = new Throwable(newMsg, orig);
        }
        return newThrowable;
    }

    private static void blowup(String string) {
        if (!!!DISABLE_FFDC_CHECKING) {
            throw new AssertionFailedError(string);
        }
    }

    /**
     * Run at the end of the whole test. Tidy up and check for any FFDCs which were produced by the cleanup.
     */
    @Override
    public Statement classBlock(RunNotifier notifier) {
        final Statement superStatement = super.classBlock(notifier);

        Statement statement = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (!RepeatTestFilter.shouldRun(getTestClass().getJavaClass())) {
                    return;
                }
                try {
                    superStatement.evaluate();
                } finally {
                    String ffdcHeader = null;

                    // This won't detect (and never did) FFDCs generated during class initialization - oh well!
                    ArrayList<String> ffdcAfterTest = retrieveFFDCLogs();
                    //Now force the post test tidy
                    LibertyServerFactory.tidyAllKnownServers(getTestClassNameForAssociatedServers());
                    ArrayList<String> ffdcAfterTidying = retrieveFFDCLogs();
                    // Get any FFDCs we care about before recovering the servers
                    List<String> unexpectedFFDCs = filterOutPreexistingFFDCs(ffdcAfterTest, ffdcAfterTidying);
                    // Any new FFDC after the tests run is bad - fail, including the message from the first one
                    if (unexpectedFFDCs.size() > 0) {
                        // Sort to pick up the chronologically first FFDC.
                        Collections.sort(unexpectedFFDCs);
                        ffdcHeader = findFFDCAndGetHeader(unexpectedFFDCs.get(0));
                    }

                    //Now recover the servers
                    LibertyServerFactory.recoverAllServers(getTestClassNameForAssociatedServers());

                    // Now that we're all done, throw any assertion failures for FFDCs we spotted earlier
                    if (ffdcHeader != null) {
                        blowup("A problem was detected during post-test tidy up. New FFDC file is generated. Please check the log directory. The beginning of the FFDC file is:\n"
                               + ffdcHeader);
                    }

                    LogPolice.checkUsedTrace();
                }
            }
        };

        return statement;
    }

    private String getTestClassNameForAssociatedServers() {
        //Some tests had to do things differently from all the others (I'm looking at you CDI!)
        //So we should check for class rule fields and see if any of those classes have our
        //special workaround annotation, and if it does, we should use the name of that class
        //instead of the test class name that we would normally use.
        String testClassName = getTestClass().getName();
        final List<FrameworkField> classRuleFields = getTestClass().getAnnotatedFields(ClassRule.class);
        for (FrameworkField ff : classRuleFields) {
            Class<?> c = ff.getType();
            if (c.isAnnotationPresent(LibertyServerWrapper.class))
                testClassName = c.getName();
        }
        return testClassName;
    }

    /**
     * Creates a new list which includes all the strings in the after list which
     * are not in the before list.
     */
    private List<String> filterOutPreexistingFFDCs(List<String> before, List<String> after) {
        // The after list is modified in this method so create a copy
        List<String> filtered = new ArrayList<String>(after);
        // Filter out pre-existing FFDCs
        filtered.removeAll(before);
        return filtered;
    }

    /**
     * Given a Map of FFDCs that occur before and after a test has run, return a map of the FFDCs that are unique to after-test map
     */
    private Map<String, FFDCInfo> filterOutPreexistingFFDCs(Map<String, FFDCInfo> ffdcBeforeTest, Map<String, FFDCInfo> ffdcAfterTest) {
        HashMap<String, FFDCInfo> filtered = new HashMap<String, FFDCInfo>(ffdcAfterTest.size());
        for (Map.Entry<String, FFDCInfo> afterEntry : ffdcAfterTest.entrySet()) {
            FFDCInfo beforeInfo = ffdcBeforeTest.get(afterEntry.getKey());
            String exeptionKey = afterEntry.getKey();
            exeptionKey = exeptionKey.substring(0, exeptionKey.indexOf(":"));

            // if the FFDC exception matches, and its header is valid, the current FFDC has previosuly occurred
            if (beforeInfo != null) {
                int newVal = afterEntry.getValue().count - beforeInfo.count;
                if (newVal != 0) {
                    FFDCInfo filteredInfo = new FFDCInfo(afterEntry.getValue(), newVal);
                    filtered.put(exeptionKey, filteredInfo);
                }
            } else {
                filtered.put(exeptionKey, afterEntry.getValue());
            }

        }

        return filtered;
    }

    private String findFFDCAndGetHeader(String ffdcFileName) {
        // Find the FFDC file with the right name
        Iterator<LibertyServer> it = getRunningLibertyServers().iterator();
        // We'll probably only have to check one server
        while (it.hasNext()) {
            try {
                LibertyServer server = it.next();
                RemoteFile ffdcLogFile = server.getFFDCLogFile(ffdcFileName);
                // Assume no two servers have FFDC logs with the same name
                return getFFDCHeader(ffdcLogFile);
            } catch (FileNotFoundException e) {
                // This is fine - it just means the file didn't exist on this server
            } catch (Exception e) {
                Log.warning(this.getClass(), "Difficulties encountered searching for exceptions in FFDC logs: " + e);
                return "[Could not read file contents because of unexpected exception: " + e + "]";
            }
        }
        // We really should never get to this code since we just found the FFDC file
        return "[Could not find FFDC file " + ffdcFileName + "]";
    }

    private String getFFDCHeader(RemoteFile ffdcLogFile) throws Exception {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(ffdcLogFile.openForReading()));
            StringBuilder lines = new StringBuilder();
            int numLines = 1;
            for (String line; (line = reader.readLine()) != null; numLines++) {
                if (line.isEmpty()) {
                    // FFDC incident reports put a blank line between
                    // the exception stack trace and the introspection.
                    break;
                }

                if (numLines > MAX_FFDC_LINES) {
                    lines.append("...").append('\n');
                    break;
                }

                lines.append('>').append(line).append('\n');
            }
            return lines.toString();
        } catch (Exception e) {
            Log.error(this.getClass(), "Could not read " + ffdcLogFile, e);
            return "[Could not read " + ffdcLogFile + ": " + e + "]";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Fail silently
                }
            }
        }
    }

    private Collection<LibertyServer> getRunningLibertyServers() {
        return LibertyServerFactory.getKnownLibertyServers(getTestClassNameForAssociatedServers());
    }

    /**
     * Returns a map of FFDCs applicable to the current server. The map keys are in the format of
     * <exception>:<ffdcFilePath>, and the map values are FFDCInfo objects. Keeping keys in this format
     * allows us to keep track of FFDCs that share the same exception across multiple servers.
     */
    private Map<String, FFDCInfo> retrieveFFDCCounts() {
        HashMap<String, FFDCInfo> ffdcPrimaryInfo = new LinkedHashMap<String, FFDCInfo>();

        try {
            for (LibertyServer server : getRunningLibertyServers()) {

                // If the server has the FFDC checking flag set to false, skip it.
                if (server.getFFDCChecking() == false) {
                    Log.info(c, "retrieveFFDCCounts", "FFDC log collection for server: " + server.getServerName() + " is skipped. FFDC Checking is disabled for this server.");
                    continue;
                }

                int readAttempts = 0;
                boolean retry = true;
                while (retry && readAttempts++ <= 5) {
                    try {
                        ArrayList<String> summaries = server.listFFDCSummaryFiles(server.getServerName());
                        if (summaries.size() > 0) {
                            Collections.sort(summaries);
                            String lastSummary = summaries.get(summaries.size() - 1);
                            // Copy ffdcInfo so any partial updates can be discarded if there is a failure
                            HashMap<String, FFDCInfo> ffdcServerInfo;
                            if ((ffdcServerInfo = parseSummary(server.getFFDCSummaryFile(lastSummary))) != null) {
                                //merge returned map from server with primary map
                                for (Map.Entry<String, FFDCInfo> entry : ffdcServerInfo.entrySet()) {
                                    FFDCInfo oldInfo = ffdcPrimaryInfo.get(entry.getKey());
                                    String file = entry.getValue().ffdcFile;
                                    if (oldInfo != null) {
                                        // Add the counts if the primary map already had a value for that exception key.
                                        oldInfo.count += entry.getValue().count;
                                        ffdcPrimaryInfo.put(entry.getKey() + ":" + file, oldInfo);
                                    } else {
                                        ffdcPrimaryInfo.put(entry.getKey() + ":" + file, entry.getValue());
                                    }
                                }
                                retry = false;
                            } else {
                                Log.info(c, "retrieveFFDCCounts", "Read incomplete FFDC summary file, readAttempts = " + readAttempts);
                                //returned null, file is truncated
                                retry = true;
                                //wait a bit and retry
                                Thread.sleep(500);
                            }
                        }
                    } catch (TopologyException e) {
                        //ignore the exception as log directory doesn't exist and no FFDC log
                        retry = false;
                    } catch (Exception e) {
                        Log.info(c, "retrieveFFDCCounts", "Exception parsing FFDC summary");
                        Log.error(c, "retrieveFFDCCounts", e);
                        retry = false;
                    }
                }
                // Only bother logging if a failure was previously logged
                if (readAttempts > 1 && !retry) {
                    Log.info(c, "retrieveFFDCCounts", "Retry Successful");
                } else if (retry) {
                    //retry failed 5 times
                    Log.info(c, "retrieveFFDCCounts", "Retry Unsuccessful");
                }
            }
        } catch (Exception e) {
            //Exception obtaining Liberty servers
            Log.error(c, "retrieveFFDCCounts", e);
        }
        return ffdcPrimaryInfo;
    }

    // FFDC summary file format:
    // """
    //
    //  Index  Count  Time of first Occurrence    Time of last Occurrence     Exception SourceId ProbeId
    // ------+------+---------------------------+---------------------------+---------------------------
    //      0      2     4/11/13 2:25:30:312 BST     4/11/13 2:25:30:312 BST java.lang.ClassNotFoundException com.ibm.ws.config.internal.xml.validator.XMLConfigValidatorFactory 112
    //                                                                             - /test/jazz_build/jbe_rheinfelden/jazz/buildsystem/buildengine/eclipse/build/build.image/wlp/usr/servers/com.ibm.ws.config.validator/logs/ffdc/ffdc_13.04.11_02.25.30.0.log
    //                                                                             - /test/jazz_build/jbe_rheinfelden/jazz/buildsystem/buildengine/eclipse/build/build.image/wlp/usr/servers/com.ibm.ws.config.validator/logs/ffdc/ffdc_13.04.11_02.25.30.0.log
    //      1      1     4/11/13 2:25:31:959 BST     4/11/13 2:25:31:959 BST java.lang.NullPointerException com.ibm.ws.threading.internal.Worker 446
    // ------+------+---------------------------+---------------------------+---------------------------
    // """
    private HashMap<String, FFDCInfo> parseSummary(RemoteFile summaryFile) throws Exception {
        HashMap<String, FFDCInfo> ffdcInfo = new LinkedHashMap<String, FFDCInfo>();
        if (summaryFile.exists()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(summaryFile.openForReading()));
            String line = null;
            try {
                // parse exception_summary
                reader.readLine(); // empty line
                reader.readLine(); // header line
                reader.readLine(); // --- line
                Machine machine = summaryFile.getMachine();
                int count = 0;
                String exception = "";

                line = reader.readLine();
                //truncated file
                if (line == null) {
                    return null;
                }

                while (!(line.startsWith("---"))) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length > 9) {
                        //      0      1     4/11/13 2:25:30:312 BST     4/11/13 2:25:30:312 BST java.lang.ClassNotFoundException com.ibm.ws.config.internal.xml.validator.XMLConfigValidatorFactory 112
                        count = Integer.parseInt(parts[1]);
                        exception = parts[8];
                    } else if (parts.length > 1 && parts[0].equals("-")) {
                        String ffdcFile = parts[1];
                        FFDCInfo oldInfo = ffdcInfo.get(exception);
                        if (oldInfo != null) {
                            oldInfo.count += count;
                        } else {
                            ffdcInfo.put(exception, new FFDCInfo(machine, ffdcFile, count));
                        }

                    } else {
                        Log.warning(this.getClass(), "Failed to match FFDC line: " + line);
                    }

                    line = reader.readLine();
                    //truncated file
                    if (line == null) {
                        return null;
                    }
                }
            } catch (Exception e) {
                // Something went wrong parsing, return null so caller tries again
                Log.info(this.getClass(), "retrieveFFDCCounts", "Exception parsing FFDC summary");
                Log.error(this.getClass(), "retrieveFFDCCounts", e);
                return null;
            } finally {
                reader.close();
            }
        }
        return ffdcInfo;
    }

    private ArrayList<String> retrieveFFDCLogs() {

        ArrayList<String> ffdcList = new ArrayList<String>();
        try {
            Iterator<LibertyServer> iterator = getRunningLibertyServers().iterator();
            while (iterator.hasNext()) {
                try {
                    ffdcList = LibertyServerFactory.retrieveFFDCFile(iterator.next());
                } catch (TopologyException e) {
                } catch (Exception e) {
                    Log.error(c, "retrieveFFDCLogs", e);
                }
            }
        } catch (Exception e) {
            Log.error(c, "retrieveFFDCLogs", e);
        }
        return ffdcList;
    }

    public List<String> getExpectedFFDCAnnotationFromTest(FrameworkMethod m) {

        ArrayList<String> annotationListPerClass = new ArrayList<String>();

        ExpectedFFDC ffdc = m.getAnnotation(ExpectedFFDC.class);
        if (ffdc != null) {
            if (RepeatTestFilter.CURRENT_REPEAT_ACTION != null) {
                for (String repeatAction : ffdc.repeatAction()) {
                    if (repeatAction.equals(ExpectedFFDC.ALL_REPEAT_ACTIONS) || repeatAction.equals(RepeatTestFilter.CURRENT_REPEAT_ACTION)) {
                        String[] exceptionClasses = ffdc.value();
                        for (String exceptionClass : exceptionClasses) {
                            annotationListPerClass.add(exceptionClass);
                        }
                    }
                }
            } else {
                String[] exceptionClasses = ffdc.value();
                for (String exceptionClass : exceptionClasses) {
                    annotationListPerClass.add(exceptionClass);
                }
            }
        }

        return annotationListPerClass;

    }

    private Set<String> getAllowedFFDCAnnotationFromTest(FrameworkMethod m) {

        Set<String> annotationListPerClass = new HashSet<String>();

        // Method
        Set<AllowedFFDC> ffdcs = new HashSet<AllowedFFDC>();
        ffdcs.add(m.getAnnotation(AllowedFFDC.class));

        // Declaring Class
        Class<?> declaringClass = m.getMethod().getDeclaringClass();
        ffdcs.add(declaringClass.getAnnotation(AllowedFFDC.class));

        // Test Class
        Class<?> testClass = getTestClass().getJavaClass();
        if (!declaringClass.equals(testClass)) {
            ffdcs.add(testClass.getAnnotation(AllowedFFDC.class));
        }

        for (AllowedFFDC ffdc : ffdcs) {
            if (ffdc != null) {
                if (RepeatTestFilter.CURRENT_REPEAT_ACTION != null) {
                    for (String repeatAction : ffdc.repeatAction()) {
                        if (repeatAction.equals(AllowedFFDC.ALL_REPEAT_ACTIONS) || repeatAction.equals(RepeatTestFilter.CURRENT_REPEAT_ACTION)) {
                            String[] exceptionClasses = ffdc.value();
                            for (String exceptionClass : exceptionClasses) {
                                annotationListPerClass.add(exceptionClass);
                            }
                        }
                    }
                } else {
                    String[] exceptionClasses = ffdc.value();
                    for (String exceptionClass : exceptionClasses) {
                        annotationListPerClass.add(exceptionClass);
                    }
                }
            }
        }

        return annotationListPerClass;

    }

    private void injectLibertyServers() {
        String method = "injectLibertyServers";

        List<FrameworkField> servers = getTestClass().getAnnotatedFields(Server.class);
        for (FrameworkField frameworkField : servers) {
            Field serverField = frameworkField.getField();

            if (!frameworkField.isStatic())
                throw new RuntimeException("Annotated field '" + serverField.getName() + "' must be static.");
            if (!frameworkField.isPublic())
                throw new RuntimeException("Annotated field '" + serverField.getName() + "' must be public.");
            if (!LibertyServer.class.isAssignableFrom(serverField.getType()))
                throw new RuntimeException("Annotated field '" + serverField.getName() + "' must be of type or subtype of " + LibertyServer.class.getCanonicalName());

            Server anno = serverField.getAnnotation(Server.class);
            String serverName = anno.value();
            Class<?> testClass = getTestClass().getJavaClass();
            try {
                LibertyServer serv = LibertyServerFactory.getLibertyServer(serverName, testClass);
                // Set the HTTP and IIOP ports for the LibertyServer instance
                if (serv.getHttpDefaultPort() == 0) {
                    // Note that this case block only applies to running a FAT locally without Ant.
                    // Any builds using Ant to invoke JUnit will bypass this block completely.
                    ServerConfiguration config = serv.getServerConfiguration();
                    HttpEndpoint http = config.getHttpEndpoints().getById("defaultHttpEndpoint");
                    IncludeElement include = config.getIncludes().getBy("location", "../fatTestPorts.xml");
                    if (http != null) {
                        Log.info(c, method, "Using ports from <httpEndpoint> element in " + serverName + " server.xml");
                        // If there is an <httpEndpoint> element in the server config, use those ports
                        serv.setHttpDefaultPort(Integer.parseInt(http.getHttpPort()));
                        serv.setHttpDefaultSecurePort(Integer.parseInt(http.getHttpsPort()));
                    } else if (include != null) {
                        Log.info(c, method, "Using BVT HTTP port defaults in fatTestPorts.xml for " + serverName);
                        serv.setHttpDefaultPort(8010);
                        serv.setHttpDefaultSecurePort(8020);
                        serv.setHttpSecondaryPort(8030);
                        serv.setHttpSecondarySecurePort(8040);
                    } else {
                        Log.info(c, method, "No http endpoint.  Using defaultInstance config for " + serverName);
                        serv.setHttpDefaultPort(9080);
                        serv.setHttpDefaultSecurePort(9443);
                    }
                }
                if (serv.getIiopDefaultPort() == 0) {
                    // Note that this case block only applies to running a FAT locally without Ant.
                    // Any builds using Ant to invoke JUnit will bypass this block completely.
                    serv.setIiopDefaultPort(2809);
                }
                serv.setConsoleLogName(testClass.getSimpleName() + ".log");
                serverField.set(testClass, serv);
                Log.info(c, method, "Injected LibertyServer " + serv.getServerName() + " to class " + testClass.getCanonicalName());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Map<String, Long> createDirectorySnapshot(String path) {

        // this check can be expensive on machines with lots of content in /tmp, skip unless needed
        if (!ENABLE_TMP_DIR_CHECKING) {
            return null;
        }

        Map<String, Long> snapshot = new HashMap<String, Long>();

        File dir = new File(path);
        File[] childFiles = dir.listFiles(); // will be null if dir is not really a directory
        if (childFiles != null) {
            for (File f : childFiles) {
                String fileName = f.getAbsolutePath();
                if (f.isDirectory()) {
                    snapshot.putAll(createDirectorySnapshot(fileName));
                } else {
                    snapshot.put(fileName, f.length());
                }
            }
        }
        return snapshot;
    }

    private static void compareDirectorySnapshots(String path, Map<String, Long> before, Map<String, Long> after) {

        if (!ENABLE_TMP_DIR_CHECKING)
            return;

        final String method = "compareDirectorySnapshots";
        if (before.isEmpty() || after.isEmpty()) {
            Log.info(c, method, "Unable to calculate directories for " + path);
            return;
        }

        long sizeDiff = 0;

        // remove all files that were previously there - adding/subtracting any changes to file sizes in between.
        for (Map.Entry<String, Long> entry : before.entrySet()) {
            String fileName = entry.getKey();
            Long afterFileSize = after.remove(fileName);
            if (afterFileSize != null) {
                Long beforeFileSize = entry.getValue();
                if (beforeFileSize != null) {
                    long difference = afterFileSize - beforeFileSize;
                    sizeDiff += difference;
                    if (difference > 0) {
                        Log.info(c, method, fileName + " grew by " + difference + " bytes.");
                    } else if (difference < 0) {
                        // using debug here, because we'll rarely care when a file takes up _less_ space
                        Log.debug(c, method + " " + fileName + " shrank by " + difference + " bytes.");
                    }
                }
            }
        }

        // Now the after map should only contain files that were created during this test class's execution.
        for (Map.Entry<String, Long> entry : after.entrySet()) {
            Long size = entry.getValue();
            if (size != null) {
                sizeDiff += size;
            }
            Log.info(c, method, "New file found during test class execution: + " + entry.getKey() + " size: " + size + " bytes.");
        }

        // While it is possible that a file was deleted during the test class execution, this is probably pretty rare,
        // so we will not consider that possibility when determining whether the test exceeded the file size threshold.
        if (ENABLE_TMP_DIR_CHECKING && sizeDiff > TMP_DIR_SIZE_THRESHOLD) {
            throw new AssertionFailedError("This test class left too much garbage in the " + path + " directory.  Total difference in size between start and finish is " + sizeDiff
                                           + " bytes");
        }
    }

    @Override
    protected void collectInitializationErrors(List<Throwable> errors) {
        // Override this method to allow test classes to only declare @Test methods
        // via the @TestServlet(s) annotation.  Otherwise JUnit will throw an
        // initialization exception because there are no tests to run
        super.collectInitializationErrors(errors);
        if (errors.size() > 0) {
            List<Throwable> remove = new ArrayList<Throwable>();
            for (Throwable error : errors) {
                if ("No runnable methods".equals(error.getMessage()))
                    remove.add(error);
            }
            errors.removeAll(remove);
        }
    }
}

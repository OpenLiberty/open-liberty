/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import componenttest.custom.junit.runner.IgnoredFFDCs.IgnoredFFDC;
import componenttest.exception.TopologyException;
import componenttest.rules.repeater.EE9PackageReplacementHelper;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.impl.LibertyServerWrapper;
import junit.framework.AssertionFailedError;

/**
 * Liberty FAT runner.
 * 
 * Particular functionality added by this test runner:
 * 
 * The FAT runner is designed to support repeat actions.  Repeat actions need
 * not be used, but when they are, several parts of the FAT runner functionality
 * is sensitive to the active repeat action.  For example, tests may be filtered
 * to run only by specific repeat actions.  FFDC annotation values may be keyed to
 * the active repeat action.
 *
 * Test names are adjusted when running within a repeat action.  Except for
 * a distinguished test action, when running within a repeat action, the action
 * name is appended to the base test name.  See {@link #testName(FrameworkMethod)}.
 *
 * When the EE9 repeat action is active, package replacement is performed on FFDC
 * exception class names.  See {@link #ee9ReplacePackages} and
 * {@link EE9PackageReplacementHelper#replacePackages(String)}. 
 * 
 * For more information about repeat actions, see
 * {@link componenttest.rules.repeater.RepeatTests} and
 * {@link componenttest.rules.repeater.RepeatTestAction}.
 *
 * Several test filters ({@link Filter}) are used.  See {@link #testFilters}.
 *
 * Generally, test filters operate using class and method annotations, and
 * occasionally, use java system properties.  See the several test filters
 * for details.
 *
 * In addition to standard JUnit test filtering, see class {@link RepeatTestFilter},
 * which is invoked by the FAT test runner explicitly. 
 *
 * Server injection is performed as an initial step of running tests of
 * a test class.  See {@link #run(RunNotifier) and {@link #injectLibertyServers()}.
 *
 * Synthetic tests are generated from {@link componenttest.annotation.TestServlet}
 * and {@link componenttest.annotation.TestServlets} annotations.
 * See {@link TestServletProcessor#getServletTests(TestClass)}. 
 *
 * Injected servers are examined for FFDC (first failure data capture) records,
 * which are written to the server specific "logs/ffdc" folder.  FFDC checking
 * is done at the conclusion of each test and at the conclusion of each test
 * class.  The presence of any FFDC records causes a test failure, unless
 * the test or test class is annotated with {@link AllowedFFDC} or
 * {@link ExpectedFFDC}.
 *
 * FFDC checking may be disabled.  See {@link #DISABLE_FFDC_CHECKING_PROPERTY_NAME}.
 *
 * Server logs may be examined for unusually large trace output.
 * See {@link LogPolice}.
 * 
 * The temporary folder ('/tmp') may be examined for unusually large
 * additions of temporary files.  See java custom property
 * {@link #ENABLE_TMP_DIR_CHECKING}.
 * 
 * Test exceptions are intercepted and updated to prepend a time stamp to
 * the test message.  See {@link #newThrowableWithTimeStamp(Throwable).  Note
 * that this will change the type of thrown exceptions, when the exception
 * does not have constructor that accepts a single string parameter.  In all
 * cases, the thrown exception will lose all state except for the exception
 * message and stack trace.  
 */
public class FATRunner extends BlockJUnit4ClassRunner {
    private static final Class<? extends FATRunner> c = FATRunner.class;

    static {
        // Move the JavaInfo logging to the beginning.
        JavaInfo.class.getName();
    }
    
    /**
     * Limit on the number of lines read from FFDC log files.
     * See {@link #getFFDCHeader}.
     */
    private static final int MAX_FFDC_LINES = 1000;

    static {
        Log.info(c, "<clinit>", "Maximum FFDC lines that will be read: " + MAX_FFDC_LINES);
    }
    
    /**
     * Read header (exception plus stack, but not the introspection output) from
     * an FFDC log file.
     *
     * Limit the number of lines read to {@link #MAX_FFDC_LINES}.
     *
     * @param ffdFile The FFDC log file which is to be read.
     * @return The exception plus stack of the FFDC log file.
     *
     * @throws Exception Thrown if the FFDC could not be opened or read.
     */
    private static String getFFDCHeader(RemoteFile ffdFile) throws Exception {
        try ( InputStream inputStream = ffdFile.openForReading() ) {
            BufferedReader reader = new BufferedReader( new InputStreamReader(inputStream) );

            StringBuilder lines = new StringBuilder();

            int numLines = 1;
            for ( String line; (line = reader.readLine()) != null; numLines++ ) {
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
            Log.error(c, "Could not read " + ffdFile, e);
            return "[Could not read " + ffdFile + ": " + e + "]";
        }
    }

    /** Property which is used to disable FFDC checking. */
    public static final String DISABLE_FFDC_CHECKING_PROPERTY_NAME = "disable.ffdc.checking";

    /** Prevent FFDC checking from causing test failures. */
    private static final boolean DISABLE_FFDC_CHECKING = Boolean.getBoolean("disable.ffdc.checking");

    static {
        Log.info(c, "<clinit>",
            "Disable FFDC checking (" + DISABLE_FFDC_CHECKING_PROPERTY_NAME + "): " +
            DISABLE_FFDC_CHECKING);
    }
    
    static {
        // Force early logging of the ignored FFDC's.
        if ( !DISABLE_FFDC_CHECKING ) {
            IgnoredFFDC.class.getName();
        }
    }

    /**
     * Fail a test with an assertion failure.
     * 
     * Do nothing if FFDC checking is disabled.  See {@link #DISABLE_FFDC_CHECKING}).
     *
     * @param failureMessage The message to provide to the assertion failure.
     */
    private static void blowup(String method, String failureMessage) {
        if ( !DISABLE_FFDC_CHECKING ) {
            throw new AssertionFailedError(failureMessage);        
        } else {
            Log.info(c, method, "Ignoring test error: " + failureMessage);
        }
    }

    /** Property used to enable temporary directory checking. */
    public static final String ENABLE_TMP_DIR_CHECKING_PROPERTY_NAME =
        "enable.tmpdir.checking";

    private static final boolean ENABLE_TMP_DIR_CHECKING =
        Boolean.getBoolean(ENABLE_TMP_DIR_CHECKING_PROPERTY_NAME);

    private static final String TMP_DIR_PATH = "/tmp";

    private static final long TMP_DIR_SIZE_THRESHOLD = 20 * 1024;

    static {
        Log.info(c, "<clinit>",
            "Enable temporary directory checking (" + TMP_DIR_PATH + "): " +
            ENABLE_TMP_DIR_CHECKING_PROPERTY_NAME + ": " +
            ENABLE_TMP_DIR_CHECKING);

        Log.info(c, "<clinit>",
            "Temporary directory size threshold (" + TMP_DIR_PATH + "): " +
            TMP_DIR_SIZE_THRESHOLD);
    }

    /**
     * Create a full nested listing of a target directory.
     *
     * Add only simple files.  Do not add directories or linked
     * directories.  (But do traverse into these.)
     *
     * @param path The path which is to be listed.  Usually '/tmp'.
     *
     * @return A table of the files within the target directory, mapping
     *     the absolute paths of the target files to the lengths of
     *     the target files.
     *     
     * @param path The path to the directory which is to be listed.
     */
    private static Map<String, Long> createDirectorySnapshot(String path) {
        // Set the root target to use an absolute path.
        // Child files will similarly have an absolute path.
        // This avoids the expensive call to 'getAbsolutePath' on child files.

        String absPath = new File(path).getAbsolutePath();

        Map<String, Long> snapshot = new HashMap<String, Long>();
        createDirectorySnapshot( new File(absPath), snapshot);
        return snapshot;
    }

    private static void createDirectorySnapshot(File file, Map<String, Long> snapshot) {
        File[] childFiles = file.listFiles();
        if ( childFiles == null ) { // Ignore: not a directory.
            return;
        }

        for ( File childFile : childFiles ) {
            if ( childFile.isDirectory() ) {
                createDirectorySnapshot(childFile, snapshot);
            } else {
                // 'getPath' will answer an absolute path, since the parent
                // file had an absolute path.
                snapshot.put( childFile.getPath(), childFile.length() );
            }
        }
    }

    /**
     * Compare two directory snapshots.  Log statistics that describe
     * the difference.
     *
     * Fail with an assertion failure exception if the snapshot total
     * size grew by more than {@link #TMP_DIR_SIZE_THRESHOLD}.
     *
     * @param path The root path of the directory listings.
     * @param initialSnapshot An initial snapshot of the directory.
     * @param finalSnapshot A final shapshot of the directory.
     */
    private static void compareDirectorySnapshots(
        String path,
        Map<String, Long> initialSnapshot,
        Map<String, Long> finalSnapshot) {

        String methodName = "compareDirectorySnapshots";

        int countGrew = 0;
        long sumSameInGrown = 0L; // The initial file size.
        long sumGrowth = 0L; // sumSameInGrown + sumGrowth == the final file size

        int countShrank = 0;
        long sumSameInShrunk = 0L; // The final file size.
        long sumShrinkage = 0L; // sumSameInShrunk + sumShrinkage == the initial file size.

        int countSame = 0;
        long sumSame = 0L; // Both the initial and the final file sizes.

        int countAdded = 0;
        long sumAdded = 0L; // The final file size.  The file has no initial size.

        int countRemoved = 0;
        long sumRemoved = 0L; // The initial file size.  The file has no final size.

        for ( Map.Entry<String, Long> initialEntry : initialSnapshot.entrySet() ) {
            String fileName = initialEntry.getKey();
            long initialSize = initialEntry.getValue().longValue();

            Long finalSizeLong = finalSnapshot.remove(fileName);
            if ( finalSizeLong != null ) { // Still there, but the size may have changed ...
                long finalSize = finalSizeLong.longValue();

                long difference = finalSize - initialSize;
                if ( difference > 0 ) {
                    countGrew ++;
                    sumSameInGrown += initialSize;
                    sumGrowth += difference;

                    Log.info(c, methodName, fileName + " grew by " + difference + " bytes.");

                } else if ( difference < 0 ) { // Unexpected
                    difference *= -1; // Record as a positive value.

                    countShrank ++;
                    sumSameInShrunk += finalSize;
                    sumShrinkage += difference;

                    // TFB: This used to be logged as a debug only method, with a comment that
                    //      files shrinking was unexpected.  But that is a reason to log the shrinkage.
                    Log.info(c, methodName, fileName + " shrank by " + difference + " bytes.");

                } else {
                    countSame++;
                    sumSame += initialSize;

                    // Don't log files which are the same size.
                }

            } else { // No longer there ...
                countRemoved++;
                sumRemoved += initialSize; // Record as a positive value.

                // TFB: These were previously not logged.
                Log.info(c, methodName, fileName + " was removed, having " + initialSize + " bytes.");
            }
        }

        // The final snapshot now contains only files which were added during this test.

        for ( Map.Entry<String, Long> finalEntry : finalSnapshot.entrySet() ) {
            String fileName = finalEntry.getKey();
            long finalSize = finalEntry.getValue().longValue();

            Log.info(c, methodName, fileName + " was added, having " + finalSize + " bytes.");

            countAdded++;
            sumAdded += finalSize;
        }

        int countBoth   = countSame + countGrew + countShrank;

        long sumInitial = (sumSame + sumSameInGrown               + (sumSameInShrunk + sumShrinkage)) + sumRemoved;
        long sumFinal   = (sumSame + (sumSameInGrown + sumGrowth) + sumSameInShrunk)                  + sumAdded;
        
        int netFiles    = countAdded - countRemoved;
        long netBytes   =            (sumGrowth                   - sumShrinkage)                     + (sumAdded - sumRemoved);
        // netBytes == sumFinal - sunInitial

        Log.info(c, methodName, "Statistics    : " + path);

        Log.info(c, methodName, "Initial files : " + (countBoth + countRemoved) + " having " + sumInitial + " bytes");
        Log.info(c, methodName, "Final files   : " + (countBoth + countAdded) + " having " + sumFinal + " bytes");
        Log.info(c, methodName, "Net files     : " + netFiles + " having " + netBytes + " bytes");

        Log.info(c, methodName, "Files in both : " + countBoth);
        Log.info(c, methodName, "  unchanged   : " + countSame + " having " + sumSame + " bytes");                
        Log.info(c, methodName, "  that grew   : " + countGrew + " by " + sumGrowth + " bytes");                
        Log.info(c, methodName, "  that shrank : " + countShrank + " by " + sumShrinkage + " bytes");

        Log.info(c, methodName, "Files added   : " + countAdded + " having " + sumAdded + " bytes");
        Log.info(c, methodName, "Files removed : " + countRemoved + " having " + sumRemoved + " bytes");        

        if ( netBytes > TMP_DIR_SIZE_THRESHOLD ) {
            throw new AssertionFailedError(
                    "This test class left too much garbage in the " + path + "." +
                    "  The total difference in size is " + netBytes + " bytes.");
        }
    }

    //

    /** Property used to indicate that tests are being run locally. */
    public static final String FAT_TEST_LOCALRUN_PROPERTY_NAME = "fat.test.localrun";

    /**
     * Flag telling if FATs are running locally.  This is used to reduce
     * timeouts to a sensible level when FATs are running locally.
     *
     * See {@link componenttest.topology.utils.ExternalTestServiceDockerClientStrategy}.
     */
    public static final boolean FAT_TEST_LOCALRUN =
        Boolean.getBoolean(FAT_TEST_LOCALRUN_PROPERTY_NAME) && !Boolean.parseBoolean(System.getenv("CI"));

    static {
        Log.info(c, "<clinit>",
            "Is this FAT running locally (" + FAT_TEST_LOCALRUN_PROPERTY_NAME + "): " + FAT_TEST_LOCALRUN);
    }

    /**
     * Filters used to select which test classes and which test methods are to be run.
     * 
     * See the specific test filter classes for details:
     * 
     * <ul>
     * <li>{@link TestModeFilter}</li>
     * <li>{@link TestNameFilter}</li>
     * <li>{@link FeatureFilter}</li>
     * <li>{@link SystemPropertyFilter}</li>
     * <li>{@link JavaLevelFilter}</li>
     * </ul>
     */
    private static final Filter[] testFilters = new Filter[] {
            new TestModeFilter(),
            new TestNameFilter(),
            new FeatureFilter(),
            new SystemPropertyFilter(),
            new JavaLevelFilter()
    };

    /**
     * Compound filter used to aggregate the test filters.  See {@link #testFilters}.
     * 
     * Filtering is performed within the initializer by an explicit invocation of
     * {@link org.junit.runners.ParentRunner#filter(Filter)} with this compound
     * filter as the parameter.
     */
    private static final CompoundFilter testFilter = new CompoundFilter(testFilters);

    static {
        Log.info(c, "<clinit>", "Test filters: " + testFilter.describe());
    }

    private static CompoundFilter getTestFilter() {
        return testFilter;
    }

    /** A shared simple date format for thrown exceptions. */
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss:SSS");

    /**
     * Answer the current date and time using a simple date format.
     * See {@link #sdf}.
     *
     * @return The current date and time using a simple date format.
     */
    private static String formatDate() {
        Date now = new Date();
        synchronized( sdf ) { // SimpleDateFormat instances are *not* thread safe.
            return sdf.format(now);
        }
    }

    /**
     * Factory helper method: Answer a clone of a throwable with the message adjusted
     * to include a time stamp.
     * 
     * The clone throwable is a shallow copy of the original throwable.  Only the stack
     * trace and the message of the original throwable are copied.
     *
     * Attempt to create a new throwable of the same concrete type as the original throwable.
     * If that fails, create a new instance of {@link Throwable}.
     *
     * @param orig A throwable which is to be cloned.
     *
     * @return A clone of the throwable with a time stamp prepended to its message.
     */
    private static Throwable newThrowableWithTimeStamp(Throwable orig) {
        String newMsg = formatDate() + " " + orig.getMessage();

        Throwable newThrowable;
        try {
            Constructor<? extends Throwable> ctor = orig.getClass().getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            newThrowable = ctor.newInstance(newMsg);
        } catch ( Throwable t ) {
            newThrowable = new Throwable(newMsg, orig);
        }

        newThrowable.setStackTrace( orig.getStackTrace() );

        return newThrowable;
    }

    /**
     * Record of what classes were run using this test runner.  Used by
     * {@link #requireFATRunner}.
     * 
     * The presumption is that this is public and static to enable use from other
     * test classes.
     * 
     * Note: A static, but populated when creating a new FAT runner for a specified
     * class.
     *
     * Use of this test runner should be consistent across test repeats because the
     * run-with annotation is a class annotation, which should be the same for every
     * run of a particular test class.
     */
    private static final Set<String> classesUsingFATRunner = new HashSet<String>();

    /**
     * Record that a class requires the FAT runner.
     *
     * @param className The fully qualified name of the class that requires the
     *     FAT runner.
     */
    private void addFATRunnerClass(String className) {
        classesUsingFATRunner.add(className);
    }

    /**
     * Helper: Test whether a specified class is using this FAT test runner.  Throw a
     * runtime exception if the specified class is not set to use this FAT test runner.
     *
     * Used by {componenttest.custom.junit.runner.RepeatTestFilter#shouldRun(FrameworkMethod)}.
     *
     * See {@link #classesUsingFATRunner} and {@link #FATRunner}.
     * 
     * @param className The name of the class which is to be tested.  This must be
     *     a fully qualified class name obtained from {@link Class#getName()}.
     */
    public static void requireFATRunner(String className) {
        if ( !classesUsingFATRunner.contains(className) ) {
            throw new IllegalStateException(
                "The class " + className + " is attempting to use functionality " +
                "that requires @RunWith(FATRunner.class) to be specified at the " +
                "class level.");
        }
    }

    /** Helper used to transform test artifacts when running in Jakarta mode. */ 
    private static EE9PackageReplacementHelper ee9Helper;

    /**
     * Answer the helper which is used to transform test artifacts when
     * running in Jakarta mode.  This helper is used locally to transform
     * FFDC exception class names, which may require transformation.
     *
     * @return An EE9 helper instance.
     */
    private static EE9PackageReplacementHelper getEE9Helper() {
        if ( ee9Helper == null ) {
            ee9Helper = new EE9PackageReplacementHelper();
        }
        return ee9Helper;
    }

    /**
     * Perform package name replacement on a class name.  FFDC exceptions
     * require this replacement when running in Jakarta mode.
     *
     * The transformed class name name is the same as the input class name
     * when running outside of Jakarta mode, or when the input class name
     * is not one which requires transformation.
     * 
     * @param className The class name which is to be transformed.
     *
     * @return The transformed class name.
     */
    private static String ee9ReplacePackages(String className) {
        return getEE9Helper().replacePackages(className);
    }
    
    /**
     * Create a FAT test runner for a specified test class.
     *
     * Steps are added to filter the test methods using the locally defined
     * test filter.  (See {@link #getTestFilter()}.  The steps modifies the
     * runner's behavior away from the default behavior.  The modified
     * behavior is to allow the test class to have no unfiltered test methods.
     * See also {@link #run(RunNotifier)}, which is modified to do nothing
     * if there are no unfiltered test methods.
     *
     * @param tc The test class which will be run.
     *
     * @throws Exception Thrown if initial processing of the test
     *     class failed.  This is unexpected.
     */
    public FATRunner(Class<?> tc) throws Exception {
        super(tc);

        addFATRunnerClass( tc.getName() );

        boolean useHasTests;

        try {
            filter( getTestFilter() );
            useHasTests = true;

        } catch ( NoTestsRemainException e ) {
            // Swallow this exception.  Tests with no unfiltered test methods are legal
            // in simplicity.  Depending on the mode under which a test class is run, no test
            // methods may be selected, and that is legal.

            // Remember that no tests were selected.  That allows {@link #run} to skip test
            // classes which have no unfiltered test methods.  That is desirable, in particular,
            // because allowing the usual steps of 'run' will do expensive test preparation and
            // cleanup steps.  This includes setup and teardown of servers.

            useHasTests = false;
            Log.info(c, "<init>", "All tests were filtered");
        }

        hasTests = useHasTests;
    }

    private final boolean hasTests;

    /**
     * Override: Do nothing if no unfiltered test methods remain.
     *
     * If unfiltered tests remain, inject liberty servers, then proceed with the
     * usual superclass steps.
     *
     * See {@link #injectLibertyServers()} and {@link ParentRunner#run(RunNotifier)}.
     *
     * @param notifier The notifier to be used when running the test class.
     */
    @Override
    public void run(RunNotifier notifier) {
        if ( hasTests ) {
            injectLibertyServers();
            super.run(notifier);
        }
    }

    /**
     * Inject public static fields which have been annotated by {@link Server}.
     *
     * Injection failure is handled by throwing a runtime exception.
     * 
     * Server injection is performed as an initial step of {@link #run(RunNotifier)}.
     *
     * Server injection is not performed if all tests are filtered.  This happens often
     * depending on the test mode.
     */
    private void injectLibertyServers() {
        List<FrameworkField> serverFields = getTestClass().getAnnotatedFields(Server.class);
        for ( FrameworkField serverField : serverFields ) {
            injectLibertyServer(serverField);
        }
    }
    
    /**
     * Perform injection on a single field which is annotated with {@link Server}.
     *
     * Injection failure is handled by throwing a runtime exception.
     *
     * @param frameworkField The field which is to be injected.
     */
    private void injectLibertyServer(FrameworkField frameworkField) {
        String methodName = "injectLibertyServers";

        Class<?> testClass = getTestClass().getJavaClass();

        Field serverField = frameworkField.getField();
        Server serverAnno = serverField.getAnnotation(Server.class);
        String serverName = serverAnno.value();

        Log.info(c, methodName, "Field " + testClass.getName() + '.' + serverField.getName() + " injects server " + serverName);

        if ( !frameworkField.isStatic() ) {
            throw new RuntimeException("Annotated field '" + serverField.getName() + "' must be static.");
        } else if ( !frameworkField.isPublic() ) {
            throw new RuntimeException("Annotated field '" + serverField.getName() + "' must be public.");
        } else if ( !LibertyServer.class.isAssignableFrom(serverField.getType()) ) {
            throw new RuntimeException("Annotated field '" + serverField.getName() + "' must be of type or subtype of " + LibertyServer.class.getName());
        }

        try {
            LibertyServer server = LibertyServerFactory.getLibertyServer(serverName, testClass);

            // Set the HTTP and IIOP ports for the LibertyServer instance.

            // Note that this case block only applies to running a FAT locally without Ant.
            // Any builds using Ant to invoke JUnit will bypass this block completely.

            if ( server.getHttpDefaultPort() == 0 ) {
                ServerConfiguration config = server.getServerConfiguration();
                HttpEndpoint defaultHttpEndpoint = config.getHttpEndpoints().getById("defaultHttpEndpoint");

                if ( defaultHttpEndpoint != null ) {
                    Log.info(c, methodName, "Using ports from <httpEndpoint> element in " + serverName + " server.xml");
                    server.setHttpDefaultPort( Integer.parseInt(defaultHttpEndpoint.getHttpPort()) );
                    server.setHttpDefaultSecurePort( Integer.parseInt(defaultHttpEndpoint.getHttpsPort()) );
                    
                } else {
                    IncludeElement testPortsInclude = config.getIncludes().getBy("location", "../fatTestPorts.xml");
                    if ( testPortsInclude != null ) {
                        // TFB: No values are obtained from the include!
                        Log.info(c, methodName, "Using BVT HTTP port defaults in fatTestPorts.xml for " + serverName);
                        server.setHttpDefaultPort(8010);
                        server.setHttpDefaultSecurePort(8020);
                        server.setHttpSecondaryPort(8030);
                        server.setHttpSecondarySecurePort(8040);

                    } else {
                        Log.info(c, methodName, "No http endpoint.  Using defaultInstance config for " + serverName);
                        server.setHttpDefaultPort(9080);
                        server.setHttpDefaultSecurePort(9443);
                    }
                }
            }

            // This case block only applies to running a FAT locally without Ant.
            // Any builds using Ant to invoke JUnit will bypass this block completely.

            if ( server.getIiopDefaultPort() == 0 ) {
                server.setIiopDefaultPort(2809);
            }

            server.setConsoleLogName(testClass.getSimpleName() + ".log");

            serverField.set(testClass, server);

            Log.info(c, methodName, "Injected LibertyServer " + server.getServerName() + " to class " + testClass.getName());

        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Override: When repeat actions are active, and test modification is allowed,
     * modify the test name to add a suffix obtained from the repeat action.
     * 
     * @param method The test method for which to obtain a test method name.
     * 
     * @return The name of the test method, adjusted according to the current
     *     repeat action.
     */
    @Override
    protected String testName(FrameworkMethod method) {
        String testName = super.testName(method);

        if ( RepeatTestFilter.isAnyRepeatActionActive() ) {
            testName = testName + RepeatTestFilter.getRepeatActionsAsString();
        }

        return testName;
    }

    /**
     * Override.  Answer the unfiltered test methods of the current test class.
     * 
     * Answer the usual collection of unfiltered test methods, and add servlet tests
     * for the test class.
     * 
     * See {@link TestServletProcessor#getServletTests(org.junit.runners.model.TestClass)}.
     *
     * The result is computed each time this method is called, which is expensive.
     * However, current usage is to obtain this collection just once, then to perform
     * test filtering, and then to cache the filtered collection.  Caching the
     * unfiltered collection is not necessary at this time.
     *
     * @return The unfiltered test methods of the test class.
     */
    @Override
    public List<FrameworkMethod> getChildren() {
        List<FrameworkMethod> unfilteredChildren = super.getChildren();

        List<FrameworkMethod> servletTests = TestServletProcessor.getServletTests( getTestClass() );
        unfilteredChildren.addAll(servletTests);

        return unfilteredChildren;
    }

    /**
     * Override: Modify the initialization errors obtained from the
     * superclass implementation to remove "No runnable methods" as an
     * initialization error.
     * 
     * That initialization error is generated by the superclass when the
     * test class has only test servlet defined tests.  That is, tests
     * which are defined by the 'TestServlet' annotation.
     * 
     * (Note that this is different from a test class having no test methods
     * as a result of test filtering.  The error which is processed by this
     * method is caused by a test class having no test methods at all.)
     *
     * Note that this override, unusually, removes values from the collection
     * of initialization errors.  That is unusual for a method named 'collect',
     * which usually will be used to add to a collection.
     * 
     * @param errors A collection of initialization errors which are to be
     *     adjusted by this method. 
     */
    @Override
    protected void collectInitializationErrors(List<Throwable> errors) {
        super.collectInitializationErrors(errors);

        if ( !errors.isEmpty() ) {
            List<Throwable> remove = new ArrayList<Throwable>();
            for ( Throwable error : errors ) {
                if ( "No runnable methods".equals( error.getMessage() ) )
                    remove.add(error);
            }
            errors.removeAll(remove);
        }
    }
    
    /**
     * Data structure for FFDC processing.
     */
    class FFDCInfo {
        /** The machine on which this FFDC was detected. */
        final Machine machine;
        
        /** The path to the first occurrence of this FFDC. */
        final String ffdcFile;
        
        /**
         * The header of the first occurrence of this FFDC.
         * Read on demand, as required by FFDC processing.  This
         * read is expensive and avoided for most FFDC.
         */
        String ffdcHeader;

        /**
         * Helper: Read and assign the FFDC header.  This contains the
         * lines up to but not including introspection output.
         *
         * The FFDC header is assigned as needed.  FFDC which are expected
         * or which are allowed do not need to have their header set.
         *
         * See {@link FATRunner#getFFDCHeader(RemoteFile)}.
         *
         * @throws Exception Thrown if the FFDC header cannot be read.
         */
        void setHeader() throws Exception {
            ffdcHeader = getFFDCHeader( new RemoteFile(machine, ffdcFile) );
        }

        /**
         * How many times this FFDC was detected.
         * 
         * The machine, FFDC file, and FFDC header values are valid for
         * only the first occurrence of this FFDC.
         * 
         * Not final: Updates are made when occurrences of similar FFDC
         * information is detected.  Similar usually means having the same
         * exception class.
         */
        int count;

        /**
         * Add to the count of this FFDC information.  Clear the print string, which
         * is no longer accurate.
         *
         * @param newCount A count to add to the count of this FFDC information.
         */
        void add(int newCount) {
            this.count += newCount;
            this.printString = null;
        }

        /**
         * A print string for this FFDC info.  Assigned on demand.
         */
        private String printString;

        /**
         * Create new FFDC information.  Leave the header unassigned.
         *
         * @param machine The machine on which this FFDC was detected.
         * @param file The path to the first occurrence of this FFDC.
         * @param count How many times this FFDC has been seen.
         */
        FFDCInfo(Machine machine, String file, int count) {
            this.machine = machine;
            this.ffdcFile = file;
            this.ffdcHeader = null;
            this.count = count;
            this.printString = null;
        }

        /**
         * Create a clone of specified FFDC information, with the count
         * of the new FFDC information updated to a new value.
         *
         * @param sourceInfo The source FFDC information. 
         * @param newCount The count to assign to the new information.
         */
        FFDCInfo(FFDCInfo sourceInfo, int newCount) {
            this(sourceInfo.machine, sourceInfo.ffdcFile, newCount);
        }

        @Override
        public String toString() {
            if ( printString == null ) {
                printString =
                    getClass().getSimpleName() +
                    "[count=" + count + ", file=" + ffdcFile + ", machine=" + machine + "]";
            }
            return printString;
        }
    }

    /**
     * Override to intercept {@link Statement#evaluate()}, on the method
     * block obtained from {@link org.junit.runners.BlockJUnit4ClassRunner#methodBlock}).
     * 
     * Several particular modifications are made:
     *
     * <ul>
     * <li>Methods are filtered according to the current test repeat.
     * See {@link RepeatTestFilter#shouldRun(FrameworkMethod)}.</li>
     * 
     * <li>Temporary directory processing is performed before and after invoking
     * {@link Statement#evaluate()}.  This optional processing determines if
     * the test created an excessive quantity of data in the temporary
     * directory.</li>
     * 
     * <li>FFDC processing is performed before and after invoking
     * {@link Statement#evaluate()}.  This processing looks for new FFDC
     * occurrences and matches them against expected and allowed FFDC.</li>
     *
     * <li>Any exceptions thrown during test evaluation are cloned with a time stamp
     * and are rethrown.</li>
     * </ul>
     *
     * The intercept could be implemented using a test rule, but that would require
     * annotating every test class, which is less than helpful.
     *
     * @param method A test method which is being intercepted.
     * 
     * @return A statement which intercepts {@link Statement#evaluate()} and which
     *     performs additional FATRunner specific processing.
     */
    @Override
    public Statement methodBlock(FrameworkMethod method) {
        Statement superStatement = super.methodBlock(method);

        Statement statement = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                String methodName = "methodBlock.evaluate";

                if ( !RepeatTestFilter.shouldRun(method) ) {
                    return;
                }

                String testClassName = getTestClass().getName();
                String testMethodName = method.getName();
                String testName = testClassName + '.' + testMethodName;

                Map<String, Long> initialTmpFiles;
                if ( ENABLE_TMP_DIR_CHECKING ) {
                    initialTmpFiles = createDirectorySnapshot(TMP_DIR_PATH);
                } else {
                    initialTmpFiles = null;
                }

                try {
                    Log.info(c, methodName, "Entering " + testName);

                    // Wrap the test method evaluation with validation of FFDC logged excetions.
                    // Do do by retrieving the FFDC counts before and after the tests,
                    // then matching the difference against the expected and allowed FFDCs.
                    //
                    // If an exception is expected, then at least one FFDC must be added that has
                    // that exception.  If no FFDCs are added which have the exception then the
                    // test fails.
                    //
                    // If an exception is allowed (and not expected), any new FFDCs which have that
                    // exception are ignored.
                    //
                    // If an exception is neither expected nor allowed, any new FFDCs which have that
                    // exception will cause the test to fail.
                    //
                    // Note that FFDC settings are provided both using annotations and using the file
                    // specified by system property "fileFFDC".  Annotations ExpectedFFDC and
                    // AllowedFFDC specify required and optional FFDC exceptions.  System property
                    // 'fileFFDC' specifies additional optional FFDCs.

                    Map<String, FFDCInfo> initialFfdc = readFFDCSummaries("initial");

                    superStatement.evaluate();

                    Map<String, FFDCInfo> finalFfdcs = readFFDCSummaries("final");
                    Map<String, FFDCInfo> addedFfdcs = removeAll(initialFfdc, finalFfdcs);

                    for ( Map.Entry<String, FFDCInfo> ffdcEntry : addedFfdcs.entrySet() ) {
                        Log.info(c,  methodName,
                            "Test " + testName +
                            ": FFDC: " + ffdcEntry.getKey() + ": " + ffdcEntry.getValue());                                
                    }

                    List<String> errors = new ArrayList<String>();

                    // Missing an expected FFDC causes a test failure.

                    List<String> expectedFfdcs = getExpectedFFDCAnnotationFromTest(method);
                    for ( String expectedFfdc : expectedFfdcs ) {
                        FFDCInfo info = addedFfdcs.remove(expectedFfdc);
                        if ( info == null ) {
                            String error =
                                "Error: Test " + testName + ": missing expected FFDC: " + expectedFfdc; 
                            Log.info(c, methodName, error);;
                            errors.add(error);
                        } else {
                            Log.info(c, methodName,
                                "Test " + testName + ": found expected FFDC: " + expectedFfdc);                             
                        }
                    }

                    // Allowed FFDC are neither required nor forbidden.

                    Set<String> allowedFfdcs = getAllowedFFDCAnnotationFromTest(method);
                    for ( String allowedFfdc : allowedFfdcs ) {
                        if ( allowedFfdc.equals(AllowedFFDC.ALL_FFDC) ) {
                            for ( String allowedException : addedFfdcs.keySet() ) {
                                Log.info(c, methodName,
                                    "Test " + testName + ": ignoring allowed FFDC: " + allowedException);                              
                            }
                            addedFfdcs.clear();

                        } else {
                            if ( addedFfdcs.remove(allowedFfdc) != null ) {
                                Log.info(c, methodName,
                                    "Test " + testName + ": ignoring allowed FFDC: " + allowedFfdc);                              
                            }
                        }
                    }

                    // Delay processing the FFDC header information until after filtering
                    // expected and allowed exceptions.  This reduces the expense of loading
                    // FFDC header information.

                    // 'fileFFDC' specified FFDC are handled as allowed FFDC: They are neither
                    // required nor forbidden.  Note that the match condition can be more than
                    // the logged exception.
                    
                    // Do our best to avoid processing FFDC headers: Limit header processing
                    // to actual ignorable FFDCs.

                    if ( !IgnoredFFDCs.FFDCs.isEmpty() ) {
                        Map<String, FFDCInfo> ignorableFFDC = null;
                        for ( IgnoredFFDC ffdcToIgnore : IgnoredFFDCs.FFDCs ) {
                            FFDCInfo ffdcInfo = addedFfdcs.get(ffdcToIgnore.exception);
                            if ( ffdcInfo == null ) {
                                continue;
                            }
                            if ( ignorableFFDC == null ) {
                                ignorableFFDC = new HashMap<String, FFDCInfo>(3);
                            }
                            ignorableFFDC.put(ffdcToIgnore.exception, ffdcInfo);
                        }

                        if ( ignorableFFDC != null ) {
                            for ( FFDCInfo ffdcInfo : ignorableFFDC.values() ) {
                                ffdcInfo.setHeader(); // throws Exception
                            }

                            // Note the slight inefficiency of iterating across all of the
                            // ignored FFDC exceptions.  This could be avoided if the ignored
                            // FFDCs collection ere indexed by exception class.
                            //
                            // The minor inefficiency is alright, since the list of ignored
                            // FFDC exceptions is expected to be small.

                            for ( IgnoredFFDC ffdcToIgnore : IgnoredFFDCs.FFDCs ) {
                                FFDCInfo ffdcInfo = ignorableFFDC.get(ffdcToIgnore.exception);
                                if ( (ffdcInfo != null) && ffdcToIgnore.ignore(ffdcInfo.ffdcHeader) ) {
                                    Log.info(c, methodName,
                                        "Test " + testName + ": ignoring allowed FFDC: " + ffdcToIgnore.exception);                              
                                    addedFfdcs.remove(ffdcToIgnore.exception);
                                }
                            }
                        }
                    }

                    // Finally, any remaining added FFDC are unexpected, and cause the test to fail.

                    for ( Map.Entry<String, FFDCInfo> ffdcEntry : addedFfdcs.entrySet() ) {
                        FFDCInfo ffdcInfo = ffdcEntry.getValue();

                        int count = ffdcInfo.count;
                        if ( count == 0 ) {
                            continue;
                        }

                        String error = 
                            "Error: Test " + testName +
                            ": unexpected FFDC: " + ffdcEntry.getKey() + ": count: " + count;
                        if ( ffdcInfo.ffdcFile != null ) {
                            error +=
                                ": " + ffdcInfo.ffdcFile +
                                ": " + ffdcInfo.ffdcHeader;
                        }
                        Log.info(c, methodName, error);;
                        errors.add(error);
                    }

                    if ( !errors.isEmpty() ) {
                        blowup( methodName, errors.toString() );
                    }

                } catch ( Throwable t ) {
                    if (t instanceof AssumptionViolatedException) {
                        Log.info(c, methodName, "assumption violated: " + t);

                    } else {
                        Log.error(c, methodName, t);
                        if ( t instanceof MultipleFailureException ) {
                            Log.info(c, methodName, "Multiple failure");
                            MultipleFailureException e = (MultipleFailureException) t;
                            for ( Throwable t2 : e.getFailures() ) {
                                Log.error(c, methodName, t2, "Specific failure:");
                            }
                        }
                    }

                    throw newThrowableWithTimeStamp(t);

                } finally {
                    if ( ENABLE_TMP_DIR_CHECKING ) {
                        Map<String, Long> tmpDirFilesAfterTest = createDirectorySnapshot(TMP_DIR_PATH);
                        compareDirectorySnapshots("/tmp", initialTmpFiles, tmpDirFilesAfterTest);
                    }

                    Log.info(c, methodName, "Exiting " + testName);
                }
            }
        };

        return statement;
    }

    private static final boolean LEAST = true;
    private static final boolean GREATEST = false;
    
    /**
     * Obtain a least or greatest of a collection of string values.  Ignore
     * any nulls in the collection.
     * 
     * @param values The values from which to select the least or greatest value.
     * @param least Control parameter.  When true, select the least value.
     *     When false, select the greatest value.
     *
     * @return The least or greatest value of a collection of values.  Null if
     *     the collection is empty, or only contains null values.
     */
    private static String select(Collection<String> values, boolean least) {
        if ( values.isEmpty() ) {
            return null;
        }
        String selectedValue = null;
        for ( String value : values ) {
            if ( value == null ) {
                continue;
            }
            if ( selectedValue == null ) {
                selectedValue = value;
            } else if ( least && selectedValue.compareTo(value) > 0 ) {
                selectedValue = value;
            } else if ( !least && selectedValue.compareTo(value) < 0 ) {
                selectedValue = value;
            } else {
                // the least value is still least.
            }
        }
        return selectedValue;
    }
    
    /**
     * Override to intercept {@link Statement#evaluate()}, on the method
     * block obtained from {@link org.junit.runners.BlockJUnit4ClassRunner#classBlock}).
     *
     * Several particular modifications are made:
     *
     * <ul>
     * <li>Classes are filtered according to the current test repeat.
     * See {@link RepeatTestFilter#shouldRun(Class)}.</li>
     * 
     * <li>Temporary directory processing is performed before and after invoking
     * {@link Statement#evaluate()}.  This optional processing determines if
     * the test created an excessive quantity of data in the temporary
     * directory.</li>
     * 
     * <li>FFDC processing is performed after invoking
     * {@link Statement#evaluate()}.  This processing looks for unexpected
     * new FFDC occurrences.</li>
     *
     * Log sizes are examined, and any which is too large causes a test failure.
     *
     * @param notifier The notifier to be used when running the test class.
     *
     * @return A statement which intercepts {@link Statement#evaluate()} and which
     *     performs additional FATRunner specific processing.
     */
    @Override
    public Statement classBlock(RunNotifier notifier) {
        Statement superStatement = super.classBlock(notifier);

        Statement statement = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                String methodName = "classBlock.evaluate";

                if ( !RepeatTestFilter.shouldRun( getTestClass().getJavaClass() ) ) {
                    return;
                }

                Log.info(c, methodName, "Beginning test class " + getTestClass());

                try {
                    superStatement.evaluate();

                } finally {
                    Log.info(c, methodName, "Ending test class " + getTestClass());

                    // Get any FFDCs we care about before recovering the servers.

                    String ffdcHeader = null;
                    try {
                        // This won't detect (and never did) FFDCs generated during class initialization - oh well!
                        List<String> ffdcAfterTest = retrieveFFDCLogs();

                        LibertyServerFactory.tidyAllKnownServers( getTestClassNameForAssociatedServers() );

                        List<String> ffdcAfterTidying = retrieveFFDCLogs();
                        List<String> ffdcAdditions = removeAll(ffdcAfterTest, ffdcAfterTidying);

                        // Remember the first added FFDC.  This will cause a test failure.
                        if ( !ffdcAdditions.isEmpty() ) {
                            ffdcHeader = readFFDCHeader( select(ffdcAdditions, LEAST) );
                        }

                    } finally {
                        LibertyServerFactory.recoverAllServers( getTestClassNameForAssociatedServers() );
                    }

                    // Now, if there was an added FFDC, fail the test.

                    if ( ffdcHeader != null ) {
                        blowup(methodName, "New FFDC detected during class level test cleanup:\n" + ffdcHeader);
                    }

                    // Fail the test if excess trace was generated.
                    //
                    // This could be done before or after the FFDC test.  Do this second,
                    // since an FFDC failure seems more severe.

                    LogPolice.checkUsedTrace();
                }
            }
        };

        return statement;
    }

    /**
     * Retrieve the names of expected FFDC exception class names which are
     * recorded on a specified test method.
     * 
     * Unlike allowed FFDC exception class names, which may also be recorded
     * on test classes, expected FFDC class names are recorded only on methods.
     * 
     * Expected exception class names are recorded using annotation
     * {@link ExpectedFFDC}.
     *
     * Filter the class names based on the {@link ExpectedFFDC#repeatAction()}
     * setting, and on what repeat action (if any) is running.
     * 
     * When running the EE9 repeat action, perform ee9 package replacement
     * on the exception class names.
     *
     * @param m The test method for which to retrieve expected FFDC
     *     exception class names.
     *
     * @return The expected FFDC exception class names recorded on the method.
     */
    public List<String> getExpectedFFDCAnnotationFromTest(FrameworkMethod m) {
        ExpectedFFDC expectedFfdc = m.getAnnotation(ExpectedFFDC.class);
        if ( expectedFfdc == null ) {
            return Collections.emptyList();
        }

        if ( RepeatTestFilter.isAnyRepeatActionActive() ) {
            boolean useThisFFDC = false;
            for ( String repeatAction : expectedFfdc.repeatAction() ) {
                if ( repeatAction.equals(ExpectedFFDC.ALL_REPEAT_ACTIONS) ||
                     RepeatTestFilter.isRepeatActionActive(repeatAction) ) {
                    useThisFFDC = true;
                    break;
                }
            }
            if ( !useThisFFDC ) {
                return Collections.emptyList();
            }
        }

        List<String> exceptionClassNames = new ArrayList<String>();

        boolean isEE9Active = JakartaEE9Action.isActive();

        for ( String exceptionClassName : expectedFfdc.value() ) {
            if ( isEE9Active ) {
                exceptionClassName = ee9ReplacePackages(exceptionClassName);
            }
            exceptionClassNames.add(exceptionClassName);
        }
        
        return exceptionClassNames;
    }

    /**
     * Retrieve the names of allowed FFDC exception class names which are
     * recorded on a specified test method.
     * 
     * Unlike expected FFDC exception class names, which are only recorded
     * on methods, allowed FFDC class names may also be recorded on test
     * classes.
     * 
     * Allowed exception class names are recorded using annotation
     * {@link AllowedFFDC}.
     *
     * Filter the class names based on the {@link AllowedFFDC#repeatAction()}
     * setting, and on what repeat action (if any) is running.
     * 
     * When running the EE9 repeat action, perform ee9 package replacement
     * on the exception class names.
     *
     * @param m The test method for which to retrieve allowed FFDC
     *     exception class names.
     *
     * @return The allowed FFDC exception class names recorded on the method,
     *    or on the active test class.
     */
    private Set<String> getAllowedFFDCAnnotationFromTest(FrameworkMethod m) {
        AllowedFFDC methodAllowedFFDC = m.getAnnotation(AllowedFFDC.class); 

        Class<?> declaringClass = m.getMethod().getDeclaringClass();
        AllowedFFDC classAllowedFFDC = declaringClass.getAnnotation(AllowedFFDC.class); 

        Class<?> testClass = getTestClass().getJavaClass();
        AllowedFFDC testClassAllowedFFDC;

        if ( !declaringClass.equals(testClass) ) {
            testClassAllowedFFDC = testClass.getAnnotation(AllowedFFDC.class);        
        } else {
            testClassAllowedFFDC = null;
        }
        
        if ( (methodAllowedFFDC == null) &&
             (classAllowedFFDC == null) &&
             (testClassAllowedFFDC == null) ) {
            return Collections.emptySet();
        }

        Set<AllowedFFDC> allowedFfdcs = new HashSet<AllowedFFDC>(3);
        if ( methodAllowedFFDC != null ) {
            allowedFfdcs.add(methodAllowedFFDC);
        }
        if ( classAllowedFFDC != null ) {
            allowedFfdcs.add(classAllowedFFDC);
        }
        if ( testClassAllowedFFDC != null ) {
            allowedFfdcs.add(testClassAllowedFFDC);
        }

        if ( RepeatTestFilter.isAnyRepeatActionActive() ) {
            Set<AllowedFFDC> selectedFfdcs = new HashSet<AllowedFFDC>(3);
            for ( AllowedFFDC allowedFfdc : allowedFfdcs ) {
                for ( String repeatAction : allowedFfdc.repeatAction() ) {
                    if ( repeatAction.equals(AllowedFFDC.ALL_REPEAT_ACTIONS) ||
                         RepeatTestFilter.isRepeatActionActive(repeatAction) ) {
                        selectedFfdcs.add(allowedFfdc);
                        break;
                    }
                }
            }
            if ( selectedFfdcs.isEmpty() ) {
                return Collections.emptySet();
            } else {
                allowedFfdcs = selectedFfdcs;
            }
        }
        
        Set<String> exceptionClassNames = new HashSet<String>();

        boolean isEE9Active = JakartaEE9Action.isActive();
        for ( AllowedFFDC allowedFfdc : allowedFfdcs ) {
            for ( String exceptionClassName : allowedFfdc.value() ) {
                if ( isEE9Active ) {
                    exceptionClassName = ee9ReplacePackages(exceptionClassName);
                }
                exceptionClassNames.add(exceptionClassName);
            }
        }

        return exceptionClassNames;
    }

    /**
     * Remove all of an initial table from a final table.
     *
     * The input tables use composite keys, which are an exception class name
     * plus an FFDC file name.
     *
     * The output table merges FFDC information from composite keys, using just
     * the exception class names as keys.
     *
     * Note: The implementation assumes that the final table is strictly the same
     * as or larger than the initial table.  If the final table has fewer entries
     * than the initial table, the result table will have FFDC information with
     * negative counts.
     *
     * @param initialTable The table which is to be removed.
     * @param finalTable The table from which to remove the initial table.
     *
     * @return A copy of final table after having removed the initial table, and
     *     projecting initial key values to exception class names.
     */
    private Map<String, FFDCInfo> removeAll(
        Map<String, FFDCInfo> initialTable, Map<String, FFDCInfo> finalTable) {

        // 'initialTable' and 'finalTable' have composite keys:
        // exceptionClass + ':' + ffdcFileName.
        //
        // 'deltaTable' has keys which are just 'exceptionClass'.
        //
        // The final table is expected to always be a superset of the initial
        // table.  That is, FFDCs should only be added by a test, never
        // removed.  (Removed FFDCs, if they ever occurred, would be harmlessly
        // ignored.)
        //
        // Difference entries are generated by subtracting any matching initial
        // entry from each final entry, then stripping out the exception from
        // the difference entry, and adding the exception plus the count difference
        // to the output delta table.
        
        Map<String, FFDCInfo> deltaTable = new HashMap<String, FFDCInfo>( finalTable.size() );

        for ( Map.Entry<String, FFDCInfo> finalEntry : finalTable.entrySet() ) {
            String finalKey = finalEntry.getKey();
            FFDCInfo finalInfo = finalEntry.getValue();
            FFDCInfo initialInfo = initialTable.get(finalKey);

            int delta;
            if ( initialInfo != null ) {
                delta = finalInfo.count - initialInfo.count;
            } else {
                delta = finalInfo.count;
            }

            // 'delta' has been generated for an exception + file name composite key.

            if ( delta != 0 ) {
                // Add to the output delta table using just the exception.  Add
                // together counts from different files.

                String finalException = finalKey.substring(0, finalKey.indexOf(':'));
                FFDCInfo deltaInfo = deltaTable.get(finalException);
                if ( deltaInfo == null ) {
                    // The first of the final entries which generated
                    // a difference is stored to the delta table.  That
                    // picks a single FFDC file to represent the exception.
                    // Which FFDC file this is is unclear.
                    deltaInfo = new FFDCInfo(finalInfo, delta);
                    deltaTable.put(finalException, deltaInfo);
                } else {
                    deltaInfo.add(delta);
                }
            }
        }

        return deltaTable;
    }

    /**
     * Read an FFDC header.
     * 
     * Attempt the read on all servers.  Answer from the first server which
     * successfully reads the file.
     *
     * Answer an error message if the read failed, either due to an exception,
     * or because the target file was not found.  This is an acceptable
     * alternative to throwing the exception, since this method is used during
     * error reporting, and the result text is displayed as a part of the
     * reported error.
     *
     * @param ffdcFileName The name of the file which is to be retrieved.
     *
     * @return The header of the specified file.
     */
    private String readFFDCHeader(String ffdcFileName) {
        for ( LibertyServer server : getRunningLibertyServers() ) {
            try {
                RemoteFile ffdcLogFile = server.getFFDCLogFile(ffdcFileName);
                return getFFDCHeader(ffdcLogFile);

            } catch ( FileNotFoundException e ) {
                // Ignore

            } catch ( Exception e ) {
                Log.warning(c, "Difficulties encountered searching for exceptions in FFDC logs: " + e);
                return "[Could not read file contents because of unexpected exception: " + e + "]";
            }
        }

        // We really should never get to this code since we just found the FFDC file
        return "[Could not find FFDC file " + ffdcFileName + "]";
    }

    /**
     * Read FFDC summaries from all running liberty servers.
     * 
     * Merge the read summaries into a single table.  Adjust the keys of the per-server tables
     * by appending the path to the single server summaries.  That enables a single table to be
     * returned while avoiding merging FFDC information from different servers.
     *
     * Retry reads of single-server summaries up to five times, with a half second delay
     * between attempts.
     * 
     * Ignore servers for which FFDC checking is disabled.
     * See {@link LibertyServer#getFFDCChecking()}.
     * 
     * @param tag A descriptive tag used with logging.
     *
     * @return A table of FFDC summary information, merged, from all running servers.
     */
    private Map<String, FFDCInfo> readFFDCSummaries(String tag) {
        String methodName = "readFFDCSummaries";

        Map<String, FFDCInfo> mergedSummary = new LinkedHashMap<String, FFDCInfo>();

        for ( LibertyServer server : getRunningLibertyServers() ) {
            if ( !server.getFFDCChecking() ) {
                Log.info(c, methodName, "Skip disabled FFDC checking for : " + server.getServerName());
                continue;
            }

            Map<String, FFDCInfo> serverSummary = readFFDCSummary(server, tag);
            if ( serverSummary == null ) {
                continue;
            }

            for ( Map.Entry<String, FFDCInfo> serverEntry : serverSummary.entrySet() ) {
                String exceptionClassName = serverEntry.getKey();
                FFDCInfo ffdcInfo = serverEntry.getValue();

                mergedSummary.put(exceptionClassName + ':' + ffdcInfo.ffdcFile, ffdcInfo);
            }
        }

        return mergedSummary;
    }

    /**
     * Read the FFDC summary of a server.
     * 
     * Retry the read up to five times.  This is an only partially successful
     * attempt to handle concurrent writes from the server to the summary file.
     * 
     * Answer null if all reads fail, or if the server folders are completely
     * unavailable.
     *
     * Only the read the last available summary file.
     *
     * @param server The server for which to read FFDC summaries.
     * @param tag A descriptive tag to use with logging.
     *
     * @return The table of FFDC information from the FFDC summary file.
     *     Null if no server folders are available, or if all read attempts
     *     fail.
     */
    private Map<String, FFDCInfo> readFFDCSummary(LibertyServer server, String tag) {
        String methodName = "readFFDCSummary";
        
        String serverName = server.getServerName();

        Map<String, FFDCInfo> ffdcs = null;
        boolean noSummaries = false;

        for ( int attempts = 0; (ffdcs == null) && (attempts < 5); attempts++ ) {
            if ( attempts > 0 ) {
                Log.info(c, methodName, "Retry read (" + tag + "): " + serverName); 
            } else {
                Log.info(c, methodName, "Read (" + tag + "): " + serverName); 
            }

            List<String> ffdcSummaries;
            try {
                ffdcSummaries = server.listFFDCSummaryFiles(serverName);
            } catch ( TopologyException e ) {
                // A topology exception indicates that no FFDC folder is
                // available for the server.  Stop trying immediately
                // in this case.
                noSummaries = true;
                break;
            } catch ( Exception e ) {
                Log.error(c, methodName, e);
                continue;
            }

            if ( ffdcSummaries.isEmpty() ) {
                Log.info(c, methodName, "No " + tag + " summaries were found: " + serverName);
                try {
                    Thread.sleep(500);
                } catch ( InterruptedException e ) {
                    // Ignore
                }
                continue;
            }

            if ( ffdcSummaries.size() > 1 ) {
                Log.info(c, methodName, "Read " + ffdcSummaries.size() + " " + tag + " FFDC summaries");
            }

            String lastSummary = select(ffdcSummaries, GREATEST);
            Log.info(c, methodName, "Selected " + tag + " summary: " + lastSummary);
            
            RemoteFile summaryFile;
            try {
                summaryFile = server.getFFDCSummaryFile(lastSummary);
            } catch ( Exception e ) {
                Log.warning(c, "Failed to locate " + tag + " summary: " + serverName);
                Log.error(c, methodName, e);
                continue;
            }

            ffdcs = readFFDCSummary(summaryFile);
        }

        if ( noSummaries ) {
            Log.info(c, methodName, "No " + tag + " summary available: " + serverName);
        } else if ( ffdcs == null ) {
            Log.info(c, methodName, "Retry failure (" + tag + "): " + serverName);
        } else {
            Log.info(c, methodName, "Read FFDC summary (" + tag + "): " + serverName);
            for ( Map.Entry<String, FFDCInfo> ffdcEntry : ffdcs.entrySet() ) {
                Log.info(c, methodName, "  " + ffdcEntry.getKey() + ": " + ffdcEntry.getValue() ); 
            }
        }
        return ffdcs;
    }

    // FFDC summary file format:
    //
    //  Index  Count  Time of first Occurrence    Time of last Occurrence     Exception SourceId ProbeId
    // ------+------+---------------------------+---------------------------+---------------------------
    //      0      2     4/11/13 2:25:30:312 BST     4/11/13 2:25:30:312 BST java.lang.ClassNotFoundException com.ibm.ws.config.internal.xml.validator.XMLConfigValidatorFactory 112
    //                                                                             - /test/jazz_build/jbe_rheinfelden/jazz/buildsystem/buildengine/eclipse/build/build.image/wlp/usr/servers/com.ibm.ws.config.validator/logs/ffdc/ffdc_13.04.11_02.25.30.0.log
    //                                                                             - /test/jazz_build/jbe_rheinfelden/jazz/buildsystem/buildengine/eclipse/build/build.image/wlp/usr/servers/com.ibm.ws.config.validator/logs/ffdc/ffdc_13.04.11_02.25.30.0.log
    //      1      1     4/11/13 2:25:31:959 BST     4/11/13 2:25:31:959 BST java.lang.NullPointerException com.ibm.ws.threading.internal.Worker 446
    // ------+------+---------------------------+---------------------------+---------------------------

    /**
     * Parse an FFDC summary file into a table.  Keys are exception class names.
     * 
     * The machine and FFDC file of the first occurrence of each recorded exception class
     * is recorded to the summary table.  Following FFDC files are not recorded.  
     * 
     * @param summaryFile The FFDC summary file which is to be read.
     *
     * @return A table of FFDC information read from the summary file.
     *
     * @throws Exception
     */
    @SuppressWarnings("null")
    private Map<String, FFDCInfo> readFFDCSummary(RemoteFile summaryFile) {
        try {
            // TFB: This previously would answer an empty map, which would bypass the
            //      retry logic.
            if ( !summaryFile.exists() ) {
                return null;
            }

            try ( InputStream summaryStream = summaryFile.openForReading() ) { 
                BufferedReader reader = new BufferedReader(new InputStreamReader(summaryStream));

                // Read three lines, plus the first actual summary line. 
                String line = null;
                for ( int lineNo = 0; lineNo < 4; lineNo++ ) {
                    line = reader.readLine();
                    if ( line == null ) {
                        Log.warning(c, "Unexpected end of file reading FFDC summary: " + summaryFile);
                        return null;
                    }
                }

                // Either a dash line, or an exception line, or an FFDC file link:
                //
                // ------+------+---------------------------+---------------------------+---------------------------
                //
                //  0      1     4/11/13 2:25:30:312 BST
                //    4/11/13 2:25:30:312 BST
                //    java.lang.ClassNotFoundException
                //    com.ibm.ws.config.internal.xml.validator.XMLConfigValidatorFactory 112
                //
                //  - /test/jazz_build/jbe_rheinfelden/jazz/buildsystem/buildengine/eclipse/build/build.image/wlp/usr/servers/com.ibm.ws.config.validator/logs/ffdc/ffdc_13.04.11_02.25.30.0.log

                Map<String, FFDCInfo> ffdcTable = new LinkedHashMap<String, FFDCInfo>();

                int exceptionCount = 0;
                String exception = "";

                while ( !line.startsWith("---") ) {
                    String[] parts = line.trim().split("\\s+");
                    if ( parts.length > 9 ) {
                        exceptionCount = Integer.parseInt(parts[1]);
                        exception = parts[8];

                    } else if ( (parts.length > 1) && parts[0].equals("-") ) {
                        String ffdcFile = parts[1];

                        FFDCInfo ffdcInfo = ffdcTable.get(exception);
                        if ( ffdcInfo == null ) {
                            ffdcInfo = new FFDCInfo( summaryFile.getMachine(), ffdcFile, exceptionCount);
                            ffdcTable.put(exception, ffdcInfo);
                        } else {
                            ffdcInfo.add(exceptionCount);
                        }

                    } else {
                        Log.warning(c, "Failed to parse FFDC summary: " + summaryFile + " [ " + line + " ]");
                        return null;
                    }

                    line = reader.readLine();
                    if ( line == null ) {
                        Log.warning(c, "Unexpected end of file reading FFDC summary: " + summaryFile);
                        return null;
                    }
                }

                return ffdcTable;
            }

        } catch ( Exception e ) {
            Log.warning(c, "Exception reading FFDC summary: " + summaryFile);
            Log.error(c, "parseSummary", e);
            return null;
        }
    }

    /**
     * Answer the names of FFDC files for all of the running liberty
     * servers.
     *
     * @return The names of FFDC files for all running liberty servers.
     */
    private List<String> retrieveFFDCLogs() {
        List<String> ffdcFileNames = new ArrayList<String>();
        try {
            for ( LibertyServer libertyServer : getRunningLibertyServers() ) {
                try {
                    ffdcFileNames = LibertyServerFactory.retrieveFFDCFile(libertyServer);
                } catch ( TopologyException e ) {
                    // Ignore
                } catch ( Exception e ) {
                    Log.error(c, "retrieveFFDCLogs", e);
                }
            }
        } catch ( Exception e ) {
            Log.error(c, "retrieveFFDCLogs", e);
        }
        return ffdcFileNames;
    }

    /**
     * Helper for FFDC processing: Retrieve the running Liberty servers which
     * are associated with this test class.
     * 
     * @return The running liberty servers associated with this test class. 
     */
    private Collection<LibertyServer> getRunningLibertyServers() {
        return LibertyServerFactory.getKnownLibertyServers(getTestClassNameForAssociatedServers());
    }
    
    /**
     * Helper for associating test classes with running servers.
     * 
     * Usually, the test class name is obtained by {@link #getTestClass()}.
     * 
     * However, test classes may override that value by supplying a field which
     * is annotated with {@link ClassRule} and with {@link LibertyServerWrapper}.
     * When these annotations are present, the test name is obtained from the type
     * of the annotated field.
     * 
     * (This rename capability is provided for specific test implementations of CDI,
     * for which the usual test class name is not correct.)
     * 
     * See {@link LibertyServerFactory#getKnownLibertyServers(String)}.
     *
     * @return The name which is used by the liberty server factory to associated
     *     running servers with test classes. 
     */
    private String getTestClassNameForAssociatedServers() {
        String testClassName = getTestClass().getName();
        List<FrameworkField> classRuleFields = getTestClass().getAnnotatedFields(ClassRule.class);
        for ( FrameworkField ff : classRuleFields ) {
            Class<?> ffType = ff.getType();
            if ( ffType.isAnnotationPresent(LibertyServerWrapper.class) ) {
                testClassName = ffType.getName();
            }
        }
        return testClassName;
    }
    
    /**
     * Remove values from a target list.  Do not modify the target list: Place
     * the remaining values in a new list.
     * 
     * @param valuesToRemove The values which are to be removed.
     * @param values The values which are to be modified.
     *
     * @return A copy of the target list with the specified values removes. 
     */
    private static List<String> removeAll(List<String> valuesToRemove, List<String> values) {
        // This implementation does an extra iteration: A more efficient implementation
        // would iterate across the target list and add those elements which are not in
        // the removal list.  However, that implementation will operate different when the
        // target list and the removal list have duplicate entries.

        List<String> adjustedValues = new ArrayList<String>(values);
        adjustedValues.removeAll(valuesToRemove);
        return adjustedValues;
    }
}

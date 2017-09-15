/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.js.test;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.jms.JMSException;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import com.ibm.mqst.jetsam.JETSAMLog;
import com.ibm.mqst.jetsam.JETSAMLogImpl;
import com.ibm.mqst.jetsam.JMSLogImpl;

/**
 * @author matrober
 * 
 *         This class implements an extension of the JUnit TestCase class which provides
 *         facility to log test output to a file and the screen. To make effective use of
 *         the logging, testcases should use the "error" and "comment" methods in preference
 *         to the "fail" method from the JUnit TestCase.
 * 
 *         Specifying log location
 *         -----------------------
 *         The location of the logs is by default in a directory called "logs" under the location
 *         of the testcase being run.
 * 
 *         It is possible to override the location of the logs in two ways;
 *         a) Call LoggingTestCase.setLoggingDir("c:\\temp\\logs"); supplying your directory
 *         b) add a flag -DLOGGING_DIR=c:\temp\logs to the java command line.
 * 
 * 
 *         Enabling and disabled logging
 *         -----------------------------
 *         By default the LoggingTestCase will log and comment or error messages to the log
 *         file (on the file system) and not to the system console. This can be altered at
 *         compile time by changing the default values of the TO_FILE and TO_SCREEN variables
 *         in this file.
 * 
 *         It can also be altered at runtime through use of the following methods;
 *         public static void setFileLoggingEnabled(boolean enabled);
 *         public static void setScreenLoggingEnabled(boolean enabled);
 * 
 * 
 *         Class and test headers
 *         ----------------------
 *         As part of 162922.5 I have added the ability to customize the test class and test
 *         method headers. The following methods can be overridden by subclasses to adjust this
 *         functionality;
 * 
 *         protected void printClassHeader(JETSAMLog log);
 *         protected void printTestHeader(JETSAMLog log);
 *         protected void printTestFooter(JETSAMLog log);
 * 
 *         The implementation of printClassHeader supplied with this class also calls the
 *         following method to provide the opportunity to log some extra information on a per
 *         test class basis - the intention is for this to be used to show build information.
 * 
 *         protected void printExtraClassInfo(JETSAMLog log);
 * 
 * 
 *         Exception message checking
 *         --------------------------
 *         As part of 162922.5 I have added the following method which provides an easy way
 *         to check the message contained within a Throwable object (i.e., Exception) is as
 *         expected;
 * 
 *         public void assertMessage(Throwable t, String srchString);
 * 
 *         It is expected that the second parameter to this method be one of the message
 *         catalog codes - for instance "SIAP0022" rather than the actual text string, in order
 *         that it can cope with NLS translation and missing message catalogs.
 * 
 *         Note that for readability of the testcase you can specify the full catalog string
 *         (e.g., EXCEPTION_RECEIVED_SIAP0022) and the assertMessage method will examine the
 *         string to determine whether it conforms to a *_CCCCnnnn pattern. If this is true,
 *         then it will only check for the SIAP0022 part in order to carry on functioning
 *         whether the message catalog is present or not.
 * 
 */
public class LoggingTestCase extends TestCase {
    // Added at version 1.36
    private static final long serialVersionUID = 951938032404317577L;

    /** This object is used to provide the logging capabilities. */
    private static JETSAMLog log = null;

    /** If non-null, the root of the log file tree */
    private static String loggingDir = null;

    /** The name of the property used to set the loggingDir */
    public static final String LOGGING_DIR_STRING = "LOGGING_DIR";

    /** Whether to send output to the screen */
    private static boolean TO_SCREEN = false;

    /** Whether to send output to file */
    private static boolean TO_FILE = true;

    /** The maximum number of indents recorded in the indents array */
    private static final int MAX_INDENTS = 10;

    /** An array of String indents */
    private static String[] indents = null;

    /** The current indent level */
    private static int level = 0;

    /** The maximum number of bytes to print when analyzing byte arrays */
    private static final int MAX_BYTE_LENGTH_TO_PRINT = 16;

    /**
     * How many extra bytes to print when printing extracts of byte arrays
     * ADDITIONAL_BYTES appear at the beginning and end, so you actually get
     * twice this number
     */
    private static final int ADDITIONAL_BYTES = 2;

    /** The maximum safety margin we need to allow us to detect a possible deadlock before JUnit shoots us out of the water */
    private static final long MAXIMUM_TIMEOUT_MARGIN = SECONDS.toMillis(20);

    /** The maximum safety margin factor (the safety margin will be, at most, 1/MAXIMUM_SAFETY_PROPORTION of the total timeout) */
    private static int MAXIMUM_SAFETY_PROPORTION = 10;

    /** The time interval between core dumps if we suspect a possible deadlock */
    private static final long TIME_BETWEEN_DUMPS = SECONDS.toMillis(5);

    /** The absolute minimum safety margin we need (enough to do a single dump) */
    private static final long MINIMUM_TIMEOUT_MARGIN = SECONDS.toMillis(10);

    /** boolean that says if we've registered some Timer Tasks */
    private static boolean timerTasksScheduled = false;

    /** Name of the thread used for the timer tasks */
    private static final String TIMEOUT_THREAD_NAME = "UnitTestTimeoutMonitorThread";

    /** Writer object for integration with the unittest RAS component. */
    private static LTCWriter writer = null;

    /**
     * Used to track the amount of time taken by an individual test method.
     */
    private long testStartTime = 0;

    /** The time when the tick count was last started */
    private long start = 0;

    // ************************ STATIC INITIALIZER ***********************

    static {
        // Set up the indents stuff
        indents = new String[MAX_INDENTS];

        StringBuffer sb = new StringBuffer("");

        for (int i = 0; i < MAX_INDENTS; i++) {
            indents[i] = sb.toString();
            sb.append(". ");
        }

        // Set up filtering lines for stack traces.
        String[] loggingFilters =
                        new String[] {
                                      "junit.framework.TestCase",
                                      "junit.framework.TestResult",
                                      "junit.framework.TestSuite",
                                      "junit.framework.Assert.",
                                      // don't filter AssertionFailure
                                      "junit.swingui.TestRunner",
                                      "junit.awtui.TestRunner",
                                      "junit.textui.TestRunner",
                                      "java.lang.reflect.Method.invoke(",
                                      "org.eclipse.jdt.internal.junit.runner",
                                      "com.ibm.js.test.LoggingTestCase",
                                      "sun.reflect.NativeMethodAccessorImpl",
                                      "sun.reflect.DelegatingMethodAccessorImpl" };

        // Initialize the filtering requirements.
        JETSAMLogImpl.setFilter(loggingFilters);

        JETSAMLogImpl.setFilterEnabled(true);

        // p  312398  Enable ObjectStore FFDC writing
        // Turn on the FFDC writing for the object store to enable better diagnosis
        // of message store startup failures.
        // Defect 427952
        try {
            Class objectManager = Class.forName("com.ibm.ws.objectManager.ObjectManager"); // Load the OM class
            Field ffdcField = objectManager.getField("ffdc"); // Get the ffdc field
            Object ffdcObject = ffdcField.get(null); // Get the static instance of FFDC
            Class ffdcClass = ffdcObject.getClass(); // Get the Class for FFDC
            Method ffdcMethod = ffdcClass.getMethod("setPrintWriter", PrintWriter.class); // Find the setPringWriter method
            ffdcMethod.invoke(ffdcObject, new PrintWriter(System.out)); // Set the PrintWriter to System.out
        } catch (ClassNotFoundException exception) {
            // OK, object manager not in the path - no problem - we just ignore its absence!
        } catch (NoSuchFieldException exception) {
            exception.printStackTrace();
        } catch (NoSuchMethodException exception) {
            exception.printStackTrace();
        } catch (InvocationTargetException exception) {
            exception.printStackTrace();
        } catch (IllegalAccessException exception) {
            exception.printStackTrace();
        } catch (SecurityException exception) {
            exception.printStackTrace();
        }

        //  Read the system property for advanced trace
        String propVal = System.getProperty("advancedTrace");
        if (propVal != null &&
            (propVal.equalsIgnoreCase("true") ||
             propVal.equalsIgnoreCase("yes") ||
            propVal.equals("1")))
            ;
        //enableTracing();

    } //static

    // ************************ CONSTRUCTORS *****************************
    /* -------------------------------------------------------------------------- */
    /*
     * LoggingTestCase constructor
     * /* --------------------------------------------------------------------------
     */
    /**
     * Construct a new LoggingTestCase.
     * 
     * @param name The name of the LoggingTestCase
     */
    public LoggingTestCase(String name) {
        super(name);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * LoggingTestCase constructor
     * /* --------------------------------------------------------------------------
     */
    /**
     * Construct a new LoggingTestCase.
     * 
     */
    /* -------------------------------------------------------------------------- */
    /*
     * LoggingTestCase constructor
     * /* --------------------------------------------------------------------------
     */
    /**
     * Construct a new LoggingTestCase.
     * 
     */
    public LoggingTestCase() {
        // Nothing to do
    }

    // ************************** TESTCASE OVERRIDES *********************

    /* -------------------------------------------------------------------------- */
    /*
     * setUp method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ensureLogEnabled();

        /*
         * try
         * {
         * // This statement is part of the TrChecker class for verifying trace
         * // entry and exit points. It should be commented out before being checked
         * // into CMVC.
         * TrChecker.reset();
         * 
         * } catch(RuntimeException rte)
         * {
         * error(rte);
         * }
         */

        // Set up ready for the next test method.
        printTestHeader(log);

        // Register ourselves with the unittest RAS infrastructure if it is available.
        // We do this at setUp time rather than testcase construction because of the relative ordering
        // of things in the JUnit infrastructure.
        modifyRASRegistration(true);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * setUp method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        /*
         * try
         * {
         * // This statement is part of the TrChecker class for verifying trace
         * // entry and exit points. It should be commented out before being checked
         * // into CMVC.
         * TrChecker.assertEmpty();
         * 
         * } catch(RuntimeException rte)
         * {
         * error(rte);
         * }
         */

        printTestFooter(log);
        super.tearDown();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * runTest method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see junit.framework.TestCase#runTest()
     * @throws Throwable
     */
    @Override
    protected void runTest() throws Throwable {

        try {
            super.runTest();
            blankLine();
        } catch (Throwable t) {
            // No FFDC code needed
            comment("**** An error was thrown by the test ****");
            comment(t);
            throw t;
        }
    }

    // ******************* AbstractTest METHODS **************************

    /* -------------------------------------------------------------------------- */
    /*
     * indent method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Increase the level by one
     */
    public static void indent() {
        if (level < MAX_INDENTS - 1)
            level++;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * outdent method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Decrease the indent level by one
     */
    public static void outdent() {
        if (level >= 1)
            level--;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * print method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add a string to the log
     * 
     * @param str
     */
    public static void print(String str) {
        log.comment(indents[level] + str);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * print method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add a int to the log
     * 
     * @param value
     */
    public static void print(int value) {
        log.comment(indents[level] + value);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * print method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add a int to the log
     * 
     * @param value
     */
    public static void print(long value) {
        log.comment(indents[level] + value);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * info method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add an informational message to log and screen
     * 
     * @param str
     */
    public static void info(String str) {
        log.blankLine();
        print(str);
    }

    // ************************* JETSAMLog METHODS ******************************

    // **************** LIFECYCLE METHODS ************************

    /* -------------------------------------------------------------------------- */
    /*
     * open method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param newFile true if a new file is needed
     */
    public static void open(boolean newFile) {
        log.open(newFile);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * isOpen method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @return true if the log is open
     */
    public static boolean isOpen() {
        return log.isOpen();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * close method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Close the log
     */
    public static void close() {

        // Deregister with the RAS infrastructure.
        // Doing this here rather than at tearDown time means that any asynchronous messages
        // that are emitted are written to the currently 'active' test log.
        modifyRASRegistration(false);
        log.close();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * setFileLoggingEnabled method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Defines whether comments made in this testcase should be logged to a file.
     * 
     * @param enabled true if comments are to be logged to a file
     */
    public static void setFileLoggingEnabled(boolean enabled) {
        TO_FILE = enabled;

    }

    /* -------------------------------------------------------------------------- */
    /*
     * setScreenLoggingEnabled method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Defines whether comments made in this testcase should be logged to the screen.
     * 
     * @param enabled true if comments should be logged to the screen
     */
    public static void setScreenLoggingEnabled(boolean enabled) {
        TO_SCREEN = enabled;
    }

    // ***************** LOGGING METHODS *************************

    /* -------------------------------------------------------------------------- */
    /*
     * comment method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add an array of bytes as a comment
     * 
     * @param bytes
     */
    public static void comment(byte[] bytes) {
        if (bytes == null) {
            comment("byte[]: null");
        } else {
            StringBuilder sb = new StringBuilder("byte[" + bytes.length + "]: ");
            addByteArrayToStringBuilder(bytes, 0, bytes.length, sb);
            comment(sb.toString());
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * comment method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add a comment to the log
     * 
     * @param text
     */
    public static void comment(String text) {
        log.comment(text);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * comment method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add an exception to the log
     * 
     * @param e The exception to be added to the log
     */
    public static void comment(Exception e) {
        log.comment(e);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * comment method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add a Throwable object to the log
     * 
     * @param e The Throwable to be added to the log
     */
    public static void comment(Throwable e) {
        log.comment(e);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * comment method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add some text followed by an exception to the log
     * 
     * @param text The text to be added to the log
     * @param e The exception to be added to the log
     */
    public static void comment(String text, Exception e) {
        log.comment(text, e);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * comment method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add some text followed by an Error to the log
     * 
     * @param text The text to be added to the log
     * @param e The Error object to be added to the log
     */
    public static void comment(String text, Error e) {
        log.comment(text, e);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * error method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Record an error in the log and fail the test
     * 
     * @param text The text of the error to be added to the log
     */
    public static void error(String text) {
        log.error(text);
        fail(text);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * error method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Record an exception and fail the test
     * 
     * @param e The Exception to be added, as an error, to the log
     */
    public static void error(Exception e) {
        log.error(e);

        AssertionFailedError afe = new AssertionFailedError("Exception occurred: " + e.getMessage());
        afe.initCause(e);

        throw afe;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * error method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Log an AssertionFailedError and fail the test with it
     * 
     * @param e The assertion that failed
     */
    public static void error(AssertionFailedError e) {
        log.error(e);
        throw e;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * error method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Log an Error and fail the test
     * 
     * @param e The error
     */
    public static void error(Throwable e) {
        log.error(e);

        AssertionFailedError afe = new AssertionFailedError("Exception occurred: " + e.getMessage());
        afe.initCause(e);

        throw afe;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * error method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Record an exception (with some text) and fail the test
     * 
     * @param text The text to be logged
     * @param e The exception
     */
    public static void error(String text, Exception e) {
        log.error(text, e);

        AssertionFailedError afe = new AssertionFailedError(text);
        afe.initCause(e);

        throw afe;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * error method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Record an error (and some text) and fail the test
     * 
     * @param text The text to be logged
     * @param e The error
     */
    public static void error(String text, Error e) {
        log.error(text, e);

        AssertionFailedError afe = new AssertionFailedError(text);
        afe.initCause(e);

        throw afe;

    }

    /* -------------------------------------------------------------------------- */
    /*
     * blankLine method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add a blank line to the log
     */
    public static void blankLine() {
        log.blankLine();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * section method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add a section marker to the log
     * 
     * @param sectionName The name of the section
     */
    public static void section(String sectionName) {
        log.section(sectionName);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * header method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add a header to the log
     * 
     * @param hdrStr The name of the header to be added to the log
     */
    public static void header(String hdrStr) {
        log.header(hdrStr);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * timestamp method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add a timestamp to the log
     */
    public static void timestamp() {
        log.timestamp();
    }

    // *************** PERFORMANCE METHODS ***********************
    /* -------------------------------------------------------------------------- */
    /*
     * performance method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add a performance marker to the log
     * 
     * @param name The name of the performance marker
     */
    public static void performance(String name) {
        log.performance(name);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * performanceStats method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Add performance stats to the log
     */
    public static void performanceStats() {
        log.performanceStats();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * getLog method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Returns the log
     * 
     * @return JETSAMLog The log being used to record the information recorded by this test
     */
    public static JETSAMLog getLog() {
        return log;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * tick method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param text Either start ticking (if text is null), or record a tick
     */
    protected void tick(String text) {

        long val = 0;

        // Convention for start
        if (text == null) {
            start = System.currentTimeMillis();
            text = "TICK started.";
        } else {
            val = System.currentTimeMillis() - start;
        }

        StringBuffer sb = new StringBuffer("                       ");
        sb.insert(0, val);
        sb.insert(10, text);
        comment(sb.toString());

    }

    // ******************** ASSERT METHODS **********************

    /* -------------------------------------------------------------------------- */
    /*
     * fail method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param e The exception that causes the test to fail
     */
    public static void fail(Exception e) {
        error(e);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * assertType method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Check that the two objects are of the same instance type.
     * 
     * @param expected The expected object
     * @param received The actual object (which will be checked to see if it can be assigned
     *            to the class of the expected object
     */
    public static void assertType(Object expected, Object received) {
        assertNotNull(expected);
        assertNotNull(received);
        assertType(expected.getClass(), received);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * assertType method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Check that the object is of the specified type.
     * 
     * @param expectedClass The class
     * @param received The object which will be checked
     */
    public static void assertType(Class expectedClass, Object received) {
        assertNotNull(expectedClass);
        assertNotNull(received);

        // Check whether the exception we got is assignment compatible with the
        // expected one.
        if (!expectedClass.isInstance(received)) {
            fail("Expected exception type "
                 + expectedClass.getName()
                 + ", got "
                 + received.getClass().getName());
        } //if

    }

    /* -------------------------------------------------------------------------- */
    /*
     * assertEquals method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Assert that two byte arrays are the same
     * 
     * @param failureMessage The message to be emitted if they're not
     * @param expected The first byte array (typically the one that the test case is expecting)
     * @param received The second byte array (typically the one that the test case got)
     */
    public static void assertEquals(String failureMessage, byte[] expected, byte[] received) {
        assertEquals(failureMessage, failureMessage, expected, received);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * assertEquals method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Assert that two bytes arrays are the same
     * 
     * @param contentFailMessage The message to be emitted if the arrays differ in content
     * @param lengthFailMessage The message to be emitted if the arrays differ in length
     * @param expected The first byte array (typically the one that the test case is expecting)
     * @param received The second byte array (typically the one that the test case got)
     */
    public static void assertEquals(String contentFailMessage, String lengthFailMessage,
                                    byte[] expected, byte[] received) {
        // Easy case, the two byte array are, in fact, the same byte array
        if (expected == received)
            return;

        // Check for null inputs (we already know they're not both null)
        failIfExpectedArrayIsNull(contentFailMessage, expected, received);
        failIfReceivedArrayIsNull(contentFailMessage, expected, received);

        // Check for zero-length arrays
        if ((expected.length == 0) && (received.length == 0))
            return;
        failIfExpectedArrayIsZeroLength(lengthFailMessage, expected, received);
        failIfReceivedArrayIsZeroLength(lengthFailMessage, expected, received);

        // Find the first point where the arrays differ (one greater than the smallest array if the arrays are the same for the entire length of the smallest array)
        int startOfDifferences = findStartOfDifferencesBetween(expected, received);

        // Decide if we need to complain
        if ((expected.length != received.length) || (startOfDifferences != expected.length)) {
            complainAboutMismatchedByteArrays(contentFailMessage, lengthFailMessage, expected, received, startOfDifferences);
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * complainAboutMismatchedByteArrays method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param contentFailMessage The message to be emitted if the arrays differ in content
     * @param lengthFailMessage The message to be emitted if the arrays differ in length
     * @param expected The first byte array (typically the one that the test case is expecting)
     * @param received The second byte array (typically the one that the test case got)
     * @param startOfDifferences The index of the first difference one greater than the smallest array if the arrays are the same for the entire length of the smallest array)
     */
    private static void complainAboutMismatchedByteArrays(String contentFailMessage, String lengthFailMessage, byte[] expected, byte[] received, int startOfDifferences) {
        // Determine how much of the tail of the arrays are the same
        int commonTailLength = findLengthOfCommonTail(expected, received);

        // Determine the number of missing and/or extra bytes
        int missingCount = expected.length - commonTailLength - startOfDifferences;
        int extraCount = received.length - commonTailLength - startOfDifferences;

        // OK, now build the failureText
        StringBuilder failureText;

        if ((expected.length == received.length)) {
            failureText = new StringBuilder(contentFailMessage);
            if (missingCount == 1)
                failureText.append(String.format(" : Expected and received byte array differ at offset %d (into the expected byte array). ", startOfDifferences));
            else
                failureText.append(String.format(" : Expected and received byte array differ starting at offset %d (into the expected byte array) for %d bytes. ",
                                                 startOfDifferences, missingCount));
            addChangedBlockToFailureText(expected, received, startOfDifferences, missingCount, extraCount, failureText);
        } else {
            failureText = new StringBuilder(lengthFailMessage);
            failureText.append(String.format(" : Expected byte array is length %d, received byte array is length %d. ", expected.length, received.length));

            if (missingCount == 0) {
                if (extraCount == 1)
                    failureText.append(String.format("A byte appears to have been inserted at offset %d of the expected byte array. ", startOfDifferences));
                else
                    failureText.append(String.format("%d bytes appear to have been inserted at offset %d of the expected byte array. ", extraCount, startOfDifferences));
                addDescriptionOfTheExtraBytesToFailureText(received, startOfDifferences, extraCount, failureText);
                addDescriptionOfAPointInTheExpectedArrayToFailureText(expected, startOfDifferences, failureText);
            } else if (extraCount == 0) {
                if (missingCount == 1)
                    failureText.append(String.format("A byte appears to be missing at offset %d of the expected byte array. ", startOfDifferences));
                else
                    failureText.append(String.format("%d bytes appear to be missing at offset %d of the expected byte array. ", missingCount, startOfDifferences));
                addChangedBlockToFailureText(expected, received, startOfDifferences, missingCount, extraCount, failureText);
            } else {
                addChangedBlockToFailureText(expected, received, startOfDifferences, missingCount, extraCount, failureText);
            }
        }

        failureText.append(".");
        fail(failureText.toString());
    }

    /* -------------------------------------------------------------------------- */
    /*
     * addDescriptionOfTheExtraBytesToFailureText method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param bytes The byte array which has extra bytes
     * @param start The index of the first extra byte
     * @param length The length of the extra bytes
     * @param text The StringBuilder to which to add the description
     */
    private static void addDescriptionOfTheExtraBytesToFailureText(byte[] bytes, int start, int length, StringBuilder text) {
        byte[] inserted = new byte[length];
        System.arraycopy(bytes, start, inserted, 0, length);
        text.append(namedByteArrayAsString("inserted", inserted, 0, length));
        text.append(". ");
    }

    /* -------------------------------------------------------------------------- */
    /*
     * addDescriptionOfAPointInTheExpectedArray method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param expected The expected byte array
     * @param start The index of the byte to be added (additional bytes around this one will also be added)
     * @param text The StringBuilder to which to add the description
     */
    private static void addDescriptionOfAPointInTheExpectedArrayToFailureText(byte[] expected, int start, StringBuilder text) {
        int lengthOfDifferencesInExpected;
        // Widen up the range a bit
        start -= ADDITIONAL_BYTES;
        if (start < 0)
            start = 0;
        lengthOfDifferencesInExpected = ADDITIONAL_BYTES * 2 + 1;
        if (start + lengthOfDifferencesInExpected > expected.length)
            lengthOfDifferencesInExpected = expected.length - start;
        text.append(namedByteArrayAsString("expected", expected, start, lengthOfDifferencesInExpected));
    }

    /* -------------------------------------------------------------------------- */
    /*
     * addDescriptionOfChangedBlockToFailureText method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param expected The expected byte array
     * @param received The received byte array
     * @param start The index of the first different byte
     * @param lengthOfDifferencesInExpected How many bytes are different in the expected byte array
     * @param lengthOfDifferencesInReceived How many bytes are different in the received byte array
     * @param text The StringBuilder to which to add the description
     */
    private static void addChangedBlockToFailureText(byte[] expected, byte[] received, int start, int lengthOfDifferencesInExpected, int lengthOfDifferencesInReceived,
                                                     StringBuilder text) {
        // Widen up the range a bit
        start -= ADDITIONAL_BYTES;
        lengthOfDifferencesInExpected += 2 * ADDITIONAL_BYTES;
        lengthOfDifferencesInReceived += 2 * ADDITIONAL_BYTES;
        if (start < 0)
            start = 0;
        if (start + lengthOfDifferencesInExpected > expected.length)
            lengthOfDifferencesInExpected = expected.length - start;
        if (start + lengthOfDifferencesInReceived > received.length)
            lengthOfDifferencesInReceived = received.length - start;

        text.append(namedByteArrayAsString("expected", expected, start, lengthOfDifferencesInExpected));
        text.append(", ");
        text.append(namedByteArrayAsString("received", received, start, lengthOfDifferencesInReceived));
    }

    /* -------------------------------------------------------------------------- */
    /*
     * findStartOfDifferencesBetween method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param first The first (non-null, length>0) byte array to be compared
     * @param second The second (non-null, length>0) byte array to be compared
     * @return The index of the first difference between the byte arrays
     *         (or the length of the shortest array if the longer one contains
     *         the shorter one as a prefix)
     */
    private static int findStartOfDifferencesBetween(byte[] first, byte[] second) {
        int start = 0;
        while ((start < first.length)
               && (start < second.length)
               && (first[start] == second[start])) {
            start++;
        }
        return start;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * findLengthOfCommonTail method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param first The first (non-null, length>0) byte array to be compared
     * @param second The second (non-null, length>0) byte array to be compared
     * @return How many bytes are the same if you look at the end of the array
     */
    private static int findLengthOfCommonTail(byte[] first, byte[] second) {
        int ep = first.length - 1; // index of a character in expected
        int rp = second.length - 1; // index of a character in received

        while ((ep >= 0)
               && (rp >= 0)
               && (first[ep] == second[rp])) {
            ep--;
            rp--;
        }
        return (first.length - ep - 1);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * failIfReceivedArrayIsZeroLength method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param message The message to be used if the test fails
     * @param expected The expected byte array (needed for the message if the test fails)
     * @param received The received byte array (which will cause a fail if it's of zero length)
     */
    private static void failIfReceivedArrayIsZeroLength(String message, byte[] expected, byte[] received) {
        if (received.length == 0) {
            fail(message + " : " + namedByteArrayAsString("expected", expected, 0, expected.length) + ", but received byte array is of zero length.");
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * failExpectedArrayIsZeroLength method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param message The message to be used if the test fails
     * @param expected The expected byte array (which will cause a fail if it's of zero length)
     * @param received The received byte array (needed for the message if the test fails)
     */
    private static void failIfExpectedArrayIsZeroLength(String message, byte[] expected, byte[] received) {
        if (expected.length == 0) {
            fail(message + " : Expected byte array is of zero length, but received byte array has " + received.length + " bytes. "
                 + namedByteArrayAsString("received", received, 0, received.length));
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * failIfReceivedArrayIsNull method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param message The message to be used if the test fails
     * @param expected The expected byte array (needed for the message if the test fails)
     * @param received The received byte array (which will cause a fail if it's null)
     */
    private static void failIfReceivedArrayIsNull(String message, byte[] expected, byte[] received) {
        if (received == null) {
            fail(message + " : " + namedByteArrayAsString("expected", expected, 0, expected.length) + ", but received byte array is null.");
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * failIfExpectedArrayIsNull method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param message The message to be used if the test fails
     * @param expected The expected byte array (which will cause a fail if it's null)
     * @param received The received byte array (needed for the message if the test fails)
     */
    private static void failIfExpectedArrayIsNull(String message, byte[] expected, byte[] received) {
        if (expected == null) {
            fail(message + " : Expected byte array is null. " + namedByteArrayAsString("received", received, 0, received.length) + ".");
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * assertEquals method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param expected The expected byte array
     * @param received The actual byte array
     */
    public static void assertEquals(byte[] expected, byte[] received) {
        assertEquals("Difference in content of byte arrays",
                     "Byte arrays are not the same length",
                     expected, received);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * namedByteArrayAsString method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Convert part of a named byte array to a string suitable for use in a fail message.
     * <p><b>Note:</b> the caller is expected to pass valid values, i.e.
     * <ul>
     * <li> bytes != null
     * <li> offset >= 0
     * <li> length >= 0
     * <li> offset+length < bytes.length
     * </ul>
     * <p><b>Note:</b> If length > MAX_BYTE_LENGTH_TO_PRINT then only MAX_BYTE_LENGTH_TO_PRINT will be actually included
     * 
     * @param name The name of the byte array
     * @param bytes The byte array
     * @param offset The offset into the array
     * @param length The number of bytes to be included
     * @return A string suitable for use in failure messages
     */
    private static String namedByteArrayAsString(String name, byte[] bytes, int offset, int length) {
        int print_length = length;
        if (print_length > MAX_BYTE_LENGTH_TO_PRINT)
            print_length = MAX_BYTE_LENGTH_TO_PRINT;

        StringBuilder s = new StringBuilder(String.format("An extract of the %s byte array at offset %d of length %d is ", name, offset, print_length));
        addByteArrayToStringBuilder(bytes, offset, print_length, s);
        return s.toString();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * byteArrayAsString method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Convert part of a byte array to a string.
     * <p><b>Note:</b> the caller is expected to pass valid values, i.e.
     * <ul>
     * <li> bytes != null
     * <li> offset >= 0
     * <li> length >= 0
     * <li> offset+length < bytes.length
     * </ul>
     * 
     * @param bytes The byte array
     * @param offset The offset into the array
     * @param length The number of bytes to be included
     * @param s The StringBuilder to which to append the byte array
     * @return The updated StringBuilder
     */
    private static StringBuilder addByteArrayToStringBuilder(byte[] bytes, int offset, int length, StringBuilder s) {
        s.append("[");
        for (int i = offset; i < offset + length - 1; i++)
            s.append(String.format("%#04x,", bytes[i]));
        s.append(String.format("%#04x", bytes[offset + length - 1]));
        s.append("]");
        return s;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * assertNull method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param o The object reference to be tested
     */
    public static void assertNull(Object o) {
        if (o != null)
            TestCase.assertNull("Object not null, actually:" + o, o);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * assertNull method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param failureMessage The message to be used if the object reference is non-null
     * @param o The object reference to be tested
     */
    public static void assertNull(String failureMessage, Object o) {
        if (o != null)
            TestCase.assertNull(failureMessage + ". Actually:" + o, o);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * assertMessage method
     * /* --------------------------------------------------------------------------
     */
    /**
     * This method checks that the message contained in the first parameter object
     * contains the text as specified in the second parameter, and raises an error
     * if this is not the case.
     * 
     * Note that for readability of the testcase you can specify the full catalog string
     * (e.g., EXCEPTION_RECEIVED_SIAP0022) and the assertMessage method will examine the
     * string to determine whether it conforms to a *_CCCCnnnn pattern. If this is true,
     * then it will only check for the SIAP0022 part in order to carry on functioning
     * whether the message catalog is present or not.
     * 
     * If either of the parameters are null then an error will be raised.
     * 
     * Additionally (as of feature 174780), if the Throwable is a JMSException,
     * check that it contains an errorCode and that it matches the code in the srchString.
     * 
     * @param t The Throwable whose message is to be tested
     * @param srchString The string expected to be found in the Throwable
     */
    public static void assertMessage(Throwable t, String srchString) {
        // Parameter checking
        if (t == null)
            error("A null value was specified for the Throwable parameter.");
        if (srchString == null)
            error("A null value was specified for the search string");

        // Retrieve the message
        String theMsg = t.getLocalizedMessage();

        if (theMsg == null) {
            log.error("No message was specified in this Throwable");
            fail("No message was specified in this Throwable");
        } else {
            String lastPart = null;

            // Examine the format of the passed in string. If it is of the forms
            // that follow, then we want to trim it down to the final token.
            // NO_MESSAGE_AVAILABLE_CWSIA0141
            // EXCEPTION_RECEIVED_SIAP0022
            int barIndex = srchString.lastIndexOf("_");

            // If it contains an underscore and no spaces (i.e., is just a message key)
            if ((barIndex != -1) && (srchString.indexOf(" ") == -1)) {
                lastPart = srchString.substring(barIndex + 1);

                // Check that the last four characters are numbers, and the rest
                // are uppercase letters.
                if (lastPart != null) {
                    boolean isCorrect = true;

                    // Check that the first four characters are uppercase chars, and the last
                    // four are numeric digits.
                    for (int i = 0; i < lastPart.length(); i++) {
                        char c = lastPart.charAt(i);

                        if (i < (lastPart.length() - 4)) {
                            // For the first n-4 characters we want to check that the char
                            // is an uppercase letter.
                            if (!(Character.isJavaIdentifierStart(c) && Character.isUpperCase(c))) {
                                isCorrect = false;
                                break;
                            }
                        } else {
                            // For the last four characters they must be numbers.
                            if (!Character.isDigit(c)) {
                                isCorrect = false;
                                break;
                            }
                        }//if
                    }//for

                    if (isCorrect) {
                        // We have successfully found the code we are looking for.
                        srchString = lastPart;
                    }
                }//if lastPart length is 8
            }//if underscore

            if (theMsg.indexOf(srchString) == -1) {
                String text = "The expected string '" + srchString + "' cannot be found in the message '" + theMsg + "'";
                log.error(text);
                fail(text);
            } else {
                // Passed the check
                comment("The correct message '" + srchString + "' was received.");
            }//if srchString

        }//if null

        // f174780, add a check for errorCode

        // is this a JMSException?
        if (t instanceof JMSException) {
            JMSException je = (JMSException) t;
            // grab the errorCode
            String ec = je.getErrorCode();
            if (ec == null) {
                log.comment("errorCode not set in this exception: " + je);
            } else {
                // check that it has something to do with the specified string
                if (!ec.equals(srchString)) {
                    String text = "errorCode (" + ec + ") inconsistent with expected text (" + srchString + ")";
                    log.error(text);
                    fail(text);
                }
            }
        }
    }//assertMessage

    /* -------------------------------------------------------------------------- */
    /*
     * assertEquals method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param expected The expected List
     * @param received The actual List
     */
    public static final void assertEquals(List expected, List received) {
        assertEquals("", expected, received);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * assertEquals method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param failureText The text to be generated if the Lists are different (in contents)
     * @param expected The expected List
     * @param received The actual List
     */
    public static final void assertEquals(String failureText, List expected, List received) {
        if ((expected == null) && (received == null))
            return;

        if (expected == null) {
            throw new AssertionFailedError(
                            failureText + " expected null, but was " + received);
        }

        if (received == null) {
            throw new AssertionFailedError(
                            failureText + " expected:" + expected + " but was null");
        }

        if (expected.equals(received))
            return;

        // OK compare lists and decide on differences - initially all the lists are different
        int startOfDifferences = 0;
        // Remove the common start of sequence
        boolean lastItemSame = true;

        for (int i = 0; i < expected.size() && i < received.size() && lastItemSame; i++) {
            if ((expected.get(i) == null) && (received.get(i) == null)) {
                lastItemSame = true;
            } else if ((expected.get(i) == null) || (received.get(i) == null)) {
                lastItemSame = false;
            } else {
                lastItemSame = expected.get(i).equals(received.get(i));
            }

            if (lastItemSame)
                startOfDifferences++;

        }//for
         // Now remove the common bit at the end
        int endOfDifferencesInExpected = expected.size();
        int endOfDifferencesInReceived = received.size();
        lastItemSame = true;

        while ((endOfDifferencesInExpected > startOfDifferences)
               && (endOfDifferencesInReceived > startOfDifferences)
               && lastItemSame) {
            int ap = endOfDifferencesInExpected - 1;
            int bp = endOfDifferencesInReceived - 1;

            if ((expected.get(ap) == null) && (received.get(bp) == null)) {
                lastItemSame = true;
            } else if ((expected.get(ap) == null) || (received.get(bp) == null)) {
                lastItemSame = false;
            } else {
                lastItemSame = expected.get(ap).equals(received.get(bp));
            }

            if (lastItemSame) {
                endOfDifferencesInExpected--;
                endOfDifferencesInReceived--;
            }

        }//while

        // OK, now build the failureText
        if (endOfDifferencesInExpected == startOfDifferences) {
            failureText =
                            failureText
                                            + " because "
                                            + received.subList(startOfDifferences, endOfDifferencesInReceived)
                                            + " inserted after element "
                                            + startOfDifferences;

        } else if (endOfDifferencesInReceived == startOfDifferences) {
            failureText =
                            failureText
                                            + " because "
                                            + expected.subList(startOfDifferences, endOfDifferencesInExpected)
                                            + " missing after element "
                                            + startOfDifferences;

        } else {
            if ((endOfDifferencesInExpected == startOfDifferences + 1)
                && (endOfDifferencesInReceived == startOfDifferences + 1)) {

                failureText =
                                failureText
                                                + " because element "
                                                + startOfDifferences
                                                + " is different (expected:"
                                                + expected.get(startOfDifferences)
                                                + " but was:" + received.get(startOfDifferences) + ") ";

            } else if (endOfDifferencesInExpected == startOfDifferences + 1) {

                failureText =
                                failureText
                                                + " because element "
                                                + startOfDifferences
                                                + " ("
                                                + expected.get(startOfDifferences)
                                                + ") has been replaced by "
                                                + received.subList(startOfDifferences, endOfDifferencesInReceived);
            } else {
                failureText =
                                failureText
                                                + " because elements between "
                                                + startOfDifferences
                                                + " and "
                                                + (endOfDifferencesInExpected - 1)
                                                + " are different (expected:"
                                                + expected.subList(startOfDifferences, endOfDifferencesInExpected)
                                                + " but was:"
                                                + received.subList(startOfDifferences, endOfDifferencesInReceived)
                                                + ")";
            }//if
        }//if

        throw new AssertionFailedError(failureText + " expected:" + expected + " but was:" + received);

    }//assertEquals(String failureText, List a, List b)

    // ************** OTHER METHODS *****************************

    /* -------------------------------------------------------------------------- */
    /*
     * constructFileName method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Calculate the name of the file that will be used to store the log for
     * this class.
     * 
     * The following precedence is used for this purpose;
     * 
     * 1. Anything specified in LOGGING_DIR (see file header for details)
     * 2. /logs underneath the current directory if it already exists. In this
     * case, this method is responsible for creating the rest of the required
     * tree underneath this directory.
     * 3. A directory /logs underneath the test source if it can be located
     * in one of the following location trees;
     * ./com
     * ./test/com
     * ./unittest/com
     * ./fv/com
     * ./src/com
     * 4. If none of the above succeeded, the /logs directory described in 2
     * is created and used to locate the log files.
     * 
     * @return the name of the log file
     */
    private String constructFileName() {

        String startPath = null;

        // Option 1: Check whether a separate directory has been set for logging.
        String ld = null;
        boolean separateLogs = false;
        if ((ld = LoggingTestCase.getLoggingDir()) != null) {
            startPath = ld;
            separateLogs = true;
        } else {
            startPath = ".";
        }

        boolean useMainLogDir = false;

        // If we are not using a separate path for logs, then find somewhere suitable
        // under the source tree where-ever it is.
        if (!separateLogs) {

            String fileDirectory = this.getClass().getPackage().getName().replace('.', File.separatorChar);

            // Option 2: Check for /logs underneath the current directory.
            String mainLogDir = startPath + File.separator + "logs";
            if ((new File(mainLogDir)).exists()) {
                // There is a directory /logs underneath the current directory.
                startPath = mainLogDir;
                useMainLogDir = true;

                // Now create the remainder of the structure under the tree.
                File theDir = new File(mainLogDir + File.separator + fileDirectory);
                theDir.mkdirs();

            } else {

                // We need to be able to cope with the com/ tree starting off the root
                // of the project, or under the /src/com style tree.
                // Also allow the /test/com structure to take precedence over /src/XXX.
                Vector<String> locations = new Vector<String>();
                locations.add("unittest");
                locations.add("test");
                locations.add("fv");
                locations.add("src");

                boolean foundLocation = false;
                Enumeration locns = locations.elements();

                // Look at each of the locations in turn to see if they contain
                // the right structure.
                while (locns.hasMoreElements()) {
                    String srcPath = (String) locns.nextElement();
                    String nextTarget = startPath + File.separator + srcPath + File.separator + fileDirectory;
                    if ((new File(nextTarget)).exists()) {

                        startPath = startPath + File.separator + srcPath;
                        foundLocation = true;
                        break;

                    }//if it is the correct location

                }//while more attempts

                if (!foundLocation) {
                    // Option 4: Create the /logs directory
                    startPath = startPath + File.separator + "logs";
                    File defaultDir = new File(startPath);
                    defaultDir.mkdirs();

                }

            }//if mainLogDir found

        }//if !separateLogs

        if (this.getClass().getPackage() != null) {
            startPath += File.separator + this.getClass().getPackage().getName().replace('.', File.separatorChar);
        }
        if (!separateLogs && !useMainLogDir)
            startPath += File.separator + "logs";

        // Work out the short name of the file.
        String className = this.getClass().getName();
        className = className.substring(className.lastIndexOf(".") + 1);

        String fileName = "";
        //if (!separateLogs) fileName += ".";
        fileName += startPath + File.separator + className + ".log";

        // Make sure that the top level directory exists.
        if ((!separateLogs) && (!startPath.startsWith(".")))
            startPath = "." + startPath;
        File theDir = new File(startPath);
        theDir.mkdirs();

        return fileName;

    }

    /* -------------------------------------------------------------------------- */
    /*
     * ensureLogEnabled method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Make sure that we have a log file
     */
    public void ensureLogEnabled() {

        if (log != null) {

            // If we have already set up the log file, then we must check that it is
            // actually designed for this testcase subclass.

            // d250974 - also reset the file if it has already been closed. This allows
            // the log to be overwritten if the same test is run several times inside
            // the same JVM (but only if it is explicitly closed).
            if ((!log.getFileName().equals(constructFileName())) || (!log.isOpen())) {
                // Close this file ready to open the new one.
                log.close();
                log = null;
            }

        } // If it has not been set.

        if (log == null) {
            // Open the log file with the given name
            String className = constructFileName();
            log = JMSLogImpl.createJMSLog(className, TO_SCREEN, TO_FILE);

            // Open the file over-writing what was there before.
            log.open(true);

            printClassHeader(log);

            // Now spin off the thread to take heap dumps if we time out.
            registerTimeoutMonitor();
        }

    }

    /* -------------------------------------------------------------------------- */
    /*
     * registerTimeoutMonitor method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Calling this method causes a new thread to be spun off that will wait until
     * the junit timeout is about to expire, and trigger JVM heap dumps so that we
     * can tell what is causing the problem.
     * 
     * We will leave JUnit to handle the termination of the process.
     */
    private static void registerTimeoutMonitor() {

        // Read the timeout value (in milliseconds) from the one of the system properties,
        // looking at the 'new' property (unittest.timeout) first.
        String realProp = null;
        String newProp = System.getProperty("unittest.timeout");

        // Don't try to parse the property value if it is just the variable name
        // (i.e. the property was not set).
        if ((newProp != null) && (!"".equals(newProp)) && (!newProp.startsWith("${"))) {
            // We have found the property.
            realProp = newProp;
        } else {
            String prop = System.getProperty("junit.timeout");
            if ((prop != null) && (!"".equals(prop)) && (!prop.startsWith("${"))) {
                realProp = prop;
            }
        }//if

        long milliseconds = -1;
        try {

            // Parse whichever property we have decided to use.
            if (realProp != null)
                milliseconds = Integer.valueOf(realProp).longValue();

        } catch (NumberFormatException nfe) {
            // No FFDC Code Needed
            nfe.printStackTrace();
        }

        if ((milliseconds != -1) && (timerTasksScheduled == false)) {
            // We need to allow time for the dumps to be produced before the JVM is terminated,
            // so set the timer for a little sooner than JUnit expects to wait.
            long marginTime; // How much time should we allow to take the memory dumps and as a safety margin
            boolean timeForMultipleDumps;

            if (milliseconds > MAXIMUM_TIMEOUT_MARGIN * MAXIMUM_SAFETY_PROPORTION) {
                marginTime = MAXIMUM_TIMEOUT_MARGIN;
                timeForMultipleDumps = true;
            } else {
                marginTime = milliseconds / MAXIMUM_SAFETY_PROPORTION;
                if (marginTime < MINIMUM_TIMEOUT_MARGIN)
                    marginTime = MINIMUM_TIMEOUT_MARGIN;
                if (marginTime > milliseconds)
                    marginTime = milliseconds - 1; // Though, in practice, with this little time JUnit will kill us first :-) 
                timeForMultipleDumps = false;
            }

            long waitTime = milliseconds - marginTime;

            // OK, We need to schedule a timer task that will cause a core dump after the timeout expires
            Timer timer = new Timer(TIMEOUT_THREAD_NAME);
            timer.schedule(new CoreDumper("The tests have not completed after %d milliseconds. Taking an initial java and heap dump.", waitTime, !timeForMultipleDumps), waitTime);

            // And if there's time two additional timer tasks for additional core dumps
            if (timeForMultipleDumps) {
                timer.schedule(
                               new CoreDumper("The tests have still not completed after %d milliseconds. Taking an second java and heap dump.", waitTime + TIME_BETWEEN_DUMPS, false),
                               waitTime + TIME_BETWEEN_DUMPS);
                timer.schedule(
                               new CoreDumper("The tests have still not completed after %d milliseconds. Taking a third and final java and heap dump.", waitTime + 2
                                                                                                                                                        * TIME_BETWEEN_DUMPS, true),
                               waitTime + 2 * TIME_BETWEEN_DUMPS);
            }

            timerTasksScheduled = true;
        }
    }//registerTimeoutMonitor

    /* -------------------------------------------------------------------------- */
    /*
     * getLoggingDir method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @return the loggingDir.
     */
    public static String getLoggingDir() {

        // The logging directory has not been set.
        if (LoggingTestCase.loggingDir == null) {
            // Try to receive it from the -D flag.
            String temp = System.getProperty(LoggingTestCase.LOGGING_DIR_STRING);

            if (temp != null)
                LoggingTestCase.loggingDir = temp;
        }

        return LoggingTestCase.loggingDir;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * setLoggingDir method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Sets the directory which will be used as the root for the log structure.
     * 
     * @param newLoggingDir The loggingDir to set
     */
    public static void setLoggingDir(String newLoggingDir) {
        LoggingTestCase.loggingDir = newLoggingDir;
    }

    /**
     * Register (or deregister) with the unittest RAS infrastructure so that
     * warning/error messages are output to the test log.
     */
    /* -------------------------------------------------------------------------- */
    /*
     * modifyRASRegistration method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Register (or deregister) with the unittest RAS infrastructure so that
     * warning/error messages are output to the test log.
     * 
     * @param isRegisterRequest true if this is a register request
     */
    private static void modifyRASRegistration(boolean isRegisterRequest) {
        try {
            // See if the class is on the classpath.
            Class loggerCls = Class.forName("com.ibm.ws.sib.unittest.ras.Logger");

            // Find the method that we use for setter the logger.
            Method setMethod = loggerCls.getMethod("setTestCaseLogger", new Class[] { Writer.class });

            Object[] args = new Object[1];
            if (isRegisterRequest) {
                // Register
                // We need to expose the LoggingTestCase as if it were a 'Writer' class.
                if (writer == null)
                    writer = new LTCWriter();
                args[0] = writer;
            } else {
                // Deregister
                args[1] = null;
            }

            // Invoke the (static) method. We don't expect a return value
            setMethod.invoke(null, args);

        } catch (ClassNotFoundException cnfe) {
            // This is the normal path for components who do not use the unittest RAS functions.
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // **************** LOG HEADER CUSTOMISATION METHODS ************************

    /**
     * Print the basic file header.
     * 
     * This method defines the header that will be printed at the top of each file.
     * In effect this is printed once per test class. It can be overridden by
     * subclasses to provide different headers if required.
     * 
     * See the comment at the top of this file for further information.
     * 
     * @param theLog the log file to be used
     */
    protected void printClassHeader(JETSAMLog theLog) {
        theLog.blankLine();
        theLog.header("JetStream Unit Test Framework");

        theLog.comment(
                        "-------------------------------------------------------");
        theLog.comment("Testclass: " + this.getClass().getName());

        printExtraClassInfo(theLog);

        theLog.comment(
                        "-------------------------------------------------------");
        theLog.blankLine();
        theLog.blankLine();
        theLog.blankLine();

    }//printClassHeader

    /**
     * This method is called by the printClassHeader method to allow subclasses to
     * supply some extra information within the pattern of the standard class header.
     * 
     * The intent is that this information be something like a build level.
     */
    /* -------------------------------------------------------------------------- */
    /*
     * printExtraClassInfo method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param theLog The log file to be used
     */
    protected void printExtraClassInfo(JETSAMLog theLog) {
        // By default we do nothing here. Subclasses should override to add
        // in their required functionality.

    }//printExtraClassInfo

    /* -------------------------------------------------------------------------- */
    /*
     * printTestHeader method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Prints the basic test header.
     * 
     * This method allows subclasses to override the header printed at the top
     * of each test method.
     * 
     * @param theLog the log file to be used
     */
    protected void printTestHeader(JETSAMLog theLog) {

        theLog.header("Test-method: " + getName());
        // Store the start time for this test.
        testStartTime = System.currentTimeMillis();
        theLog.comment("-----------------------------------------------");
        theLog.blankLine();

    }//printTestHeader

    /* -------------------------------------------------------------------------- */
    /*
     * printTestFooter method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Prints the basic test footer.
     * 
     * This method allows subclasses to override the footer printed at the bottom
     * of each test method.
     * 
     * @param theLog The log in which to print the footer
     */
    protected void printTestFooter(JETSAMLog theLog) {

        theLog.comment("-----------------------------------------------");
        long testEndTime = System.currentTimeMillis();
        theLog.comment("   Time taken (millis): " + (testEndTime - testStartTime));
        // Reset the test start time counter
        testStartTime = 0;
        theLog.comment("End of test-method: " + getName());
        theLog.blankLine();
        theLog.blankLine();
        theLog.blankLine();

    }//printTestFooter

    /* -------------------------------------------------------------------------- */
    /*
     * enableTracing method
     * /* --------------------------------------------------------------------------
     */
    /**
     * 
     * Intercept the parsing of the trace settings file and add support for a
     * traceType entry which determines whether the trace is advanced, basic or
     * loganalyzer format.
     * 
     * To use, supply -DadvancedTrace=true in the VM args section of the Run
     * panel. You also need to add ras.jar from SERV1 to the top of the user
     * entries section of the classpath in the Run panel, otherwise the runtime
     * finds other versions of Tr which don't have the getComponentManager method.
     * 
     * The default settings trace everything from com.ibm.ws.sib to defaultTrace.txt
     * in the current working directory, overwriting any previous contents.
     * To modify the behaviour, use -DtraceSettingsFile to name a properties file
     * which specifies what and where to trace, e.g.
     * com.ibm.ws.sib.*=all=enabled:com.ibm.ws.sib.msgstore.*=all=disabled
     * traceFileName=myTrace.txt
     * traceFormat=advanced
     * truncate=true
     * 
     * In the past this file had to be put into a jarfile which was then added to
     * the classpath of the test, but currently that doesn't seem to work (!!)
     * and instead you need to specify the absolute path of the file on the disk,
     * e.g.
     * -DtraceSettingsFile=c:\jbk\jetstream\traceEnabler\contents\trcSettings.txt
     * 
     */
    /*
     * private static void enableTracing()
     * {
     * // Use these defaults if only advancedTrace is set and no
     * // traceSettingsFile has been specified. If the traceSettingsFile
     * // has been specified then a different (the original) set of
     * // defaults apply, which are defined a bit further down.
     * String traceSpec = "com.ibm.ws.sib.*=all=enabled";
     * String traceFileName = "defaultTrace.txt";
     * String traceOutputDest = ManagerAdmin.file;
     * String traceType = ManagerAdmin.ADVANCED;
     * boolean truncateFile = true;
     * 
     * String fileName = System.getProperty("traceSettingsFile");
     * if (fileName != null)
     * {
     * FileInputStream fis = null;
     * 
     * try
     * {
     * Properties props = new Properties();
     * 
     * // This is the set of defaults that was used when a file was
     * // specified in the previous version
     * traceSpec = "*=all=disabled";
     * traceFileName = "stdout";
     * traceOutputDest = ManagerAdmin.stdout;
     * traceType = ManagerAdmin.BASIC;
     * truncateFile = false;
     * 
     * fis = new FileInputStream(fileName);
     * props.load(fis);
     * 
     * if (props.size() != 0)
     * {
     * Enumeration en1 = props.keys();
     * while(en1.hasMoreElements())
     * {
     * String key = (String) en1.nextElement();
     * 
     * if (key.equals("traceFileName"))
     * {
     * traceFileName = props.getProperty(key);
     * if (!traceFileName.equals("stdout"))
     * {
     * traceOutputDest = ManagerAdmin.file;
     * }
     * }
     * else if(key.equals("traceFormat"))
     * {
     * traceType = props.getProperty(key).toLowerCase();
     * }
     * else if(key.equals("truncate"))
     * {
     * truncateFile = Boolean.valueOf(props.getProperty(key)).booleanValue();
     * }
     * else
     * {
     * traceSpec = key + "=" + props.getProperty(key);
     * traceSpec = traceSpec.trim();
     * }
     * } // while elements in enumeration
     * } // props.size > 0
     * }
     * catch (IOException io)
     * {
     * System.out.println("**** Unable to load trace settings from file: " + fileName + " due to " + io.getMessage() + " ****");
     * }
     * finally
     * {
     * if (fis != null)
     * {
     * try
     * {
     * fis.close();
     * }
     * catch(IOException io)
     * {
     * // This exception would be quite surprising and one we're going to ignore
     * }
     * }
     * }
     * } // fileName != null
     * 
     * System.out.print("Tracing: " + traceSpec + " to " + traceFileName + " in " + traceType + " mode. ");
     * if (truncateFile)
     * {
     * System.out.println("File will be truncated.");
     * }
     * else
     * {
     * System.out.println("File will be appended to.");
     * }
     * 
     * try
     * {
     * // Ensure the trace code doesn't try and process the file twice
     * Properties systemProps = System.getProperties();
     * systemProps.remove("traceSettingsFile");
     * System.setProperties(systemProps);
     * 
     * // Lets check the trace string
     * ManagerAdmin.checkTraceString(traceSpec);
     * 
     * // Start the trace
     * ManagerAdmin.configureClientTrace(traceSpec,
     * traceOutputDest,
     * traceFileName,
     * truncateFile,
     * traceType,
     * false);
     * }
     * catch (MalformedTraceStringException e)
     * {
     * System.out.println("**** The trace specification '" + traceSpec + "' was rejected ****");
     * System.out.println("Tracing will be disabled.");
     * }
     * 
     * }
     */

    /**
     * This class provides a 'Writer' style interface so that we can
     * pass a string into the LTC.
     * 
     * Note that we don't care about all the other methods - this isn't a _real_
     * Writer - just a convenient interface.
     * 
     */
    private static class LTCWriter extends Writer {

        /**
         * @see java.io.Writer#append(char)
         */
        @Override
        public Writer append(char c) throws IOException {
            throw new IOException("This method is not supported for the LoggingTestCase.LTCWriter class");
        }

        /**
         * @see java.io.Writer#append(java.lang.CharSequence, int, int)
         */
        @Override
        public Writer append(CharSequence csq, int start, int end) throws IOException {
            throw new IOException("This method is not supported for the LoggingTestCase.LTCWriter class");
        }

        /**
         * @see java.io.Writer#append(java.lang.CharSequence)
         */
        @Override
        public Writer append(CharSequence csq) throws IOException {
            throw new IOException("This method is not supported for the LoggingTestCase.LTCWriter class");
        }

        /**
         * @see java.io.Writer#close()
         */
        @Override
        public void close() throws IOException {
            throw new IOException("This method is not supported for the LoggingTestCase.LTCWriter class");
        }

        /**
         * @see java.io.Writer#flush()
         */
        @Override
        public void flush() throws IOException {
            throw new IOException("This method is not supported for the LoggingTestCase.LTCWriter class");
        }

        /**
         * @see java.io.Writer#write(char[], int, int)
         */
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            throw new IOException("This method is not supported for the LoggingTestCase.LTCWriter class");

        }

        /**
         * @see java.io.Writer#write(char[])
         */
        @Override
        public void write(char[] cbuf) throws IOException {
            throw new IOException("This method is not supported for the LoggingTestCase.LTCWriter class");
        }

        /**
         * @see java.io.Writer#write(int)
         */
        @Override
        public void write(int c) throws IOException {
            throw new IOException("This method is not supported for the LoggingTestCase.LTCWriter class");
        }

        /**
         * @see java.io.Writer#write(java.lang.String, int, int)
         */
        @Override
        public void write(String str, int off, int len) throws IOException {
            throw new IOException("This method is not supported for the LoggingTestCase.LTCWriter class");
        }

        /**
         * @see java.io.Writer#write(java.lang.String)
         */
        @Override
        public void write(String str) throws IOException {
            comment(str);
        }

    }

    /* ************************************************************************** */
    /**
     * A CoreDumper is a TimerTask that causes a JavaCore and a HeapDump to be taken
     * 
     */
    /* ************************************************************************** */
    public static class CoreDumper extends TimerTask {
        private final String _comment;
        private final boolean _closeTheLog;

        /* -------------------------------------------------------------------------- */
        /*
         * CoreDumper constructor
         * /* --------------------------------------------------------------------------
         */
        /**
         * Construct a new CoreDumper.
         * 
         * @param string A format string that describe why we're dumping
         * @param waitTime An insert in the format string
         * @param closeTheLog Whether or not to forcibly close the log
         */
        public CoreDumper(String string, long waitTime, boolean closeTheLog) {
            _comment = String.format(string, waitTime);
            _closeTheLog = closeTheLog;
        }

        /* -------------------------------------------------------------------------- */
        /*
         * run method
         * /* --------------------------------------------------------------------------
         */
        /**
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            if ((log != null) && log.isOpen()) {
                log.comment("-------------------------------------------------------------------------------------");
                log.comment(_comment);
                log.timestamp();
                log.comment("-------------------------------------------------------------------------------------");
            }

            if ((log != null) && log.isOpen() && _closeTheLog) {
                log.close();
            }
        }

    }

}

/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * <p>{@link LogHandler} type used for FAT logs.</p>
 *
 * <p>When a member variable is annotated {@link org.junit.Rule}, JUnit
 * invokes {@link #before()} before any method annotated {@link org.junit.Before},
 * and {@link #after()} after any method annotated {@link org.junit.After}.</p>
 *
 * <p>Similarly, when a static variable is annotated {@link org.junit.ClassRule},
 * JUnit invokes {@link #before()} before any method annotated {@link org.junit.BeforeClass},
 * and {@link #after()} after any method annotated {@link org.junit.AfterClass}.</p>
 *
 * <p>JUnit invokes {@link #apply(Statement, Description)} before {@link #before()}.</p>
 * 
 * @see #createLogDirectory(Description)
 * @author Tim Burns
 */
public class FatLogHandler extends ExternalResource {
    private final static Logger LOG = Logger.getLogger(FatLogHandler.class.getName());

    // Property utility ...

    public static String getProperty(String propertyName) {
        return Props.getInstance().getProperty(propertyName);
    }

    public static File getFileProperty(String propertyName) {
        return Props.getInstance().getFileProperty(propertyName);
    }

    public static boolean getBooleanProperty(String propertyName) {
        return Props.getInstance().getBooleanProperty(propertyName);
    }

    public static int getIntProperty(String propertyName) {
        return Props.getInstance().getIntProperty(propertyName);
    }

    // Java properties; "simplicity.version", "host.address", "host.name",
    // "host.name.canonical", and possibly other properties
    // as determined by Props.collectVersionInformation.

    public static PropertyMap collectVersionInfo() {
        return Props.getInstance().collectVersionInformation();
    }

    // Help logging ...

    /**
     * Write help text to "output.txt" in logging directory.  Describe how to find
     * the usual FAT log file, and emit commonly used properties.
     *
     * Do nothing if at least INFO logging is not enabled.
     */
    public static void generateHelpFile() {
        if ( !LOG.isLoggable(Level.INFO) ) {
            return;
        }

        String logDirParentName = getProperty(Props.LOGGING_DIRS_PARENT);
        PropertyMap versionInfo = collectVersionInfo();
        String delimiter = getProperty(Props.LOGGING_BREAK_LARGE);

        File useLogDir = getFileProperty(Props.DIR_LOG);
        LogHandler helpHandler = new LogHandler();
        helpHandler.enable(useLogDir);

        try {
            LOG.info(delimiter);

            LOG.info("Hi there! If you're debugging a test failure, you should check out the log files found in the '" + logDirParentName + "' directory.");
            LOG.info(delimiter);

            LOG.info("How do I find test logs?");
            LOG.info("  (1)  Start by finding the results/" + logDirParentName + " directory");
            LOG.info("  (2)  Each test class has its own directory; let's call that a \"test fixture.\"");
            LOG.info("  (3)  Test fixtures are ordered by the order they were executed.");
            LOG.info("  (4)  Each test fixture has two logs: output.txt and trace.txt.");
            LOG.info("  (5)  output.txt contains INFO and above log records from all encapsulated test cases, and trace.txt contains ALL log records.");
            LOG.info("  (6)  Inside each test fixture, you'll find one directory for each encapsulated test case.");
            LOG.info("  (7)  Test cases are ordered by the order they were executed.");
            LOG.info("  (8)  Each test case has its own output.txt and trace.txt file; they only contain log records from that specific test.");
            LOG.info("  (9)  You might find additional files inside each test directory, such as request/response information for HTTP requests.");
            LOG.info("  (10) Have fun!");
            LOG.info(delimiter);

            LOG.info("What else can you tell me?");
            versionInfo.log(Level.INFO, "  ", true);
            LOG.info(delimiter);

        } finally {
            helpHandler.disable();
        }
    }

    // File naming utility ...

    //                                      012345678901234567
    private static final String PAD_TEXT = "000000000000000000";
    private static final int PAD_LIM = 18; // PAD_TEXT.length();

    /**
     * Answer the value converted to its decimal text and prepended to the left with
     * zeros.  The result always has the specified width.  Values which are larger than
     * will fit in the specified with are truncated.
     *
     * Negative values are not handled.  Widths greater than 18 are not handled.
     *
     * @param value A non-zero value which is to be left padded with zeros.
     * @param width The width to which to left pad the value.
     *
     * @return The value left padded with zeros (or truncated), to the specified
     *     width.
     */
    protected static String zeroPad(long value, int width) {
        if ( width <= 0 ) {
            return ""; // Unexpected, but handled to avoid exceptions.
        }
        if ( width > PAD_LIM ) {
            width = PAD_LIM; // Unexpected
        }

        if ( value < 0 ) {
            value = -1 * value; // Unexpected
        }

        String valueText = Long.toString(value);
        int valueLen = valueText.length();
        if ( valueLen == width ) {
            return valueText; // Need neither padding nor truncation
        } else if ( valueLen > width ) {
            return valueText.substring(valueLen - width); // Truncate
        } else {
            return PAD_TEXT.substring(width - valueLen) + valueText; // Pad
        }
    }

    /**
     * Pattern for valid file prefix and file suffix values.  The character
     * range "[a-zA-Z_0-9\\.]".  Note that '-' is not a valid charater
     * for file prefix and file suffix values.
     */
    protected final static Pattern WORD_CHARACTERS = Pattern.compile("[\\w\\.]*");

    public static boolean isWord(String text) {
        return ( WORD_CHARACTERS.matcher(text).matches() );
    }

    /**
     * Generate a new file name which is unique amoung the file names of a
     * specified directory.
     *
     * Files names are generated using the pattern:
     * <code>
     *     prefix + digits + '-' + suffix
     * </code>
     *
     * Digits are assigned sequentially from zero.  Digigs are left filled with
     * zeros ('0') to a specified width.
     *
     * A file name is generated: The file itself is not generated.  Concurrent
     * calls won't always generate unique names.
     *
     * @param parentDir The directory for which to generate the file name.
     * @param prefix Optional prefix to the file name.
     * @param digits The number of digits to place in the digits field of the
     *     generated file name.  May be zero.
     * @param suffix Optional suffix of the generated file name.
     *
     * @return A new file in the specified directory having a name which is unique
     *     for the directory and which is generated using the specified pattern
     *     parameters.  Null if a unique file name could not be generated.
     */
    protected File createOrderedFileInDirectory(
        File parentDir,
        String prefix, int digits, String suffix) {

        if ( parentDir == null ) {
            LOG.warning("Cannot create unique file: Null directory");
            return null;
        }

        int prefixLen;
        if ( prefix != null ) {
            if ( !isWord(prefix) ) {
                LOG.warning("Cannot create unique file: Non-valid characters detected in file name prefix: " + prefix);
                return null;
            } else {
                prefixLen = prefix.length();
            }
        } else {
            prefixLen = 0;
        }

        if ( digits < 0 ) {
            digits = 0; // Means that exactly one file name will be generated: "prefix" + '-' + "suffix".
        }

        int suffixLen;
        if ( suffix != null ) {
            if ( !isWord(suffix) ) {
                LOG.warning("Cannot create unique file: Non-valid characters detected in file name suffix: " + suffix);
                return null;
            } else {
                suffixLen = suffix.length();
            }
        } else {
            suffixLen = 0;
        }

        // The pattern is:
        //     prefix + digits + '-' + suffix
        //
        // The minimal generated file name is '-'.  Both
        // prefix and suffix may be null or empty, and the count
        // of digits can be zero.

        long highestChildNumber = 0;
        String nextHighestText = null;

        if ( digits > 0 ) {
            String[] childNames = parentDir.list();
            if ( childNames == null ) {
                childNames = new String[0]; // instead of requiring the parent to exist, treat it as an empty directory
            }

            int patternLen = prefixLen + digits + 1 + suffixLen;
            int dashOffset = prefixLen + digits;

            for ( String childName : childNames ) {
                if ( childName.length() != patternLen ) {
                    continue; // Doesn't match the pattern.
                }

                if ( (suffixLen > 0) && !childName.endsWith(suffix) ) {
                    continue; // Doesn't match the pattern.
                } else if ( (prefixLen > 0) && !childName.startsWith(prefix) ) {
                    continue; // Doesn't match the pattern.
                } else if ( childName.charAt(dashOffset) != '-' ) {
                    continue; // Doesn't match the pattern.
                }

                String childNumberText = childName.substring(prefixLen, dashOffset);

                long childNumber;
                try {
                    childNumber = Long.parseLong(childNumberText); // throws NumberFormatException
                } catch ( NumberFormatException e ) {
                    continue; // Doesn't match the pattern.
                }

                if ( childNumber > highestChildNumber ) {
                    highestChildNumber = childNumber;
                }
            }

            // This cycles a value which is all 9's to all 0's.
            nextHighestText = zeroPad(highestChildNumber + 1, digits);
        }

        StringBuilder nameBuilder = new StringBuilder();
        if ( prefixLen > 0 ) {
            nameBuilder.append(prefix);
        }
        if ( digits > 0 ) {
            nameBuilder.append(nextHighestText);
        }
        nameBuilder.append("-");
        if ( suffixLen > 0 ) {
            nameBuilder.append(suffix);
        }

        String nextHighestName = nameBuilder.toString();
        File nextHighestFile = new File(parentDir, nextHighestName);

        if ( nextHighestFile.exists() ) {
            LOG.warning("Cannot create unique file: Generated name is non-unique: " + nextHighestName);
            return null;
        }

        return nextHighestFile;
    }

    //

    /**
     * Determine and answer the log directory used by this handler.
     *
     * Answer null if the log directory could not be determined.
     *
     * Always fail (and answer null) if the description parameter is null.
     *
     * As a side effect, set the timer of this handler.
     *
     * @param description The description supplying parameters for the log directory.
     *
     * @return The log directory used by this handler.
     */
    protected File createLogDirectory(Description description) {
        if ( description == null ) {
            LOG.warning("Cannot create the log directory: Null JUnit test description");
            setTimer(null);
            return null;
        }

        File parentLogDir = getParentLogDir();
        if ( parentLogDir == null ) {
            File fatLogDir = getFileProperty(Props.DIR_LOG);
            parentLogDir = new File(fatLogDir, getProperty(Props.LOGGING_DIRS_PARENT));
        }

        String testMethodName = description.getMethodName();
        if ( testMethodName == null ) {
            testMethodName = "";
        } else {
            testMethodName = testMethodName.trim();
        }

        // Set the suffix either to the test method name or, if the test method
        // name is null or empty, to the test method name.
        //
        // If the suffix is set to the test method name, a timer will be created.

        String suffix;
        boolean setTimer;

        if ( testMethodName.isEmpty() ) {
            suffix = description.getClassName();
            if ( suffix == null ) {
                LOG.warning("Cannot create the log directory: Null JUnit test class name");
                setTimer(null);
                return null;
            }

            suffix = suffix.trim();
            if ( suffix.isEmpty() ) {
                LOG.warning("Cannot create the log directory: Empty JUnit test class name");
                setTimer(null);
                return null;
            }

            // Conditionally use the fully qalified class name of the simple class name.

            if ( !getBooleanProperty(Props.LOGGING_DIRS_FULLNAME) ) {
                suffix = suffix.substring( suffix.lastIndexOf(".") + 1 );
            }

            setTimer = true;

        } else {
            suffix = testMethodName;
            setTimer = false;
        }

        // Prepare the parent log directory ...

        if ( parentLogDir.exists() ) {
            if ( !parentLogDir.isDirectory() ) {
                LOG.warning("Cannot create the log directory: Parent exists and is not a directory: " + parentLogDir.getPath());
                setTimer(null);
                return null;
            }

        } else {
            parentLogDir.mkdirs();

            if ( !parentLogDir.exists() ) {
                LOG.warning("Failed to create the log directory: Parent could not be created: " + parentLogDir.getPath());
                setTimer(null);
                return null;
            } else if ( !parentLogDir.isDirectory() ) {
                LOG.warning("Failed to create the log directory: Parent was created, but not as a directory: " + parentLogDir.getPath());
                setTimer(null);
                return null;
            }
        }

        // Assign and create the log directory ...

        String prefix = getProperty(Props.LOGGING_DIRS_PREFIX);
        int digits = getIntProperty(Props.LOGGING_DIRS_DIGITS);

        File useLogDir = createOrderedFileInDirectory(parentLogDir, prefix, digits, suffix);
        if ( useLogDir == null ) {
            // Logging in 'createOrderdFileInDirectory'.
            setTimer(null);
            return null;
        }

        useLogDir.mkdirs();

        if ( !useLogDir.exists() ) {
            LOG.warning("Failed to create log directory: " + useLogDir.getName() + " under: " + parentLogDir.getPath() );
            setTimer(null);
            return null;
        } else if ( !useLogDir.isDirectory() ) {
            LOG.warning("Failed to create log directory: Created, but not as a directory: " + useLogDir.getName() + " under: " + parentLogDir.getPath());
            setTimer(null);
            return null;
        }

        setTimer( setTimer ? (new StopWatch()) : null );

        return useLogDir;
    }

    //

    private String descriptionText;

    protected FatLogHandler setDescription(Description description) {
        descriptionText = ( (description == null) ? null : description.toString() );
        return this;
    }

    protected String getDescriptionText() {
        return descriptionText;
    }

    //

    private File logDir;

    protected File setLogDirectory(Description description) {
        logDir = createLogDirectory(description);
        return logDir;
    }

    public File getLogDirectory() {
        return logDir;
    }

    // Parent handler ...

    private FatLogHandler parentLogHandler;
    public FatLogHandler setParent(FatLogHandler parentLogHandler) {
        this.parentLogHandler = parentLogHandler;
        return this;
    }

    public FatLogHandler getParent() {
        return parentLogHandler;
    }

    public File getParentLogDir() {
        FatLogHandler useParent = getParent();
        return ( (useParent == null) ? null : useParent.getLogDirectory() );
    }

    // The log handler ...

    private LogHandler logHandler;

    public FatLogHandler setLogHandler(LogHandler logHandler) {
        this.logHandler = logHandler;
        return this;
    }

    public LogHandler getLogHandler() {
        return logHandler;
    }

    protected void startLogging() {
        File useLogDir = getLogDirectory();
        LogHandler useLogHandler = getLogHandler();

        if ( (useLogHandler != null) && (useLogDir != null) ) {
            LOG.info("Activating log handlers for: " + useLogDir + ". Handler: " + useLogHandler);
            useLogHandler.enable(useLogDir); // null directory is ignored

        } else {
            LOG.info("Log handlers for " + useLogDir + " will NOT be activated. Handler: " + useLogHandler);
        }
    }

    protected void stopLogging() {
        File useLogDir = getLogDirectory();
        LogHandler useLogHandler = getLogHandler();

        if ( (useLogHandler != null) && (useLogDir != null) ) {
            LOG.info("Deactivating log handlers for " + useLogDir + ". Handler: " + useLogHandler);
            useLogHandler.disable();

        } else {
            LOG.info("Log handlers for " + useLogDir + " will NOT be deactivated. Handler: " + useLogHandler);
        }

    }

    // Timer utility ...
    //
    // A timer is set when preparing the test log folder
    // if the test method is not set and the test class is set.
    //
    // If a timer is set, it is triggered as "before" JUnit processing,
    // and again as "after" JUnit processing.
    // 

    private StopWatch timer;

    public StopWatch getTimer() {
        return timer;
    }

    public void setTimer(StopWatch timer) {
        this.timer = timer;
    }

    protected void startTimer() {
        if ( timer != null ) {
            LOG.info("Starting " + getDescriptionText());
            timer.start();
        }
    }

    protected void stopTimer() {
        if ( timer != null ) {
            timer.stop();
            LOG.info("Completed " + getDescriptionText() + " after " + timer.getTimeElapsedAsString());
        }
    }

    // Test API ...

    /**
     * Extend: Set the log description and create the log directory, then
     * continue to the superclass implementation.
     *
     * Emit a warning if the log directory could not be created (but continue
     * to the superclass implementation).
     *
     * @param The base statement which is to be run.
     * @param description The desciption of the log file to use for
     *
     * @return The statement obtained from the superlass implementation.
     */
    @Override
    public Statement apply(Statement base, Description description) {
        setDescription(description);

        File useLogDir = setLogDirectory(description);
        if ( useLogDir == null ) {
            LOG.warning("Failed to create a log directory for " + description);
        } else {
            LOG.info("Created log directory; " + useLogDir + " for: " + description);
        }

        return super.apply(base, description);
    }

    /**
     * Override: Start logging and start the timer.
     */
    @Override
    protected void before() {
        startLogging();
        startTimer();
    }

    /**
     * Override: Stop the timer and complete logging.
     */
    @Override
    protected void after() {
        stopTimer();
        stopLogging();
    }
}

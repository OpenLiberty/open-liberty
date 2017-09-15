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
package com.ibm.ws.fat.util;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * <p>Encapsulates a {@link LogHandler} to handle logs for FATs.</p>
 * <p>When a member variable is annotated {@link org.junit.Rule}, JUnit invokes {@link #before()} before any method annotated {@link org.junit.Before}, and {@link #after()} after
 * any method annotated {@link org.junit.After}.</p>
 * <p>Similarly, when a static variable is annotated {@link org.junit.ClassRule}, JUnit invokes {@link #before()} before any method annotated {@link org.junit.BeforeClass}, and
 * {@link #after()} after any method annotated {@link org.junit.AfterClass}.</p>
 * <p>Note that JUnit invokes {@link #apply(Statement, Description)} before {@link #before()}.</p>
 * 
 * @see #createLogDirectory(Description)
 * @author Tim Burns
 */
public class FatLogHandler extends ExternalResource {

    private final static Logger LOG = Logger.getLogger(FatLogHandler.class.getName());

    /**
     * Most users expect FAT buckets to have an output.txt file in the results directory.
     * This method generates an output.txt file that explains how to find the real test logs.
     */
    public static void generateHelpFile() {
        if (!LOG.isLoggable(Level.INFO)) {
            return;
        }
        LogHandler handler = new LogHandler();
        Props props = Props.getInstance();
        handler.enable(props.getFileProperty(Props.DIR_LOG));
        String parent = props.getProperty(Props.LOGGING_DIRS_PARENT);
        String delimiter = props.getProperty(Props.LOGGING_BREAK_LARGE);
        LOG.info(delimiter);
        LOG.info("Hi there! If you're debugging a test failure, you should check out the log files found in the '" + parent + "' directory.");
        LOG.info(delimiter);
        LOG.info("How do I find test logs?");
        LOG.info("  (1)  Start by finding the results/" + parent + " directory");
        LOG.info("  (2)  Each test class has its own directory; let's call that a \"test fixture.\"");
        LOG.info("  (3)  Test fixtures are ordered by the order they were executed.");
        LOG.info("  (4)  Each test fixture has two logs: output.txt and trace.txt.");
        LOG.info("  (5)  output.txt contains INFO and above log records from all encapsulated test cases, and trace.txt contains ALL log records.");
        LOG.info("  (6)  Inside each test fixture, you'll find one directory for each encapsulated test case.");
        LOG.info("  (7)  Test cases are ordered by the order they were executed.");
        LOG.info("  (8)  Each test case has its own output.txt and trace.txt file; they only contain log records from that specific test.");
        LOG.info("  (9)  You might find additional files inside each test directory, such as request/response information for HTTP requests.");
        LOG.info("  (10) Have fun!");
        LOG.info("What else can you tell me?");
        props.collectVersionInformation().log(Level.INFO, "  ", true);
        LOG.info(delimiter);
        handler.disable();
    }

    /** zero or more characters matching [a-zA-Z_0-9\\.]. The '-' character is not allowed! */
    protected final static Pattern WORD_CHARACTERS = Pattern.compile("[\\w\\.]*");

    /**
     * <p>
     * Searches a directory for file names matching a numeric prefix, and
     * returns a new File instance representing a new (unique) file in that
     * directory. Successive calls to this method for the same directory and
     * prefix will produce an ordered list of child files. For example:
     * prefix01_nameA, prefix02_nameB, prefix03_nameC, prefix04_nameC, etc. Note
     * that the returned File will not yet exist on the file system.
     * </p>
     * <p>
     * This method is not thread-safe.
     * </p>
     * 
     * @param parent
     *            the directory where you want to create a new file
     * @param prefix
     *            an optional String that proceeds ordering information. All
     *            characters must match: [a-zA-Z_0-9\\.]
     * @param digits
     *            the number of digits to use for generated identifiers
     * @param name
     *            the name of the desired file, without any numeric identifier.
     *            All characters must match: [a-zA-Z_0-9\\.]
     * @return a new File representing a unique child of the parent
     * @throws IllegalArgumentException
     *             if input arguments are invalid
     */
    protected File createOrderedFileInDirectory(File parent, String prefix, int digits, String name) throws IllegalArgumentException {
        if (parent == null) {
            throw new IllegalArgumentException("Can't create a file in a directory that doesn't exist: " + parent);
        }
        String[] children = parent.list();
        if (children == null) {
            //                  throw new IllegalArgumentException("The parent does not denote a directory: "+parent);
            children = new String[0]; // instead of requiring the parent to exist, treat it as an empty directory
        }
        if (!WORD_CHARACTERS.matcher(name).matches()) { // if the file name contains a non-word character
            throw new IllegalArgumentException("Invalid characters detected in proposed file name: " + name);
        }
        int prefixLength = 0;
        if (prefix != null) {
            if (!WORD_CHARACTERS.matcher(prefix).matches()) { // if the file name contains a non-word character
                throw new IllegalArgumentException("Invalid characters detected in file prefix: " + prefix);
            }
            prefixLength = prefix.length();
        }
        long highest = 0;
        for (String child : children) {
            if (child == null) {
                continue; // ignore children with a null name
            }
            int dashIndex = child.indexOf("-");
            if (dashIndex < 0) {
                continue; // ignore children without a dash in their name
            }
            if (prefixLength > 0 && !child.startsWith(prefix)) {
                continue; // ignore children missing our prefix in their name
            }
            String numberString = child.substring(prefixLength, dashIndex);
            long number = Long.parseLong(numberString);
            if (number > highest) {
                highest = number;
            }
        }
        StringBuilder newName = new StringBuilder();
        if (prefixLength > 0) {
            newName.append(prefix);
        }
        newName.append(this.zeroPad(highest + 1, digits));
        newName.append("-");
        newName.append(name);
        return new File(parent, newName.toString());
    }

    /**
     * Prepends zeros to the left of the input number to ensure that the input
     * number is a total of <code>width</code> digits. Truncates the input
     * number if it has more than <code>width</code> digits.
     * 
     * @param number
     *            a positive integer (negative integers cause problems with odd
     *            widths)
     * @param width
     *            the number of characters that you want in a String
     *            representation of <code>number</code>; must be a positive
     *            integer smaller than 18 (larger numbers cause an overflow
     *            issue)
     * @return a zero-padded String representation of the input number
     */
    protected String zeroPad(long number, int width) {
        long n = Math.abs(number);
        long w = width;
        if (w < 0) {
            w = 0;
        } else if (w > 18) {
            w = 18;
        }
        long wrapAt = (long) Math.pow(10, w);
        return String.valueOf(n % wrapAt + wrapAt).substring(1);
    }

    private File logDirectory;
    private LogHandler logHandler;
    private FatLogHandler parent;
    private String description;
    private StopWatch timer;

    /**
     * @return the encapsulated {@link LogHandler}
     */
    public LogHandler getLogHandler() {
        return this.logHandler;
    }

    /**
     * @param logHandler the {@link LogHandler} to encapsulate; null indicates that no log files should be created.
     * @return this instance
     */
    public FatLogHandler setLogHandler(LogHandler logHandler) {
        this.logHandler = logHandler;
        return this;
    }

    /**
     * @return the parent FatLogHandler, or null if no parent exists
     */
    public FatLogHandler getParent() {
        return this.parent;
    }

    /**
     * @param parent the parent FatLogHandler, or null if no parent exists
     * @return this instance
     */
    public FatLogHandler setParent(FatLogHandler parent) {
        this.parent = parent;
        return this;
    }

    /**
     * @return the directory where this instance will log results, or null if results will not be logged
     */
    public File getLogDirectory() {
        return this.logDirectory;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        this.description = description == null ? null : description.toString();
        this.logDirectory = this.createLogDirectory(description);
        if (this.logDirectory == null) {
            LOG.warning("Failed to create a log directory for: " + this.description);
        } else {
            LOG.info("Created log directory: " + this.logDirectory + " for " + this.description);
        }
        return super.apply(base, description);
    }

    /**
     * Defines the directory where logs will be stored for the current test fixture / test case
     * 
     * @param description the test fixture currently being applied
     * @return the directory where results from this test fixture should be logged, or null if no valid directory could be found
     */
    protected File createLogDirectory(Description description) {
        if (description == null) {
            LOG.warning("Cannot create log directory because test case description from JUnit is null");
            return null;
        }
        Props props = Props.getInstance();
        File parentLogDir = this.parent == null ? null : this.parent.getLogDirectory();
        if (parentLogDir == null) {
            File fatLogDir = props.getFileProperty(Props.DIR_LOG);
            parentLogDir = new File(fatLogDir, props.getProperty(Props.LOGGING_DIRS_PARENT));
        }
        String suffix = null;
        String testMethodName = description.getMethodName();
        if (testMethodName == null) {
            testMethodName = "";
        } else {
            testMethodName = testMethodName.trim();
        }
        if (testMethodName.isEmpty()) {
            this.timer = new StopWatch();
            suffix = description.getClassName();
            if (suffix == null) {
                LOG.warning("Cannot create log directory because test class name is null");
                return null;
            }
            suffix = suffix.trim();
            if (suffix.isEmpty()) {
                LOG.warning("Cannot create log directory because test class name is empty");
                return null;
            }
            if (!props.getBooleanProperty(Props.LOGGING_DIRS_FULLNAME)) {
                // determine simple class name, and avoid ArrayIndexOutOfBoundsException
                int afterLastDot = suffix.lastIndexOf(".") + 1;
                if (afterLastDot > -1 && afterLastDot < suffix.length()) {
                    suffix = suffix.substring(afterLastDot);
                }
            }
        } else {
            this.timer = null; // reset state in case this handler is reused
            suffix = testMethodName;
        }
        String prefix = props.getProperty(Props.LOGGING_DIRS_PREFIX);
        int digits = props.getIntProperty(Props.LOGGING_DIRS_DIGITS);
        parentLogDir.mkdirs(); // parent directory must exist in order to examine it
        File directory;
        try {
            directory = this.createOrderedFileInDirectory(parentLogDir, prefix, digits, suffix);
        } catch (IllegalArgumentException e) {
            LOG.warning("Cannot create log directory: " + e.getMessage());
            return null;
        }
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                LOG.warning("Cannot create log directory because a weird file is already there; can't use it as a directory: " + directory);
                return null;
            }
        } else {
            directory.mkdirs();
        }
        return directory;
    }

    @Override
    protected void before() {
        LogHandler handler = this.getLogHandler();
        File directory = this.getLogDirectory();
        if (handler != null && directory != null) {
            LOG.info("Activating log handlers for: " + directory);
            handler.enable(directory); // null directory is ignored
        } else {
            LOG.info("Log handlers for " + directory + " will NOT be created. Handler: " + handler);
        }
        if (this.timer != null) {
            this.timer.start();
        }
    }

    @Override
    protected void after() {
        if (this.timer != null) {
            this.timer.stop();
            LOG.info("Completed " + this.description + " after " + this.timer.getTimeElapsedAsString());
        }
        LogHandler handler = this.getLogHandler();
        if (handler != null) {
            LOG.info("Deactivating log handlers for " + this.getLogDirectory());
            handler.disable();
        } else {
            LOG.info("Log handlers for " + this.getLogDirectory() + " will NOT be deactivated. Handler: " + handler);
        }
    }

}

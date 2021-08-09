/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.viewer;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.logging.hpel.FormatSet;
import com.ibm.ws.logging.internal.hpel.HpelHeader;

/**
 * Extending HPEL LogViewer for Liberty environment.
 */
public class BinaryLog extends LogViewer {
    private final String action;
    private final File unvalidatedRepositoryDir;
    private final File unvalidatedTargetDir;

    private static final String BUNDLE_NAME = "com.ibm.ws.logging.hpel.viewer.internal.resources.BinaryLogMessages";
    private static final String NAME_VALUE_PAIR_SEPARATOR = "=";

    private static final String ACTION_VIEW = "view";
    private static final String ACTION_COPY = "copy";
    private static final String ACTION_LISTINSTANCES = "listinstances";
    private static final String ACTION_HELP = "help";

    private static final String LATEST_INSTANCE = "latest";

    private static final int RC_SUCCESS = 0;
    private static final int RC_BAD_INPUT = 20;

    // copied from BootstrapConstants since we don't want to introduce a new dependency
    private static final String DEFAULT_SERVER_NAME = "defaultServer";

    // this dateTime format matches what is used by HpelFormatter,
    // which is the ONLY dateTime format we should use to ensure consistency
    private final DateFormat defaultDateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
    private final DateFormat defaultDateTimeFormat = FormatSet.customizeDateFormat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM));
    private final DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final DateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private DateFormat dateFormat;
    private DateFormat dateTimeFormat;
    // Initialize WAS specific LEVELS.
    private final static Map<String, LevelDetails> LEVELS = new HashMap<String, LevelDetails>(3);
    static {
        LEVELS.put("FATAL", new LevelDetails(1100, "F", null));
        LEVELS.put("AUDIT", new LevelDetails(850, "A", null));
        LEVELS.put("DETAIL", new LevelDetails(625, "D", null));
    }

    // strip off any punctuation or other noise, see if the rest appears to be a help request.
    // note that the string is already trim()'d by command-line parsing unless user explicitly escaped a space
    private static boolean looksLikeHelp(String taskname) {
        if (taskname == null)
            return false; // applied paranoia, since this isn't in a performance path
        int start = 0, len = taskname.length();
        while (start < len && !Character.isLetter(taskname.charAt(start)))
            ++start;
        return ACTION_HELP.equalsIgnoreCase(taskname.substring(start).toLowerCase());
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // Just print the short usage instructions if this is a no-arg invocation
        if (args.length == 0) {
            printShortUsage();
            System.exit(RC_SUCCESS);
        }

        int numProcessedArgs = 0;

        // Extract the action parameter
        String actionLowerCase = args[0].toLowerCase();

        // Can the action be construed as "help" even though it isn't an exact match? (--help, helpme, etc)
        if (looksLikeHelp(actionLowerCase))
            actionLowerCase = ACTION_HELP;

        if (!actionLowerCase.equals(ACTION_HELP) &&
            !actionLowerCase.equals(ACTION_VIEW) &&
            !actionLowerCase.equals(ACTION_COPY) &&
            !actionLowerCase.equals(ACTION_LISTINSTANCES)) {

            System.err.println(getLocalizedParmString("BL_INVALID_ACTION", new Object[] { args[0] }));
            System.err.println(getLocalizedString("BL_USE_HELP"));
            System.exit(RC_BAD_INPUT);
        }

        numProcessedArgs++;

        // For all actions other than 'help' the next parm is the serverName / repositoryPath.
        // Figure out what the repository directory is...
        File repositoryDir = null;
        if (!actionLowerCase.equals(ACTION_HELP)) {
            String serverNameOrRepositoryPath;
            if (args.length > 1) {
                serverNameOrRepositoryPath = args[1];
                if (serverNameOrRepositoryPath.isEmpty()) {
                    serverNameOrRepositoryPath = DEFAULT_SERVER_NAME;
                    numProcessedArgs++;
                } else if (serverNameOrRepositoryPath.startsWith("-")) {
                    serverNameOrRepositoryPath = DEFAULT_SERVER_NAME;
                } else {
                    numProcessedArgs++;
                }
            } else {
                serverNameOrRepositoryPath = DEFAULT_SERVER_NAME;
            }

            // serverNameOrRepositoryPath could point to either a server name or a
            // path on the file system.  If it contains a file separator or it points
            // to an existing file, we assume it's a repositoryPath.
            // Else, we assume it's a server name.
            repositoryDir = new File(serverNameOrRepositoryPath);
            if (!serverNameOrRepositoryPath.contains(File.separator)) {
                File serverOutputDir = Utils.getServerOutputDir(serverNameOrRepositoryPath);
                if (repositoryDir.exists()) {
                    File currentDir = new File(".", serverNameOrRepositoryPath);
                    try {
                        if (currentDir.getCanonicalFile().equals(serverOutputDir.getCanonicalFile())) {
                            repositoryDir = new File(serverOutputDir, "logs");
                        }
                    } catch (IOException e) {
                        // Do nothing if failed to compare
                    }
                } else {
                    repositoryDir = new File(serverOutputDir, "logs");
                }
            }
        }

        // For copy action the next parm is the targetDir.
        File targetDir = null;
        if (actionLowerCase.equals(ACTION_COPY)) {
            String targetDirString = null;
            if (args.length > 2) {
                if (!args[2].startsWith("-")) {
                    targetDirString = args[2];
                    numProcessedArgs++;
                }
            }

            if (targetDirString != null) {
                targetDir = new File(targetDirString);
            } else {
                System.err.println(getLocalizedString("BL_COPY_REQUIRES_TARGETDIR"));
                System.err.println(getLocalizedString("BL_USE_HELP"));
                System.exit(RC_BAD_INPUT);
            }
        }

        String[] newArgs = new String[args.length - numProcessedArgs];
        System.arraycopy(args, numProcessedArgs, newArgs, 0, newArgs.length);
        args = newArgs;

        BinaryLog viewer = new BinaryLog(actionLowerCase, repositoryDir, targetDir);

        int code = viewer.execute(args, LEVELS, HpelHeader.getLibertyRuntimeHeader());
        if (code > 0) {
            System.err.println(getLocalizedString("BL_USE_HELP"));
            System.exit(RC_BAD_INPUT);
        }

        System.exit(RC_SUCCESS);
    }

    BinaryLog(String action, File repositoryDir, File targetDir) {
        this.action = action;
        this.unvalidatedRepositoryDir = repositoryDir;
        this.unvalidatedTargetDir = targetDir;

        dateFormat = defaultDateFormat;
        dateTimeFormat = defaultDateTimeFormat;
        dateFormat.setLenient(false);
        dateTimeFormat.setLenient(false);
        isoDateFormat.setLenient(false);
        isoDateTimeFormat.setLenient(false);
    }

    private static abstract class Option {
        // Display name of this option.
        final String name;
        // Number of extra arguments required by this option.
        final boolean hasArg;

        Option(String name, boolean hasArg) {
            this.name = name;
            this.hasArg = hasArg;
        }

        /**
         * Assign value indicated by this option.
         *
         * @param value
         *            the value of the option
         */
        abstract void accept(String value) throws IllegalArgumentException;
    }

    private static abstract class OneArgOption extends Option {
        OneArgOption(String name) {
            super(name, true);
        }

        @Override
        void accept(String value) throws IllegalArgumentException {
            // Default implementation is for options with one required argument.
            setValue(value);
        }

        /**
         * Set value for this option.
         *
         * @param arg
         */
        abstract void setValue(String arg) throws IllegalArgumentException;
    }

    private abstract static class NoArgOption extends Option {
        NoArgOption(String name) {
            super(name, false);
        }

        @Override
        void accept(String value) throws IllegalArgumentException {
            enableOption();
        }

        abstract void enableOption();
    }

    private class TailOption extends NoArgOption {
        TailOption(String name) {
            super(name);
        }

        @Override
        void enableOption() throws IllegalArgumentException {
            setTailInterval(1);
        }
    }

    private final Option[] filterOptions = new Option[] { new OneArgOption("--minDate") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setStartDate(arg);
        }
    }, new OneArgOption("--maxDate") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setStopDate(arg);
        }
    }, new OneArgOption("--minLevel") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setMinLevel(arg);
        }
    }, new OneArgOption("--maxLevel") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setMaxLevel(arg);
        }
    }, new OneArgOption("--includeLogger") {
        @Override
        void setValue(String arg) {
            setIncludeLoggers(arg);
        }
    }, new OneArgOption("--excludeLogger") {
        @Override
        void setValue(String arg) {
            setExcludeLoggers(arg);
        }
    }, new OneArgOption("--includeMessage") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setMessage(arg);
        }
    }, new OneArgOption("--excludeMessage") {
        @Override
        void setValue(String arg) {
            setExcludeMessages(arg);
        }
    }, new OneArgOption("--includeThread") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setHexThreadID(arg);
        }
    }, new OneArgOption("--includeExtension") {
        @Override
        public void setValue(String arg) throws IllegalArgumentException {
            setExtensions(arg);
        }
    }, new OneArgOption("--includeInstance") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            if (arg.toLowerCase().equals(LATEST_INSTANCE)) {
                setLatestInstance(true);
            } else {
                setInstanceId(arg);
            }
        }
    } };

    private final Option[] formatOptions = new Option[] { new OneArgOption("--format") {
        @Override
        void setValue(String arg) throws IllegalArgumentException {
            setOutFormat(arg);
        }
    }, new OneArgOption("--encoding") {
        @Override
        public void setValue(String arg) throws IllegalArgumentException {
            setEncoding(arg);
        }
    }, new OneArgOption("--locale") {
        @Override
        void setValue(String arg) {
            setLocale(arg);
        }
    } };

    private final Option[] monitorOptions = new Option[] { new TailOption("--monitor") };
    private final Option[] isoDateFormatOptions = new Option[] { new TailOption("--isoDateFormat") {
        @Override
        public void enableOption() throws IllegalArgumentException {
            setDateFormat();
            useISODateFormatObjects();
        }
    } };

    private final Option[][] viewActionOptions = new Option[][] { filterOptions, monitorOptions, formatOptions, isoDateFormatOptions };
    private final Option[][] copyActionOptions = new Option[][] { filterOptions };
    private final Option[][] listInstancesActionOptions = new Option[][] { isoDateFormatOptions };

    /**
     * The parseCmdLineArgs method.
     * <p>
     * This method parses the cmd line args and stores them into the local variables.
     *
     * @param args
     *            - command line arguments to LogViewer
     */
    @Override
    protected boolean parseCmdLineArgs(String[] args) throws IllegalArgumentException {

        if (action.equals(ACTION_HELP)) {
            if (args.length == 0)
                helpAction(null);
            else
                helpAction(args[0]);

            return true;
        }

        if (action.equals(ACTION_VIEW)) {
            parseOptions(args, viewActionOptions);
            return false;
        } else if (action.equals(ACTION_LISTINSTANCES)) {
            parseOptions(args, listInstancesActionOptions);
            setListInstances(true);
            return false;
        } else if (action.equals(ACTION_COPY)) {
            parseOptions(args, copyActionOptions);
            return false;
        }

        return true;

    }

    private static void parseOptions(String[] args, Option[][] actionOptions) throws IllegalArgumentException {

        for (int i = 0; i < args.length; i++) {
            boolean accepted = false;

            // compute the argName and argValue
            String argName;
            String argValue = null;
            int equalsIndex = args[i].indexOf(NAME_VALUE_PAIR_SEPARATOR);
            if (equalsIndex < 0) {
                argName = args[i];
            } else {
                argName = args[i].substring(0, equalsIndex);
                if (equalsIndex + 1 < args[i].length()) {
                    argValue = args[i].substring(equalsIndex + 1);
                }
            }

            // find a matching option and initialize it
            for (Option[] options : actionOptions) {
                if (accepted) {
                    break;
                }
                for (Option option : options) {
                    if (argName.equalsIgnoreCase(option.name)) {
                        if (option.hasArg && argValue == null) {
                            throw new IllegalArgumentException(getLocalizedParmString("BL_OPTION_REQUIRES_A_VALUE",
                                                                                      new Object[] { option.name }));
                        }
                        option.accept(argValue);
                        accepted = true;
                        break;
                    }
                }
            }
            if (!accepted) {
                throw new IllegalArgumentException(getLocalizedParmString("BL_UNKNOWN_OPTION", new Object[] { args[i] }));
            }
        }

    }

    /**
     * The validateSettings method.
     * <p>
     * This method validates the settings which are configured via cmd line arguments. Invalid settings will either be
     * set to defaults or the program must exit.
     *
     */
    @Override
    protected boolean validateSettings() throws IllegalArgumentException {
        // Validate values which do not require user input first to avoid unnecesary
        // work on the user part.

        // dates
        Date startDate = getStartDate();
        Date stopDate = getStopDate();
        if (startDate != null && stopDate != null) {
            if (startDate.after(stopDate)) {
                // stop date is before the start date.
                throw new IllegalArgumentException(getLocalizedString("BL_MINDATE_AFTER_MAXDATE"));
            }
        }

        // Validate levels if specified.
        Level minLevel = getMinLevel();
        Level maxLevel = getMaxLevel();
        if (maxLevel != null && minLevel != null) {
            // We have both a max and min level. Check that the min level is
            // equal to or less than the max level
            if (minLevel.intValue() > maxLevel.intValue()) {
                // Min is greater than Max level. Report error
                throw new IllegalArgumentException(getLocalizedString("BL_MINLEVEL_GREATER_THAN_MAXLEVEL"));
            }
        }

        // Required options
        try {
            setBinaryRepositoryDir(unvalidatedRepositoryDir.getCanonicalPath());
            System.out.println(getLocalizedParmString("BL_REPOSITORY_DIRECTORY", new Object[] { getValidatedBinaryRepositoryDir() }));
        } catch (IOException e) {
            throw new IllegalArgumentException(getLocalizedParmString("BL_INVALID_REPOSITORYDIR", new Object[] { unvalidatedRepositoryDir }));
        }

        // in case of copy action, we have an unvalidatedTargetDir set
        try {
            if (unvalidatedTargetDir != null) {
                String dir = unvalidatedTargetDir.getCanonicalPath();
                setOutputRepositoryDir(dir);
                System.out.println(getLocalizedParmString("BL_TARGET_DIRECTORY", new Object[] { dir }));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(getLocalizedParmString("BL_INVALID_TARGETDIR", new Object[] { unvalidatedTargetDir }));
        }

        return false;

    }

    /**
     * @param newStartDate
     *            the startDate to set
     */
    @Override
    void setStartDate(String newStartDate) throws IllegalArgumentException {

        try {
            if (newStartDate.contains(":")) {
                // User passed in a time as well as date
                setStartDate(defaultDateTimeFormat.parse(newStartDate));
            } else {
                setStartDate(defaultDateFormat.parse(newStartDate));
            }
        } catch (ParseException e) {
            try {
                if (newStartDate.contains(":")) {
                    // User passed in a time as well as date
                    setStartDate(isoDateTimeFormat.parse(newStartDate));
                } else {
                    setStartDate(isoDateFormat.parse(newStartDate));
                }
            } catch (ParseException pe) {
                throw new IllegalArgumentException(getLocalizedString("BL_INVALID_MINDATE"), e);
            }
        }
    }

    /**
     * @param newStopDate
     *            - the stopDate to set
     *
     */
    @Override
    void setStopDate(String newStopDate) throws IllegalArgumentException {

        try {
            if (newStopDate.contains(":")) {
                // User passed in a time as well as date
                setStopDate(defaultDateTimeFormat.parse(newStopDate));
            } else {
                // when stop date is supplied with no time, we need to add 1 day - 1 ms
                // to make the stop date be treated as a maximum
                Date stopDate = new Date(defaultDateFormat.parse(newStopDate).getTime() + 86400000 - 1);
                setStopDate(stopDate);
            }
        } catch (ParseException e) {
            try {
                if (newStopDate.contains(":")) {
                    // User passed in a time as well as date
                    setStopDate(isoDateTimeFormat.parse(newStopDate));
                } else {
                    // when stop date is supplied with no time, we need to add 1 day - 1 ms
                    // to make the stop date be treated as a maximum
                    Date stopDate = new Date(isoDateFormat.parse(newStopDate).getTime() + 86400000 - 1);
                    setStopDate(stopDate);
                }
            } catch (ParseException p) {
                throw new IllegalArgumentException(getLocalizedString("BL_INVALID_MAXDATE"), e);
            }
        }
    }

    /*
     * Prints the appropriate help, based on the arg
     */
    private void helpAction(String arg) {
        if (arg == null) {
            printMainUsage();
            return;
        }

        String helpTarget = arg.toLowerCase();

        if (helpTarget.equals(ACTION_VIEW)) {
            printViewUsage();
        } else if (helpTarget.equals(ACTION_COPY)) {
            printCopyUsage();
        } else if (helpTarget.equals(ACTION_LISTINSTANCES)) {
            printListInstancesUsage();
        } else {
            printMainUsage();
        }
    }

    private static void printShortUsage() {
        System.out.println(getLocalizedString("BL_MAIN_USAGE_001") + "\n\n");
    }

    private void printMainUsage() {

        System.out.println(getLocalizedString("BL_MAIN_USAGE_001"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_004"));
        System.out.println(getLocalizedString("BL_MAIN_USAGE_005"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_006"));
        System.out.println(getLocalizedString("BL_MAIN_USAGE_007"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_008"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_009"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_010"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_011"));
        System.out.println(getLocalizedString("BL_MAIN_USAGE_012"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_013"));
        System.out.println(getLocalizedString("BL_MAIN_USAGE_014"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_015"));
        System.out.println(getLocalizedString("BL_MAIN_USAGE_016"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_017"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_018"));
        System.out.println();

    }

    private void printViewUsage() {

        // create a date sample and a date/time sample using the same technique we use in HpelFormatter for date/time formatting
        Date sampleDate = new Date();
        String dateSample = dateFormat.format(sampleDate);
        String dateTimeSample = dateTimeFormat.format(sampleDate);
        String isoDateSample = isoDateFormat.format(sampleDate);
        String isoDateTimeSample = isoDateTimeFormat.format(sampleDate);

        System.out.println(getLocalizedString("BL_VIEW_USAGE_001"));
        System.out.println(getLocalizedString("BL_MAIN_USAGE_004"));
        System.out.println(getLocalizedString("BL_MAIN_USAGE_005"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_006"));
        System.out.println(getLocalizedString("BL_MAIN_USAGE_007"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_008"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_002"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_003"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_004"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_005"));
        System.out.println(getLocalizedParmString("BL_VIEW_USAGE_006", new Object[] { dateSample, dateTimeSample, isoDateSample, isoDateTimeSample }));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_007"));
        System.out.println(getLocalizedParmString("BL_VIEW_USAGE_008", new Object[] { dateSample, dateTimeSample, isoDateSample, isoDateTimeSample }));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_009"));
        System.out.println(getLocalizedParmString("BL_VIEW_USAGE_010", new Object[] { getLevelString() }));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_011"));
        System.out.println(getLocalizedParmString("BL_VIEW_USAGE_012", new Object[] { getLevelString() }));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_013"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_014"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_015"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_016"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_017"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_018"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_035"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_036"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_019"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_020"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_021"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_022"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_023"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_024"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_025"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_026"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_027"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_028"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_029"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_030"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_031"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_032"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_033"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_034"));
        System.out.println();

    }

    private void printCopyUsage() {

        // create a date sample and a date/time sample using the same technique we use in HpelFormatter for date/time formatting
        Date sampleDate = new Date();
        String dateSample = dateFormat.format(sampleDate);
        String dateTimeSample = dateTimeFormat.format(sampleDate);
        String isoDateSample = isoDateFormat.format(sampleDate);
        String isoDateTimeSample = isoDateTimeFormat.format(sampleDate);

        System.out.println(getLocalizedString("BL_COPY_USAGE_001"));
        System.out.println(getLocalizedString("BL_MAIN_USAGE_004"));
        System.out.println(getLocalizedString("BL_MAIN_USAGE_005"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_006"));
        System.out.println(getLocalizedString("BL_MAIN_USAGE_007"));
        System.out.println();
        System.out.println(getLocalizedString("BL_COPY_USAGE_002"));
        System.out.println(getLocalizedString("BL_COPY_USAGE_003"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_008"));
        System.out.println();
        System.out.println(getLocalizedString("BL_COPY_USAGE_004"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_003"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_004"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_005"));
        System.out.println(getLocalizedParmString("BL_VIEW_USAGE_006", new Object[] { dateSample, dateTimeSample, isoDateSample, isoDateTimeSample }));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_007"));
        System.out.println(getLocalizedParmString("BL_VIEW_USAGE_008", new Object[] { dateSample, dateTimeSample, isoDateSample, isoDateTimeSample }));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_009"));
        System.out.println(getLocalizedParmString("BL_VIEW_USAGE_010", new Object[] { getLevelString() }));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_011"));
        System.out.println(getLocalizedParmString("BL_VIEW_USAGE_012", new Object[] { getLevelString() }));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_013"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_014"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_015"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_016"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_017"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_018"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_035"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_036"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_019"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_020"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_021"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_022"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_023"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_024"));
        System.out.println();

    }

    private void printListInstancesUsage() {

        System.out.println(getLocalizedString("BL_LISTINSTANCES_USAGE_001"));
        System.out.println(getLocalizedString("BL_MAIN_USAGE_004"));
        System.out.println(getLocalizedString("BL_MAIN_USAGE_005"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_006"));
        System.out.println(getLocalizedString("BL_MAIN_USAGE_007"));
        System.out.println();
        System.out.println(getLocalizedString("BL_MAIN_USAGE_008"));
        System.out.println();
        System.out.println(getLocalizedString("BL_LISTINSTANCES_USAGE_002"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_028"));
        System.out.println();
        System.out.println(getLocalizedString("BL_VIEW_USAGE_033"));
        System.out.println(getLocalizedString("BL_VIEW_USAGE_034"));
        System.out.println();

    }

    @Override
    String getLocalizedString_UNABLE_TO_COPY() {
        return getLocalizedString("BL_UNABLE_TO_COPY");
    }

    @Override
    String getLocalizedString_NO_FILES_FOUND_MONITOR() {
        return getLocalizedString("BL_NO_FILES_FOUND_MONITOR");
    }

    @Override
    String getLocalizedString_NO_FILES_FOUND() {
        return getLocalizedString("BL_NO_FILES_FOUND");
    }

    @Override
    String getLocalizedParmString_BAD_FORMAT(Object[] parms) {
        return getLocalizedParmString("BL_BAD_FORMAT", parms);
    }

    private static String getLocalizedString(String key) {
        return getLocalizedString(BUNDLE_NAME, key);
    }

    private static String getLocalizedParmString(String key, Object[] parms) {
        return getLocalizedParmString(BUNDLE_NAME, key, parms);
    }

    // this dateTime format matches what is used by HpelFormatter,
    // which is the ONLY dateTime format we should use to ensure consistency
    @Override
    String getFormattedDateTime(Date dateTime) {
        return dateTimeFormat.format(dateTime);
    }

    // used by our unit tests
    @Override
    public String toString() {
//        return super.toString() + ", action=" + action;

        // super.toString() is returning startDate and stopDate from Logviewer, which is picked from nls so need to change both the dates.
        String str = super.toString() + ", action=" + this.action;
        String[] values = str.split(",");
        StringBuffer buf = new StringBuffer();

        if (getStartDate() != null && getStopDate() != null) {
            values[3] = (" startDate=" + this.dateTimeFormat.format(getStartDate()));
            values[4] = (" stopDate=" + this.dateTimeFormat.format(getStopDate()));
            for (int i = 0; i < values.length; i++) {
                if (i != values.length - 1)
                    buf.append(values[i]).append(",");
                else {
                    buf.append(values[i]);
                }
            }
            return buf.toString();
        } else {
            return str;
        }

    }

    public void useISODateFormatObjects() {
        dateFormat = isoDateFormat;
        dateTimeFormat = isoDateTimeFormat;
    }

}

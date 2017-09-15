/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import java.security.AccessController;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.KernelBootstrap;
import com.ibm.ws.kernel.boot.internal.ServerLock;
import com.ibm.ws.kernel.boot.internal.commands.ListServerHelper;

/**
 * The platform launcher; processes command line options and
 * bootstrap.properties file(s) and launches the OSGi framework.
 *
 * Use the --help option (passed to main) for details on available command line
 * options.
 */
public class Launcher {
    final static long launchTime = System.nanoTime();

    /**
     * The runtime launcher/main. String arguments are treated and parsed as
     * command line parameters. This method will call <code>System.exit</code> when the framework shuts down.
     *
     * @param args
     *            Command line arguments.
     * @throws Exception
     *
     * @see Launcher#createPlatform(String[])
     */
    public static void main(String[] args) {
        Launcher launcher = new Launcher();

        System.exit(launcher.createPlatform(args));
    }

    /**
     * Launch the platform-- this method will not return until the
     * framework exits. Subsequent calls to this method (while the platform is
     * running) will immediately return.
     *
     * @param args
     *            Command line arguments
     *
     * @return 0 if platform was launched and shut down successfully, > 20 if
     *         an exception or unexpected condition occurred during
     *         platform/framework startup or shutdown.
     */
    public int createPlatform(String[] args) {
        ReturnCode rc = ReturnCode.OK;

        Map<String, String> initProps = new HashMap<String, String>(20);
        initProps.put(BootstrapConstants.LAUNCH_TIME, Long.toString(launchTime));
        initProps.put("org.apache.aries.blueprint.preemptiveShutdown", Boolean.toString(false));

        BootstrapConfig bootProps = createBootstrapConfig();

        try {
            // process and consume Launcher-specific command line arguments
            // (bottom) bootstrap.properties file <- command line args <- system properties (top)
            LaunchArguments launchArgs = createLaunchArguments(args, initProps);
            rc = launchArgs.getRc(); // Pay attention that rc is not modified before calling handleActions().

            // Figure out log locations, etc.
            findLocations(bootProps, launchArgs.getProcessName());

            if (rc.logName != null) {
                // If the server doesn't exist (and won't be created), then
                // create the log in the common .logs dir.  We always put
                // create.log in the common .logs dir because we don't want to
                // create the server directory before verifying it.
                if (rc == ReturnCode.CREATE_ACTION || (!bootProps.getConfigFile(null).exists() && !rc.getVerifyServer().willCreate())) {
                    Debug.open(bootProps.getUserOutputFile(".logs"), rc.logName);
                } else {
                    Debug.open(bootProps.getLogDirectory(), rc.logName);
                }
            }

            // Test/create required directories if this is a new server that should be created (--create)
            bootProps.verifyProcess(rc.getVerifyServer(), launchArgs);

            // Initialize bootstrap configuration: bootstrap properties, etc.
            if (rc.readInitialConfig()) {

                // Read/merge bootstrap properties and System.properties into initProps
                bootProps.configure(initProps);

                // Set system properties based on configuration result..
                bootProps.setSystemProperties();
            }
            bootProps.setCmdArgs(launchArgs.getExtraArguments());

            if (launchArgs.getProcessName() == null && launchArgs.getRc().defaultServerNameMessage()) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString(getDefaultProcessNameMessageKey()),
                                                        getDefaultProcessName()));
            }

            // Now perform appropriate processing based on the current return code
            rc = handleActions(bootProps, launchArgs);
        } catch (LocationException e) {
            System.out.println(BootstrapConstants.messages.getString("error.unableToLaunch"));
            System.out.println(e.getTranslatedMessage());

            if (Debug.isOpen()) {
                Debug.printStackTrace(e);
                Debug.println(bootProps.printLocations(true));
            }
            if (e.getCause() != null) {
                e.printStackTrace();
            }
            if (e.getCause() != null || bootProps.isConfigured()) {
                System.out.println(bootProps.printLocations(true));
            }

            rc = ReturnCode.LOCATION_EXCEPTION;
        } catch (ClientRunnerException e) {
            System.out.println(e.getTranslatedMessage());

            if (Debug.isOpen()) {
                Debug.printStackTrace(e);
            }
            if (e.getCause() != null) {
                e.printStackTrace();
            }

            rc = e.getReturnCode();
        } catch (LaunchException e) {
            System.out.println(BootstrapConstants.messages.getString("error.unableToLaunch"));
            System.out.println(e.getTranslatedMessage());

            if (Debug.isOpen()) {
                Debug.printStackTrace(e);
            }
            if (e.getCause() != null) {
                e.printStackTrace();
            }

            rc = e.getReturnCode();
        } catch (RuntimeException e) {
            Throwable t = e.getCause();
            if (t == null)
                t = e;

            System.out.println("in Launcher, about to error.unknownException");
            System.out.println(BootstrapConstants.messages.getString("error.unableToLaunch"));
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.unknownException"), t.toString()));

            if (Debug.isOpen()) {
                Debug.printStackTrace(e);
            }
            e.printStackTrace();
            rc = ReturnCode.RUNTIME_EXCEPTION;
        } catch (Throwable e) {
            System.out.println(BootstrapConstants.messages.getString("error.unableToLaunch"));
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.unknownException"), e.toString()));

            if (Debug.isOpen()) {
                Debug.printStackTrace(e);
            }
            e.printStackTrace();
            rc = ReturnCode.UNKNOWN_EXCEPTION;
        } finally {
            System.out.flush();
            if (!Debug.isOpen()) {
                System.err.flush();
            } else {
                boolean delete = rc == ReturnCode.OK;

                if (!delete) {
                    Debug.println();

                    for (int i = 0; i < args.length; i++) {
                        Debug.println("arg" + i + "=" + args[i]);
                    }

                    Debug.println("exit=" + rc.val);

                    Debug.println();
                    Debug.println(bootProps.printLocations(true));
                }

                Debug.close(delete);
            }
        }

        return rc.val;
    }

    /**
     * Return an instance of bootstrap configuration class for the process being launched.
     *
     * @return BootstrapConfig
     */
    protected BootstrapConfig createBootstrapConfig() {
        return new BootstrapConfig();
    }

    /**
     * Return an instance of LaunchArguments.
     *
     * @return LaunchArguments
     */
    private LaunchArguments createLaunchArguments(String[] args, Map<String, String> initProps) {
        List<String> cmdArgs = processBatchFileArgs(new ArrayList<String>(Arrays.asList(args)));
        return new LaunchArguments(cmdArgs, initProps, isClient());
    }

    protected boolean isClient() {
        return false;
    }

    /**
     * Handle the process action.
     *
     * @param bootProps An instance of BootstrapConfig
     * @param launchArgs An instance of LaunchArguments
     */
    protected ReturnCode handleActions(BootstrapConfig bootProps, LaunchArguments launchArgs) {
        ReturnCode rc = launchArgs.getRc();

        switch (rc) {
            case OK:

                rc = new KernelBootstrap(bootProps).go();
                break;
            case CREATE_ACTION:
                // Use initialized bootstrap configuration to create the server lock.
                // This ensures the server and nested workarea directory exist and are writable
                ServerLock.createServerLock(bootProps);
                rc = bootProps.disablePermGenIfNecessary();
                break;
            case MESSAGE_ACTION:
                rc = showMessage(launchArgs);
                break;
            case HELP_ACTION:
                rc = showHelp(launchArgs);
                break;
            case VERSION_ACTION:
                KernelBootstrap.showVersion(bootProps);
                rc = ReturnCode.OK;
                break;
            case STOP_ACTION:
                rc = new com.ibm.ws.kernel.boot.internal.commands.ProcessControlHelper(bootProps, launchArgs).stop();
                break;
            case STATUS_ACTION:
                rc = new com.ibm.ws.kernel.boot.internal.commands.ProcessControlHelper(bootProps, launchArgs).status(false);
                break;
            case STARTING_STATUS_ACTION:
                rc = new com.ibm.ws.kernel.boot.internal.commands.ProcessControlHelper(bootProps, launchArgs).status(true);
                break;
            case START_STATUS_ACTION:
                rc = new com.ibm.ws.kernel.boot.internal.commands.ProcessControlHelper(bootProps, launchArgs).startStatus();
                break;
            case PACKAGE_ACTION:
                rc = new com.ibm.ws.kernel.boot.internal.commands.PackageCommand(bootProps, launchArgs).doPackage();
                break;
            case PACKAGE_WLP_ACTION:
                rc = new com.ibm.ws.kernel.boot.internal.commands.PackageCommand(bootProps, launchArgs).doPackageRuntimeOnly();
                break;
            case DUMP_ACTION:
                rc = new com.ibm.ws.kernel.boot.internal.commands.ProcessControlHelper(bootProps, launchArgs).dump();
                break;
            case JAVADUMP_ACTION:
                rc = new com.ibm.ws.kernel.boot.internal.commands.ProcessControlHelper(bootProps, launchArgs).dumpJava();
                break;
            case PAUSE_ACTION:
                rc = new com.ibm.ws.kernel.boot.internal.commands.ProcessControlHelper(bootProps, launchArgs).pause();
                break;
            case RESUME_ACTION:
                rc = new com.ibm.ws.kernel.boot.internal.commands.ProcessControlHelper(bootProps, launchArgs).resume();
                break; 
            case LIST_ACTION:
                rc = new ListServerHelper(bootProps, launchArgs).listServers();
                break;
            default:
                showHelp(launchArgs);
                rc = ReturnCode.BAD_ARGUMENT;
        }
        return rc;
    }

    /**
     * Return the ResoueceBundle for process command options.
     *
     * @return ResourceBundle
     */
    protected ResourceBundle getOptionsResourceBundle() {
        /** Since this launches the framework, we have to do translation ourselves.. */
        return ResourceBundle.getBundle("com.ibm.ws.kernel.boot.resources.LauncherOptions");
    }

    /**
     * Find main locations
     *
     * @param bootProps An instance of BootstrapConfig
     * @param processName Process name to be used
     */
    protected void findLocations(BootstrapConfig bootProps, String processName) {
        // Check for environment variables...
        String userDirStr = getEnv(BootstrapConstants.ENV_WLP_USER_DIR);
        String serversDirStr = getEnv(bootProps.getOutputDirectoryEnvName());

        // Check for the variable calculated by the shell script first (X_LOG_DIR)
        // If that wasn't found, check for LOG_DIR set for java -jar invocation
        String logDirStr = getEnv(BootstrapConstants.ENV_X_LOG_DIR);
        if (logDirStr == null)
            logDirStr = getEnv(BootstrapConstants.ENV_LOG_DIR);

        // Likewise for X_LOG_FILE and LOG_FILE.
        String consoleLogFileStr = getEnv(BootstrapConstants.ENV_X_LOG_FILE);
        if (consoleLogFileStr == null)
            consoleLogFileStr = getEnv(BootstrapConstants.ENV_LOG_FILE);

        // Do enough processing to know where the directories should be..
        // this should not cause any directories to be created
        bootProps.findLocations(processName, userDirStr, serversDirStr, logDirStr, consoleLogFileStr);
    }

    protected ReturnCode showHelp(LaunchArguments launchArgs) {
        ResourceBundle options = getOptionsResourceBundle();

        // If we are showing help but someone put in a messed up command,
        // e.g. "server package --help", we should show/prefer script usage
        String script = launchArgs.getScript();

        // show java args only if requested: otherwise prefer the script
        if (script == null) {
            System.out.println(options.getString("briefUsage"));
            showUsageInfo(options, true);
        } else {
            System.out.println(MessageFormat.format(options.getString("scriptUsage"), script));
            showUsageInfo(options, false);
        }

        return ReturnCode.OK;
    }

    /**
     * Show usage info based on the translated messages available for each
     * option. Will display in sorted order. Each option should have two keys in
     * the LauncherMessages file:
     * <ul>
     * <li>option-key.name: this should contain the name of the option and it's parameters. e.g. option-key.clean=-clean</li>
     * <li>option-desc.name: this should describe what the option is for. e.g. option-desc.clean=Clean the OSGi framework cache.</li>
     * </ul>
     */
    protected void showUsageInfo(ResourceBundle options, boolean forJava) {
        final String okpfx = "option-key.";
        final String odpfx = "option-desc.";
        final String akpfx = "action-key.";
        final String adpfx = "action-desc.";

        System.out.println();
        System.out.println(options.getString("processName.key"));
        System.out.println(options.getString("processName.desc"));
        System.out.println();

        Enumeration<String> keys = options.getKeys();
        Set<String> optionKeys = new TreeSet<String>();
        Set<String> actionKeys = new TreeSet<String>();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (key.startsWith(okpfx)) {
                optionKeys.add(key);
            } else if (key.startsWith(akpfx)) {
                actionKeys.add(key);
            }
        }

        System.out.println(options.getString("use.actions"));
        System.out.println();
        // Print each action and it's associated descriptive text
        for (String actionKey : actionKeys) {
            String action = actionKey.substring(akpfx.length());
            String actionKeyString = options.getString(actionKey);
            String actionDesc = options.getString(adpfx + action);

            //TODO revert this when schemagen is delivered based on design issue #153300 conclusion.
            if (actionKeyString.trim().equals("--schemagen"))
                continue;

            if (forJava) {
                // These actions are implied for java -jar.
                String actionKeyStringTrimmed = actionKeyString.trim();
                if (actionKeyStringTrimmed.equals("--start") ||
                    actionKeyStringTrimmed.equals("--run") ||
                    actionKeyStringTrimmed.equals("--debug")) {
                    continue;
                }
            } else {
                actionKeyString = actionKeyString.replace("--", "");
            }

            System.out.println(actionKeyString);
            if (!actionDesc.isEmpty())
                System.out.println(actionDesc);
            System.out.println();
        }

        if (optionKeys.size() > 0) {
            System.out.println(options.getString("use.options"));
            System.out.println();

            // Print each option and it's associated descriptive text
            for (String optionKey : optionKeys) {
                String option = optionKey.substring(okpfx.length());
                System.out.println(options.getString(optionKey));
                System.out.println(options.getString(odpfx + option));
                System.out.println();
            }
        }

        if (forJava) {
            System.out.println(options.getString("use.jvmarg"));
            System.out.println();
            System.out.println(options.getString("javaAgent.key"));
            System.out.println(options.getString("javaAgent.desc"));
            System.out.println();
        }
    }

    /**
     * @param launchArgs LaunchArguments
     * @return ReturnCode.OK or ReturnCode.BAD_ARGUMENT
     */
    public ReturnCode showMessage(LaunchArguments launchArgs) {
        String message = launchArgs.getOption("message");
        try {
            String messageString = BootstrapConstants.messages.getString(message);

            List<String> messageArgs = launchArgs.getExtraArguments();
            if (messageArgs.isEmpty()) {
                System.out.println(messageString);
            } else {
                Object[] messageArgsArray = messageArgs.toArray(new Object[messageArgs.size()]);
                System.out.println(MessageFormat.format(messageString, messageArgsArray));
            }
        } catch (MissingResourceException e) {
            return ReturnCode.BAD_ARGUMENT;
        }
        return ReturnCode.OK;
    }

    /**
     * Get value from initial configuration properties. If property is not
     * present in initial/framework properties, try finding it in system
     * properties.
     *
     * @param key
     *            Property key
     * @return Object value, or null if not found.
     */
    protected String getEnv(final String key) {
        if (key == null)
            return null;

        String value = null;
        try {
            value = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws Exception {
                    return System.getenv(key);
                }
            });
        } catch (Exception ex) {
            // Oh well.
        }

        return value;
    }

    protected String getDefaultProcessName() {
        return BootstrapConstants.DEFAULT_SERVER_NAME;
    }

    protected String getDefaultProcessNameMessageKey() {
        return "info.defaultServer";
    }

    protected List<String> processBatchFileArgs(List<String> args) {
        if (args.size() > 1) {
            // There is no convenient mechanism to shift positional parameters
            // in a batch file, so the server script passes all parameters and
            // a --batch-file option to indicate post-processing is needed.
            String arg = args.get(0);
            if (arg.equals("--batch-file")) {
                args = processBatchFileArgs(args, null);
            } else if (arg.startsWith("--batch-file=")) {
                args = processBatchFileArgs(args, arg.substring("--batch-file=".length()));
            }
        }

        return args;
    }

    private List<String> processBatchFileArgs(List<String> args, String actionOption) {
        // --batch-file[=--ACTION] ACTION [SERVER|CLIENT] [PARAM]...

        // Extract server parameter in the same way as the server script.
        String processName;
        if (args.size() > 2) {
            processName = args.get(2);
            if (processName.isEmpty()) {
                // no extra parameters or server name
                processName = getDefaultProcessName();
            } else if (processName.startsWith("-")) {
                // this is another parameter, no server name
                processName = getDefaultProcessName();
            } else {
                args.remove(2); // remove processName
            }
        } else {
            processName = getDefaultProcessName();
        }

        if (args.size() > 1) {
            if (actionOption == null) {
                // Replace the --batch-file parameter with the server name
                args.set(0, processName);
                args.remove(1); // remove the old action parameter
            } else {
                args.set(0, actionOption); // set the real action option
                args.set(1, processName); // set the processName
            }
        } else {
            // Replace the --batch-file parameter with the server name
            args.set(0, processName);
        }

        return args;
    }
}

/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.KernelUtils;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 *
 */
public class LaunchArguments {
    static final String MESSAGE_ACTION = "--message:";
    static final String HELP_UNKNOWN_ACTION = "--help:actions:";

    private static final List<String> KNOWN_OPTIONS = Collections.unmodifiableList(Arrays.asList(new String[] { "archive", "include",
                                                                                                                "os", "pid", "pid-file",
                                                                                                                "script", "template", "force",
                                                                                                                "target", "no-password", "server-root", "timeout" }));

    /**
     * Script argument: set by both batch and shell scripts to record the
     * command used to invoke the script.
     */
    private final String script = System.getenv("INVOKED");

    private final String actionOption;
    private final String processName;
    private final ReturnCode returnCode;

    Map<String, String> options = new HashMap<String, String>();

    /** Extra command line arguments occurring after a --. These are passed into the framework unchanged. */
    private List<String> extraArguments = Collections.emptyList();

    /**
     * Package protected: parse arguments passed to the Launcher.
     * When adding new actions, please make sure to:
     * <ul>
     * <li>Add action to LauncherAction
     * <li>Add descriptive text to the LauncherOptions.properties file so that your
     * option will show up when --help is used.
     * </ul>
     *
     * @param cmdArgs
     * @param initProps
     */
    LaunchArguments(List<String> cmdArgs, Map<String, String> initProps) {
        this(cmdArgs, initProps, false);
    }

    /**
     * Package protected: parse arguments passed to the Launcher.
     * When adding new actions, please make sure to:
     * <ul>
     * <li>Add action to LauncherAction
     * <li>Add descriptive text to the LauncherOptions.properties file so that your
     * option will show up when --help is used.
     * </ul>
     *
     * @param cmdArgs
     * @param initProps
     * @param isClient
     */
    LaunchArguments(List<String> cmdArgs, Map<String, String> initProps, boolean isClient) {
        ReturnCode returnValue = ReturnCode.OK;

        String action = null;
        String processNameArg = null;
        String checkpointPhase = null;

        // Remember the help action in case multiple are specified
        String helpAction = null;

        if (cmdArgs.size() > 0) {

            Iterator<String> i = cmdArgs.listIterator();
            while (i.hasNext() && returnValue != ReturnCode.BAD_ARGUMENT) {
                String arg = i.next();

                if (arg.equals("--")) {
                    // Marker for beginning of extra options...
                    i.remove();
                    break;
                }

                // Ignore java parameters as they are processed in the .bat script.
                if (arg.startsWith("-D")) {
                    i.remove();
                    break;
                }

                if (arg.startsWith("-")) {
                    String argToLower = arg.toLowerCase(Locale.ENGLISH);
                    i.remove(); // consume this argument

// ------- ACTIONS -----------------------------------------------------------
                    // Pick out actions (we unfortunately have one that is a reserved word, 'package')
                    // Some also require special processing..
                    if (argToLower.contains("-help")) {
                        // there are help variants, we need to remember what was asked for
                        options.put("arg", argToLower);
                        if (argToLower.startsWith(HELP_UNKNOWN_ACTION)) {
                            String unknownAction = arg.substring(HELP_UNKNOWN_ACTION.length());
                            // this is a bad argument detected by the script..
                            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.unknownArgument"),
                                                                    unknownAction));
                            System.out.println();
                            returnValue = ReturnCode.BAD_ARGUMENT;
                            break;
                        }
                        helpAction = argToLower;
                    } else if (argToLower.equals("--version")) {
                        returnValue = checkPreviousAction(returnValue, ReturnCode.VERSION_ACTION, arg);
                        action = setActionIfOk(returnValue, action, arg);
                    } else if (argToLower.equals("--list")) {
                        returnValue = checkPreviousAction(returnValue, ReturnCode.LIST_ACTION, arg);
                        action = setActionIfOk(returnValue, action, arg);
                    } else if (argToLower.equals("--create")) {
                        returnValue = checkPreviousAction(returnValue, ReturnCode.CREATE_ACTION, arg);
                        action = setActionIfOk(returnValue, action, arg);
                    } else if (argToLower.equals("--stop")) {
                        returnValue = checkPreviousAction(returnValue, ReturnCode.STOP_ACTION, arg);
                        action = setActionIfOk(returnValue, action, arg);
                    } else if (argToLower.startsWith("--status")) {
                        if (argToLower.equals("--status")) {
                            returnValue = checkPreviousAction(returnValue, ReturnCode.STATUS_ACTION, arg);
                        } else if (argToLower.equals("--status:starting")) {
                            returnValue = checkPreviousAction(returnValue, ReturnCode.STARTING_STATUS_ACTION, arg);
                        } else if (argToLower.equals("--status:start")) {
                            returnValue = checkPreviousAction(returnValue, ReturnCode.START_STATUS_ACTION, arg);
                        } else {
                            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.unknownArgument"), arg));
                            System.out.println();
                            returnValue = ReturnCode.BAD_ARGUMENT;
                            break;
                        }
                        action = setActionIfOk(returnValue, action, arg);
                    } else if (argToLower.equals("--package")) {
                        returnValue = checkPreviousAction(returnValue, ReturnCode.PACKAGE_ACTION, arg);
                        action = setActionIfOk(returnValue, action, arg);
                    } else if (argToLower.equals("--dump")) {
                        returnValue = checkPreviousAction(returnValue, ReturnCode.DUMP_ACTION, arg);
                        action = setActionIfOk(returnValue, action, arg);
                    } else if (argToLower.equals("--javadump")) {
                        returnValue = checkPreviousAction(returnValue, ReturnCode.JAVADUMP_ACTION, arg);
                        action = setActionIfOk(returnValue, action, arg);
                    } else if (argToLower.equals("--pause")) {
                        returnValue = checkPreviousAction(returnValue, ReturnCode.PAUSE_ACTION, arg);
                        action = setActionIfOk(returnValue, action, arg);
                    } else if (argToLower.equals("--resume")) {
                        returnValue = checkPreviousAction(returnValue, ReturnCode.RESUME_ACTION, arg);
                        action = setActionIfOk(returnValue, action, arg);
                    } else if (argToLower.startsWith(MESSAGE_ACTION)) {
                        returnValue = checkPreviousAction(returnValue, ReturnCode.MESSAGE_ACTION, arg);
                        action = setActionIfOk(returnValue, action, arg);

                        String value = arg.substring(MESSAGE_ACTION.length());
                        options.put("message", value);

                        // Special action: break now! leftovers -> extra arguments -> message parameters!
                        break;
// ------- OPTIONS -----------------------------------------------------------
                    } else if (argToLower.equals("--clean")) {
                        // special handling for clean: it needs to over-ride a system property
                        initProps.put(BootstrapConstants.INITPROP_OSGI_CLEAN, BootstrapConstants.OSGI_CLEAN_VALUE);
                        System.clearProperty(BootstrapConstants.INITPROP_OSGI_CLEAN);
                    } else if (argToLower.startsWith("--internal-checkpoint-at=")) {

                        if (isBetaEdition()) {
                            checkpointPhase = argToLower.substring("--internal-checkpoint-at=".length());
                        } else {
                            // we cannot efficiently do beta guard from the server script so we hard code this check here
                            // for the checkpoint action
                            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.unknownArgument"),
                                                                    "checkpoint"));
                            System.out.println();
                            returnValue = ReturnCode.BAD_ARGUMENT;
                        }
                    } else if (isClient && argToLower.equals("--autoacceptsigner")) {
                        initProps.put(BootstrapConstants.AUTO_ACCEPT_SIGNER, "true");

                        // options with a value.  (option=value)
                    } else {
                        int eqIndex = arg.indexOf('=');
                        String value = "";
                        String key;
                        if (argToLower.startsWith("--")) {
                            if (eqIndex != -1) {
                                key = argToLower.substring(2, eqIndex);
                                value = arg.substring(eqIndex + 1);
                            } else {
                                key = arg.substring(2);
                            }
                        } else {
                            key = null;
                        }
                        if (KNOWN_OPTIONS.contains(key)) {

                            //  **** T I M E O U T   o p t i o n ****
                            if (key != null && key.equals("timeout")) {
                                // --timeout is only valid for the stop command
                                // action can be null if user enter the "run","debug", or checkpoint commands
                                if (action == null || !action.equals("--stop")) {
                                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.optionNotApplicableToCommand"), arg));
                                    returnValue = ReturnCode.BAD_ARGUMENT;
                                    break;
                                } else {
                                    if (eqIndex == -1) {
                                        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.optionRequiresEquals"), arg));
                                        returnValue = ReturnCode.BAD_ARGUMENT;
                                        break;
                                    }
                                }
                                String saveValue = value;

                                value = KernelUtils.parseDuration(value, TimeUnit.SECONDS);

                                if (saveValue.startsWith("-") || !isValidTimeoutValue(value)) {
                                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.badOptionValue"), saveValue, arg));
                                    returnValue = ReturnCode.BAD_ARGUMENT;
                                    break;
                                }
                            }

                            //  **** A L L   o p t i o n s  with a value ****
                            options.put(key, value);

                        } else {

                            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.unknownArgument"), arg));
                            System.out.println();

                            returnValue = ReturnCode.BAD_ARGUMENT;
                            break;

                        }
                    }
                } else {
                    i.remove(); // consume parameter

                    // Single positional parameter: processName
                    if (processNameArg != null) {
                        if (isClient)
                            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("warning.singleClient"), processNameArg, arg));
                        else
                            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("warning.singleServer"), processNameArg, arg));
                    } else {
                        processNameArg = arg;
                    }
                }
            }
        }

        // do some recovery in case help was specified strangely...
        if (helpAction != null) {
            returnValue = ReturnCode.HELP_ACTION;
            // If two actions were specified, allow that to reach the help command
            // undisturbed so we can show appropriate usage strings..
            if (action == null) {
                action = helpAction;
            }
        }

        // Any left over arguments are passed on into the framework
        if (returnValue == ReturnCode.OK || returnValue == ReturnCode.MESSAGE_ACTION)
            extraArguments = cmdArgs;

        // Allow the command to work even if the server does not exist.
        if (returnValue == ReturnCode.PACKAGE_ACTION) {
            String includeValue = options.get("include");
            if ("wlp".equals(includeValue)) {
                options.remove("include");
                returnValue = ReturnCode.PACKAGE_WLP_ACTION;
            }
        }

        returnCode = setCheckpointPhase(checkpointPhase, returnValue);
        processName = processNameArg;
        actionOption = action;
    }

    /**
     * @param timeout
     */
    private boolean isValidTimeoutValue(String timeoutString) {
        try {
            int x = Integer.parseInt(timeoutString);
            if (x < 0) {
                return false;
            }
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * @param checkpointPhase
     */
    private ReturnCode setCheckpointPhase(String checkpointPhase, ReturnCode returnValue) {
        try {
            Method setPhase = CheckpointPhase.class.getDeclaredMethod("setPhase", String.class);
            setPhase.setAccessible(true);
            setPhase.invoke(setPhase, checkpointPhase);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (CheckpointPhase.getPhase() == CheckpointPhase.INACTIVE && checkpointPhase != null) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.invalidPhaseName"), checkpointPhase));
            System.out.println();
            return ReturnCode.BAD_ARGUMENT;
        }
        return returnValue;
    }

    /*
     * Duplicating ProductInfo logic here to avoid unnecessary calls to ThreadIdentityManager too early
     */
    static public boolean isBetaEdition() {
        return Boolean.getBoolean(ProductInfo.BETA_EDITION_JVM_PROPERTY);
    }

    private ReturnCode checkPreviousAction(ReturnCode oldRC, ReturnCode newActionRC, String arg) {
        if (oldRC == ReturnCode.OK)
            return newActionRC;

        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.unknownArgument"), arg));
        System.out.println();
        return ReturnCode.BAD_ARGUMENT;
    }

    private String setActionIfOk(ReturnCode returnCode, String oldAction, String newAction) {
        if (returnCode != ReturnCode.BAD_ARGUMENT)
            return newAction; // return the new action
        else
            return oldAction; // keep the old one
    }

    public ReturnCode getRc() {
        return returnCode;
    }

    public String getAction() {
        return actionOption;
    }

    public String getOption(String name) {
        return options.get(name);
    }

    public List<String> getExtraArguments() {
        return extraArguments;
    }

    public String getProcessName() {
        return processName;
    }

    public String getScript() {
        return script;
    }

    // The timeout is the for whatever action is being processed.
    // So it might be a start timeout (currently not implemented) or a stop timeout.
    private int timeoutInSeconds = -1;

    public int getStopTimeout() {
        return getTimeout(BootstrapConstants.SERVER_STOP_WAIT_TIME_DEFAULT);
    }

    /**
     * @param defaultValue
     * @return Value of --timeout option in seconds if specified. If not specified return the default.
     */
    private int getTimeout(String defaultValue) {
        // If we've already computed the timeout, just return it.
        if (timeoutInSeconds > 0) {
            return timeoutInSeconds;
        }

        // Start with the default value
        timeoutInSeconds = Integer.valueOf(defaultValue);

        // Then see if it was overridden on the command line.
        String timeoutString = getOption("timeout");
        if (timeoutString != null) {
            timeoutInSeconds = Integer.valueOf(timeoutString);
        }
        return timeoutInSeconds;
    }

}

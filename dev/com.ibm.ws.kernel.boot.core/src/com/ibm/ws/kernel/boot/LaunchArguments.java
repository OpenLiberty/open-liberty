/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

/**
 *
 */
public class LaunchArguments {
    static final String MESSAGE_ACTION = "--message:";
    static final String HELP_UNKNOWN_ACTION = "--help:actions:";

    private static final List<String> KNOWN_OPTIONS = Collections.unmodifiableList(Arrays.asList(new String[] { "archive", "include",
                                                                                                                "os", "pid", "pid-file",
                                                                                                                "script", "template", "force",
                                                                                                                "target", "no-password", "server-root" }));

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
                    } else if (isClient && argToLower.equals("--autoacceptsigner")) {
                        initProps.put(BootstrapConstants.AUTO_ACCEPT_SIGNER, "true");
                    } else {
                        int index = arg.indexOf('=');
                        String value = "";
                        String key;
                        if (argToLower.startsWith("--")) {
                            if (index != -1) {
                                key = argToLower.substring(2, index);
                                value = arg.substring(index + 1);
                            } else {
                                key = arg.substring(2);
                            }
                        } else {
                            key = null;
                        }
                        if (KNOWN_OPTIONS.contains(key)) {
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

        processName = processNameArg;
        returnCode = returnValue;
        actionOption = action;
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

}

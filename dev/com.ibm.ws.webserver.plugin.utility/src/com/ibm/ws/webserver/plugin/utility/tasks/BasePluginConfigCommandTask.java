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
package com.ibm.ws.webserver.plugin.utility.tasks;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.ws.webserver.plugin.utility.utils.CommandUtils;
import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.product.utility.CommandTask;


public abstract class BasePluginConfigCommandTask implements CommandTask {
    static final String SLASH = "/";
    static final String NL = System.getProperty("line.separator");
    static final String TAB = "\t";
    
    protected String scriptName;
    
    // required arguments
    protected Collection<String> reqArgs = new HashSet<String>();
    // all possible arguments
    protected Collection<String> knownArgs = new HashSet<String>();
 

    public BasePluginConfigCommandTask(String scriptName) {
       this.scriptName = scriptName;
    }

    protected String getMessage(String key, Object... args) {
        return CommandUtils.getMessage(key, args);
    }

    protected String getOption(String key, Object... args) {
        return CommandUtils.getOption(key, args);
    }
    

    protected String buildScriptOptions(String optionKeyPrefix, String optionDescPrefix) {
        StringBuilder scriptOptions = new StringBuilder();
        if (optionKeyPrefix != null && !optionKeyPrefix.isEmpty() && optionDescPrefix != null && !optionDescPrefix.isEmpty()) {
            Enumeration<String> keys = CommandUtils.getOptions().getKeys();
            Set<String> optionKeys = new TreeSet<String>();

            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                if (key.startsWith(optionKeyPrefix)) {
                    optionKeys.add(key);
                }
            }

            if (optionKeys.size() > 0) {
                for (String optionKey : optionKeys) {
                    String option = optionKey.substring(optionKeyPrefix.length());
                    scriptOptions.append(NL);
                    scriptOptions.append(CommandUtils.getOptions().getString(optionKey));
                    scriptOptions.append(NL);
                    scriptOptions.append(CommandUtils.getOptions().getString(optionDescPrefix + option));
                    scriptOptions.append(NL);
                }
            }
        }

        return scriptOptions.toString();
    }
    
    protected String buildScriptOptions(String optionKeyPrefix, String[] optionDescPrefixes) {
        StringBuilder scriptOptions = new StringBuilder();
        if (optionKeyPrefix != null && !optionKeyPrefix.isEmpty() && optionDescPrefixes != null && !(optionDescPrefixes.length==0)) {
            Enumeration<String> keys = CommandUtils.getOptions().getKeys();
            Set<String> optionKeys = new TreeSet<String>();

            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                if (key.startsWith(optionKeyPrefix)) {
                    optionKeys.add(key);
                }
            }

            if (optionKeys.size() > 0) {
                for (String optionKey : optionKeys) {
                    String option = optionKey.substring(optionKeyPrefix.length());
                    scriptOptions.append(NL);
                    scriptOptions.append(CommandUtils.getOptions().getString(optionKey));
                    scriptOptions.append(NL);
                    for (String optionDescPrefix : optionDescPrefixes) {
                       scriptOptions.append(CommandUtils.getOptions().getString(optionDescPrefix + option));
                       scriptOptions.append(NL);
                    }   
                }
            }
        }

        return scriptOptions.toString();
    }

    
    /**
     * Returns the value at i+1, guarding against ArrayOutOfBound exceptions.
     * If the next element is not a value but an argument flag (starts with -)
     * return null.
     *
     * @return String value as defined above, or {@code null} if at end of args.
     */
    private String getValue(String arg) {
        String[] split = arg.split("=");
        if (split.length == 1) {
            return null;
        } else if (split.length == 2) {
            return split[1];
        } else {
            // Handle DN case with multiple =s
            StringBuffer value = new StringBuffer();
            for (int i = 1; i < split.length; i++) {
                value.append(split[i]);
                if (i < (split.length - 1)) {
                    value.append("=");
                }
            }
            return value.toString();
        }
    }

    
    /**
     * Gets the argument name for the given --name=value pair.
     *
     * @param arg
     * @return
     */
    private String getArgName(String arg) {
        return arg.split("=")[0];
    }

    /**
     * Checks if the argument is a known argument to the task.
     *
     * @param arg
     * @return
     */
    private boolean isKnownArgument(String arg) {
        final String argName = getArgName(arg);
        for (String key : knownArgs) {
            if (key.equalsIgnoreCase(argName)) {
                return true;
            }
        }
        return false;
    }

    
    protected void validateArgumentList(String[] args, boolean supportsDefaultTarget) {
        validateArgumentList(args, true, supportsDefaultTarget);
    }

    /**
     * Validates that there are no unknown arguments or values specified
     * to the task.
     *
     * @param args the arguments to the task
     * @param supportsTarget
     * @param supportsDefaultTarget
     * @throws IllegalArgumentException if an argument is defined is unknown
     */
    protected void validateArgumentList(String[] args, boolean supportsTarget, boolean supportsDefaultTarget) {
        checkRequiredArguments(args, !supportsTarget || supportsDefaultTarget);

        // If the command supports a target name
        // skip the first argument if it is the target name.
        int firstArg = 1;
        if (supportsTarget) {
            if (!supportsDefaultTarget) {
                firstArg = 2;
            } else {
                if (args.length > 1) {
                    String primArg = args[1];
                    if (!primArg.startsWith("-"))
                        firstArg = 2;
                }
            }
        }

        // Arguments and values come in pairs (expect -password).
        // Anything outside of that pattern is invalid.
        // Loop through, jumping in pairs except when we encounter
        // -password -- that may be an interactive prompt which won't
        // define a value.
        for (int i = firstArg; i < args.length; i++) {
            String argName = getArgName(args[i]);
            if (!isKnownArgument(argName)) {
                throw new IllegalArgumentException(getMessage("invalidArg", argName));
            }
            // Everything we accept as an arg expects an name=value pair.
            // If we see anything without the =value pair, error
            if (!args[i].contains("=")) {
                throw new IllegalArgumentException(getMessage("missingValue", argName));
            }
        }
    }
    
    private void checkRequiredArguments(String[] args, boolean supportsDefaultTarget) {
        int additionalArgs;
        StringBuilder message = new StringBuilder();

        if (supportsDefaultTarget) {
            additionalArgs = 1; // task name
        } else {
            additionalArgs = 2; // task name and target
        }

        // We may require the server name, which means at least length 2.
        // We also may have other required arguments.
        if (args.length < (reqArgs.size() + additionalArgs)) {
            message.append(getMessage("insufficientArgs"));
            message.append(NL);
        }
        if (!supportsDefaultTarget) {
            if (args.length < 2 || args[1].startsWith("-")) {
                message.append(getMessage("missingServerName"));
                message.append(NL);
            }
        }

        for (String reqArg : reqArgs) {
            String lowerReqArg = reqArg.toLowerCase();

            boolean argFound = false;
            for (String arg : args) {
                if (arg.toLowerCase().startsWith(lowerReqArg)) {
                    argFound = true;
                    break;
                }
            }
            if (!argFound) {
                message.append(getMessage("missingArg", reqArg));
                message.append(NL);
            }
        }

        if (!message.toString().isEmpty()) {
            throw new IllegalArgumentException(message.toString());
        }
    }

    /**
     * Gets the value for the specified argument String. If the default
     * value argument is null, it indicates the argument is required.
     *
     * No validation is done in the format of args as it was done previously.
     *
     * @param arg Argument name to resolve a value for
     * @param args List of arguments
     * @param defalt Default value if the argument is not specified
     * @return Value of the argument, or null if there was no value.
     * @throws IllegalArgumentException if the argument is defined but no value
     *             is given.
     */
    protected String getArgumentValue(String arg, String[] args, String defalt) {
        String argName = getArgName(arg);
        for (int i = 1; i < args.length; i++) {
            String currentArgName = getArgName(args[i]); // return what's to left of = if there is one
            if (currentArgName.equalsIgnoreCase(argName)) {
                return getValue(args[i]);
            }
        }
        return defalt;
    }

    /**
     * Generate the formatted task help.
     *
     * @param desc the description NLS key
     * @param usage the usage NLS key
     * @param optionKeyPrefix the option name NLS key prefix
     * @param optionDescPrefix the option description NLS key prefix
     * @param addonKey an addon NLS key prefix
     * @param footer a raw (already translated) String to append to the output
     * @param args any arguments to pass to the formating keys (order matters)
     * @return
     */
    protected String getTaskHelp(String desc, String usage,
                                 String optionKeyPrefix, String optionDescPrefix,
                                 String addonKey, String footer,
                                 Object... args) {

        StringBuilder scriptHelp = new StringBuilder();
        scriptHelp.append(NL);       
        scriptHelp.append(getOption("global.description"));
        scriptHelp.append(NL);
        scriptHelp.append(getOption(desc));
        scriptHelp.append(NL);
        // print a empty line
        scriptHelp.append(NL);
        scriptHelp.append(getOption("global.usage"));
        scriptHelp.append(NL);
        scriptHelp.append(getOption(usage));
        scriptHelp.append(NL);

        String options = buildScriptOptions(optionKeyPrefix, optionDescPrefix);
        if (!options.isEmpty()) {
            // print a empty line
            scriptHelp.append(NL);

            scriptHelp.append(getOption("global.options"));

            scriptHelp.append(options);
        }

        if (addonKey != null && !addonKey.isEmpty()) {
            // print a empty line
            scriptHelp.append(NL);
            scriptHelp.append(getOption(addonKey));
        }

        if (footer != null && !footer.isEmpty()) {
            scriptHelp.append(footer);
        }
        scriptHelp.append(NL);

        if (args.length == 0) {
            return scriptHelp.toString();
        } else {
            return MessageFormat.format(scriptHelp.toString(), args);
        }
    }

    protected void abort(CommandConsole console,String message) {
        console.printlnInfoMessage(getMessage("generateWebServerPluginTask.abort"));
        console.printlnErrorMessage(message);
    }

    
}

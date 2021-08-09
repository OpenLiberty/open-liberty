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
package com.ibm.ws.security.utility.tasks;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.ws.security.utility.SecurityUtilityTask;
import com.ibm.ws.security.utility.utils.CommandUtils;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

public abstract class BaseCommandTask implements SecurityUtilityTask {

    public static final String NL = System.getProperty("line.separator");

    protected final String scriptName;

    public BaseCommandTask(String scriptName) {
        this.scriptName = scriptName;
    }

    protected String getMessage(String key, Object... args) {
        return CommandUtils.getMessage(key, args);
    }

    protected String getOption(String key, boolean forceFormat, Object... args) {
        return CommandUtils.getOption(key, forceFormat, args);
    }

    /**
     * Constructs the options segment for a script's help screen for the given
     * optionKeyPrefix and optionDescPrefix.
     *
     * @param optionKeyPrefix
     * @param optionDescPrefix
     * @return formatted output String for the set of options
     */
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
                // Print each option and it's associated descriptive text
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
                                 String requiredKeyPrefix, String requiredDescPrefix,
                                 String optionKeyPrefix, String optionDescPrefix,
                                 String addonKey, String footer, Object... args) {

        StringBuilder scriptHelp = new StringBuilder();
        scriptHelp.append(getOption("global.usage", false));
        scriptHelp.append(NL);
        scriptHelp.append(getOption(usage, false));
        scriptHelp.append(NL);

        // print a empty line
        scriptHelp.append(NL);

        scriptHelp.append(getOption("global.description", false));
        scriptHelp.append(NL);
        scriptHelp.append(getOption(desc, false));
        scriptHelp.append(NL);

        String requireds = buildScriptOptions(requiredKeyPrefix, requiredDescPrefix);

        if (!requireds.isEmpty()) {
            // print a empty line
            scriptHelp.append(NL);

            scriptHelp.append(getOption("global.required", false));

            scriptHelp.append(requireds);

        }
        // print a empty line
        scriptHelp.append(NL);

        String options = buildScriptOptions(optionKeyPrefix, optionDescPrefix);
        if (!options.isEmpty()) {
            // print a empty line
            scriptHelp.append(NL);

            scriptHelp.append(getOption("global.options", false));

            scriptHelp.append(options);
        }

        if (addonKey != null && !addonKey.isEmpty()) {
            // print a empty line
            scriptHelp.append(NL);
            scriptHelp.append(getOption(addonKey, false));
        }

        if (footer != null && !footer.isEmpty()) {
            scriptHelp.append(footer);
        }

        return MessageFormat.format(scriptHelp.toString(), args);
    }

    /**
     * Returns the value at i+1, guarding against ArrayOutOfBound exceptions.
     * If the next element is not a value but an argument flag (starts with -)
     * return null.
     *
     * @return String value as defined above, or {@code null} if at end of args.
     */
    protected String getValue(String arg) {
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
     * Prompt the user to enter password.
     * Prompts twice and compares to ensure it was entered correctly.
     *
     * @return Entered String
     */
    protected String promptForPassword(ConsoleWrapper stdin, PrintStream stdout) {
        return promptForText(stdin, stdout,
                             "password.enterText", "password.reenterText",
                             "password.readError", "password.entriesDidNotMatch");
    }

    /**
     * Prompt the user to enter text to encode.
     * Prompts twice and compares to ensure it was entered correctly.
     *
     * @return Entered String
     */
    protected String promptForText(ConsoleWrapper stdin, PrintStream stdout) {
        return promptForText(stdin, stdout,
                             "encode.enterText", "encode.reenterText",
                             "encode.readError", "encode.entriesDidNotMatch");
    }

    /**
     * Prompt the user to enter text.
     * Prompts twice and compares to ensure it was entered correctly.
     *
     * @return Entered String
     */
    private String promptForText(ConsoleWrapper stdin, PrintStream stdout,
                                 String enterText, String reenterText,
                                 String readError, String entriesDidNotMatch) {
        String read1 = stdin.readMaskedText(getMessage(enterText) + " ");
        String read2 = stdin.readMaskedText(getMessage(reenterText) + " ");
        if (read1 == null && read2 == null) {
            throw new IllegalArgumentException("Unable to read either entry. Aborting prompt.");
        } else if (read1 == null || read2 == null) {
            stdout.println(getMessage(readError));
            return promptForText(stdin, stdout, enterText, reenterText, readError, entriesDidNotMatch);
        } else if (read1.equals(read2)) {
            return read1;
        } else {
            stdout.println(getMessage(entriesDidNotMatch));
            return promptForText(stdin, stdout, enterText, reenterText, readError, entriesDidNotMatch);
        }
    }

    /**
     * Gets the value for the specified argument String. If the default
     * value argument is null, it indicates the argument is required.
     *
     * No validation is done in the format of args as it is assumed to
     * have been done previously.
     *
     * @param arg Argument name to resolve a value for
     * @param args List of arguments. Assumes the script name is included and therefore minimum length is 2.
     * @param defalt Default value if the argument is not specified
     * @param passwordArgKey The password argument key (required to know when to prompt for password)
     * @param stdin Standard in interface
     * @param stdout Standard out interface
     * @return Value of the argument
     * @throws IllegalArgumentException if the argument is defined but no value is given.
     */
    protected String getArgumentValue(String arg, String[] args,
                                      String defalt, String passwordArgKey,
                                      ConsoleWrapper stdin, PrintStream stdout) {
        for (int i = 1; i < args.length; i++) {
            String key = args[i].split("=")[0];
            if (key.equals(arg)) {
                String value = getValue(args[i]);
                if (arg.equals(passwordArgKey) && value == null) {
                    return promptForPassword(stdin, stdout);
                } else {
                    return value;
                }
            }
        }
        return defalt;
    }

    /**
     * Checks if the argument is a known argument to the task.
     *
     * @param arg The argument key (does not include the = or the value)
     * @return {@code true} if the argument key is known to the task, {@code false} otherwise
     */
    abstract boolean isKnownArgument(String arg);

    /**
     * Check we have the required parameters before proceeding.
     *
     * @param args The script invocation arguments
     * @throws IllegalArgumentException If a required argument is missing
     */
    abstract void checkRequiredArguments(String[] args) throws IllegalArgumentException;

    /**
     * Validates that there are no unknown arguments or values specified
     * to the task.
     *
     * @param args The script arguments
     * @parma keyOnlyArgs The List of argument keys which do not require a value
     * @throws IllegalArgumentException if an argument is defined is unknown
     */
    protected void validateArgumentList(String[] args, List<String> keyOnlyArgs) {
        checkRequiredArguments(args);

        // Skip the first argument as it is the task name
        // Arguments and values come in pairs (expect -password).
        // Anything outside of that pattern is invalid.
        // Loop through, jumping in pairs except when we encounter
        // -password -- that may be an interactive prompt which won't
        // define a value.
        for (int i = 1; i < args.length; i++) {
            String argPair = args[i];
            String arg = null;
            String value = null;
            if (argPair.contains("=")) {
                arg = argPair.split("=")[0];
                value = getValue(argPair);
            } else {
                arg = argPair;
            }

            if (!isKnownArgument(arg)) {
                throw new IllegalArgumentException(getMessage("invalidArg", arg));
            } else {
                if (!keyOnlyArgs.contains(arg) && (value == null)) {
                    throw new IllegalArgumentException(getMessage("missingValue", arg));
                }
            }
        }
    }

}

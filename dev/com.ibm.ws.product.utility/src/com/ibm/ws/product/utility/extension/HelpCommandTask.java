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
package com.ibm.ws.product.utility.extension;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ibm.ws.product.utility.BaseCommandTask;
import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.product.utility.CommandConstants;
import com.ibm.ws.product.utility.CommandTask;
import com.ibm.ws.product.utility.ExecutionContext;

public class HelpCommandTask extends BaseCommandTask {

    public static final String HELP_TASK_NAME = "help";

    /** {@inheritDoc} */
    @Override
    public Set<String> getSupportedOptions() {
        return new HashSet<String>(Arrays.asList("compare", "featureInfo", "viewLicenseInfo", "viewLicenseAgreement", "version", "validate"));
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskName() {
        return HELP_TASK_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskDescription() {
        return getOption("help.desc");
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskHelp() {
        return super.getTaskHelp("help.desc", "help.usage.options", null, null, null);
    }

    /** {@inheritDoc} */
    @Override
    public void doExecute(ExecutionContext context) {
        if (context.getArguments().length == 0) {
            doVerboseScriptUsage(context);
        } else if (context.getArguments().length == 1) {
            doTaskHelp(context, context.getArguments()[0]);
        }
    }

    public void doScriptUsage(ExecutionContext context, boolean error) {
        CommandConsole commandConsole = context.getCommandConsole();
        printlnMessage(commandConsole, "", error);

        CommandTask[] commandTasks = context.getCommandTaskRegistry().getCommandTasks();
        StringBuilder scriptUsage = new StringBuilder();
        boolean separatorRequired = false;
        scriptUsage.append(MessageFormat.format(getMessage("usage"), context.getAttribute(CommandConstants.SCRIPT_NAME, String.class)));
        scriptUsage.append(" {");
        for (CommandTask commandTask : commandTasks) {
            if (separatorRequired) {
                scriptUsage.append("|");
            } else {
                separatorRequired = true;
            }
            scriptUsage.append(commandTask.getTaskName());
        }
        scriptUsage.append("} [options]");
        printlnMessage(commandConsole, scriptUsage.toString(), error);
    }

    private void printlnMessage(CommandConsole commandConsole, String message, boolean error) {
        if (error) {
            commandConsole.printlnErrorMessage(message);
        } else {
            commandConsole.printlnInfoMessage(message);
        }
    }

    private void doTaskHelp(ExecutionContext context, String taskName) {
        CommandConsole commandConsole = context.getCommandConsole();

        CommandTask commandTask = context.getCommandTaskRegistry().getCommandTask(taskName);
        if (commandTask == null) {
            commandConsole.printlnErrorMessage("");
            commandConsole.printlnErrorMessage(getMessage("ERROR_UNKNOWN_COMMAND_TASK", taskName));
            return;
        }

        commandConsole.printlnInfoMessage("");
        String scriptName = context.getAttribute(CommandConstants.SCRIPT_NAME, String.class);
        commandConsole.printlnInfoMessage(MessageFormat.format(commandTask.getTaskHelp(), scriptName));
    }

    private void doVerboseScriptUsage(ExecutionContext context) {
        doScriptUsage(context, false);

        CommandConsole commandConsole = context.getCommandConsole();

        StringBuilder verboseHelp = new StringBuilder();
        verboseHelp.append(CommandConstants.LINE_SEPARATOR);
        verboseHelp.append(getMessage("tasks"));
        verboseHelp.append(CommandConstants.LINE_SEPARATOR);
        for (CommandTask commandTask : context.getCommandTaskRegistry().getCommandTasks()) {
            verboseHelp.append(CommandConstants.LINE_SEPARATOR);
            verboseHelp.append("    " + commandTask.getTaskName());
            verboseHelp.append(CommandConstants.LINE_SEPARATOR);
            verboseHelp.append(commandTask.getTaskDescription());
            verboseHelp.append(CommandConstants.LINE_SEPARATOR);
        }
        verboseHelp.append(CommandConstants.LINE_SEPARATOR);
        verboseHelp.append(getOption("global.options"));
        verboseHelp.append(CommandConstants.LINE_SEPARATOR);
        verboseHelp.append(getOption("global.options.statement"));

        commandConsole.printlnInfoMessage(verboseHelp.toString());
    }

    @Override
    protected boolean validateArguments(ExecutionContext context) {
        boolean optionsValid = true;
        Set<String> supportedOptions = new LinkedHashSet<String>();
        supportedOptions.add(CommandConstants.OUTPUT_FILE_OPTION);
        supportedOptions.addAll(getSupportedOptions());
        String supportedOptionsString = null;
        Set<String> suppliedOptions = context.getOptionNames();
        for (String option : suppliedOptions) {
            if (!supportedOptions.contains(option)) {
                optionsValid = false;
                if (supportedOptionsString == null) {
                    supportedOptionsString = getSupportedOptionsString(supportedOptions);
                }
                context.getCommandConsole().printlnErrorMessage(getMessage("ERROR_INVALID_COMMAND_OPTION", option, supportedOptionsString));
            }
        }
        return optionsValid;
    }
}

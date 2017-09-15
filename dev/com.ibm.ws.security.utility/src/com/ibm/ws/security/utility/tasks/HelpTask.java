/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
import java.util.List;

import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.SecurityUtilityTask;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

/**
 *
 */
public class HelpTask extends BaseCommandTask {

    private final List<SecurityUtilityTask> tasks;

    public HelpTask(String scriptName, List<SecurityUtilityTask> tasks) {
        super(scriptName);
        this.tasks = tasks;
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskName() {
        return "help";
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskHelp() {
        return getTaskHelp("help.desc", "help.usage.options", null, null,
                           null, null,
                           null, null,
                           scriptName);
    }

    @Override
    public String getTaskDescription() {
        return getOption("help.desc", true);
    }

    /**
     * {@inheritDoc} Prints the usage statement. The format is:
     *
     * <pre>
     * Usage: {tasks|...} [arguments]
     * </pre>
     *
     */
    public String getScriptUsage() {
        StringBuffer scriptUsage = new StringBuffer(NL);
        scriptUsage.append(getMessage("usage", scriptName));
        scriptUsage.append(" {");
        for (int i = 0; i < tasks.size(); i++) {
            SecurityUtilityTask task = tasks.get(i);
            scriptUsage.append(task.getTaskName());
            if (i != (tasks.size() - 1)) {
                scriptUsage.append("|");
            }
        }
        scriptUsage.append("} [options]");
        scriptUsage.append(NL);
        return scriptUsage.toString();
    }

    /**
     * Constructs a string to represent the usage for a particular task.
     *
     * @param task
     * @return
     */
    public String getTaskUsage(SecurityUtilityTask task) {
        StringBuffer taskUsage = new StringBuffer(NL);
        taskUsage.append(NL);
        taskUsage.append(task.getTaskHelp());
        return taskUsage.toString();
    }

    /**
     * Constructs a string to represent the verbose help, which lists the
     * usage for all tasks.
     *
     * @return String for the verbose help message
     */
    private String verboseHelp() {
        StringBuffer verboseHelp = new StringBuffer(getScriptUsage());
        if (tasks.size() > 0) {
            verboseHelp.append(NL);
            verboseHelp.append(getOption("global.actions", true));
            verboseHelp.append(NL);
            for (SecurityUtilityTask task : tasks) {
                verboseHelp.append(NL);
                verboseHelp.append("    ");
                verboseHelp.append(task.getTaskName());
                verboseHelp.append(NL);
                verboseHelp.append(task.getTaskDescription());
                verboseHelp.append(NL);
            }
            verboseHelp.append(NL);
            verboseHelp.append(getOption("global.options", true));
            verboseHelp.append(NL);
            verboseHelp.append(getOption("global.options.statement", true));
        }
        return verboseHelp.toString();
    }

    /**
     * Constructs a string to represent the help for a particular task.
     *
     * @param task
     * @return
     */
    private String taskHelp(SecurityUtilityTask task) {
        StringBuffer taskUsage = new StringBuffer(NL);
        taskUsage.append(task.getTaskHelp());
        taskUsage.append(NL);
        return taskUsage.toString();
    }

    /**
     * Given a task name, return the corresponding SecurityUtilityTask.
     *
     * @param taskName desired task name
     * @return corresponding SecurityUtilityTask, or null if
     *         no match is found
     */
    private SecurityUtilityTask getTask(String taskName) {
        SecurityUtilityTask task = null;
        for (SecurityUtilityTask availTask : tasks) {
            if (availTask.getTaskName().equals(taskName)) {
                task = availTask;
            }
        }
        return task;
    }

    /** {@inheritDoc} */
    @Override
    public SecurityUtilityReturnCodes handleTask(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr, String[] args) {
        if (args.length == 1) {
            stdout.println(verboseHelp());
        } else {
            SecurityUtilityTask task = getTask(args[1]);
            if (task == null) {
                stderr.println(NL +
                               getMessage("task.unknown", args[1]) +
                               NL);
            } else {
                stdout.println(taskHelp(task));
            }
        }

        return SecurityUtilityReturnCodes.OK;
    }

    /** {@inheritDoc} */
    @Override
    boolean isKnownArgument(String arg) {
        // validateArgumentList is not used by this implementation
        return false;
    }

    /** {@inheritDoc} */
    @Override
    void checkRequiredArguments(String[] args) {
        // validateArgumentList is not used by this implementation
    }
}

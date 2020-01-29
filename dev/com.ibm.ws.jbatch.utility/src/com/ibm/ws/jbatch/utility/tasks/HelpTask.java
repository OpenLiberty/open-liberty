/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.utility.tasks;

import java.io.PrintStream;

import com.ibm.ws.jbatch.utility.JBatchUtilityTask;
import com.ibm.ws.jbatch.utility.utils.ConsoleWrapper;
import com.ibm.ws.jbatch.utility.utils.StringUtils;
import com.ibm.ws.jbatch.utility.utils.TaskList;

/**
 *
 */
public class HelpTask extends BaseCommandTask {

    /**
     * List of registered tasks.
     */
    private final TaskList tasks;

    /**
     * CTOR.
     */
    public HelpTask(String scriptName, TaskList tasks) {
        super("help", scriptName);
        this.tasks = tasks;
    }

    /** 
     * {@inheritDoc} 
     */
    @Override
    public String getTaskHelp() {
        return joinMsgs( getUsage("help.usage.options", scriptName, getTaskName()),
                         getDesc("help.desc") );
    }

    /**
     * {@inheritDoc} Prints the usage statement. The format is:
     * <pre>
     * Usage: {tasks|...} [arguments]
     * </pre>
     * 
     */
    public String getScriptUsage() {
        
        StringBuffer scriptUsage = new StringBuffer(NL);
        scriptUsage.append(getMessage("usage", scriptName));
        scriptUsage.append(" {");
        scriptUsage.append( StringUtils.join( tasks.getTaskNames(), "|" ) );
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
    public String getTaskUsage(JBatchUtilityTask task) {
        
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
            verboseHelp.append(getOption("global.actions"));
            verboseHelp.append(NL);
            for (JBatchUtilityTask task : tasks) {
                verboseHelp.append(NL);
                verboseHelp.append("    ");
                verboseHelp.append(task.getTaskName());
                verboseHelp.append(NL);
                verboseHelp.append(task.getTaskDescription());
                verboseHelp.append(NL);
            }
            verboseHelp.append(NL);
            verboseHelp.append(getOption("global.options"));
            verboseHelp.append(NL);
            verboseHelp.append(getOption("global.options.statement"));
        }
        return verboseHelp.toString();
    }

    /**
     * Constructs a string to represent the help for a particular task.
     * 
     * @param task
     * @return
     */
    private String taskHelp(JBatchUtilityTask task) {
        return task.getTaskHelp() + NL;
    }

    /** 
     * {@inheritDoc} 
     */
    @Override
    public int handleTask(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr, String[] args) {
        if (args.length == 1) {
            stdout.println(verboseHelp());
        } else {
            JBatchUtilityTask task = tasks.forName(args[1]);
            if (task == null) {
                stderr.println(NL +
                               getMessage("task.unknown", args[1]) +
                               NL);
            } else {
                stdout.println(taskHelp(task));
            }
        }
        
        return 0;
    }
}

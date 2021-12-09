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
package com.ibm.ws.security.utility;

import java.io.PrintStream;

import com.ibm.ws.security.utility.utils.ConsoleWrapper;

/**
 * Defines the access point for all child tasks so the main task controller
 * can query and invoke the available tasks.
 */
public interface SecurityUtilityTask {

    /**
     * Answers the name of the task, which should be as succinct as possible.
     * The task name is used in help display and is how the task is invoked
     * by the script user.
     *
     * @return the name of the task
     */
    String getTaskName();

    /**
     * Answers the help message for the task, which is used by the script
     * help statement. This message should be more verbose than the usage
     * statement, and should explain the required and optional arguments
     * that the task supports.
     * <p>
     * Limit the output to 80 characters per line and include all formatting,
     * including tabs and newlines. Wrapping newlines should not be included.
     *
     * @return the help message for the task
     */
    String getTaskHelp();

    /**
     * Answer the description of of the task, which will be used in help display
     * to show what does the task do.
     *
     * @return the description of the task
     */
    String getTaskDescription();

    /**
     * Perform the task logic.
     *
     * @param stdin handle to standard input wrapper
     * @param stdout handle to standard output
     * @param stderr handle to standard error
     * @param args The arguments passed to the script, including the task name
     * @return Return code to be returned by the utility
     * @throws IllegalArgumentException if the task was called with invalid arguments
     */
    SecurityUtilityReturnCodes handleTask(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr, String[] args) throws Exception;
}

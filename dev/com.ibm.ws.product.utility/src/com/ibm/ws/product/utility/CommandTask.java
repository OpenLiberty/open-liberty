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
package com.ibm.ws.product.utility;

import java.util.Set;

public interface CommandTask {

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
     * Returns a set of command line options that are supported by this task.
     *
     * @return the set of supported options
     */
    Set<String> getSupportedOptions();

    /**
     * Perform the task logic.
     *
     * @param context
     * @throws IllegalArgumentException if the task was called with invalid arguments
     */
    void execute(ExecutionContext context);
}

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
package com.ibm.ws.kernel.boot.cmdline;

import java.io.PrintStream;

/**
 * Defines the access point for all child tasks so the main task controller
 * can query and invoke the available tasks.
 */
public interface ActionHandler {

    /**
     * Perform the action logic.
     * 
     * @param stdout handle to standard output
     * @param stderr handle to standard error
     * @param args The arguments passed to the script.
     * @throws IllegalArgumentException if the task was called with invalid arguments
     */
    ExitCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args);
}

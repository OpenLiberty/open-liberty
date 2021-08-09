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

import java.util.List;

/**
 * The definition of a command line action. Allows retrieval of known
 * options and required positional arguments for verification.
 */
public interface ActionDefinition {

    /** Return fixed array of required command options */
    public List<String> getCommandOptions();

    /** Return the number of expected positional arguments for command line verification */
    public int numPositionalArgs();

    /**
     * Perform the action logic, usually by delegating to an internal {@link ActionHandler}
     * 
     * @param args The arguments passed to the script.
     * @throws IllegalArgumentException if the task was called with invalid arguments
     */
    public ExitCode handleTask(Arguments args);
}

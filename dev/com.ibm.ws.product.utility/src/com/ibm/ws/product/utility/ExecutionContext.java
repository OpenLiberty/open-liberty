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

import java.io.PrintStream;
import java.util.Set;

/**
 *
 */
public interface ExecutionContext {

    public CommandConsole getCommandConsole();

    /**
     * Return the arguments input from the console, will NOT contain the task name
     *
     * @return
     */
    public String[] getArguments();

    /**
     * Returns a set of all option names specified to the execution environment.
     *
     * @return a set of all specified options
     */
    public Set<String> getOptionNames();

    public String getOptionValue(String option);

    public boolean optionExists(String option);

    public CommandTaskRegistry getCommandTaskRegistry();

    public <T> T getAttribute(String name, Class<T> cls);

    public Object getAttribute(String name);

    public void setAttribute(String name, Object value);

    public void setOverrideOutputStream(PrintStream outputStream);
}
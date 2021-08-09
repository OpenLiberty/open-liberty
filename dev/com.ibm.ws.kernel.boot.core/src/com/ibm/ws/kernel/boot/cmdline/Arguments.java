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
 * The arguments used to invoke this command line tool. It handles options denoted using --name[=value]
 * and positional arguments which lack these.
 */
public interface Arguments {
    /**
     * @return the list of positional arguments
     */
    public List<String> getPositionalArguments();

    /**
     * @param name the name of the option.
     * @return the value. If no value was provided on the command line the empty string is returned.
     */
    public String getOption(String name);

    /**
     * Allows the caller to validate that the expected options were provided. If an option is provided that
     * is not passed in here then the Arguments are considered bad and the tool should exit using the
     * appropriate error code.
     * 
     * @param expectedArgs The list of expected arguments.
     * @return list of invalid arguments or null of none were found
     */
    List<String> findInvalidOptions(List<String> expectedOptions);

    /**
     * @return The action selected when arguments were parsed.
     */
    String getAction();
}
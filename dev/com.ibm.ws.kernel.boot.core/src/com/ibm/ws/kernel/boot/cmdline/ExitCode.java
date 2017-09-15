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

/**
 * Exit code with an integer value is required for command line calls
 * to System.exit. For consistent command line behavior across all platforms:
 * <ul>
 * <li>0 for successful execution</li>
 * <li>1-19 reserved for other successful executions. An example is the status command,
 * which returns 0 if the server is running, 1 if the server is stopped, and 2 if we can't
 * tell. Each is unique, none are errors.</li>
 * <li>20 for invalid command line arguments</li>
 * <li>21-256 for other invalid conditions</li>
 * <li>Do not expose negative exit code values: they can be used for internal control,
 * but should not be exposed to customers via System.exit().</li>
 * </ul>
 */
public interface ExitCode {

    final int OK = 0;
    final int BAD_ARGUMENT = 20;

    final int ERROR_BAD_JAVA_VERSION = 30;
    final int ERROR_BAD_JAVA_BITMODE = 31;
    final int ERROR_UNKNOWN_EXCEPTION_CMD = 32;

    /**
     * @return The integer exit code for use with System.exit();
     */
    int getValue();
}

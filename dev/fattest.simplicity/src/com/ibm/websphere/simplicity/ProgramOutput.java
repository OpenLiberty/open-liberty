/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.simplicity;

/**
 * This class is a container to hold output information for command line command results
 */
public class ProgramOutput {

	private String command = "";
    private String stdout = "";
    private String stderr = "";
    private int returnCode;
    
    public ProgramOutput(String command, int returnCode, String stdout, String stderr) {
    	this.command = command;
    	this.returnCode = returnCode;
    	this.stdout = stdout;
    	this.stderr = stderr;
    }

    /**
     * @return The shell command executed on the target machine.
     */
    public String getCommand() {
    	return command;
    }
    
    /**
     * Returns the command line return code
     * 
     * @return The command line return code
     */
    public int getReturnCode() {
        return returnCode;
    }

    /**
     * Get the output written to stderr
     * 
     * @return stderr
     */
    public String getStderr() {
        return stderr;
    }

    /**
     * Get the output written to stdout
     * 
     * @return stdout
     */
    public String getStdout() {
        return stdout;
    }
    
}

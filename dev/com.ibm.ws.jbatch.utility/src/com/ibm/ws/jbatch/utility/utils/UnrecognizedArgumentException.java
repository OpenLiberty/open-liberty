/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.utility.utils;

/**
 * Thrown when a supplied argument is not expected by the task.
 */
public class UnrecognizedArgumentException extends RuntimeException {

    /**
     * The unexpected arg
     */
    private String unrecognizedArg;
    
    /**
     * The expected argument that the user may have meant to specify.
     * This will be filled in only if the unexpectedArg is a close match
     * to the expectedArg (e.g if it's only missing a leading "-").
     */
    private String expectedArg;
    
    public UnrecognizedArgumentException(String unrecognizedArg) {
        this(unrecognizedArg, null);
    }
    
    public UnrecognizedArgumentException(String unrecognizedArg, String expectedArg) {
        this.unrecognizedArg = unrecognizedArg;
        this.expectedArg = expectedArg;
    }

    /**
     * @return expectedArg
     */
    public String getExpectedArg() {
        return expectedArg;
    }

    /**
     * @return unrecognizedArg
     */
    public String getUnrecognizedArg() {
        return unrecognizedArg;
    }
    
}

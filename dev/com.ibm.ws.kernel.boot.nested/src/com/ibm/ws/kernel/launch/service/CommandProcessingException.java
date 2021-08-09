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
package com.ibm.ws.kernel.launch.service;

/**
 * Exception representing failure during Server Command Processing
 */
public class CommandProcessingException extends Exception {

    /** serial version id for this exception. */
    static final long serialVersionUID = -6243464083666116670L;

    /**
     * Creates an exception with the provided message.
     * 
     * @param message The error message.
     */
    public CommandProcessingException(String message) {
        super(message);
    }

    /**
     * Creates an exception from the provided cause.
     * 
     * @param cause the cause of the current exception
     */
    public CommandProcessingException(Throwable cause) {
        super(cause);
    }

}

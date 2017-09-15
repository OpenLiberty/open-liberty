/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security;

/**
 * Thrown to indicate that the userId/Password combination does not exist
 * in the specified custom registry.
 * 
 * @ibm-spi
 */
public class PasswordCheckFailedException extends WSSecurityException {

    private static final long serialVersionUID = 3640506429677174874L; //@vj1: Take versioning into account if incompatible changes are made to this class

    /**
     * Create a new PasswordCheckFailedException with an empty description string.
     */
    public PasswordCheckFailedException() {
        super();
    }

    /**
     * Create a new PasswordCheckFailedException with the associated string description.
     * 
     * @param message the String describing the exception.
     */
    public PasswordCheckFailedException(String message) {
        super(message);
    }

    /**
     * Create a new PasswordCheckFailedException with the Throwable root cause.
     * 
     * @param t the Throwable root cause.
     */
    public PasswordCheckFailedException(Throwable t) {
        super(t);
    }

    /**
     * Create a new PasswordCheckFailedException with the string description and Throwable root cause.
     * 
     * @param message the String describing the exception.
     * @param t the Throwable root cause.
     */
    public PasswordCheckFailedException(String message, Throwable t) {
        super(message, t);
    }

}

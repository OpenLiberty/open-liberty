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
 * Thrown to indicate that the specified entry is not found in the
 * custom registry.
 * 
 * @ibm-spi
 */
public class EntryNotFoundException extends WSSecurityException {

    private static final long serialVersionUID = 5789163023036418269L; //@vj1: Take versioning into account if incompatible changes are made to this class

    /**
     * Create a new EntryNotFoundException with an empty description string.
     */
    public EntryNotFoundException() {
        super();
    }

    /**
     * Create a new EntryNotFoundException with the associated string description.
     * 
     * @param message the String describing the exception.
     */
    public EntryNotFoundException(String message) {
        super(message);
    }

    /**
     * Create a new EntryNotFoundException with the associated Throwable root cause.
     * 
     * @param t the Throwable root cause
     */
    public EntryNotFoundException(Throwable t) {
        super(t);
    }

    /**
     * Create a new EntryNotFoundException with the string description and Throwable root cause.
     * 
     * @param message the String describing the exception.
     * @param t the Throwable root cause.
     */
    public EntryNotFoundException(String message, Throwable t) {
        super(message, t);
    }

}

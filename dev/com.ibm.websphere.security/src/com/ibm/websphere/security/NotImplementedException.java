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
 * Thrown to indicate that the method is not implemented.
 * 
 * @ibm-spi
 */
public class NotImplementedException extends WSSecurityException {

    private static final long serialVersionUID = 1680889074992585609L; //@vj1: Take versioning into account if incompatible changes are made to this class

    /**
     * Create a new NotImplementedException with an empty description string.
     */
    public NotImplementedException() {
        super();
    }

    /**
     * Create a new NotImplementedException with the associated string description.
     * 
     * @param message the String describing the exception.
     */
    public NotImplementedException(String name) {
        super(name);
    }

    /**
     * Create a new NotImplementedException with the Throwable root cause.
     * 
     * @param t the Throwable root cause.
     */
    public NotImplementedException(Throwable t) {
        super(t);
    }

    /**
     * Create a new NotImplementedException with the string description and Throwable root cause.
     * 
     * @param message the String describing the exception.
     * @param t the Throwable root cause.
     */
    public NotImplementedException(String message, Throwable t) {
        super(message, t);
    }

}

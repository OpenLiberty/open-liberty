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
 * Thrown to indicate that a error occurred while using the
 * specified custom registry.
 * 
 * @ibm-spi
 */

public class CustomRegistryException extends WSSecurityException {

    private static final long serialVersionUID = -7806436129183927093L; //@vj1: Take versioning into account if incompatible changes are made to this class

    /**
     * Create a new CustomRegistryException with an empty description string.
     */
    public CustomRegistryException() {
        super();
    }

    /**
     * Create a new CustomRegistryException with the associated string description.
     * 
     * @param message the String describing the exception.
     */
    public CustomRegistryException(String message) {
        super(message);
    }

    /**
     * Create a new CustomRegistryException with the associated Throwable root cause.
     * 
     * @param t the Throwable root cause.
     */
    public CustomRegistryException(Throwable t) {
        super(t);
    }

    /**
     * Create a new CustomRegistryException with the string description and Throwable root cause.
     * 
     * @param message the String describing the exception.
     * @param t the Throwable root cause.
     */
    public CustomRegistryException(String message, Throwable t) {
        super(message, t);
    }

}

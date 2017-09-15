/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry;

/**
 * Generic Exception type for UserRegistry issues. Any UserRegistry
 * implementation specific problem should be reported back via a
 * thrown RegistryException.
 */
public class RegistryException extends Exception {
    private static final long serialVersionUID = 1L;

    // Implementation note:
    // No default constructor should be provided.
    // A RegistryException without a message is meaningless.

    /**
     * @see java.lang.Exception#Exception(String)
     */
    public RegistryException(String msg) {
        super(msg);
    }

    /**
     * @see java.lang.Exception#Exception(String, Throwable)
     */
    public RegistryException(String msg, Throwable cause) {
        super(msg, cause);
    }

}

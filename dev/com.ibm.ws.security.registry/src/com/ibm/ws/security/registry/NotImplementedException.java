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

package com.ibm.ws.security.registry;

/**
 * Thrown by UserRegistry to indicate no such method is implemented
 * in the UserRegistry.
 */
public class NotImplementedException extends Exception {
    private static final long serialVersionUID = 1L;

    // Implementation note:
    // No default constructor should be provided:
    // An EntryNotFoundException should inform the caller which entry could not be found.

    /**
     * @see java.lang.Exception#Exception(String)
     */
    public NotImplementedException(String msg) {
        super(msg);
    }

    /**
     * @see java.lang.Exception#Exception(String, Throwable)
     */
    public NotImplementedException(String msg, Throwable cause) {
        super(msg, cause);
    }

}

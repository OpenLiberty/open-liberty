/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.wim.exception;

public class ChangeControlException extends WIMApplicationException {

    private static final long serialVersionUID = 3843615929049817327L;

    /**
     * Constructs a ChangeControlException with no message key, no detail message, and no cause.
     */
    public ChangeControlException() {
        super();
    }

    /**
     * Constructs a ChangeControlException with the specified message key and detail message.
     *
     * @param key The message key.
     * @param message The detail message.
     */
    public ChangeControlException(String key, String message) {
        super(key, message);
    }

    /**
     * Constructs a ChangeControlException with the specified cause.
     *
     * @param cause The cause.
     */
    public ChangeControlException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a ChangeControlException with the specified message key, detail message, and cause.
     *
     * @param key The message key.
     * @param message The detail message.
     * @param cause The cause.
     */
    public ChangeControlException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

}

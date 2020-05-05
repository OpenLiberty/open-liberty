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

public class PasswordCheckFailedException extends WIMApplicationException {

    private static final long serialVersionUID = -9067114908519104286L;

    /**
     * Constructs a PasswordCheckFailedException with no message key, no detail message, and no cause.
     */
    public PasswordCheckFailedException() {
        super();

    }

    /**
     * Constructs a PasswordCheckFailedException with the specified message key and detail message.
     *
     * @param key The message key.
     * @param message The detail message.
     */
    public PasswordCheckFailedException(String key, String message) {
        super(key, message);
    }

    /**
     * Constructs a PasswordCheckFailedException with the specified cause.
     *
     * @param cause The cause.
     */
    public PasswordCheckFailedException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a PasswordCheckFailedException with the specified message key, detail message, and cause.
     *
     * @param key The message key.
     * @param message The detail message.
     * @param cause The cause.
     */
    public PasswordCheckFailedException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

}

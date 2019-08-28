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
package com.ibm.wsspi.security.oauth20;

/**
 *
 */
public class UserIdentityException extends Exception {
    static final long serialVersionUID = -2287516993124229949L;

    public UserIdentityException() {
        super();
    }

    public UserIdentityException(String message) {
        super(message);
    }

    public UserIdentityException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserIdentityException(Throwable cause) {
        super(cause);
    }
}

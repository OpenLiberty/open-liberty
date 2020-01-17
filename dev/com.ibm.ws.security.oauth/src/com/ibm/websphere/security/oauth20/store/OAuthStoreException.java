/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.oauth20.store;

/**
 * Exception thrown by the <code>OAuthStore</code> implementation when it cannot perform the requested operation.
 */
public class OAuthStoreException extends Exception {

    private static final long serialVersionUID = 3322869018137833604L;

    /**
     * Constructs a new <code>OAuthStoreException</code> with the given message.
     */
    public OAuthStoreException(String message) {
        super(message);
    }

    /**
     * Constructs a new <code>OAuthStoreException</code> with the given message and cause.
     */
    public OAuthStoreException(String message, Throwable cause) {
        super(message, cause);
    }

}

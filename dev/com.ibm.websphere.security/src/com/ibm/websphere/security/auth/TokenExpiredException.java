/*******************************************************************************
 * Copyright (c) 1997, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.auth;

/**
 * This exception is thrown when a token is no longer valid.
 * 
 * @ibm-api
 * @author IBM
 * @version 1.0
 * @ibm-spi
 */
public class TokenExpiredException extends com.ibm.websphere.security.WSSecurityException {
    private static final long serialVersionUID = 3704719086095702906L;
    private long expiration = 0;

    public TokenExpiredException() {
        super();
    }

    public TokenExpiredException(String debug_message) {
        super(debug_message);
    }

    public TokenExpiredException(Throwable t) {
        super(t);
    }

    public TokenExpiredException(String debug_message, Throwable t) {
        super(debug_message, t);
    }

    public TokenExpiredException(long expiration, String debug_message) {
        super(debug_message);
        this.expiration = expiration;
    }

    public long getExpiration() {
        return expiration;
    }
}

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
 * This exception is thrown whenever authentication fails.
 * 
 * @ibm-api
 * @author IBM
 * @version 1.0
 * @ibm-spi
 */
public class InvalidTokenException extends com.ibm.websphere.security.WSSecurityException {

    private static final long serialVersionUID = 5335739079891689755L;

    public InvalidTokenException() {
        super();
    }

    public InvalidTokenException(String debug_message) {
        super(debug_message);
    }

    public InvalidTokenException(Throwable t) {
        super(t);
    }

    public InvalidTokenException(String debug_message, Throwable t) {
        super(debug_message, t);
    }

}

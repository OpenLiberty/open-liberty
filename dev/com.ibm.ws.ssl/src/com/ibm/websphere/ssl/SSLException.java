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

package com.ibm.websphere.ssl;

/**
 * <p>
 * This is a generic exception thrown for most SSL-related errors.
 * </p>
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since WAS 6.1
 * @see com.ibm.websphere.ssl.JSSEHelper
 * @ibm-api
 **/
public class SSLException extends Exception {
    private static final long serialVersionUID = -3236620232328367856L;

    /**
     * Constructor.
     */
    public SSLException() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param cause
     */
    public SSLException(Exception cause) {
        super(cause);
    }

    /**
     * Constructor.
     * 
     * @param message
     */
    public SSLException(String message) {
        super(message);
    }

    /**
     * Constructor.
     * 
     * @param message
     * @param cause
     */
    public SSLException(String message, Exception cause) {
        super(message, cause);
    }

}

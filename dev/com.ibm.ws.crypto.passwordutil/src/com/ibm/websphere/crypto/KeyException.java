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
package com.ibm.websphere.crypto;

/**
 * <p>
 * This is a generic exception thrown for most key management-related errors.
 * </p>
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since WAS 6.1
 * @see com.ibm.websphere.ssl.JSSEHelper
 * @ibm-api
 **/
public class KeyException extends Exception {
    private static final long serialVersionUID = 7626200077347108110L;

    /**
     * Constructor.
     */
    public KeyException() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param cause
     */
    public KeyException(Exception cause) {
        super(cause);
    }

    /**
     * Constructor.
     * 
     * @param message
     */
    public KeyException(String message) {
        super(message);
    }

    /**
     * Constructor.
     * 
     * @param message
     * @param cause
     */
    public KeyException(String message, Exception cause) {
        super(message, cause);
    }

}

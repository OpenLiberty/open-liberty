/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.filter;

public class FilterException extends Exception {

    private static final long serialVersionUID = -1508488910426331394L;

    public FilterException() {
        super();
    }

    /**
     * @param message
     */
    public FilterException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public FilterException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public FilterException(Throwable cause) {
        super(cause);
    }

}

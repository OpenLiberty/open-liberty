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

public class PropertyNotDefinedException extends WIMApplicationException {

    private static final long serialVersionUID = -1164308229190880710L;

    /**
     *
     */
    public PropertyNotDefinedException() {
        super();
    }

    /**
     * @param message
     */
    public PropertyNotDefinedException(String key, String message) {
        super(key, message);
    }

    /**
     * @param cause
     */
    public PropertyNotDefinedException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public PropertyNotDefinedException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

}

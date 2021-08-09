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
package com.ibm.websphere.security.web;

import javax.servlet.ServletException;

/**
 * This exception is thrown when a programmatic login is performed against a user registry which is configured
 * to report when a user's password is expired.
 */
public class PasswordExpiredException extends ServletException{

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 12345622414392883L;
    
    /**
     * Creates a new PasswordExpiredException with a message
     *
     * @param message Exception message about what caused the error
     */
    public PasswordExpiredException(String message) {
        super(message);
    }
}

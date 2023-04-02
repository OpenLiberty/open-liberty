/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication;

/**
 *
 */
public class PasswordExpiredException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public PasswordExpiredException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param e
     */
    public PasswordExpiredException(String message, Exception e) {
        super(message, e);
    }

}

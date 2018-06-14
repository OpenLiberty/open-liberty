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
package com.ibm.ws.security.registry;

/**
 *
 */
public class PasswordExpiredException extends RegistryException {

    /**  */
    private static final long serialVersionUID = 1L;

    /**
     * @param msg
     */
    public PasswordExpiredException(String msg) {
        super(msg);
        // TODO Auto-generated constructor stub
    }

    public PasswordExpiredException(String msg, Throwable cause) {
        super(msg, cause);
    }

}

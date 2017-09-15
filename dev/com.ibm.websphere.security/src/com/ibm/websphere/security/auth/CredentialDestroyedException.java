/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
 * <p>
 * Thrown if credential is destroyed. A destroyed credential can not be used at all.
 * </p>
 * 
 * @ibm-api
 * @author IBM Corporation
 * @version 1.0
 * @see java.security.GeneralSecurityException
 * @since 1.0
 * @ibm-spi
 */

public class CredentialDestroyedException extends Exception {
    private static final long serialVersionUID = 1L;

    public CredentialDestroyedException(String msg) {
        super(msg);
    }

    public CredentialDestroyedException(Throwable t) {
        super(t);
    }

    public CredentialDestroyedException(String debug_message, Throwable t) {
        super(debug_message, t);
    }
}

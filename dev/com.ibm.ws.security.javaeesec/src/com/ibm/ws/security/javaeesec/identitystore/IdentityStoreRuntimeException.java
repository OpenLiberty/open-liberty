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
package com.ibm.ws.security.javaeesec.identitystore;

/**
 * A RuntimeException thrown by the DatabaseIdentityStore and LdapIdentityStore.
 */
public class IdentityStoreRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IdentityStoreRuntimeException(String msg) {
        super(msg);
    }

    public IdentityStoreRuntimeException(String msg, Throwable t) {
        super(msg, t);
    }

}

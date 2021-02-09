/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.util;

import java.security.GeneralSecurityException;

public class UnauthenticatedException extends GeneralSecurityException {
    private static final long serialVersionUID = 98764567532541290L;

    public UnauthenticatedException() {
    }

    public UnauthenticatedException(String msg) {
        super(msg);
    }

    public UnauthenticatedException(Throwable cause) {
        super(cause);
    }

    public UnauthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }
}

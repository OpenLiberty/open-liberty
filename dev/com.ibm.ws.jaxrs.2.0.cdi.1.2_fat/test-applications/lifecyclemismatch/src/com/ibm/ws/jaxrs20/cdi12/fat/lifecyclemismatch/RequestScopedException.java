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
package com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemismatch;

public class RequestScopedException extends Exception {

    private static final long serialVersionUID = -1975560538784455458L;

    public RequestScopedException() {
        super();
    }

    public RequestScopedException(String message) {
        super(message);
    }

    public RequestScopedException(Throwable cause) {
        super(cause);
    }

    public RequestScopedException(String message, Throwable cause) {
        super(message, cause);
    }
}

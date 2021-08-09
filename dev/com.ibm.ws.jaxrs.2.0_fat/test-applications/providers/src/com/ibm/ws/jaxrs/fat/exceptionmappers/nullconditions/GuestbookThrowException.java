/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.exceptionmappers.nullconditions;

public class GuestbookThrowException extends Exception {

    private static final long serialVersionUID = 1L;

    public GuestbookThrowException() {
        super();
    }

    public GuestbookThrowException(String message) {
        super(message);
    }

    public GuestbookThrowException(Throwable cause) {
        super(cause);
    }

    public GuestbookThrowException(String message, Throwable cause) {
        super(message, cause);
    }
}

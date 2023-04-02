/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.fat.exceptionmappers.nullconditions;

public class GuestbookNullException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GuestbookNullException() {
        super();
    }

    public GuestbookNullException(String message) {
        super(message);
    }

    public GuestbookNullException(Throwable cause) {
        super(cause);
    }

    public GuestbookNullException(String message, Throwable cause) {
        super(message, cause);
    }
}

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
package com.ibm.ws.jaxrs.fat.exceptionmappers.mapped;

public class GuestbookDatabaseException extends Exception {

    private static final long serialVersionUID = 3656497291087821230L;

    public GuestbookDatabaseException() {
        super();
    }

    public GuestbookDatabaseException(String message) {
        super(message);
    }

    public GuestbookDatabaseException(Throwable cause) {
        super(cause);
    }

    public GuestbookDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

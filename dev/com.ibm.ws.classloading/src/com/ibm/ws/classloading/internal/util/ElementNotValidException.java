/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal.util;

public class ElementNotValidException extends ElementNotFetchedException {
    private static final long serialVersionUID = 1L;

    public ElementNotValidException() {
        super();
    }

    public ElementNotValidException(String message, Throwable cause) {
        super(message, cause);
    }

    public ElementNotValidException(String message) {
        super(message);
    }

    public ElementNotValidException(Throwable cause) {
        super(cause);
    }
}

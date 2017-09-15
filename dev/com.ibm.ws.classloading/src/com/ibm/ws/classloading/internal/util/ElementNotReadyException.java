/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal.util;

public class ElementNotReadyException extends ElementNotFetchedException {
    private static final long serialVersionUID = 1L;

    public ElementNotReadyException() {}

    public ElementNotReadyException(String message) {
        super(message);
    }

    public ElementNotReadyException(Throwable cause) {
        super(cause);
    }

    public ElementNotReadyException(String message, Throwable cause) {
        super(message, cause);
    }

}

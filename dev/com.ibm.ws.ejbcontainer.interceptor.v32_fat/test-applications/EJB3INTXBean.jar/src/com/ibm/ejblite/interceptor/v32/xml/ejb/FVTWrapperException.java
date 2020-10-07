/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejblite.interceptor.v32.xml.ejb;

/**
 * FVTWrapperException is a simple exception wrapper class used in the
 * Interceptor FAT to test checked exception handling.
 */

public class FVTWrapperException extends Exception {
    private static final long serialVersionUID = 2784403751784682731L;

    public FVTWrapperException() {
        super();
    }

    public FVTWrapperException(String message) {
        super(message);
    }

    public FVTWrapperException(String message, Throwable cause) {
        super(message, cause);
    }

    public FVTWrapperException(Throwable cause) {
        super(cause);
    }

    @Override
    public Throwable getCause() {
        return super.getCause();
    }
}
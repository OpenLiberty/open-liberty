/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.diagnostics.puscanner;

public class PersistenceUnitScannerException extends Exception {
    private static final long serialVersionUID = -5233597954879762961L;

    public PersistenceUnitScannerException() {
    }

    public PersistenceUnitScannerException(String message) {
        super(message);
    }

    public PersistenceUnitScannerException(Throwable cause) {
        super(cause);
    }

    public PersistenceUnitScannerException(String message, Throwable cause) {
        super(message, cause);
    }

    public PersistenceUnitScannerException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}

/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
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

package com.ibm.ws.jpa.diagnostics.class_scanner.ano;

public class ClassScannerException extends Exception {
    private static final long serialVersionUID = -9104837619165320112L;

    public ClassScannerException() {
    }

    public ClassScannerException(String message) {
        super(message);
    }

    public ClassScannerException(Throwable cause) {
        super(cause);
    }

    public ClassScannerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClassScannerException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}

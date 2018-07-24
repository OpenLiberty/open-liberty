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

package com.ibm.ws.jpa.diagnostics.puparser;

public class PersistenceUnitParserException extends Exception {
    private static final long serialVersionUID = -1482168559365962149L;

    public PersistenceUnitParserException() {
    }

    public PersistenceUnitParserException(String message) {
        super(message);
    }

    public PersistenceUnitParserException(Throwable cause) {
        super(cause);
    }

    public PersistenceUnitParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public PersistenceUnitParserException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}

/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20;

/**
 * Represents a problem which occurred while reading the modules from an application
 */
public class ApplicationReadException extends Exception {

    private static final long serialVersionUID = -3196443502443015385L;

    public ApplicationReadException() {
        super();
    }

    public ApplicationReadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ApplicationReadException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplicationReadException(String message) {
        super(message);
    }

    public ApplicationReadException(Throwable cause) {
        super(cause);
    }

}

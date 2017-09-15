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

package com.ibm.ws.microprofile.config.interfaces;

public class ConversionException extends ConfigException {

    /**
     * The exception was thrown when unable to convert to the specified type.
     */
    private static final long serialVersionUID = 1L;

    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(Throwable throwable) {
        super(throwable);
    }

    public ConversionException(String message, Throwable throwable) {
        super(message, throwable);
    }
}

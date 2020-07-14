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

import io.openliberty.microprofile.config.internal.common.ConfigException;

/**
 * Thrown if errors occur during value conversion that are unrelated to the input value.
 * If there is a problem with the input value then an IllegalArgumentException should be thrown.
 */
public class ConversionException extends ConfigException {

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

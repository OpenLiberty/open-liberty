/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.converters;

import java.time.format.DateTimeParseException;

import com.ibm.ws.microprofile.config.interfaces.ConversionException;

/**
 *
 */
public class DateTimeConverter extends AutomaticConverter {

    public DateTimeConverter(Class<?> type) {
        super(type);
    }

    /** {@inheritDoc} */
    @Override
    public Object convert(String value) {
        Object converted = null;
        try {
            converted = super.convert(value);
        } catch (ConversionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof DateTimeParseException) {
                throw new IllegalArgumentException(cause);
            } else {
                throw e;
            }
        }

        return converted;
    }
}

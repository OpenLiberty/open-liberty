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
package com.ibm.ws.microprofile.config12.converters;

import com.ibm.ws.microprofile.config.converters.BuiltInConverter;
import com.ibm.ws.microprofile.config.converters.ExtendedGenericConverter;
import com.ibm.ws.microprofile.config.impl.ConversionManager;

/**
 * Convert from the String name of a class to an actual Class, as loaded by Class.forName
 */
public class ClassConverter extends BuiltInConverter implements ExtendedGenericConverter {

    public ClassConverter() {
        super(Class.class);
    }

    /** {@inheritDoc} */
    @Override
    public Class<?> convert(String rawString) {
        Class<?> converted = null;
        if (rawString != null) {
            try {
                converted = Class.forName(rawString);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return converted;
    }

    /** {@inheritDoc} */
    @Override
    public <T> Object convert(String rawString, Class<T> genericType, ConversionManager conversionManager, ClassLoader classLoader) {
        Class<?> converted = null;
        if (rawString != null) {
            try {
                converted = classLoader.loadClass(rawString);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return converted;
    }

}

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

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.ibm.ws.microprofile.config.impl.ConversionManager;

/**
 *
 */
public class OptionalConverter extends BuiltInConverter implements ExtendedGenericConverter {

    public OptionalConverter() {
        super(Optional.class);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<String> convert(String value) {
        //optional identity function
        return Optional.ofNullable(value);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> convert(String rawString, Class<T> genericType, ConversionManager conversionManager, ClassLoader classLoader) {
        T value = null;
        if (!ConfigProperty.UNCONFIGURED_VALUE.equals(rawString)) {
            value = (T) conversionManager.convert(rawString, genericType);
        }
        return Optional.ofNullable(value);
    }
}

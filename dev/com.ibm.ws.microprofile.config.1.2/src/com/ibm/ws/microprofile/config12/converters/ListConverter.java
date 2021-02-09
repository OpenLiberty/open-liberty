/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config12.converters;

import java.util.Arrays;
import java.util.List;

import com.ibm.ws.microprofile.config.converters.BuiltInConverter;
import com.ibm.ws.microprofile.config.converters.ExtendedGenericConverter;
import com.ibm.ws.microprofile.config.impl.ConversionManager;

/**
 *
 */
public class ListConverter extends BuiltInConverter implements ExtendedGenericConverter {

    public ListConverter() {
        super(List.class);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> convert(String value) {
        //shouldn't ever get called but if it does, the best we can do is return a String list
        List<String> list = Arrays.asList(ConversionManager.split(value));
        return list;
    }

    /** {@inheritDoc} */
    @Override
    public <T> List<Object> convert(String rawString, Class<T> genericTypeNullable, ConversionManager conversionManager, ClassLoader classLoader) {
        // If the generic type is not specified, use String by default.
        // E.g. if the user called: Config.getConfig().getValue("foo", List.class);
        Class<?> genericType = genericTypeNullable;
        if (genericType == null) {
            genericType = String.class;
        }
        Object[] array = conversionManager.convertArray(rawString, genericType);
        List<Object> list = Arrays.asList(array);

        return list;
    }
}

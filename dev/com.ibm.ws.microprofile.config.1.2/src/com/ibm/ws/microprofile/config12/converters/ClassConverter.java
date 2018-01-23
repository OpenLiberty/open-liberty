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

/**
 * Convert from the String name of a class to an actual Class, as loaded by Class.forName
 */
public class ClassConverter extends BuiltInConverter {

    public ClassConverter() {
        super(Class.class);
    }

    /** {@inheritDoc} */
    @Override
    public Class<?> convert(String value) {
        Class<?> converted = null;
        if (value != null) {
            try {
                converted = Class.forName(value);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return converted;
    }

}

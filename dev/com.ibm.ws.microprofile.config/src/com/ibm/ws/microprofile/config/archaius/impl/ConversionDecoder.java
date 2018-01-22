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
package com.ibm.ws.microprofile.config.archaius.impl;

import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;
import com.ibm.ws.microprofile.config.impl.ConversionManager;
import com.netflix.archaius.api.Decoder;

/**
 *
 */
public class ConversionDecoder extends ConversionManager implements Decoder {

    /**
     * Constructor
     *
     * @param converters
     */
    public ConversionDecoder(PriorityConverterMap converters, ClassLoader classLoader) {
        super(converters, classLoader);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T decode(Class<T> type, String encoded) {
        return (T) convert(encoded, type);
    }

}

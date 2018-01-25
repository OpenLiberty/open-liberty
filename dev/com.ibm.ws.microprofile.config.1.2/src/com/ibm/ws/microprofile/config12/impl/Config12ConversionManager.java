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

package com.ibm.ws.microprofile.config12.impl;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.config.archaius.impl.ConversionDecoder;
import com.ibm.ws.microprofile.config.converters.AutomaticConverter;
import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;
import com.ibm.ws.microprofile.config.impl.ConversionStatus;
import com.ibm.ws.microprofile.config.interfaces.ConverterNotFoundException;

public class Config12ConversionManager extends ConversionDecoder {

    /**
     * @param converters all the converters to use
     */
    public Config12ConversionManager(PriorityConverterMap converters) {
        super(converters);
    }

    /**
     * Attempt to apply a valueOf or T(String s) constructor
     *
     * @param rawString
     * @param type
     * @return a converted T object
     */
    @Override
    @FFDCIgnore(ConverterNotFoundException.class)
    protected <T> ConversionStatus implicitConverters(String rawString, Class<T> type) {
        ConversionStatus status = new ConversionStatus();

        try {
            AutomaticConverter automaticConverter = new AutomaticConverter(type);
            Object converted = automaticConverter.convert(rawString);
            status.setConverted(converted);
        } catch (ConverterNotFoundException e) {
            //no FFDC
        }

        return status;
    }

}

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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.config.converters.BuiltInConverter;
import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;
import com.ibm.ws.microprofile.config.impl.ConversionManager;
import com.ibm.ws.microprofile.config.impl.ConversionStatus;
import com.ibm.ws.microprofile.config12.converters.ImplicitConverter;

import io.openliberty.microprofile.config.internal.common.ConfigException;

public class Config12ConversionManager extends ConversionManager {

    private static final TraceComponent tc = Tr.register(Config12ConversionManager.class);

    /**
     * @param converters all the converters to use
     */
    public Config12ConversionManager(PriorityConverterMap converters, ClassLoader classLoader) {
        super(converters, classLoader);
    }

    /**
     * Attempt to apply a valueOf or T(String s) constructor
     *
     * @param rawString
     * @param type
     * @return a converted T object
     */
    @Override
    protected <T> ConversionStatus implicitConverters(String rawString, Class<T> type) {
        ConversionStatus status = new ConversionStatus();
        BuiltInConverter automaticConverter = getConverter(type);

        //will be null if a suitable string constructor method could not be found
        if (automaticConverter != null) {
            try {
                Object converted = automaticConverter.convert(rawString);
                status.setConverted(converted);
            } catch (IllegalArgumentException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "implicitConverters: An automatic converter for type ''{0}'' and raw String ''{1}'' threw an exception: {2}.", type, rawString, e);
                }
                throw e;
            } catch (Throwable t) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "implicitConverters: An automatic converter for type ''{0}'' and raw String ''{1}'' threw an exception: {2}.", type, rawString, t);
                }
                throw new ConfigException(t);
            }
        }

        return status;
    }

    @FFDCIgnore(IllegalArgumentException.class)
    protected <T> BuiltInConverter getConverter(Class<T> type) {
        ImplicitConverter automaticConverter = null;

        try {
            automaticConverter = new ImplicitConverter(type);
        } catch (IllegalArgumentException e) {
            //no FFDC
            //this means that a suitable string constuctor method could not be found for the given class
            //ignore the exception and return null
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getConverter (INFO): An automatic converter for type ''{0}'' could not be constructed: {2}.", type, e);
            }
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getConverter: An automatic converter for type ''{0}'' could not be constructed: {2}.", type, t);
            }
            throw new ConfigException(t);
        }

        return automaticConverter;
    }

}

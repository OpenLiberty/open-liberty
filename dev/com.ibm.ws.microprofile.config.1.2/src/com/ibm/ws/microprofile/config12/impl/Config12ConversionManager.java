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

package com.ibm.ws.microprofile.config12.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;
import com.ibm.ws.microprofile.config.impl.ConversionManager;
import com.ibm.ws.microprofile.config.impl.ConversionStatus;
import com.ibm.ws.microprofile.config12.converters.ImplicitConverter;

import io.openliberty.microprofile.config.internal.common.ConfigException;

public class Config12ConversionManager extends ConversionManager {

    private static final TraceComponent tc = Tr.register(Config12ConversionManager.class);

    private final Map<Class<?>, ImplicitConverter> implicitConverterCache = Collections.synchronizedMap(new HashMap<>());

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
        ImplicitConverter implicitConverter = getImplicitConverter(type);

        //will be null if a suitable string constructor method could not be found
        if (implicitConverter != null) {
            try {
                Object converted = implicitConverter.convert(rawString);
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

    protected <T> ImplicitConverter getImplicitConverter(Class<T> type) {
        ImplicitConverter implicitConverter = implicitConverterCache.get(type);

        if (implicitConverter == null) {
            implicitConverter = newImplicitConverter(type);
            implicitConverterCache.put(type, implicitConverter);
        }
        return implicitConverter;
    }

    @FFDCIgnore(IllegalArgumentException.class)
    protected <T> ImplicitConverter newImplicitConverter(Class<T> type) {
        ImplicitConverter implicitConverter = null;
        try {
            implicitConverter = new ImplicitConverter(type);
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
        return implicitConverter;
    }

}

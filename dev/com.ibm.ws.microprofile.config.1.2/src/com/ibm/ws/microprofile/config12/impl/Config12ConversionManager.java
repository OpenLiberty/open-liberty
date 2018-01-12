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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.config.archaius.impl.ConversionDecoder;
import com.ibm.ws.microprofile.config.converters.AutomaticConverter;
import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;
import com.ibm.ws.microprofile.config.impl.ConversionStatus;
import com.ibm.ws.microprofile.config.interfaces.ConverterNotFoundException;

public class Config12ConversionManager extends ConversionDecoder {

    private static final TraceComponent tc = Tr.register(Config12ConversionManager.class);

    /**
     * @param converters all the converters to use
     */
    public Config12ConversionManager(PriorityConverterMap converters) {
        super(converters);
    }

    /**
     * Convert a String to a Type using registered converters for the Type
     *
     * @param <T>
     *
     * @param rawString
     * @param type
     * @return
     */
    @Override
    public <T extends Type> Object convert(String rawString, T type) {
        //do a simple lookup in the map of converters
        ConversionStatus status = simpleConversion(rawString, type);

        //check if the type is a List or Set
        if (!status.isConverterFound() && type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type rawType = paramType.getRawType();
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (rawType == List.class) {
                status = convertList(rawString, typeArgs[0]);
            } else if (rawType == Set.class) {
                status = convertSet(rawString, typeArgs[0]);
            }
        }

        if (!status.isConverterFound() && type instanceof Class) {
            Class<?> requestedClazz = (Class<?>) type;

            //try array conversion
            if (requestedClazz.isArray()) {
                Class<?> arrayType = requestedClazz.getComponentType();
                status = convertArray(rawString, arrayType);// convertArray will throw ConverterNotFoundException if it can't find a converter
            }

            //try implicit converters (e.g. String Constructors)
            if (!status.isConverterFound()) {
                status = implicitConverters(rawString, requestedClazz);
            }

            //try to find any compatible converters (e.g. asked for type Fruit but only have a converter for Apple)
            if (!status.isConverterFound()) {
                status = convertCompatible(rawString, requestedClazz);
            }

        }

        //if nothing was found then throw an exception
        if (!status.isConverterFound()) {
            throw new ConverterNotFoundException(Tr.formatMessage(tc, "could.not.find.converter.CWMCG0014E", type.getTypeName()));
        }

        Object converted = status.getConverted();
        return converted;
    }

    /**
     * Attempt to apply a valueOf or T(String s) constructor
     *
     * @param rawString
     * @param type
     * @return a converted T object
     */
    @FFDCIgnore(ConverterNotFoundException.class)
    private <T> ConversionStatus implicitConverters(String rawString, Class<T> type) {
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

    /**
     * Apply convert across a list
     *
     * @param rawString
     * @param listType
     * @return an list of converted objects.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private ConversionStatus convertList(String rawString, Type listType) {
        ConversionStatus status = new ConversionStatus();

        String[] elements = rawString.split(",");
        List list = new ArrayList();
        try {
            for (String element : elements) {
                Object converted = convert(element, listType); // will throw ConverterNotFoundException if it can't find a converter
                list.add(converted);
            }
            status.setConverted(list);
        } catch (ConverterNotFoundException e) {
            //No FFDC
        }

        return status;
    }

    /**
     * Apply convert across a set
     *
     * @param rawString
     * @param setType
     * @return an set of converted objects.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private ConversionStatus convertSet(String rawString, Type setType) {
        ConversionStatus status = new ConversionStatus();

        String[] elements = rawString.split(",");
        Set set = new HashSet();
        try {
            for (String element : elements) {
                Object converted = convert(element, setType); // will throw ConverterNotFoundException if it can't find a converter
                set.add(converted);
            }
            status.setConverted(set);
        } catch (ConverterNotFoundException e) {
            //No FFDC
        }

        return status;
    }

}

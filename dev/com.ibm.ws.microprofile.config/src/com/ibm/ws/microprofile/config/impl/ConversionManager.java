/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//This class was inspired by com.netflix.archaius.DefaultDecoder

package com.ibm.ws.microprofile.config.impl;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.config.converters.PriorityConverter;
import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;
import com.ibm.ws.microprofile.config.interfaces.ConfigException;
import com.ibm.ws.microprofile.config.interfaces.ConversionException;
import com.ibm.ws.microprofile.config.interfaces.ConverterNotFoundException;

public class ConversionManager {

    private static final TraceComponent tc = Tr.register(ConversionManager.class);

    private final PriorityConverterMap converters;

    /**
     * @param converters all these are stored in the instance
     */
    public ConversionManager(PriorityConverterMap converters) {
        this.converters = converters;
        this.converters.setUnmodifiable(); //probably already done but make sure
    }

    protected <T extends Type> ConversionStatus simpleConversion(String rawString, T type) {
        ConversionStatus status = new ConversionStatus();
        if (converters.hasType(type)) {
            PriorityConverter converter = converters.getConverter(type);
            if (converter != null) {
                try {
                    Object converted = converter.convert(rawString);
                    status.setConverted(converted);
                } catch (ConversionException e) {
                    throw e;
                } catch (IllegalArgumentException e) {
                    throw new ConversionException(Tr.formatMessage(tc, "conversion.exception.CWMCG0007E", converter, rawString, e));
                } catch (Throwable e) {
                    throw new ConfigException(Tr.formatMessage(tc, "conversion.exception.CWMCG0007E", converter, rawString, e));
                }

                if (status.isConverterFound() && status.getConverted() == null) {
                    if (TraceComponent.isAnyTracingEnabled()) {
                        Tr.debug(tc, "The converted value is null. The rawString is " + rawString);
                    }
                }

            }
        }
        return status;
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
    public <T extends Type> Object convert(String rawString, T type) {
        //do a simple lookup in the map of converters
        ConversionStatus status = simpleConversion(rawString, type);

        if (!status.isConverterFound() && type instanceof Class) {
            Class<?> requestedClazz = (Class<?>) type;

            //try array conversion
            if (requestedClazz.isArray()) {
                Class<?> arrayType = requestedClazz.getComponentType();
                status = convertArray(rawString, arrayType);// convertArray will throw ConverterNotFoundException if it can't find a converter
            }

            //try to find any compatible converters
            if (!status.isConverterFound()) {
                status = convertCompatible(rawString, requestedClazz);
            }

        }

        if (!status.isConverterFound()) {
            throw new ConverterNotFoundException(Tr.formatMessage(tc, "could.not.find.converter.CWMCG0014E", type.getTypeName()));
        }

        Object converted = status.getConverted();
        return converted;

    }

    /**
     * Converts from String based on isAssignableFrom or instanceof
     *
     * @param rawString
     * @param type
     * @return ConversionStatus<T> whether a converter is found and the converted value
     */
    protected ConversionStatus convertCompatible(String rawString, Class<?> type) {
        ConversionStatus status = new ConversionStatus();
        for (PriorityConverter con : converters.getAll()) {
            Type key = con.getType();
            if (key instanceof Class) {
                Class<?> clazz = (Class<?>) key;
                if (type.isAssignableFrom(clazz)) {
                    Object converted = convert(rawString, key);
                    status.setConverted(converted);
                    break;
                }
            } else if (key instanceof TypeVariable) {
                TypeVariable<?> typeVariable = (TypeVariable<?>) key;
                status = convertGenericClazz(rawString, type, typeVariable);
                if (status.isConverterFound()) {
                    break;
                }
            }
        }
        return status;

    }

    /**
     * Front end to (T)convert(rawString, typeVariable) if isAssignableFrom compatible Converter present
     *
     * @param rawString
     * @param type
     * @param typeVariable
     * @return ConversionStatus<T> whether a converter is found and the converted value
     */
    @SuppressWarnings("unchecked")
    private <T> ConversionStatus convertGenericClazz(String rawString, Class<T> type, TypeVariable<?> typeVariable) {
        ConversionStatus status = new ConversionStatus();
        AnnotatedType[] bounds = typeVariable.getAnnotatedBounds();
        for (AnnotatedType bound : bounds) {
            Type bType = bound.getType();
            if (bType instanceof Class) {
                Class<?> bClazz = (Class<?>) bType;
                if (bClazz.isAssignableFrom(type)) {
                    T converted = (T) convert(rawString, typeVariable);
                    status.setConverted(converted);
                    break;
                }
            }
        }

        return status;
    }

    /**
     * Apply convert across an array
     *
     * @param rawString
     * @param arrayType
     * @return an array of converted T objects.
     */
    @SuppressWarnings("unchecked")
    @FFDCIgnore(ConverterNotFoundException.class)
    protected <T> ConversionStatus convertArray(String rawString, Class<T> arrayType) {
        ConversionStatus status = new ConversionStatus();

        String[] elements = rawString.split(",");
        T[] array = (T[]) Array.newInstance(arrayType, elements.length);
        try {
            for (int i = 0; i < elements.length; i++) {
                array[i] = (T) convert(elements[i], arrayType); // will throw ConverterNotFoundException if it can't find a converter
            }
            status.setConverted(array);
        } catch (ConverterNotFoundException e) {
            //No FFDC
        }

        return status;
    }

}

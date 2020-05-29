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
//This class was originally inspired by com.netflix.archaius.DefaultDecoder

package com.ibm.ws.microprofile.config.impl;

import java.lang.ref.WeakReference;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.apache.commons.lang3.ClassUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.common.ConfigException;
import com.ibm.ws.microprofile.config.converters.ExtendedGenericConverter;
import com.ibm.ws.microprofile.config.converters.PriorityConverter;
import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;

public class ConversionManager {

    private static final TraceComponent tc = Tr.register(ConversionManager.class);

    private final PriorityConverterMap converters;

    private final WeakReference<ClassLoader> classLoader;

    /**
     * @param converters all these are stored in the instance
     */
    public ConversionManager(PriorityConverterMap converters, ClassLoader classLoader) {
        this.converters = converters;
        this.converters.setUnmodifiable(); //probably already done but make sure
        this.classLoader = new WeakReference<>(classLoader);
    }

    protected ConversionStatus simpleConversion(String rawString, Type type, Class<?> genericSubType) {
        ConversionStatus status = new ConversionStatus();

        if (this.converters.hasType(type)) {
            PriorityConverter converter = this.converters.getConverter(type);
            if (converter != null) {
                Object converted = null;
                try {
                    if (converter instanceof ExtendedGenericConverter) {
                        converted = ((ExtendedGenericConverter) converter).convert(rawString, genericSubType, this, this.classLoader.get());
                    } else {
                        converted = converter.convert(rawString);
                    }
                    status.setConverted(converted);
                } catch (IllegalArgumentException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "simpleConversion: A converter ''{0}'', for type ''{1}'', sub type ''{2}'' and raw String '{3}' threw an exception: {4}.", converter, type,
                                 genericSubType, rawString, e);
                    }
                    throw e;
                } catch (Throwable t) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "simpleConversion: A converter ''{0}'', for type ''{1}'', sub type ''{2}'' and raw String ''{3}'' threw an exception: {4}.", converter, type,
                                 genericSubType, rawString, t);
                    }
                    throw new ConfigException(Tr.formatMessage(tc, "conversion.exception.CWMCG0007E", type, rawString, t.getMessage()), t);
                }

                if (status.isConverterFound() && (status.getConverted() == null)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "simpleConversion: The converted value is null. The rawString is {0}", rawString);
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
    public Object convert(String rawString, Type type) {
        Object value = convert(rawString, type, null);
        return value;
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
    public Object convert(String rawString, Type type, Class<?> genericSubType) {

        Object converted = null;

        if ((genericSubType == null) && (type instanceof ParameterizedType)) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] aTypes = pType.getActualTypeArguments();
            Type genericTypeArg = aTypes.length == 1 ? aTypes[0] : null; //initially only support one type arg
            if (genericTypeArg instanceof Class) { //for the moment we only support a class ... so no type variables; e.g. List<String> is ok but List<T> is not
                Class<?> genericClassArg = (Class<?>) genericTypeArg;
                converted = convert(rawString, pType.getRawType(), genericClassArg);
            } else {
                throw new IllegalArgumentException(Tr.formatMessage(tc, "generic.type.variables.notsupported.CWMCG0018E", type, genericTypeArg));
            }
        } else {
            //first box any primitives
            if (type instanceof Class<?>) {
                Class<?> clazz = (Class<?>) type;
                if (clazz.isPrimitive()) {
                    type = ClassUtils.primitiveToWrapper(clazz);
                }
            }

            //do a simple lookup in the map of converters
            ConversionStatus status = simpleConversion(rawString, type, genericSubType);

            if (!status.isConverterFound() && (type instanceof Class)) {
                Class<?> requestedClazz = (Class<?>) type;

                //try array conversion
                if (requestedClazz.isArray()) {
                    Class<?> arrayType = requestedClazz.getComponentType();
                    Class<?> conversionType = arrayType;
                    //first convert primitives to wrapper types
                    if (arrayType.isPrimitive()) {
                        conversionType = ClassUtils.primitiveToWrapper(arrayType);
                    }
                    //convert to an array of the wrapper type
                    Object[] wrappedArray = convertArray(rawString, conversionType);// convertArray will throw ConverterNotFoundException if it can't find a converter

                    //switch back to the primitive type if required
                    Object value = wrappedArray;
                    if (arrayType.isPrimitive()) {
                        value = toPrimitiveArray(wrappedArray, arrayType);
                    }

                    status.setConverted(value);
                }

                //try implicit converters (e.g. String Constructors)
                if (!status.isConverterFound()) {
                    status = implicitConverters(rawString, requestedClazz);
                }

                //try to find any compatible converters
                if (!status.isConverterFound()) {
                    status = convertCompatible(rawString, requestedClazz);
                }

            }

            if (!status.isConverterFound()) {
                throw new IllegalArgumentException(Tr.formatMessage(tc, "could.not.find.converter.CWMCG0014E", type.getTypeName()));
            }

            converted = status.getConverted();
        }
        return converted;

    }

    protected static <T> Object toPrimitiveArray(Object[] array, Class<T> primitiveType) {
        Object primArray = Array.newInstance(primitiveType, array.length);
        for (int i = 0; i < array.length; i++) {
            Array.set(primArray, i, array[i]);
        }
        return primArray;
    }

    /**
     * @param <T>
     * @param rawString
     * @param requestedClazz
     * @return
     */
    protected <T> ConversionStatus implicitConverters(String rawString, Class<T> requestedClazz) {
        // no-op in this version
        return new ConversionStatus();
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
        for (PriorityConverter con : this.converters.getAll()) {
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
    public <T> T[] convertArray(String rawString, Class<T> arrayType) {
        String[] elements = split(rawString);
        T[] array = convertArray(elements, arrayType);

        return array;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] convertArray(String[] elements, Class<T> arrayType) {
        T[] array = (T[]) Array.newInstance(arrayType, elements.length);
        for (int i = 0; i < elements.length; i++) {
            array[i] = (T) convert(elements[i], arrayType); // will throw ConverterNotFoundException if it can't find a converter
        }

        return array;
    }

    public static String[] split(String rawString) {
        String[] elements = null;
        if (rawString != null) {
            //split on comma "," unless preceeded by the escape char "\"
            elements = rawString.split("(?<!\\\\),");
            //remove any escape chars
            for (int i = 0; i < elements.length; i++) {
                elements[i] = elements[i].replaceAll("\\\\,", ",");
            }
        } else {
            elements = new String[0];
        }
        return elements;
    }

}

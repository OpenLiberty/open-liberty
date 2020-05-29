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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.common.ConfigException;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

/**
 * A triple of a MP Converter, with an explicit Type and priority
 */
public class UserConverter<T> extends PriorityConverter {

    private static final TraceComponent tc = Tr.register(UserConverter.class);

    private final Converter<T> converter;

    /**
     * Construct a new PriorityConverter using discovered or default type and priority
     *
     * @param converter
     */
    @Trivial
    public static <K> UserConverter<K> newInstance(Converter<K> converter) {
        return newInstance(getType(converter), converter);
    }

    /**
     * Construct a new PriorityConverter using discovered or default priority
     *
     * @param converter
     */
    @Trivial
    public static <K> UserConverter<K> newInstance(Type type, Converter<K> converter) {
        return newInstance(type, getPriority(converter), converter);
    }

    @Trivial
    public static <K> UserConverter<K> newInstance(Type type, int priority, Converter<K> converter) {
        return new UserConverter<>(type, priority, converter);
    }

    /**
     * Construct a new PriorityConverter using explicit type and priority values
     *
     * @param type The type to convert to
     * @param priority The priority of the converter
     * @param converter The actual converter
     */
    protected UserConverter(Type type, int priority, Converter<T> converter) {
        super(type, priority);
        this.converter = converter;
    }

    /** {@inheritDoc} */
    @Override
    public T convert(String value) {
        //just pass-through to user converter
        return this.converter.convert(value);
    }

    /**
     * Reflectively get the priority from the @Priority annotation
     *
     * @param converter
     * @return
     */
    @Trivial
    private static int getPriority(Converter<?> converter) {
        int value = ConfigConstants.DEFAULT_CONVERTER_PRIORITY;
        //get Priority annotations from class only, not from the super-class
        Priority[] priorities = converter.getClass().getDeclaredAnnotationsByType(Priority.class);
        if ((priorities != null) && (priorities.length > 0)) {
            Priority priority = priorities[0];
            value = priority.value();
        }
        return value;
    }

    /**
     * Reflectively work out the Type of a Converter
     *
     * @param converter
     * @return
     */
    @Trivial
    private static Type getType(Converter<?> converter) {
        Type type = null;

        Type[] itypes = converter.getClass().getGenericInterfaces();
        for (Type itype : itypes) {
            ParameterizedType ptype = (ParameterizedType) itype;
            if (ptype.getRawType() == Converter.class) {
                Type[] atypes = ptype.getActualTypeArguments();
                if (atypes.length == 1) {
                    type = atypes[0];
                    break;
                } else {
                    throw new ConfigException(Tr.formatMessage(tc, "unable.to.determine.conversion.type.CWMCG0009E", converter.getClass().getName()));
                }
            }
        }
        if (type == null) {
            throw new ConfigException(Tr.formatMessage(tc, "unable.to.determine.conversion.type.CWMCG0009E", converter.getClass().getName()));
        }

        return type;
    }

    @Override
    public String toString() {
        return "User Converter for type " + getType() + "(" + getPriority() + ")";
    }

}

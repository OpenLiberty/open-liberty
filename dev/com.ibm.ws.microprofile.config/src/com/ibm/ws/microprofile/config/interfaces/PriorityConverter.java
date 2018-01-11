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
package com.ibm.ws.microprofile.config.interfaces;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A triple of a MP Converter, with an explicit Type and priority
 */
public class PriorityConverter<T> implements Converter<T>, Comparable<PriorityConverter<T>> {

    private static final TraceComponent tc = Tr.register(PriorityConverter.class);

    private final Type type;
    private final int priority;
    private final Converter<T> converter;

    /**
     * Construct a new PriorityConverter using discovered or default type and priority
     *
     * @param converter
     */
    public PriorityConverter(Converter<T> converter) {
        this(getType(converter), converter);
    }

    /**
     * Construct a new PriorityConverter using discovered or default priority
     *
     * @param converter
     */
    public PriorityConverter(Type type, Converter<T> converter) {
        this(type, getPriority(converter), converter);
    }

    /**
     * Construct a new PriorityConverter using explicit type and priority values
     *
     * @param type The type to convert to
     * @param priority The priority of the converter
     * @param converter The actual converter
     */
    public PriorityConverter(Type type, int priority, Converter<T> converter) {
        this.type = type;
        this.priority = priority;
        this.converter = converter;
    }

    /** {@inheritDoc} */
    @Override
    public T convert(String value) {
        //just pass-through to inner converter
        return this.converter.convert(value);
    }

    /**
     * @return the priority of this converter
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @return the type of this converter
     */
    public Type getType() {
        return this.type;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(PriorityConverter<T> other) {
        if (this == other) {
            return 0;
        }
        if (other == null) {
            return 1;
        }

        //we want highest priority first in the list
        int otherPriority = other.getPriority();

        if (this.priority > otherPriority) {
            return 1;
        }
        if (this.priority < otherPriority) {
            return -1;
        }
        //if the priorities are equal, fallback to an arbitrary but repeatable order based on hashCode ...
        //TODO there is a really small possibility that this could still result in 0
        return (this.hashCode() - other.hashCode());
    }

    /**
     * @param converter
     * @return
     */
    private static int getPriority(Converter<?> converter) {
        int value = ConfigConstants.DEFAULT_CONVERTER_PRIORITY;
        //get Priority annotations from class only, not from the super-class
        Priority[] priorities = converter.getClass().getDeclaredAnnotationsByType(Priority.class);
        if (priorities != null && priorities.length > 0) {
            Priority priority = priorities[0];
            value = priority.value();
        }
        return value;
    }

    /**
     * Get the Type of a Converter object
     *
     * @param converter
     * @return
     */
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
        //TODO this satisfies the current FAT tests that expect the converter class name in the message ... but we can do better, adding actual type and priority etc
        return this.converter.getClass().getName();
    }
}

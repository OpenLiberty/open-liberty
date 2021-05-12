/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config12.converters;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.config.interfaces.ConversionException;

public class ConstructorFunction<X> implements Function<String, X> {
    private final Constructor<X> constructor;

    public ConstructorFunction(Constructor<X> constructor) {
        this.constructor = constructor;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(InvocationTargetException.class)
    public X apply(String value) {
        X converted = null;
        if (value != null) { //if the value is null then we always return null
            try {
                converted = constructor.newInstance(value);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) cause;
                } else {
                    throw new ConversionException(cause);
                }
            } catch (IllegalAccessException | InstantiationException e) {
                throw new ConversionException(e);
            }
        }
        return converted;
    }

    @FFDCIgnore(NoSuchMethodException.class)
    @Trivial
    public static <X> Function<String, X> getConstructorFunction(Class<X> reflectionClass) {
        Function<String, X> implicitFunction = null;
        try {
            Constructor<X> ctor = reflectionClass.getConstructor(String.class);
            implicitFunction = new ConstructorFunction<X>(ctor);
        } catch (NoSuchMethodException e) {
            //No FFDC
        }

        return implicitFunction;
    }

}
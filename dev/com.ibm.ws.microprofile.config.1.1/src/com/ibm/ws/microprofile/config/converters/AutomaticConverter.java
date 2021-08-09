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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.config.interfaces.ConversionException;

/**
 *
 */
public class AutomaticConverter extends BuiltInConverter {

    private static final TraceComponent tc = Tr.register(AutomaticConverter.class);
    private final Method valueOfMethod;
    private Constructor<?> ctor;
    private Method parseMethod;

    @Trivial
    /**
     *
     * @param converterType The class to convert using
     */
    public AutomaticConverter(Class<?> converterType) {
        super(converterType);

        //in version 1.1 we always look for valueOf before a String constructor
        this.valueOfMethod = getValueOfMethod(converterType);
        if (this.valueOfMethod == null) {
            this.ctor = getConstructor(converterType);
        }

        if (this.ctor == null && this.valueOfMethod == null) {
            this.parseMethod = getParse(converterType);
        }

        if (this.ctor == null && this.valueOfMethod == null && this.parseMethod == null) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "implicit.string.constructor.method.not.found.CWMCG0017E", converterType));
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                if (this.ctor != null) {
                    Tr.debug(tc, "Automatic converter for {0} using {1}", converterType, this.ctor);
                } else if (this.valueOfMethod != null) {
                    Tr.debug(tc, "Automatic converter for {0} using {1}", converterType, this.valueOfMethod);
                } else if (this.parseMethod != null) {
                    Tr.debug(tc, "Automatic converter for {0} using {1}", converterType, this.parseMethod);
                }
            }
        }
    }

    @FFDCIgnore(NoSuchMethodException.class)
    @Trivial
    private static <M> Constructor<M> getConstructor(Class<M> reflectionClass) {
        Constructor<M> ctor = null;
        try {
            ctor = reflectionClass.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
            //No FFDC
        }
        return ctor;
    }

    @FFDCIgnore(NoSuchMethodException.class)
    @Trivial
    private static Method getValueOfMethod(Class<?> reflectionClass) {
        Method method = null;
        try {
            method = reflectionClass.getMethod("valueOf", String.class);
            if ((method.getModifiers() & Modifier.STATIC) == 0) {
                method = null;
            } else if (!reflectionClass.equals(method.getReturnType())) {
                method = null;
            }
        } catch (NoSuchMethodException e) {
            //No FFDC
        }

        return method;
    }

    @FFDCIgnore(NoSuchMethodException.class)
    @Trivial
    private static Method getParse(Class<?> reflectionClass) {
        Method method = null;
        try {
            method = reflectionClass.getMethod("parse", CharSequence.class);
            if ((method.getModifiers() & Modifier.STATIC) == 0) {
                method = null;
            } else if (!reflectionClass.equals(method.getReturnType())) {
                method = null;
            }
        } catch (NoSuchMethodException e) {
            //No FFDC
        }
        return method;
    }

    /** {@inheritDoc} */
    @Override
    public Object convert(String value) {
        Object converted = null;
        if (value != null) { //if the value is null then we always return null
            try {
                //in version 1.1 we always look for valueOf before a String constructor
                if (this.valueOfMethod != null) {
                    converted = this.valueOfMethod.invoke(null, value);
                } else if (this.ctor != null) {
                    converted = this.ctor.newInstance(value);
                } else if (this.parseMethod != null) {
                    converted = this.parseMethod.invoke(null, value);
                }
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

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Automatic Converter for type " + getType();
    }
}

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
import java.lang.reflect.Type;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.config.interfaces.ConversionException;

/**
 *
 */
public class AutomaticConverter extends BuiltInConverter {

    private static final TraceComponent tc = Tr.register(AutomaticConverter.class);
    private Method valueOfMethod;
    private Constructor<?> ctor;
    private Method parseMethod;
    private final boolean useCtorFirst;

    public AutomaticConverter(Class<?> type) {
        this(type, type);
    }

    public AutomaticConverter(Type converterType, Class<?> reflectionClass) {
        this(converterType, reflectionClass, false); //by default valueOf(String) is preferred to the Ctor(String)
    }

    public AutomaticConverter(Type converterType, Class<?> reflectionClass, boolean useCtorFirst) {
        super(converterType);
        this.useCtorFirst = useCtorFirst;
        if (useCtorFirst) {
            this.ctor = getConstructor(reflectionClass);
            if (this.ctor == null) {
                this.valueOfMethod = getValueOfMethod(reflectionClass);
            }
        } else {
            this.valueOfMethod = getValueOfMethod(reflectionClass);
            if (this.valueOfMethod == null) {
                this.ctor = getConstructor(reflectionClass);
            }
        }
        if (this.ctor == null && this.valueOfMethod == null) {
            this.parseMethod = getParse(reflectionClass);
        }
        if (this.ctor == null && this.valueOfMethod == null && this.parseMethod == null) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "implicit.string.constructor.method.not.found.CWMCG0017E", converterType));
        }
    }

    @FFDCIgnore(NoSuchMethodException.class)
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
    private static Method getValueOfMethod(Class<?> reflectionClass) {
        Method method = null;
        try {
            method = reflectionClass.getMethod("valueOf", String.class);
            if ((method.getModifiers() & Modifier.STATIC) == 0) {
                method = null;
            } else if (method.getReturnType() == Void.TYPE) {
                method = null;
            }
        } catch (NoSuchMethodException e) {
            //No FFDC
        }

        return method;
    }

    @FFDCIgnore(NoSuchMethodException.class)
    private static Method getParse(Class<?> reflectionClass) {
        Method method = null;
        try {
            method = reflectionClass.getMethod("parse", CharSequence.class);
            if ((method.getModifiers() & Modifier.STATIC) == 0) {
                method = null;
            } else if (method.getReturnType() == Void.TYPE) {
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
                if (this.useCtorFirst) {
                    if (this.ctor != null) {
                        converted = this.ctor.newInstance(value);
                    } else if (this.valueOfMethod != null) {
                        converted = this.valueOfMethod.invoke(null, value);
                    } else if (this.parseMethod != null) {
                        converted = this.parseMethod.invoke(null, value);
                    }
                } else {
                    if (this.valueOfMethod != null) {
                        converted = this.valueOfMethod.invoke(null, value);
                    } else if (this.ctor != null) {
                        converted = this.ctor.newInstance(value);
                    } else if (this.parseMethod != null) {
                        converted = this.parseMethod.invoke(null, value);
                    }
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
}

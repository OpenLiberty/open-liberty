/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */

public class ReflectUtil {

    @FFDCIgnore(value = { ClassNotFoundException.class })
    public static Class<?> loadClass(ClassLoader cl, String className) {
        if (cl == null)
            return null;

        Class<?> c = null;
        try {
            c = cl.loadClass(className);
        } catch (ClassNotFoundException e) {

        }

        return c;
    }

    @FFDCIgnore(value = { NoSuchMethodException.class, SecurityException.class })
    public static Method getMethod(Class<?> c, String methodName, Class<?>[] paramTypes) {
        if (c == null || methodName == null) {
            return null;
        }

        Method m = null;
        try {
            m = c.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {

        } catch (SecurityException e) {

        }
        return m;
    }

    @FFDCIgnore(value = { IllegalAccessException.class, IllegalArgumentException.class, InvocationTargetException.class })
    public static Object invoke(Method m, Object instance, Object[] args) throws Throwable {

        Object res = null;
        try {
            res = m.invoke(instance, args);
        } catch (IllegalAccessException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (InvocationTargetException e) {
            //ignore
            throw e.getCause();
        }

        return res;
    }
}

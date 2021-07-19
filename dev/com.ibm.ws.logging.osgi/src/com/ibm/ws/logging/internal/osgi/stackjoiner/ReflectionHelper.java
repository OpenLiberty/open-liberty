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

package com.ibm.ws.logging.internal.osgi.stackjoiner;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Helper class that performs necessary doPriv during reflection.
 * 
 * This class is borrowed from com.ibm.ws.monitor/src/com/ibm/ws/monitor/internal/ReflectionHelper.java.
 */
class ReflectionHelper {

    private final static boolean securityEnabled = System.getSecurityManager() != null;

    private ReflectionHelper() {}

    static Field getDeclaredField(final Class<?> clazz, final String fieldName) {
        if (clazz != null) {
            return securityEnabled ? doPrivGetDeclaredField(clazz, fieldName) : doGetDeclaredField(clazz, fieldName);
        }
        return null;
    }

    private static Field doGetDeclaredField(final Class<?> clazz, final String fieldName) {
        Field field = null;
        try {
            field = clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException nsfe) {
        }
        return field;
    }

    private static Field doPrivGetDeclaredField(final Class<?> clazz, final String fieldName) {
        return AccessController.doPrivileged(new PrivilegedAction<Field>() {
            public Field run() {
                return doGetDeclaredField(clazz, fieldName);
            }
        });
    }

    static Method getDeclaredMethod(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes) {
        if (clazz != null) {
            return securityEnabled ?
                            doPrivGetDeclaredMethod(clazz, methodName, parameterTypes) :
                            doGetDeclaredMethod(clazz, methodName, parameterTypes);
        }
        return null;
    }

    private static Method doGetDeclaredMethod(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes) {
        Method method = null;
        try {
            method = clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
        }
        return method;
    }

    private static Method doPrivGetDeclaredMethod(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes) {
        return AccessController.doPrivileged(new PrivilegedAction<Method>() {
            public Method run() {
                return doGetDeclaredMethod(clazz, methodName, parameterTypes);
            }
        });
    }

    static void setAccessible(final AccessibleObject accessibleObject, final boolean visible) {
        if (securityEnabled) {
            doPrivSetAccessible(accessibleObject, visible);
        } else {
            doSetAccessible(accessibleObject, visible);
        }
    }

    private static void doSetAccessible(final AccessibleObject accessibleObject, final boolean visible) {
        accessibleObject.setAccessible(visible);
    }

    private static void doPrivSetAccessible(final AccessibleObject accessibleObject, final boolean visible) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                doSetAccessible(accessibleObject, visible);
                return null;
            }
        });
    }
}

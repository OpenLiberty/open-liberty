/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.monitor.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.framework.Bundle;

import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Helper class that performs necessary doPriv during reflection.
 */
class ReflectionHelper {

    private final static boolean securityEnabled = System.getSecurityManager() != null;

    private ReflectionHelper() {}

    static <T> Class<? super T> getSuperclass(final Class<T> clazz) {
        if (clazz != null) {
            if (securityEnabled) {
                return doPrivGetSuperclass(clazz);
            } else {
                return doGetSuperclass(clazz);
            }
        }
        return null;
    }

    private static <T> Class<? super T> doGetSuperclass(final Class<T> clazz) {
        return clazz.getSuperclass();
    }

    private static <T> Class<? super T> doPrivGetSuperclass(final Class<T> clazz) {
        return AccessController.doPrivileged(new PrivilegedAction<Class<? super T>>() {
            public Class<? super T> run() {
                return doGetSuperclass(clazz);
            }
        });
    }

    static <T> Class<T> loadClass(final Bundle bundle, final String className) {
        if (securityEnabled) {
            return doPrivLoadClass(bundle, className);
        } else {
            return doLoadClass(bundle, className);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> doLoadClass(final Bundle bundle, final String className) {
        try {
            return (Class<T>) bundle.loadClass(className);
        } catch (ClassNotFoundException cnfe) {
        }
        return null;
    }

    private static <T> Class<T> doPrivLoadClass(final Bundle bundle, final String className) {
        return AccessController.doPrivileged(new PrivilegedAction<Class<T>>() {
            public Class<T> run() {
                return doLoadClass(bundle, className);
            }
        });
    }

    static Class<?>[] getInterfaces(final Class<?> clazz) {
        return securityEnabled ? doPrivGetInterfaces(clazz) : doGetInterfaces(clazz);
    }

    private static Class<?>[] doGetInterfaces(final Class<?> clazz) {
        return clazz.getInterfaces();
    }

    private static Class<?>[] doPrivGetInterfaces(final Class<?> clazz) {
        return AccessController.doPrivileged(new PrivilegedAction<Class<?>[]>() {
            public Class<?>[] run() {
                return doGetInterfaces(clazz);
            }
        });
    }

    static Method[] getDeclaredMethods(final Class<?> clazz) {
        if (clazz != null) {
            return securityEnabled ? doPrivGetDeclaredMethods(clazz) : doGetDeclaredMethods(clazz);
        }
        return new Method[0];
    }

    private static Method[] doGetDeclaredMethods(final Class<?> clazz) {
        return clazz.getDeclaredMethods();
    }

    private static Method[] doPrivGetDeclaredMethods(final Class<?> clazz) {
        return AccessController.doPrivileged(new PrivilegedAction<Method[]>() {
            public Method[] run() {
                return doGetDeclaredMethods(clazz);
            }
        });
    }

    static Method[] getMethods(final Class<?> clazz) {
        return securityEnabled ? doPrivGetMethods(clazz) : doGetMethods(clazz);
    }

    private static Method[] doGetMethods(final Class<?> clazz) {
        return clazz.getMethods();
    }

    private static Method[] doPrivGetMethods(final Class<?> clazz) {
        return AccessController.doPrivileged(new PrivilegedAction<Method[]>() {
            public Method[] run() {
                return doGetMethods(clazz);
            }
        });
    }

    static Constructor<?>[] getConstructors(final Class<?> clazz) {
        if (clazz != null) {
            return securityEnabled ? doPrivGetConstructors(clazz) : doGetConstructors(clazz);
        }
        return new Constructor<?>[0];
    }

    private static Constructor<?>[] doGetConstructors(final Class<?> clazz) {
        return clazz.getConstructors();
    }

    private static Constructor<?>[] doPrivGetConstructors(final Class<?> clazz) {
        return AccessController.doPrivileged(new PrivilegedAction<Constructor<?>[]>() {
            public Constructor<?>[] run() {
                return doGetConstructors(clazz);
            }
        });
    }

    static Constructor<?>[] getDeclaredConstructors(final Class<?> clazz) {
        if (clazz != null) {
            return securityEnabled ? doPrivGetDeclaredConstructors(clazz) : doGetDeclaredConstructors(clazz);
        }
        return new Constructor<?>[0];
    }

    private static Constructor<?>[] doGetDeclaredConstructors(final Class<?> clazz) {
        return clazz.getDeclaredConstructors();
    }

    private static Constructor<?>[] doPrivGetDeclaredConstructors(final Class<?> clazz) {
        return AccessController.doPrivileged(new PrivilegedAction<Constructor<?>[]>() {
            public Constructor<?>[] run() {
                return doGetDeclaredConstructors(clazz);
            }
        });
    }

    static <T> T newInstance(final Constructor<T> ctor, final Object... args) {
        return securityEnabled ? doPrivNewInstance(ctor, args) : doNewInstance(ctor, args);
    }

    private static <T> T doNewInstance(final Constructor<T> ctor, final Object... args) {
        T instance = null;
        try {
            instance = ctor.newInstance(args);
        } catch (IllegalArgumentException e) {
            FFDCFilter.processException(e, "com.ibm.ws.monitor.internal.ReflectionHelper", "doNewInstance");
        } catch (InstantiationException e) {
            FFDCFilter.processException(e, "com.ibm.ws.monitor.internal.ReflectionHelper", "doNewInstance");
        } catch (IllegalAccessException e) {
            FFDCFilter.processException(e, "com.ibm.ws.monitor.internal.ReflectionHelper", "doNewInstance");
        } catch (InvocationTargetException e) {
            FFDCFilter.processException(e, "com.ibm.ws.monitor.internal.ReflectionHelper", "doNewInstance");
        }
        return instance;
    }

    private static <T> T doPrivNewInstance(final Constructor<T> ctor, final Object... args) {
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            public T run() {
                return doNewInstance(ctor, args);
            }
        });
    }

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

    static Annotation[] getAnnotations(final AnnotatedElement annotatedElement) {
        return securityEnabled ? doPrivGetAnnotations(annotatedElement) : doGetAnnotations(annotatedElement);
    }

    private static Annotation[] doGetAnnotations(final AnnotatedElement annotatedElement) {
        return annotatedElement.getAnnotations();
    }

    private static Annotation[] doPrivGetAnnotations(final AnnotatedElement annotatedElement) {
        return AccessController.doPrivileged(new PrivilegedAction<Annotation[]>() {
            public Annotation[] run() {
                return doGetAnnotations(annotatedElement);
            }
        });
    }

    static <A extends Annotation> A getParameterAnnotation(Method method, int index, Class<A> annotationType) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (Annotation a : parameterAnnotations[index]) {
            if (annotationType.isInstance(a)) {
                return annotationType.cast(a);
            }
        }
        return null;
    }
}

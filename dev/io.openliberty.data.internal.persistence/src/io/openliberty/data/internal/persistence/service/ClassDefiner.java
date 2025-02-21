/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal.persistence.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AllPermission;
import java.security.Permissions;
import java.security.ProtectionDomain;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.persistence.Util;

/**
 * Initially copied from @nmittles pull #25248
 */
class ClassDefiner {
    private static final TraceComponent tc = Tr.register(ClassDefiner.class);

    /**
     * Accessible {@link ClassLoader#findLoadedClass}. This field is lazily
     * initialized by {@link #findLoadedClass}.
     */
    private static volatile Method svFindLoadedClassMethod;

    /**
     * Accessible {@link ClassLoader#defineClass}. This field is lazily
     * initialized by {@link #defineClass}.
     */
    private static volatile Method svDefineClassMethod;

    /**
     * A protection domain containing {@link AllPermission}. This field is
     * lazily initialized by {@link #defineClass}, with the synchronization
     * managed by the volatile read/write of {@link #svDefineClassMethod}.
     */
    private static ProtectionDomain svAllPermissionProtectionDomain;

    /**
     * Return {@link ClassLoader#findLoadedClass}.
     *
     * @param classLoader the class loader
     * @param className   the class name
     * @return the class, or null if not loaded
     */
    Class<?> findLoadedClass(ClassLoader classLoader, String className) {
        try {
            Method findLoadedClassMethod = svFindLoadedClassMethod;
            if (findLoadedClassMethod == null) {
                try {
                    findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
                } catch (NoSuchMethodException ex) {
                    throw new IllegalStateException(ex);
                }

                findLoadedClassMethod.setAccessible(true);
                svFindLoadedClassMethod = findLoadedClassMethod;
            }

            return (Class<?>) findLoadedClassMethod.invoke(classLoader, className);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException(cause);
        }
    }

    /**
     * Return {@link ClassLoader#defineClass} with a PermissionCollection containing AllPermission.
     *
     * @param classLoader the class loader
     * @param className   the class name
     * @param classbytes  the class bytes
     * @return the class
     * @throws LinkageError     if a class is defined twice within the given class loader
     * @throws ClassFormatError if the bytes passed in are invalid
     */
    @Trivial
    Class<?> defineClass(ClassLoader classLoader, String className, byte[] classbytes) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "defineClass", classLoader, className, classbytes.length);

        Method defineClassMethod = svDefineClassMethod;
        if (defineClassMethod == null) {
            Permissions perms = new Permissions();
            perms.add(new AllPermission());
            svAllPermissionProtectionDomain = new ProtectionDomain(null, perms);

            try {
                defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass",
                                                                        String.class,
                                                                        byte[].class,
                                                                        int.class,
                                                                        int.class,
                                                                        ProtectionDomain.class);
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException(ex);
            }

            defineClassMethod.setAccessible(true);
            svDefineClassMethod = defineClassMethod;
        }

        try {
            // We declare the class using a non-dynamic AllPermission
            // ProtectionDomain so that it will be ignored by Java 2 security.
            Class<?> c = (Class<?>) defineClassMethod.invoke(classLoader,
                                                             className,
                                                             classbytes,
                                                             0,
                                                             classbytes.length,
                                                             svAllPermissionProtectionDomain);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "defineClass", Util.toString(c, ""));

            return c;
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }

            throw new IllegalStateException(cause);
        }
    }

    /**
     * Return {@link ClassLoader#defineClass} with a PermissionCollection containing AllPermission.
     * This method searches for a class and returns it. If it's not found, it's defined.
     *
     * @param classLoader the class loader
     * @param className   the class name
     * @param classbytes  the class bytes
     * @return the class
     * @throws LinkageError     if a class is defined twice within the given class loader
     * @throws ClassFormatError if the bytes passed in are invalid
     */
    @Trivial
    Class<?> findLoadedOrDefineClass(ClassLoader classLoader, String className, byte[] classbytes) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "findLoadedOrDefineClass", classLoader, className, classbytes.length);

        Class<?> klass = findLoadedClass(classLoader, className);
        if (klass == null) {
            try {
                klass = defineClass(classLoader, className, classbytes);
            } catch (LinkageError ex) {
                klass = findLoadedClass(classLoader, className);
                if (klass == null) {
                    throw ex;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "findLoadedOrDefineClass", klass);

        return klass;
    }
}
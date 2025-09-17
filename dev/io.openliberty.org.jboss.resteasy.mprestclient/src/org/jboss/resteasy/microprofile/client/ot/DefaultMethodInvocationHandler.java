/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
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
package org.jboss.resteasy.microprofile.client.ot;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Set;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.microprofile.client.ProxyInvocationHandler;

public class DefaultMethodInvocationHandler implements InvocationHandler {

    private final InvocationHandler delegateHandler;
    private final Class<?> restClientInterface;
    private final Object target;

    public DefaultMethodInvocationHandler(final Class<?> restClientInterface,
                                          final Object target,
                                          final Set<Object> providerInstances,
                                          final ResteasyClient client,
                                          final BeanManager beanManager,
                                          final ClassLoader classLoader) {
        this.delegateHandler = new ProxyInvocationHandler(restClientInterface, target, providerInstances, client, beanManager);
        this.restClientInterface = restClientInterface;
        this.target = createDefaultMethodTarget(restClientInterface, classLoader);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isDefault()) {
            return invokeDefaultMethod(restClientInterface, proxy, method, args);
        }
        return delegateHandler.invoke(proxy, method, args);
    }

    private static Object createDefaultMethodTarget(Class<?> interfaceClass, ClassLoader classLoader) {
        return AccessController.doPrivileged((PrivilegedAction<Object>) () -> 
            Proxy.newProxyInstance(classLoader, new Class[]{interfaceClass}, (Object proxy, Method method, Object[] arguments) -> null));
    }

    private static Object invokeDefaultMethod(Class<?> declaringClass, Object o, Method m, Object[] params)
                    throws Throwable {

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    try {
                        final MethodHandles.Lookup lookup = MethodHandles
                                        .publicLookup()
                                        .in(declaringClass);
                        // force private access so unreflectSpecial can invoke the interface's default method
                        Field f;
                        try { 
                            f = MethodHandles.Lookup.class.getDeclaredField("allowedModes");
                        } catch (NoSuchFieldException nsfe) {
                            // IBM and OpenJ9 JDKs use a different field name
                            f = MethodHandles.Lookup.class.getDeclaredField("accessMode");
                            m.setAccessible(true);
                        }
                        final int modifiers = f.getModifiers();
                        if (Modifier.isFinal(modifiers)) {
                            final Field modifiersField = Field.class.getDeclaredField("modifiers");
                            modifiersField.setAccessible(true);
                            modifiersField.setInt(f, modifiers & ~Modifier.FINAL);
                            f.setAccessible(true);
                            f.set(lookup, MethodHandles.Lookup.PRIVATE);
                        }
                        MethodHandle mh = lookup.unreflectSpecial(m, declaringClass).bindTo(o);
                        return params != null && params.length > 0 ? mh.invokeWithArguments(params) : mh.invoke();
                    } catch (Throwable t) {
                        try { // try using built-in JDK 9+ API for invoking default method
                            return invokeDefaultMethodUsingPrivateLookup(declaringClass, o, m, params);
                        } catch (final NoSuchMethodException ex) {
                            throw new WrappedException(t);
                        }
                    }
                }
            });
        } catch (PrivilegedActionException pae) {
            Throwable wrapped = pae.getCause();
            if (wrapped instanceof WrappedException) {
                throw ((WrappedException)wrapped).getWrapped();
            }
            throw wrapped;
        }
    }

    /**
     * For JDK 9+, we could use MethodHandles.privateLookupIn, which is not 
     * available in JDK 8.
     */
    private static Object invokeDefaultMethodUsingPrivateLookup(Class<?> declaringClass, Object o, Method m, 
                                                                Object[] params) throws WrappedException, NoSuchMethodException {
        try {
            final Method privateLookup = MethodHandles
                            .class
                            .getDeclaredMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);

            return ((MethodHandles.Lookup)privateLookup
                            .invoke(null, declaringClass, MethodHandles.lookup()))
                            .unreflectSpecial(m, declaringClass)
                            .bindTo(o)
                            .invokeWithArguments(params);
        } catch (NoSuchMethodException t) {
            throw t;
        } catch (Throwable t) {
            throw new WrappedException(t);
        }
    }

    private static class WrappedException extends Exception {
        private static final long serialVersionUID = 1183890106889852917L;

        final Throwable wrapped;
        WrappedException(Throwable wrapped) {
            this.wrapped = wrapped;
        }
        Throwable getWrapped() {
            return wrapped;
        }
    }
}

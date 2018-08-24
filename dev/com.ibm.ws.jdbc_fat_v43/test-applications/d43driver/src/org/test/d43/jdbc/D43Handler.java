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
package org.test.d43.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class D43Handler implements InvocationHandler {
    private final Object instance;

    D43Handler(Object instance) {
        this.instance = instance;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?> returnType = method.getReturnType();
        if ("hashCode".equals(methodName))
            return System.identityHashCode(proxy);
        if ("toString".equals(methodName))
            return "D43Handler@" + Integer.toHexString(System.identityHashCode(proxy)) + " for "
                   + instance.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(instance));
        if ("getJDBCMajorVersion".equals(methodName))
            return 4;
        if ("getJDBCMinorVersion".equals(methodName))
            return 3;
        try {
            Object result = method.invoke(instance, args);
            if (returnType.isInterface() && (returnType.getPackage().getName().startsWith("java.sql") || returnType.getPackage().getName().startsWith("javax.sql")))
                return Proxy.newProxyInstance(D43Handler.class.getClassLoader(), new Class[] { returnType }, new D43Handler(result));
            return result;
        } catch (InvocationTargetException x) {
            throw x.getCause();
        }
    }
}

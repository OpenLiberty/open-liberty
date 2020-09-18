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
package com.ibm.ws.wsat.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Proxy-up a mock object. Subclass this to provide required mock method implementations.
 */
public class MockProxy implements InvocationHandler {

    public <T> T asMock(Class<T> t) {
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { t }, this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return getClass().getDeclaredMethod(method.getName(), method.getParameterTypes()).invoke(this, args);
        } catch (NoSuchMethodException e) {
            return null;
        }

    }
}

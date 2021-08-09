/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.validator.jmsadapter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Proxy for JMS session where all methods return null.
 */
public class NoOpSessionImpl implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();

        if ("hashCode".equals(name))
            return System.identityHashCode(proxy);
        if ("toString".equals(name))
            return getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy));

        return null;
    }
}

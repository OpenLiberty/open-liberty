/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal.cdi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

class QueryHandler<T> implements InvocationHandler {
    private final Class<T> type;

    QueryHandler(Class<T> type) {
        this.type = type;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if (args == null || args.length == 0) {
            if ("hashCode".equals(methodName))
                return System.identityHashCode(proxy);
            else if ("toString".equals(methodName))
                return type.getName() + "[QueryHandler]@" + Integer.toHexString(System.identityHashCode(proxy));
        } else if (args.length == 1) {
            if ("equals".equals(methodName))
                return proxy == args[0];
        }

        // Useful implementation would go here - TODO find out if we can invoke JPA persistence service from here
        System.out.println("Handler invoke " + method);
        return "TheResult";
    }
}
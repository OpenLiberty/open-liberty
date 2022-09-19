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
package io.openliberty.data.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.enterprise.inject.spi.Bean;

public class QueryHandler<R> implements InvocationHandler {

    private final Class<R> beanClass;
    private final Class<?> defaultEntityClass; // repository methods can return subclasses in the case of @Inheritance

    @SuppressWarnings("unchecked")
    public QueryHandler(Bean<R> bean, Class<?> entityClass) {
        beanClass = (Class<R>) bean.getBeanClass();
        defaultEntityClass = entityClass;
    }

    @Override
    @Trivial
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if (args == null) {
            if ("hashCode".equals(methodName))
                return System.identityHashCode(proxy);
            else if ("toString".equals(methodName))
                return beanClass.getName() + "[QueryHandler]@" + Integer.toHexString(System.identityHashCode(proxy));
        } else if (args.length == 1) {
            if ("equals".equals(methodName))
                return proxy == args[0];
        }

        System.out.println("Handler invoke " + method);
        return null;
    }
}
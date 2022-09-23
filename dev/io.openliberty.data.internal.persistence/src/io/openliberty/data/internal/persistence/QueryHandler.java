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
package io.openliberty.data.internal.persistence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.ibm.websphere.ras.annotation.Trivial;

public class QueryHandler<R, E> implements InvocationHandler {

    private final Class<E> defaultEntityClass; // repository methods can return subclasses in the case of @Inheritance
    private final Class<R> repositoryInterface;

    public QueryHandler(Class<R> repositoryInterface, Class<E> entityClass) {
        this.repositoryInterface = repositoryInterface;
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
                return repositoryInterface.getName() + "[QueryHandler]@" + Integer.toHexString(System.identityHashCode(proxy));
        } else if (args.length == 1) {
            if ("equals".equals(methodName))
                return proxy == args[0];
        }

        System.out.println("Handler invoke " + method);
        return null;
    }
}
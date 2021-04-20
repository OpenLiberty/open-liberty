/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.Test;
import org.osgi.service.component.ComponentContext;

/**
 *
 */
public class CheckpointImplTest {
    static class Factories implements InvocationHandler {
        final Object[] factories;

        public Factories(Object[] factories) {
            super();
            this.factories = factories;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("locateServices".equals(method.getName())) {
                return factories;
            }
            throw new UnsupportedOperationException(method.getName());
        }
    }

    ComponentContext createComponentContext(Object[] factories) {
        return (ComponentContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ComponentContext.class }, new Factories(factories));
    }

    @Test
    public void basicTest() {
        new CheckpointImpl(null);
    }

}

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.ejbbnddd;

import java.util.concurrent.atomic.AtomicInteger;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.ibm.ws.javaee.dd.commonbnd.Interceptor;

public class InterceptorMockery {
    private static final AtomicInteger numInterceptors = new AtomicInteger();

    final Mockery mockery;
    private final String className;

    InterceptorMockery(Mockery mockery, String name) {
        this.mockery = mockery;
        this.className = name;
    }

    public Interceptor mock() {
        final Interceptor interceptor = mockery.mock(Interceptor.class, "interceptor-" + numInterceptors.incrementAndGet());
        mockery.checking(new Expectations() {
            {
                allowing(interceptor).getClassName();
                will(returnValue(className));
            }
        });
        return interceptor;
    }
}

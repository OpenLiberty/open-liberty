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
package org.jboss.resteasy.microprofile.client.ot;

import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;

public class InterceptorInvoker {
    @SuppressWarnings("rawtypes")
    private final Interceptor interceptor;

    private final Object interceptorInstance;

    public InterceptorInvoker(Interceptor<?> interceptor, Object interceptorInstance) {
        this.interceptor = interceptor;
        this.interceptorInstance = interceptorInstance;
    }

    @SuppressWarnings("unchecked")
    Object invoke(InvocationContext ctx) throws Exception {
        return interceptor.intercept(InterceptionType.AROUND_INVOKE, interceptorInstance, ctx);
    }
}

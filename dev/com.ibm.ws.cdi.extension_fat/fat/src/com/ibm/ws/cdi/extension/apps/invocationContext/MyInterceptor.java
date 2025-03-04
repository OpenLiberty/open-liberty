/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
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
package com.ibm.ws.cdi.extension.apps.invocationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ibm.ws.cdi.internal.interfaces.CDIUtils;

@Priority(1)
@Interceptor
@MyInterceptorBinding
public class MyInterceptor {

    @AroundInvoke
    public Object aroundInvokeMethod(InvocationContext ctx) throws Exception {
        InvocationContextTestServlet.bindingInterceptorRan = true;

        //In a real app this wouldn't be possible, but for now we're just testing this utility method.
        Set<Annotation> interceptorBindings = CDIUtils.getInterceptorBindingsFromInvocationContext(ctx);
        assertThat(interceptorBindings, containsInAnyOrder(instanceOf(MyNonBindingInterceptorBinding.class),
                                                           instanceOf(MyInterceptorBinding.class),
                                                           instanceOf(MyUnusedInterceptorBinding.class),
                                                           instanceOf(MyAddedByExtensionBinding.class)));
        assertEquals(4, interceptorBindings.size());

        return ctx.proceed();
    }
}

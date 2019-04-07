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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.interceptors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.interceptors.LateInterceptor.LateBinding;

/**
 * An interceptor which should be called after the fault tolerance interceptor
 * <p>
 * It records all its invocations using {@link InterceptionRecorder#record(Class, javax.interceptor.InvocationContext)}
 */
@Interceptor
@LateBinding
@Priority(Interceptor.Priority.PLATFORM_AFTER + 100)
public class LateInterceptor {

    @InterceptorBinding
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface LateBinding {
    }

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        InterceptionRecorder.record(LateInterceptor.class, ctx);
        return ctx.proceed();
    }

}

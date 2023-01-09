/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.cdi.internal.core.interceptors.app;

import static javax.interceptor.Interceptor.Priority.APPLICATION;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@AroundInvokeBinding
@Priority(APPLICATION)
public class AroundInvokeInterceptor {

    @Inject
    private ExecutionRecorder recorder;

    @AroundInvoke
    private Object aroundInvoke(InvocationContext context) throws Exception {
        recorder.record("interceptorPreInvoke");
        try {
            return context.proceed();
        } finally {
            recorder.record("interceptorPostInvoke");
        }
    }

}

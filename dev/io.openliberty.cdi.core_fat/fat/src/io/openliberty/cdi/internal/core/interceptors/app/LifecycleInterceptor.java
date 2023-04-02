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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@LifecycleBinding
@Priority(APPLICATION)
public class LifecycleInterceptor {

    @Inject
    private ExecutionRecorder recorder;

    @PostConstruct
    private void postConstruct(InvocationContext ctx) throws Exception {
        recorder.record("interceptorPostConstruct");
        ctx.proceed();
    }

    @PreDestroy
    private void preDestroy(InvocationContext ctx) throws Exception {
        recorder.record("interceptorPreDestroy");
        ctx.proceed();
    }
}

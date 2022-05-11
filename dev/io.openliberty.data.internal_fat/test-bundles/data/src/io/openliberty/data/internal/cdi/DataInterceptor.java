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

import java.io.Serializable;
import java.lang.reflect.Method;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.data.Data;

@Data
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_AFTER - 218)
public class DataInterceptor implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(DataInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext invocation) throws Exception {
        Method method = invocation.getMethod();
        Data anno = method.getAnnotation(Data.class);
        return "Data";
    }
}
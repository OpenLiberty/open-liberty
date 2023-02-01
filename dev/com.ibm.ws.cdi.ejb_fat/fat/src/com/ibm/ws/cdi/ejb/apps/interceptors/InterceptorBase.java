/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.interceptors;

import javax.annotation.PostConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

abstract class InterceptorBase {

    @PostConstruct
    private void postConstruct(InvocationContext ic) throws Exception {
        System.out.println(">InterceptorBase postConstruct - " + this.hashCode());
        try {
            ic.proceed();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        System.out.println("<InterceptorBase postConstruct - " + this.hashCode());
    }

    @AroundInvoke
    private Object aroundInvoke(InvocationContext ic) throws Exception {
        System.out.println(">InterceptorBase aroundInvoke - " + this.hashCode());
        Object res;
        try {
            res = "Intercepted by " + this.getClass().getSimpleName() + "! " + ic.proceed();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        System.out.println("<InterceptorBase aroundInvoke - " + this.hashCode());
        return res;
    }

}

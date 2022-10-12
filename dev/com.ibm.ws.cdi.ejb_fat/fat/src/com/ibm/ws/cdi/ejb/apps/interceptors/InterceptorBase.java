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
package com.ibm.ws.cdi.ejb.apps.interceptors;

import javax.annotation.PostConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

abstract class InterceptorBase {

    @PostConstruct
    private void postConstruct(InvocationContext ic) {
        System.out.println(">InterceptorBase postConstruct - " + this.hashCode());
        try {
            ic.proceed();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
            e.printStackTrace();
        }
        System.out.println("<InterceptorBase postConstruct - " + this.hashCode());
    }

    @AroundInvoke
    private Object aroundInvoke(InvocationContext ic) {
        System.out.println(">InterceptorBase aroundInvoke - " + this.hashCode());
        Object res;
        try {
            res = ic.proceed();
        } catch (Exception e) {
            res = e;
            e.printStackTrace();
        }
        System.out.println("<InterceptorBase aroundInvoke - " + this.hashCode());
        return res;
    }

}

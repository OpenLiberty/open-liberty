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

import javax.interceptor.AroundConstruct;
import javax.interceptor.InvocationContext;

public class InterceptorSubClass1 extends InterceptorBase {

    @AroundConstruct
    private void aroundConstruct(InvocationContext ic) throws Exception {
        System.out.println(">InterceptorSubClass1 aroundConstruct - " + this.hashCode());
        try {
            ic.proceed();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        System.out.println("<InterceptorSubClass1 aroundConstruct - " + this.hashCode());
    }

}

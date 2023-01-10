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
package com.ibm.ws.cdi.ejb.apps.interceptors;

import javax.interceptor.AroundConstruct;
import javax.interceptor.InvocationContext;

public class Interceptor9 extends InterceptorBase {

    @AroundConstruct
    private void aroundConstruct(InvocationContext ic) {
        System.out.println(">Interceptor9 aroundConstruct - " + this.hashCode());
        try {
            ic.proceed();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
            e.printStackTrace();
        }
        System.out.println("<Interceptor9 aroundConstruct - " + this.hashCode());
    }

}

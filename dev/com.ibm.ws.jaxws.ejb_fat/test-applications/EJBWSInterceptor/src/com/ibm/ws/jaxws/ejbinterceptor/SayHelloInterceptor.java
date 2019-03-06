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
package com.ibm.ws.jaxws.ejbinterceptor;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class SayHelloInterceptor {

    @AroundInvoke
    public Object checkName(InvocationContext ic) throws Exception {
        Object result = null;
        try {
            System.out.println("INFO: " + getClass().getName()
                               + " intercepted the method: " + ic.getMethod().getName());
            result = ic.proceed();
            return result;
        } finally {
        }
    }
}

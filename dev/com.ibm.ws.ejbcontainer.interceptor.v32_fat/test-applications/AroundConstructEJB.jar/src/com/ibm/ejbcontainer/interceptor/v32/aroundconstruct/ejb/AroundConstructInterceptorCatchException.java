/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb;

import javax.interceptor.AroundConstruct;
import javax.interceptor.InvocationContext;

public class AroundConstructInterceptorCatchException {

    @AroundConstruct
    public Object aroundConstruct(InvocationContext ctx) throws Exception {
        Object o = null;

        try {
            o = ctx.proceed();
        } catch (Exception e) {
        }

        return o;
    }
}

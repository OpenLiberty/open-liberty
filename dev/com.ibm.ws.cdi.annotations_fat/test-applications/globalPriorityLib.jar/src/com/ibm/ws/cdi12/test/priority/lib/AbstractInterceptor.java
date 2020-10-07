/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.priority.lib;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import com.ibm.ws.cdi12.test.utils.ChainableList;
import com.ibm.ws.cdi12.test.utils.Utils;

public abstract class AbstractInterceptor {
    private final String name;

    public AbstractInterceptor(final Class<?> subclass) {
        this.name = Utils.id(subclass);
    }

    @SuppressWarnings("unchecked")
    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        return ((ChainableList<String>) context.proceed()).chainAdd(name);
    }
}

/*******************************************************************************
 * Copyright (c) 2010, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.ann.ejb;

import java.util.List;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class BasicStatelessLocalInterceptor {
    @AroundInvoke
    public Object aroundInvoke(InvocationContext ic) throws Exception {
        @SuppressWarnings("unchecked")
        List<String> value = (List<String>) ic.getParameters()[0];
        value.add("interceptor");
        return ic.proceed();
    }
}

/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.beans;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 *
 */
public class BeanInterceptor {

    @AroundInvoke
    public Object checkParams(InvocationContext context) throws Exception {
        Object[] params = context.getParameters();
        params[0] = params[0] + ":BeanInterceptor:";
        context.setParameters(params);
        return context.proceed();
    }

}

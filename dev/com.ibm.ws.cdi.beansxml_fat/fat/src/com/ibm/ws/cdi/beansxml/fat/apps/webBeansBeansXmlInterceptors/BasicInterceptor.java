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
package com.ibm.ws.cdi.beansxml.fat.apps.webBeansBeansXmlInterceptors;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * Notifies the intercepted beans that they were intercepted
 */
@BasicInterceptorBinding
@Interceptor
public class BasicInterceptor {

    @AroundInvoke
    public Object notifyInterception(InvocationContext invocationContext) throws Exception {
        Object target = invocationContext.getTarget();
        if (target instanceof InterceptedBean) {
            InterceptedBean bean = (InterceptedBean) target;
            bean.setLastInterceptedBy(this.getClass().getSimpleName());
        }
        return invocationContext.proceed();
    }

}

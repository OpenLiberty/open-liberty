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
package cdi.listeners.interceptors;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeEvent;

/**
 *
 */
@Interceptor
@RequestAttributeListenerType
@Priority(Interceptor.Priority.APPLICATION)
public class RequestAttributeListenerInterceptor {

    @AroundInvoke
    public Object checkParams(InvocationContext context) throws Exception {

        Object[] params = context.getParameters();

        ServletRequestAttributeEvent srae = (ServletRequestAttributeEvent) params[0];

        String attrKey = srae.getName();

        // Don't process an attribute set by an interceptor
        if ((attrKey != null) && attrKey.startsWith("CDI")) {

            ServletRequest req = srae.getServletRequest();

            Object attrValue = srae.getValue();

            if (attrValue != null) {
                char lastChar = attrValue.toString().charAt(attrValue.toString().length() - 1);
                req.setAttribute("RALInterceptor", ":I" + lastChar);
            } else {
                req.setAttribute("RALInterceptor", ":Ix");
            }
        }

        return context.proceed();
    }

}

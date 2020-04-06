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
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;

/**
 *
 */
@Interceptor
@ContextAttributeListenerType
@Priority(Interceptor.Priority.APPLICATION)
public class ContextAttributeListenerInterceptor {

    @AroundInvoke
    public Object checkParams(InvocationContext context) throws Exception {

        Object[] params = context.getParameters();

        ServletContextAttributeEvent scae = (ServletContextAttributeEvent) params[0];

        String attrKey = scae.getName();

        // Don't process an attribute set by an interceptor
        if ((attrKey != null) && attrKey.startsWith("CDI")) {

            ServletContext sc = scae.getServletContext();

            Object attrValue = scae.getValue();

            if (attrValue != null) {
                char lastChar = attrValue.toString().charAt(attrValue.toString().length() - 1);
                sc.setAttribute("CALInterceptor", ":I" + lastChar);
            } else {
                sc.setAttribute("CALInterceptor", ":Ix");
            }
        }

        return context.proceed();
    }

}

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
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.interceptors;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;

/**
 *
 */
@Interceptor
@SessionAttributeListenerType
@Priority(Interceptor.Priority.APPLICATION)
public class SessionAttributeListenerInterceptor {

    @AroundInvoke
    public Object checkParams(InvocationContext context) throws Exception {

        Object[] params = context.getParameters();

        HttpSessionBindingEvent sbe = (HttpSessionBindingEvent) params[0];

        String attrKey = sbe.getName();

        // Don't process an attribute set by an interceptor
        if ((attrKey != null) && attrKey.startsWith("CDI")) {

            HttpSession sess = sbe.getSession();

            Object attrValue = sbe.getValue();

            if (attrValue != null) {
                char lastChar = attrValue.toString().charAt(attrValue.toString().length() - 1);
                sess.setAttribute("SALInterceptor", ":I" + lastChar);
            } else {
                sess.setAttribute("SALInterceptor", ":Ix");
            }
        }

        return context.proceed();
    }

}

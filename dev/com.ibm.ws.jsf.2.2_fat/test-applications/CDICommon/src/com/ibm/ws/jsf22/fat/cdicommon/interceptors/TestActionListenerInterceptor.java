/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.cdicommon.interceptors;

import javax.annotation.Priority;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import  com.ibm.ws.jsf22.fat.cdicommon.beans.ActionListenerBean;

/**
 * Interceptor locates the actionListenerBean and, if found (means it has been used for
 * the current session) adds data to the bean to show that the interceptor was called.
 */
@Interceptor
@TestActionListener
@Priority(Interceptor.Priority.APPLICATION)
public class TestActionListenerInterceptor {

    @AroundInvoke
    public Object checkParams(InvocationContext context) throws Exception {
        Object[] params = context.getParameters();

        if (params.length >= 1) {
            ActionEvent event = ((ActionEvent) params[0]);

            ActionListenerBean testBean = (ActionListenerBean) FacesContext.getCurrentInstance().
                            getExternalContext().getSessionMap().get("actionListenerBean");

            if (testBean != null) {
                testBean.setData("TestActionListenerInterceptor");
            }
        }
        context.setParameters(params);
        return context.proceed();
    }

}

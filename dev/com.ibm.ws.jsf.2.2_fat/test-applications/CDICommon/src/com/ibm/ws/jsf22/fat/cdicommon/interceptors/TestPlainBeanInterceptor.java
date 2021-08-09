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
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import  com.ibm.ws.jsf22.fat.cdicommon.beans.TestBean;

/**
 * Interceptor locates the testBean and, if found (means it has been used for
 * the current session), adds data to the bean to show that the interceptor was called.
 */
@Interceptor
@TestPlainBean
@Priority(Interceptor.Priority.APPLICATION)
public class TestPlainBeanInterceptor {

    @AroundInvoke
    public Object checkParams(InvocationContext context) throws Exception {
        TestBean testBean = (TestBean) FacesContext.getCurrentInstance().
                        getExternalContext().getSessionMap().get("testBean");

        if (testBean != null) {
            testBean.setData(":TestPlainBeanInterceptor:");
        }

        return context.proceed();
    }

}

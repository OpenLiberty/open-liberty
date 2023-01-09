/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.jee.ejbWithJsp.ejb;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.interceptors.MyAnotherEJBInterceptor;
import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.interceptors.MyEJBInterceptor;

@Stateful(name = "MySessionBean1")
@LocalBean
@Interceptors({ MyAnotherEJBInterceptor.class, MyEJBInterceptor.class })
public class MySessionBean1 implements SessionBeanInterface {

    @Inject
    MyCDIBean1 cdiBean;

    @Resource(name = "greeting")
    Integer greeting;

    public String hello() {
        return cdiBean.hello() + greeting + "\n";
    }

}

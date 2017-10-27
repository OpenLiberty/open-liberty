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
package cdi12.helloworld.jeeResources.ejb;

import javax.annotation.ManagedBean;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import cdi12.helloworld.jeeResources.ejb.interceptors.MyAnotherEJBInterceptor;
import cdi12.helloworld.jeeResources.ejb.interceptors.MyEJBInterceptor;
import cdi12.helloworld.jeeResources.ejb.interceptors.MyManagedBeanEJBInterceptor;

@ManagedBean("MyManagedBean1")
@Interceptors({ MyEJBInterceptor.class, MyAnotherEJBInterceptor.class, MyManagedBeanEJBInterceptor.class })
public class MyManagedBean1 implements ManagedBeanInterface {

    @Inject
    MyCDIBean1 cdiBean;

    @Resource(lookup = "globalGreeting")
    private String greeting;

    public String hello() {
        return greeting + "\n" + cdiBean.hello();
    }

}

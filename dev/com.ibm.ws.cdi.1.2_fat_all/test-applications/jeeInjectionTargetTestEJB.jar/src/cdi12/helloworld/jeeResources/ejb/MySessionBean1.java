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

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import cdi12.helloworld.jeeResources.ejb.interceptors.MyAnotherEJBInterceptor;
import cdi12.helloworld.jeeResources.ejb.interceptors.MyEJBInterceptor;

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

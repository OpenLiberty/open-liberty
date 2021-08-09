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

package cdi12.helloworld.jeeResources.test;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import cdi12.helloworld.jeeResources.ejb.MyEJBDefinedInXml;
import cdi12.helloworld.jeeResources.ejb.MyManagedBean1;
import cdi12.helloworld.jeeResources.ejb.MySessionBean1;
import cdi12.helloworld.jeeResources.ejb.MySessionBean2;

@RequestScoped
public class HelloWorldExtensionBean2 {

    @Inject
    MySessionBean1 bean1;

    @Inject
    MySessionBean2 bean2;

    @Resource
    MyManagedBean1 managedBean1;

    @Inject
    MyEJBDefinedInXml bean3;

    public String hello() {
        return bean1.hello() + bean2.hello() + managedBean1.hello() + bean3.hello();
    }

}

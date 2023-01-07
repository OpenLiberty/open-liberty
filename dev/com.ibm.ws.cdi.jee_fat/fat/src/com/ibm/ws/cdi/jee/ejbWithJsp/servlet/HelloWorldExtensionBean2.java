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

package com.ibm.ws.cdi.jee.ejbWithJsp.servlet;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.MyEJBDefinedInXml;
import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.MyManagedBean1;
import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.MySessionBean1;
import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.MySessionBean2;

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

/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.fat.ejbinwarpackaging;

import static org.junit.Assert.assertEquals;

import javax.ejb.EJB;

import org.junit.Assert;

import com.ibm.ws.ejbcontainer.fat.beaninterfaceholderlib.BasicInterceptorLocal;
import com.ibm.ws.ejbcontainer.fat.beaninterfaceholderlib.EJBInWARPackagingLocal;
import com.ibm.ws.ejbcontainer.fat.beaninterfaceholderlib.EJBInWARPackagingStatefulBean;
import com.ibm.ws.ejbcontainer.fat.ejbinwarpackaging.ejb.EJBInWARPackagingSingletonBean;
import com.ibm.ws.ejbcontainer.fat.ejbinwarpackaging.ejb.EJBInWARPackagingStatelessBean;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
public class EJBInWARPackagingServlet extends FATServlet {
    EJBInWARPackagingLocal singleton;
    EJBInWARPackagingLocal stateful;
    EJBInWARPackagingLocal stateless;

    @EJB(beanName = "BasicInterceptorStatefulBean")
    BasicInterceptorLocal beanInterceptor;

    public void verifyBeanName() {
        Assert.assertEquals(EJBInWARPackagingStatelessBean.class.getName(), stateless.getBeanName());
        Assert.assertEquals(EJBInWARPackagingSingletonBean.class.getName(), singleton.getBeanName());
        Assert.assertEquals(EJBInWARPackagingStatefulBean.class.getName(), stateful.getBeanName());
    }

    public void verifyInterceptor() {
        assertEquals("Incorrect bean was injected", "BasicInterceptorStatefulBean", beanInterceptor.getSimpleBeanName());

        // Verify PostConstruct was called which checks if interceptor's postConstruct was called.
        beanInterceptor.verifyPostConstruct();

        // Verify interceptor's AroundInvoke was called.
        beanInterceptor.verifyInterceptorAroundInvoke();

        beanInterceptor.remove();
    }

}

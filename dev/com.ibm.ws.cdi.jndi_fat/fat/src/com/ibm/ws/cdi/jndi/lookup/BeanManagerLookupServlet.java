/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.jndi.lookup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/")
public class BeanManagerLookupServlet extends FATServlet {

    private static final long serialVersionUID = 8549700799591343964L;

    @Inject
    BeanManager injectedBeanManager;

    @Inject
    MyBean myRequestScopedBean;

    private BeanManager getBeanMangerViaJNDI() throws Exception {
        return (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
    }

    public void testBeanManager(String beanManagerDescription, BeanManager beanManager) {
        assertNotNull(beanManagerDescription + " was null", beanManager);

        Set<Bean<?>> set = beanManager.getBeans(MyBean.class);
        assertFalse(beanManagerDescription + " could not find any Beans for class MyBean", set.isEmpty());
    }

    @Test
    public void testCDICurrent() throws Exception {
        CDI cdi = CDI.current();
        BeanManager cdiCurrentBeanManager = cdi.getBeanManager();
        testBeanManager("BeanManager from CDI.current()", cdiCurrentBeanManager);
    }

    @Test
    public void testServletJNDILookup() throws Exception {
        BeanManager jndiLookupBeanManager = getBeanMangerViaJNDI();
        testBeanManager("BeanManager from Servlet JNDI lookup", jndiLookupBeanManager);
    }

    @Test
    public void testInjected() throws Exception {
        testBeanManager("BeanManager injected into Servlet", injectedBeanManager);
    }

    @Test
    public void testBeanJNDILookup() throws Exception {
        BeanManager jndiLookupBeanManager = myRequestScopedBean.getBeanMangerViaJNDI();
        testBeanManager("BeanManager from Bean JNDI lookup", jndiLookupBeanManager);
    }
}

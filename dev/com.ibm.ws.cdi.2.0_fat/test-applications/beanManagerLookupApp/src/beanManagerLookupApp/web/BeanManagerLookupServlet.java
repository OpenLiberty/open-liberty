/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package beanManagerLookupApp.web;

import static org.junit.Assert.assertTrue;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/beanManagerLookupApp")
public class BeanManagerLookupServlet extends FATServlet {

    @Inject
    BeanManager bmI;

    @Inject
    MyBeanCDI20 mb;

    @Test
    @Mode(TestMode.LITE)
    public void testbeanManagerLookup() throws Exception {
        CDI cdi = CDI.current();
        BeanManager bm = cdi.getBeanManager();
        assertTrue("Bean manager from CDI.current().getBeanManager was not a BeanManager - " + bm, bm instanceof BeanManager);
    }

    @Test
    @Mode(TestMode.LITE)
    public void testbeanManagerLookupInject() throws Exception {
        assertTrue("Bean manager from inject was not a BeanManager - " + bmI, bmI instanceof BeanManager);
    }

    @Test
    @Mode(TestMode.LITE)
    public void testbeanManagerLookupJndi() throws Exception {
        Context c;
        BeanManager bmJ = null;
        try {
            c = new InitialContext();
            bmJ = (BeanManager) c.lookup("java:comp/BeanManager");
            assertTrue("Bean manager from JNDI was not a BeanManager - " + bmJ, bmJ instanceof BeanManager);
        } catch (NamingException e) {
            throw new AssertionError("JNDI lookup failed");
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.fat.ejbinwar;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.EJBInWARLocal;
import com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.EJBInWARSingletonBean;
import com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.EJBInWARStatelessBean;

import componenttest.app.FATServlet;

@WebServlet("/EJBInWARServlet")
@SuppressWarnings("serial")
public class EJBInWARServlet extends FATServlet {
    @EJB(beanName = "EJBInWARSingletonBean")
    EJBInWARLocal singleton;

    @EJB(name = "ejb/servletdef/stateless", beanName = "EJBInWARStatelessBean")
    EJBInWARLocal stateless;

    /**
     * Ensure that an EJB in a WAR can be injected into a servlet.
     */
    @Test
    public void testInjection() {
        singleton.verifyInjection();
        stateless.verifyInjection();
    }

    private static Object lookup(String name) {
        try {
            return new InitialContext().lookup(name);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Ensure that a servlet and two EJBs all share the same component
     * namespace.
     */
    @Test
    public void testSharedNamespace() {
        Assert.assertEquals(EJBInWARStatelessBean.class, ((EJBInWARLocal) lookup("java:comp/env/ejb/servletdef/stateless")).getEJBClass());
        Assert.assertEquals(EJBInWARStatelessBean.class, ((EJBInWARLocal) lookup("java:comp/env/ejb/singletondef/stateless")).getEJBClass());
        Assert.assertEquals(EJBInWARStatelessBean.class, ((EJBInWARLocal) lookup("java:comp/env/ejb/statelessdef/stateless")).getEJBClass());

        singleton.verifySharedLookup();
        stateless.verifySharedLookup();

        try {
            new InitialContext().lookup("java:comp/EJBContext");
            Assert.fail();
        } catch (NamingException e) {
            System.out.println("Ignoring expected: " + e);
        }
    }

    /**
     * Ensure that java:global/app/module works for EJB in WAR.
     */
    @Test
    public void testJavaColonLookup() {
        Assert.assertEquals(EJBInWARStatelessBean.class,
                            ((EJBInWARLocal) lookup("java:module/EJBInWARStatelessBean!com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.EJBInWARLocal")).getEJBClass());
        Assert.assertEquals(EJBInWARSingletonBean.class,
                            ((EJBInWARLocal) lookup("java:module/EJBInWARSingletonBean!com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.EJBInWARLocal")).getEJBClass());

        Assert.assertEquals(EJBInWARStatelessBean.class,
                            ((EJBInWARLocal) lookup("java:app/EJBInWAR/EJBInWARStatelessBean!com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.EJBInWARLocal")).getEJBClass());
        Assert.assertEquals(EJBInWARSingletonBean.class,
                            ((EJBInWARLocal) lookup("java:app/EJBInWAR/EJBInWARSingletonBean!com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.EJBInWARLocal")).getEJBClass());

        Assert.assertEquals(EJBInWARStatelessBean.class,
                            ((EJBInWARLocal) lookup("java:global/EJBInWAR/EJBInWARStatelessBean!com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.EJBInWARLocal")).getEJBClass());
        Assert.assertEquals(EJBInWARSingletonBean.class,
                            ((EJBInWARLocal) lookup("java:global/EJBInWAR/EJBInWARSingletonBean!com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.EJBInWARLocal")).getEJBClass());

        singleton.verifyJavaColonLookup();
        stateless.verifyJavaColonLookup();
    }
}

/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejb2x.defbnd.web;

import static org.junit.Assert.fail;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.defbnd.ejb.EJB2XDefBnd;
import com.ibm.ejb2x.defbnd.ejb.EJB2XDefBndHome;
import com.ibm.ejb2x.defbnd.ejb.EJB2XDefBndRemote;
import com.ibm.ejb2x.defbnd.ejb.EJB2XDefBndRemoteHome;
import com.ibm.websphere.ejbcontainer.LocalHomeAccessor;

import componenttest.app.FATServlet;

/**
 * Tests creating a "default" binding for 2X and 1X. WAS has an app install step
 * where it will generate a bnd.xmi if you do not have one so they will always have a
 * JNDIName on WAS, but might not on Liberty.
 */
@SuppressWarnings("serial")
@WebServlet("/EJB2XDefBndTestServlet")
public class EJB2XDefBndTestServlet extends FATServlet {

    @Test
    public void test2XLocalDefault() throws Exception {
        EJB2XDefBndHome beanHome = (EJB2XDefBndHome) new InitialContext().lookup("local:ejb/Test2XDefBndBean");
        if (beanHome == null) {
            fail("lookup local:ejb/Test2XDefBndBean should have worked");
        }
        EJB2XDefBnd bean = beanHome.create();
        if (beanHome.create() == null) {
            fail("home.create() for lookup local:ejb/Test2XDefBndBean should have worked");
        }
        System.out.println("Got bean, calling method");
        if (bean.foo() == null) {
            fail("bean.method() for lookup local:ejb/Test2XDefBndBean should have worked");
        }
    }

    @Test
    public void test2XLocalDefaultLocalHomeAccessor() throws Exception {
        EJB2XDefBndHome beanHome = (EJB2XDefBndHome) LocalHomeAccessor.lookup("Test2XDefBndBean");
        if (beanHome == null) {
            fail("LocalHomeAccessor lookup Test2XDefBndBean should have worked");
        }
        EJB2XDefBnd bean = beanHome.create();
        if (beanHome.create() == null) {
            fail("home.create() for LocalHomeAccessor lookup Test2XDefBndBean should have worked");
        }
        System.out.println("Got bean, calling method");
        if (bean.foo() == null) {
            fail("bean.method() for LocalHomeAccessor lookup Test2XDefBndBean should have worked");
        }
    }

    @Test
    public void test2XEJBLocalDefault() throws Exception {
        EJB2XDefBndHome beanHome = (EJB2XDefBndHome) new InitialContext().lookup("ejblocal:ejb/Test2XDefBndBean");
        if (beanHome == null) {
            fail("lookup ejblocal:ejb/Test2XDefBndBean should have worked");
        }
        EJB2XDefBnd bean = beanHome.create();
        if (beanHome.create() == null) {
            fail("home.create() for lookup ejblocal:ejb/Test2XDefBndBean should have worked");
        }
        System.out.println("Got bean, calling method");
        if (bean.foo() == null) {
            fail("bean.method() for lookup ejblocal:ejb/Test2XDefBndBean should have worked");
        }
    }

    @Test
    public void test2XRemoteDefault() throws Exception {
        EJB2XDefBndRemoteHome beanHome = (EJB2XDefBndRemoteHome) new InitialContext().lookup("ejb/Test2XDefBndBean");
        if (beanHome == null) {
            fail("lookup ejb/Test2XDefBndBean should have worked");
        }
        EJB2XDefBndRemote bean = beanHome.create();
        if (beanHome.create() == null) {
            fail("home.create() for lookup ejb/Test2XDefBndBean should have worked");
        }
        System.out.println("Got bean, calling method");
        if (bean.foo() == null) {
            fail("bean.method() for lookup ejb/Test2XDefBndBean should have worked");
        }
    }

    @Test
    public void test3XStyleDefaultDisabled() throws Exception {
        try {
            Object lookup = new InitialContext().lookup("ejblocal:EJB2XDefBndTestApp/EJB2XDefBndEJB.jar/Test2XDefBndBean#com.ibm.ejb2x.defbnd.ejb.EJB2XDefBndHome");
            if (lookup != null) {
                fail("EJB3X style default bindings should not have worked for 2X bean.");
            }
        } catch (NamingException e) {
            // expected to not work
        }
    }

    @Test
    public void test3XStyleRemoteDefaultDisabled() throws Exception {
        try {
            Object lookup = new InitialContext().lookup("ejb/EJB2XDefBndTestApp/EJB2XDefBndEJB.jar/Test2XDefBndBean#com.ibm.ejb2x.defbnd.ejb.EJB2XDefBndRemoteHome");
            if (lookup != null) {
                fail("EJB3X style remote default bindings should not have worked for 2X bean.");
            }
        } catch (NamingException e) {
            // expected to not work
        }
    }
}

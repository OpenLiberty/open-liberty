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
package com.ibm.ws.ejbcontainer.bindings.configtests.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsLocalEJB;
import com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsLocalHome;
import com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsRemoteEJB;
import com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsRemoteHome;
import com.ibm.ws.ejbcontainer.bindings.configtests.ejb.JavaColonLookupLocalEJB;
import com.ibm.ws.ejbcontainer.bindings.configtests.ejb.JavaColonLookupLocalHome;

import componenttest.app.FATServlet;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/BindToServerRootServlet")
public class BindToServerRootServlet extends FATServlet {

    private InitialContext ctx = null;

    @PostConstruct
    protected void setUp() {
        try {
            ctx = new InitialContext();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    // bindToServerRoot not set (default true)
    public void testNoBindToServerRootElement() throws Exception {
        testHelper(true);
    }

    // bindToJavaGlobal false
    public void testFalseBindToServerRootElement() throws Exception {
        testHelper(false);
    }

    // bindToJavaGlobal explicitly set true
    public void testTrueBindToServerRootElement() throws Exception {
        testHelper(true);
    }

    private void testHelper(boolean bindToServerRoot) throws Exception {
        lookupShort(bindToServerRoot);
        lookupRemoteShort(bindToServerRoot);
        lookupLong(bindToServerRoot);
        lookupRemoteLong(bindToServerRoot);

        // use helper bean to lookup java namespaces because of java:module
        JavaColonLookupLocalHome home = (JavaColonLookupLocalHome) ctx.lookup("java:global/ConfigTestsTestApp/ConfigTestsEJB/JavaColonLookupBean!com.ibm.ws.ejbcontainer.bindings.configtests.ejb.JavaColonLookupLocalHome");
        JavaColonLookupLocalEJB jclbean = home.create();
        //Should always work for this test
        jclbean.lookupJavaNamespaces(true);

    }

    private void lookupShort(boolean bindToServerRoot) throws Exception {
        try {
            ConfigTestsLocalHome home = (ConfigTestsLocalHome) ctx.lookup("ejblocal:com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsLocalHome");
            if (bindToServerRoot) {
                ConfigTestsLocalEJB bean = home.create();
                assertNotNull("ConfigTestsLocalEJB short default lookup did not succeed.", bean);
                String str = bean.getString();
                assertEquals("getString() returned unexpected value", "Success", str);
            } else {
                assertNull("ConfigTestsLocalEJB short default lookup should have failed", home);
            }
        } catch (NameNotFoundException nnfe) {
            if (bindToServerRoot) {
                fail("ConfigTestsLocalEJB short default lookup did not succeed. Got NameNotFoundException");
            }
            // else expected
        }
    }

    private void lookupRemoteShort(boolean bindToServerRoot) throws Exception {
        try {
            Object lookup = ctx.lookup("com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsRemoteHome");
            if (bindToServerRoot) {
                ConfigTestsRemoteHome home = (ConfigTestsRemoteHome) lookup;
                ConfigTestsRemoteEJB bean = home.create();
                assertNotNull("ConfigTestsRemoteEJB short default lookup did not succeed.", bean);
                String str = bean.getString();
                assertEquals("getString() returned unexpected value", "Success", str);
            } else {
                assertNull("ConfigTestsRemoteEJB short default lookup should have failed", lookup);
            }
        } catch (NameNotFoundException nnfe) {
            if (bindToServerRoot) {
                fail("ConfigTestsRemoteEJB short default lookup did not succeed. Got NameNotFoundException");
            }
            // else expected
        }

    }

    private void lookupLong(boolean bindToServerRoot) throws Exception {
        try {
            ConfigTestsLocalHome home = (ConfigTestsLocalHome) ctx.lookup("ejblocal:ConfigTestsTestApp/ConfigTestsEJB.jar/ConfigTestsTestBean#com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsLocalHome");
            if (bindToServerRoot) {
                ConfigTestsLocalEJB bean = home.create();
                assertNotNull("ConfigTestsLocalEJB long default lookup did not succeed.", bean);
                String str = bean.getString();
                assertEquals("getString() returned unexpected value", "Success", str);
            } else {
                assertNull("ConfigTestsLocalEJB long default lookup should have failed", home);
            }
        } catch (NameNotFoundException nnfe) {
            if (bindToServerRoot) {
                fail("ConfigTestsLocalEJB long default lookup did not succeed. Got NameNotFoundException");
            }
            // else expected
        }

    }

    private void lookupRemoteLong(boolean bindToServerRoot) throws Exception {
        try {
            Object lookup = ctx.lookup("ejb/ConfigTestsTestApp/ConfigTestsEJB.jar/ConfigTestsTestBean#com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsRemoteHome");
            if (bindToServerRoot) {
                ConfigTestsRemoteHome home = (ConfigTestsRemoteHome) lookup;
                ConfigTestsRemoteEJB bean = home.create();
                assertNotNull("ConfigTestsRemoteEJB long default lookup did not succeed.", bean);
                String str = bean.getString();
                assertEquals("getString() returned unexpected value", "Success", str);
            } else {
                assertNull("ConfigTestsRemoteEJB long default lookup should have failed", lookup);
            }
        } catch (NameNotFoundException nnfe) {
            if (bindToServerRoot) {
                fail("ConfigTestsRemoteEJB long default lookup did not succeed. Got NameNotFoundException");
            }
            // else expected
        }
    }

}

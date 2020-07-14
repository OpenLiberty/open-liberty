/*******************************************************************************
 * Copyright (c)  2020 IBM Corporation and others.
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
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsLocalEJB;
import com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsLocalHome;
import com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsRemoteEJB;
import com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsRemoteHome;

import componenttest.app.FATServlet;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/DisableShortBndServlet")
public class DisableShortBndServlet extends FATServlet {

    private InitialContext ctx = null;

    @PostConstruct
    protected void setUp() {
        try {
            ctx = new InitialContext();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test all lookups work without config element
     *
     * @throws Exception
     */
    public void testNoShortBindingsDisabled() throws Exception {
        testHelper(true, true);
    }

    /**
     * Test short is disabled when "*" is specified
     *
     * @throws Exception
     */
    public void testAllShortBindingsDisabled() throws Exception {
        testHelper(false, false);
    }

    /**
     * Test short is NOT disabled when other app is specified
     *
     * @throws Exception
     */
    public void testOtherAppShortBindingsDisabled() throws Exception {
        testHelper(true, true);
    }

    /**
     * Test short is disabled when app is specified
     *
     * @throws Exception
     */
    public void testAppShortBindingsDisabled() throws Exception {
        testHelper(false, false);
    }

    /**
     * Test short is disabled when both apps are specified
     *
     * @throws Exception
     */
    public void testBothAppShortBindingsDisabled() throws Exception {
        testHelper(false, false);
    }

    private void testHelper(boolean ShortLocalShouldWork, boolean ShortRemoteShouldWork) throws Exception {
        // Local short
        ConfigTestsLocalEJB bean = lookupShort(ShortLocalShouldWork);
        if (ShortLocalShouldWork) {
            assertNotNull("1 ---> ConfigTestsLocalEJB short default lookup did not succeed.", bean);
            String str = bean.getString();
            assertEquals("2 ---> getString() returned unexpected value", "Success", str);
        } else {
            assertNull("3 --> ConfigTestsLocalEJB lookup should have failed.", bean);
        }

        // Remote Short
        ConfigTestsRemoteEJB rbean = lookupRemoteShort(ShortRemoteShouldWork);
        if (ShortRemoteShouldWork) {
            assertNotNull("4 ---> ConfigTestsRemoteEJB short default lookup did not succeed.", rbean);
            String rstr = rbean.getString();
            assertEquals("5 ---> getString() returned unexpected value", "Success", rstr);
        } else {
            assertNull("6 --> ConfigTestsRemoteEJB lookup should have failed.", rbean);
        }

        // Local Long
        ConfigTestsLocalEJB longBean = lookupLong();
        assertNotNull("7 ---> ConfigTestsLocalEJB long default lookup did not succeed.", longBean);
        String lstr = longBean.getString();
        assertEquals("8 ---> getString() returned unexpected value", "Success", lstr);

        // Remote Long
        ConfigTestsRemoteEJB rlongBean = lookupRemoteLong();
        assertNotNull("9 ---> ConfigTestsRemoteEJB long default lookup did not succeed.", rlongBean);
        String rlstr = rlongBean.getString();
        assertEquals("10 ---> getString() returned unexpected value", "Success", rlstr);
    }

    private ConfigTestsLocalEJB lookupShort(boolean shouldWork) throws Exception {
        try {
            ConfigTestsLocalHome home = (ConfigTestsLocalHome) ctx.lookup("ejblocal:com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsLocalHome");
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("ConfigTestsLocalEJB short default lookup did not succeed.");
            } else {
                //expected
            }
        }
        return null;
    }

    private ConfigTestsRemoteEJB lookupRemoteShort(boolean shouldWork) throws Exception {
        try {
            Object lookup = ctx.lookup("com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsRemoteHome");
            ConfigTestsRemoteHome home = (ConfigTestsRemoteHome) PortableRemoteObject.narrow(lookup, ConfigTestsRemoteHome.class);
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("ConfigTestsRemoteEJB short default lookup did not succeed.");
            } else {
                //expected
            }
        }
        return null;
    }

    private ConfigTestsLocalEJB lookupLong() throws Exception {
        ConfigTestsLocalHome home = (ConfigTestsLocalHome) ctx.lookup("ejblocal:ConfigTestsTestApp/ConfigTestsEJB.jar/ConfigTestsTestBean#com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsLocalHome");
        return home.create();
    }

    private ConfigTestsRemoteEJB lookupRemoteLong() throws Exception {
        Object lookup = ctx.lookup("ejb/ConfigTestsTestApp/ConfigTestsEJB.jar/ConfigTestsTestBean#com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsRemoteHome");
        ConfigTestsRemoteHome home = (ConfigTestsRemoteHome) PortableRemoteObject.narrow(lookup, ConfigTestsRemoteHome.class);
        return home.create();
    }

}

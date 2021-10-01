/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.configtests.otherweb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.ejbcontainer.bindings.configtests.otherejb.ConfigTestsOtherLocalEJB;
import com.ibm.ws.ejbcontainer.bindings.configtests.otherejb.ConfigTestsOtherLocalHome;
import com.ibm.ws.ejbcontainer.bindings.configtests.otherejb.ConfigTestsOtherRemoteEJB;
import com.ibm.ws.ejbcontainer.bindings.configtests.otherejb.ConfigTestsOtherRemoteHome;

import componenttest.app.FATServlet;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/DisableShortBndOtherServlet")
public class DisableShortBndOtherServlet extends FATServlet {

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
     * Test short is disabled when app is specified
     *
     * @throws Exception
     */
    public void testOtherAppShortBindingsDisabled() throws Exception {
        testHelper(false, false);
    }

    /**
     * Test short is NOT disabled when other app is specified
     *
     * @throws Exception
     */
    public void testAppShortBindingsDisabled() throws Exception {
        testHelper(true, true);
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
        ConfigTestsOtherLocalEJB bean = lookupShort2x(ShortLocalShouldWork);
        if (ShortLocalShouldWork) {
            assertNotNull("1 ---> ConfigTestsOtherLocalEJB short default lookup did not succeed.", bean);
            String str = bean.getString();
            assertEquals("2 ---> getString() returned unexpected value", "Success", str);
        } else {
            assertNull("3 --> ConfigTestsOtherLocalEJB lookup should have failed.", bean);
        }

        // Remote Short
        ConfigTestsOtherRemoteEJB rbean = lookupRemoteShort2x(ShortRemoteShouldWork);
        if (ShortRemoteShouldWork) {
            assertNotNull("4 ---> ConfigTestsOtherRemoteEJB short default lookup did not succeed.", rbean);
            String rstr = rbean.getString();
            assertEquals("5 ---> getString() returned unexpected value", "Success", rstr);
        } else {
            assertNull("6 --> ConfigTestsOtherRemoteEJB lookup should have failed.", rbean);
        }

        // Local Long
        ConfigTestsOtherLocalEJB longBean = lookupLong2x();
        assertNotNull("7 ---> ConfigTestsOtherLocalEJB long default lookup did not succeed.", longBean);
        String lstr = longBean.getString();
        assertEquals("8 ---> getString() returned unexpected value", "Success", lstr);

        // Remote Long
        ConfigTestsOtherRemoteEJB rlongBean = lookupRemoteLong2x();
        assertNotNull("9 ---> ConfigTestsOtherRemoteEJB long default lookup did not succeed.", rlongBean);
        String rlstr = rlongBean.getString();
        assertEquals("10 ---> getString() returned unexpected value", "Success", rlstr);
    }

    private ConfigTestsOtherLocalEJB lookupShort2x(boolean shouldWork) throws Exception {
        try {
            ConfigTestsOtherLocalHome home = (ConfigTestsOtherLocalHome) ctx.lookup("ejblocal:com.ibm.ws.ejbcontainer.bindings.configtests.otherejb.ConfigTestsOtherLocalHome");
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("ConfigTestsOtherLocalEJB short default lookup did not succeed.");
            } else {
                //expected
            }
        }
        return null;
    }

    private ConfigTestsOtherRemoteEJB lookupRemoteShort2x(boolean shouldWork) throws Exception {
        try {
            ConfigTestsOtherRemoteHome home = (ConfigTestsOtherRemoteHome) ctx.lookup("com.ibm.ws.ejbcontainer.bindings.configtests.otherejb.ConfigTestsOtherRemoteHome");
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("ConfigTestsOtherRemoteEJB short default lookup did not succeed.");
            } else {
                //expected
            }
        }
        return null;
    }

    private ConfigTestsOtherLocalEJB lookupLong2x() throws Exception {
        ConfigTestsOtherLocalHome home = (ConfigTestsOtherLocalHome) ctx.lookup("ejblocal:ConfigTestsOtherTestApp/ConfigTestsOtherEJB.jar/ConfigTestsOtherTestBean#com.ibm.ws.ejbcontainer.bindings.configtests.otherejb.ConfigTestsOtherLocalHome");
        return home.create();
    }

    private ConfigTestsOtherRemoteEJB lookupRemoteLong2x() throws Exception {
        ConfigTestsOtherRemoteHome home = (ConfigTestsOtherRemoteHome) ctx.lookup("ejb/ConfigTestsOtherTestApp/ConfigTestsOtherEJB.jar/ConfigTestsOtherTestBean#com.ibm.ws.ejbcontainer.bindings.configtests.otherejb.ConfigTestsOtherRemoteHome");
        return home.create();
    }

}

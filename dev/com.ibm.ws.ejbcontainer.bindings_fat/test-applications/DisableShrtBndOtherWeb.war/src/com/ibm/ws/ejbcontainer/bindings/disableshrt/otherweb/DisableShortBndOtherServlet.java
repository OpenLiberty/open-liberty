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
package com.ibm.ws.ejbcontainer.bindings.disableshrt.otherweb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.ejbcontainer.bindings.disableshrt.otherejb.DShrtOtherLocalEJB;
import com.ibm.ws.ejbcontainer.bindings.disableshrt.otherejb.DShrtOtherRemoteEJB;
import com.ibm.ws.ejbcontainer.bindings.disableshrt.otherejb.DShrtOtherTestLocalHome;
import com.ibm.ws.ejbcontainer.bindings.disableshrt.otherejb.DShrtOtherTestRemoteHome;

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
        DShrtOtherLocalEJB bean = lookupShort2x(ShortLocalShouldWork);
        if (ShortLocalShouldWork) {
            assertNotNull("1 ---> DShrtOtherLocalEJB short default lookup did not succeed.", bean);
            String str = bean.getString();
            assertEquals("2 ---> getString() returned unexpected value", "Success", str);
        } else {
            assertNull("1 --> DShrtOtherLocalEJB lookup should have failed.", bean);
        }

        // Remote Short
        DShrtOtherRemoteEJB rbean = lookupRemoteShort2x(ShortRemoteShouldWork);
        if (ShortRemoteShouldWork) {
            assertNotNull("3 ---> DShrtOtherRemoteEJB short default lookup did not succeed.", rbean);
            String rstr = rbean.getString();
            assertEquals("4 ---> getString() returned unexpected value", "Success", rstr);
        } else {
            assertNull("3 --> DShrtOtherRemoteEJB lookup should have failed.", rbean);
        }

        // Local Long
        DShrtOtherLocalEJB longBean = lookupLong2x();
        assertNotNull("5 ---> DShrtOtherLocalEJB long default lookup did not succeed.", longBean);
        String lstr = longBean.getString();
        assertEquals("6 ---> getString() returned unexpected value", "Success", lstr);

        // Remote Long
        DShrtOtherRemoteEJB rlongBean = lookupRemoteLong2x();
        assertNotNull("7 ---> DShrtOtherRemoteEJB long default lookup did not succeed.", rlongBean);
        String rlstr = rlongBean.getString();
        assertEquals("8 ---> getString() returned unexpected value", "Success", rlstr);
    }

    private DShrtOtherLocalEJB lookupShort2x(boolean shouldWork) throws Exception {
        try {
            DShrtOtherTestLocalHome home = (DShrtOtherTestLocalHome) ctx.lookup("ejblocal:com.ibm.ws.ejbcontainer.bindings.disableshrt.otherejb.DShrtOtherTestLocalHome");
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("DShrtOtherLocalEJB short default lookup did not succeed.");
            } else {
                //expected
            }
        }
        return null;
    }

    private DShrtOtherRemoteEJB lookupRemoteShort2x(boolean shouldWork) throws Exception {
        try {
            Object lookup = ctx.lookup("com.ibm.ws.ejbcontainer.bindings.disableshrt.otherejb.DShrtOtherTestRemoteHome");
            DShrtOtherTestRemoteHome home = (DShrtOtherTestRemoteHome) PortableRemoteObject.narrow(lookup, DShrtOtherTestRemoteHome.class);
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("DShrtOtherRemoteEJB short default lookup did not succeed.");
            } else {
                //expected
            }
        }
        return null;
    }

    private DShrtOtherLocalEJB lookupLong2x() throws Exception {
        DShrtOtherTestLocalHome home = (DShrtOtherTestLocalHome) ctx.lookup("ejblocal:DisableShrtBndOtherTestApp/DisableShrtBndOtherEJB.jar/DShrtOtherTestBean#com.ibm.ws.ejbcontainer.bindings.disableshrt.otherejb.DShrtOtherTestLocalHome");
        return home.create();
    }

    private DShrtOtherRemoteEJB lookupRemoteLong2x() throws Exception {
        Object lookup = ctx.lookup("ejb/DisableShrtBndOtherTestApp/DisableShrtBndOtherEJB.jar/DShrtOtherTestBean#com.ibm.ws.ejbcontainer.bindings.disableshrt.otherejb.DShrtOtherTestRemoteHome");
        DShrtOtherTestRemoteHome home = (DShrtOtherTestRemoteHome) PortableRemoteObject.narrow(lookup, DShrtOtherTestRemoteHome.class);
        return home.create();
    }

}

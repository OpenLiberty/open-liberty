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
package com.ibm.ws.ejbcontainer.bindings.disableshrt.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.ejbcontainer.bindings.disableshrt.ejb.DShrtLocalEJB;
import com.ibm.ws.ejbcontainer.bindings.disableshrt.ejb.DShrtRemoteEJB;
import com.ibm.ws.ejbcontainer.bindings.disableshrt.ejb.DShrtTestLocalHome;
import com.ibm.ws.ejbcontainer.bindings.disableshrt.ejb.DShrtTestRemoteHome;

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
        DShrtLocalEJB bean = lookupShort(ShortLocalShouldWork);
        if (ShortLocalShouldWork) {
            assertNotNull("1 ---> DShrtLocalEJB short default lookup did not succeed.", bean);
            String str = bean.getString();
            assertEquals("2 ---> getString() returned unexpected value", "Success", str);
        } else {
            assertNull("1 --> DShrtLocalEJB lookup should have failed.", bean);
        }

        // Remote Short
        DShrtRemoteEJB rbean = lookupRemoteShort(ShortRemoteShouldWork);
        if (ShortRemoteShouldWork) {
            assertNotNull("3 ---> DShrtRemoteEJB short default lookup did not succeed.", rbean);
            String rstr = rbean.getString();
            assertEquals("4 ---> getString() returned unexpected value", "Success", rstr);
        } else {
            assertNull("3 --> DShrtRemoteEJB lookup should have failed.", rbean);
        }

        // Local Long
        DShrtLocalEJB longBean = lookupLong();
        assertNotNull("5 ---> DShrtLocalEJB long default lookup did not succeed.", longBean);
        String lstr = longBean.getString();
        assertEquals("6 ---> getString() returned unexpected value", "Success", lstr);

        // Remote Long
        DShrtRemoteEJB rlongBean = lookupRemoteLong();
        assertNotNull("7 ---> DShrtRemoteEJB long default lookup did not succeed.", rlongBean);
        String rlstr = rlongBean.getString();
        assertEquals("8 ---> getString() returned unexpected value", "Success", rlstr);
    }

    private DShrtLocalEJB lookupShort(boolean shouldWork) throws Exception {
        try {
            DShrtTestLocalHome home = (DShrtTestLocalHome) ctx.lookup("ejblocal:com.ibm.ws.ejbcontainer.bindings.disableshrt.ejb.DShrtTestLocalHome");
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("DShrtLocalEJB short default lookup did not succeed.");
            } else {
                //expected
            }
        }
        return null;
    }

    private DShrtRemoteEJB lookupRemoteShort(boolean shouldWork) throws Exception {
        try {
            Object lookup = ctx.lookup("com.ibm.ws.ejbcontainer.bindings.disableshrt.ejb.DShrtTestRemoteHome");
            DShrtTestRemoteHome home = (DShrtTestRemoteHome) PortableRemoteObject.narrow(lookup, DShrtTestRemoteHome.class);
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("DShrtRemoteEJB short default lookup did not succeed.");
            } else {
                //expected
            }
        }
        return null;
    }

    private DShrtLocalEJB lookupLong() throws Exception {
        DShrtTestLocalHome home = (DShrtTestLocalHome) ctx.lookup("ejblocal:DisableShrtBndTestApp/DisableShrtBndEJB.jar/DShrtTestBean#com.ibm.ws.ejbcontainer.bindings.disableshrt.ejb.DShrtTestLocalHome");
        return home.create();
    }

    private DShrtRemoteEJB lookupRemoteLong() throws Exception {
        Object lookup = ctx.lookup("ejb/DisableShrtBndTestApp/DisableShrtBndEJB.jar/DShrtTestBean#com.ibm.ws.ejbcontainer.bindings.disableshrt.ejb.DShrtTestRemoteHome");
        DShrtTestRemoteHome home = (DShrtTestRemoteHome) PortableRemoteObject.narrow(lookup, DShrtTestRemoteHome.class);
        return home.create();
    }

}

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

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
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
@WebServlet("/BindToJavaGlobalServlet")
public class BindToJavaGlobalServlet extends FATServlet {

    private InitialContext ctx = null;

    @PostConstruct
    protected void setUp() {
        try {
            ctx = new InitialContext();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    // bindToJavaGlobal not set (default true)
    public void testNoBindToJavaGlobalElement() throws Exception {
        testHelper(true);
    }

    // bindToJavaGlobal false
    public void testFalseBindToJavaGlobalElement() throws Exception {
        testHelper(false);
    }

    // bindToJavaGlobal explicitly set true
    public void testTrueBindToJavaGlobalElement() throws Exception {
        testHelper(true);
    }

    private void testHelper(boolean bindToJavaGlobal) throws Exception {
        // Local short
        ConfigTestsLocalEJB bean = lookupShort();
        assertNotNull("1 ---> ConfigTestsLocalEJB short default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);

        // Remote Short
        ConfigTestsRemoteEJB rbean = lookupRemoteShort();
        assertNotNull("3 ---> ConfigTestsRemoteEJB short default lookup did not succeed.", rbean);
        String rstr = rbean.getString();
        assertEquals("4 ---> getString() returned unexpected value", "Success", rstr);

        // Local Long
        ConfigTestsLocalEJB longBean = lookupLong();
        assertNotNull("5 ---> ConfigTestsLocalEJB long default lookup did not succeed.", longBean);
        String lstr = longBean.getString();
        assertEquals("6 ---> getString() returned unexpected value", "Success", lstr);

        // Remote Long
        ConfigTestsRemoteEJB rlongBean = lookupRemoteLong();
        assertNotNull("7 ---> ConfigTestsRemoteEJB long default lookup did not succeed.", rlongBean);
        String rlstr = rlongBean.getString();
        assertEquals("8 ---> getString() returned unexpected value", "Success", rlstr);

        // use helper bean to lookup java namespaces because of java:module
        JavaColonLookupLocalHome home = (JavaColonLookupLocalHome) ctx.lookup("ejblocal:ConfigTestsTestApp/ConfigTestsEJB.jar/JavaColonLookupBean#com.ibm.ws.ejbcontainer.bindings.configtests.ejb.JavaColonLookupLocalHome");
        JavaColonLookupLocalEJB jclbean = home.create();
        jclbean.lookupJavaNamespaces(bindToJavaGlobal);

    }

    private ConfigTestsLocalEJB lookupShort() throws Exception {
        ConfigTestsLocalHome home = (ConfigTestsLocalHome) ctx.lookup("ejblocal:com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsLocalHome");
        return home.create();
    }

    private ConfigTestsRemoteEJB lookupRemoteShort() throws Exception {
        ConfigTestsRemoteHome home = (ConfigTestsRemoteHome) ctx.lookup("com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsRemoteHome");
        return home.create();
    }

    private ConfigTestsLocalEJB lookupLong() throws Exception {
        ConfigTestsLocalHome home = (ConfigTestsLocalHome) ctx.lookup("ejblocal:ConfigTestsTestApp/ConfigTestsEJB.jar/ConfigTestsTestBean#com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsLocalHome");
        return home.create();
    }

    private ConfigTestsRemoteEJB lookupRemoteLong() throws Exception {
        ConfigTestsRemoteHome home = (ConfigTestsRemoteHome) ctx.lookup("ejb/ConfigTestsTestApp/ConfigTestsEJB.jar/ConfigTestsTestBean#com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsRemoteHome");
        return home.create();
    }

}

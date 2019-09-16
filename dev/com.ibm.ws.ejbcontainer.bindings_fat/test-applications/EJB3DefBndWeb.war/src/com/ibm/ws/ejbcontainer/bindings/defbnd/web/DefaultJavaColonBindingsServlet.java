/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.defbnd.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalBusiness;
import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalEJB;
import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.RemoteBusiness;
import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.RemoteEJB;
import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestLocalHome;
import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestRemoteHome;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> DefaultJavaColonBindingsTest.
 *
 * <dt><b>Test Description:</b>
 * <dd>The purpose of this test is to test lookups of business and home intefaces using 'java:global'
 * <p>
 * <dt><b>Test Matrix:</b>
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testJavaColonLocalBI lookup and call local business interface using java: global.
 * <li>testJavaColonLocalCI lookup and call local home view interface using java: global.
 * <li>testJavaColonRemoteBI lookup and call remote business interface using java: global.
 * <li>testJavaColonRemoteCI lookup and call remote home view interface using java: global.
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/DefaultJavaColonBindingsServlet")
public class DefaultJavaColonBindingsServlet extends FATServlet {

    /**
     * lookup and call local business interface using java: global.
     */
    @Test
    public void testJavaColonLocalBI() throws Exception {
        LocalBusiness bean = Helper.lookupDefaultJavaColonLocal(LocalBusiness.class, "TestBean");
        assertNotNull("1 ---> LocalBusiness standard default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call local home view interface using java: global.
     */
    @Test
    public void testJavaColonLocalCI() throws Exception {
        TestLocalHome homeBean = Helper.lookupDefaultJavaColonLocal(TestLocalHome.class, "TestBean");
        assertNotNull("1 ---> LocalHome standard default lookup did not succeed.", homeBean);

        LocalEJB bean = homeBean.create();
        assertNotNull("2 ---> LocalHome create did not succeed.", bean);

        String str = bean.getString();
        assertEquals("3 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call remote business interface using java: global.
     */
    @Test
    public void testJavaColonRemoteBI() throws Exception {
        RemoteBusiness bean = Helper.lookupDefaultJavaColonRemote(RemoteBusiness.class, "TestBean");
        assertNotNull("1 ---> RemoteBusiness standard default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call remote home view interface using java: global.
     */
    @Test
    public void testJavaColonRemoteCI() throws Exception {
        TestRemoteHome homeBean = Helper.lookupDefaultJavaColonRemote(TestRemoteHome.class, "TestBean");
        assertNotNull("1 ---> RemoteHome standard default lookup did not succeed.", homeBean);

        RemoteEJB bean = homeBean.create();
        assertNotNull("2 ---> RemoteHome create did not succeed.", bean);

        String str = bean.getString();
        assertEquals("3 ---> getString() returned unexpected value", "Success", str);
    }
}

/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
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
 * <dt><b>Test Name:</b> DefaultBindingsTest.
 *
 * <dt><b>Test Description:</b>
 * <dd>The purpose of this test is to test legacy style lookups of component view and
 * business interfaces using both styles of default bindings, short and default component format.
 * <p>
 * <dt><b>Test Matrix:</b>
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testLocalBIShort lookup and call local business interface using short name.
 * <li>testRemoteBIShort lookup and call remote business interface using short name.
 * <li>testLocalCIShort lookup and call local component view interface using short name.
 * <li>testRemoteCIShort lookup and call remote component view interface using short name.
 * <li>testLocalBI lookup and call local business interface using component name.
 * <li>testRemoteBI lookup and call remote business interface using component name.
 * <li>testLocalCI lookup and call local component view interface using component name.
 * <li>testRemoteCI lookup and call remote component view interface using component name.
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/DefaultBindingsServlet")
public class DefaultBindingsServlet extends FATServlet {

    /**
     * lookup and call local business interface using short name.
     */
    @Test
    public void testLocalBIShort() throws Exception {
        LocalBusiness bean = Helper.lookupShortLocal(LocalBusiness.class);
        assertNotNull("1 ---> LocalBusiness short default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call remote business interface using short name.
     */
    @Test
    public void testRemoteBIShort() throws Exception {
        RemoteBusiness bean = Helper.lookupShortRemote(RemoteBusiness.class);
        assertNotNull("1 ---> RemoteBusiness short default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call local component view interface using short name.
     */
    @Test
    public void testLocalCIShort() throws Exception {
        TestLocalHome homeBean = Helper.lookupShortLocal(TestLocalHome.class);
        assertNotNull("1 ---> LocalHome short default lookup did not succeed.", homeBean);

        LocalEJB bean = homeBean.create();
        assertNotNull("2 ---> LocalHome create did not succeed.", bean);

        String str = bean.getString();
        assertEquals("3 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call remote component view interface using short name.
     */
    @Test
    public void testRemoteCIShort() throws Exception {
        TestRemoteHome homeBean = Helper.lookupShortRemote(TestRemoteHome.class);
        assertNotNull("1 ---> RemoteHome short default lookup did not succeed.", homeBean);

        RemoteEJB bean = homeBean.create();
        assertNotNull("2 ---> RemoteHome create did not succeed.", bean);

        String str = bean.getString();
        assertEquals("3 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call local business interface using component name.
     */
    @Test
    public void testLocalBI() throws Exception {
        LocalBusiness bean = Helper.lookupDefaultLocal(LocalBusiness.class, "TestBean");
        assertNotNull("1 ---> LocalBusiness standard default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call remote business interface using component name.
     */
    @Test
    public void testRemoteBI() throws Exception {
        RemoteBusiness bean = Helper.lookupDefaultRemote(RemoteBusiness.class, "TestBean");
        assertNotNull("1 ---> RemoteBusiness standard default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call local component view interface using component name.
     */
    @Test
    public void testLocalCI() throws Exception {
        TestLocalHome homeBean = Helper.lookupDefaultLocal(TestLocalHome.class, "TestBean");
        assertNotNull("1 ---> LocalHome standard default lookup did not succeed.", homeBean);

        LocalEJB bean = homeBean.create();
        assertNotNull("2 ---> LocalHome create successful.", bean);

        String str = bean.getString();
        assertEquals("3 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call remote component view interface using component name.
     */
    @Test
    public void testRemoteCI() throws Exception {
        TestRemoteHome homeBean = Helper.lookupDefaultRemote(TestRemoteHome.class, "TestBean");
        assertNotNull("1 ---> RemoteHome standard default lookup did not succeed.", homeBean);

        RemoteEJB bean = homeBean.create();
        assertNotNull("2 ---> RemoteHome create did not succeed.", bean);

        String str = bean.getString();
        assertEquals("3 ---> getString() returned unexpected value", "Success", str);
    }
}

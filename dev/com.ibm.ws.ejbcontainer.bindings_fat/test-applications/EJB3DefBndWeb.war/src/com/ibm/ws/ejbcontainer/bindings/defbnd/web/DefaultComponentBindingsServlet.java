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

import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalComponentBusiness;
import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalComponentEJB;
import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.RemoteComponentBusiness;
import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.RemoteComponentEJB;
import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestLocalComponentHome;
import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestRemoteComponentHome;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> DefaultComponentBindingsTest.
 *
 * *
 * <dt><b>Test Description:</b>
 * <dd>The purpose of this test is to test lookups of component view and
 * business interfaces using legacy styles of default bindings, along with short and specific component format.
 * This test currently uses one of the WSTestHelpers for lookup, so it expects and creates a 3
 * part component id.
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
 * <li>testLocalBIComp lookup and call local business interface using component name.
 * <li>testRemoteBIComp lookup and call remote business interface using component name.
 * <li>testLocalCIComp lookup and call local component view interface using component name.
 * <li>testRemoteCIComp lookup and call remote component view interface using component name.
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/DefaultComponentBindingsServlet")
public class DefaultComponentBindingsServlet extends FATServlet {

    /**
     * lookup and call local business interface using short name.
     */
    @Test
    public void testLocalBIShort_DefaultComponentBindings() throws Exception {
        LocalComponentBusiness bean = Helper.lookupShortLocal(LocalComponentBusiness.class);
        assertNotNull("1 ---> LocalBusiness short default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call remote business interface using short name.
     */
    @Test
    public void testRemoteBIShort_DefaultComponentBindings() throws Exception {
        RemoteComponentBusiness bean = Helper.lookupShortRemote(RemoteComponentBusiness.class);
        assertNotNull("1 ---> RemoteBusiness short default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call local component view interface using short name.
     */
    @Test
    public void testLocalCIShort_DefaultComponentBindings() throws Exception {
        TestLocalComponentHome homeBean = Helper.lookupShortLocal(TestLocalComponentHome.class);
        assertNotNull("1 ---> LocalHome short default lookup did not succeed.", homeBean);

        LocalComponentEJB bean = homeBean.create();
        assertNotNull("2 ---> LocalHome create did not succeed.", bean);

        String str = bean.getString();
        assertEquals("3 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call remote component view interface using short name.
     */
    @Test
    public void testRemoteCIShort_DefaultComponentBindings() throws Exception {
        TestRemoteComponentHome homeBean = Helper.lookupShortRemote(TestRemoteComponentHome.class);
        assertNotNull("1 ---> RemoteHome short default lookup did not succeed.", homeBean);

        RemoteComponentEJB bean = homeBean.create();
        assertNotNull("2 ---> RemoteHome create did not succeed.", bean);

        String str = bean.getString();
        assertEquals("3 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call local business interface using component name.
     */
    @Test
    public void testLocalBIComp() throws Exception {
        LocalComponentBusiness bean = Helper.lookupCompLocal(LocalComponentBusiness.class);
        assertNotNull("1 ---> LocalBusiness standard default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call remote business interface using component name.
     */
    @Test
    public void testRemoteBIComp() throws Exception {
        RemoteComponentBusiness bean = Helper.lookupCompRemote(RemoteComponentBusiness.class);
        assertNotNull("1 ---> RemoteBusiness standard default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call local component view interface using component name.
     */
    @Test
    public void testLocalCIComp() throws Exception {
        TestLocalComponentHome homeBean = Helper.lookupCompLocal(TestLocalComponentHome.class);
        assertNotNull("1 ---> LocalHome standard default lookup did not succeed.", homeBean);

        LocalComponentEJB bean = homeBean.create();
        assertNotNull("2 ---> LocalHome create did not succeed.", bean);

        String str = bean.getString();
        assertEquals("3 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call remote component view interface using component name.
     */
    @Test
    public void testRemoteCIComp() throws Exception {
        TestRemoteComponentHome homeBean = Helper.lookupCompRemote(TestRemoteComponentHome.class);
        assertNotNull("1 ---> RemoteHome standard default lookup did not succeed.", homeBean);

        RemoteComponentEJB bean = homeBean.create();
        assertNotNull("2 ---> RemoteHome create did not succeed.", bean);

        String str = bean.getString();
        assertEquals("3 ---> getString() returned unexpected value", "Success", str);
    }
}

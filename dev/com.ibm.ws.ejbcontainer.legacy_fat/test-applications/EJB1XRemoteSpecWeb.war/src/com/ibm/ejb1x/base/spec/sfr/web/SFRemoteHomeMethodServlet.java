/*******************************************************************************
 * Copyright (c) 2002, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb1x.base.spec.sfr.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJBHome;
import javax.ejb.EJBMetaData;
import javax.ejb.HomeHandle;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb1x.base.spec.sfr.ejb.SFRa;
import com.ibm.ejb1x.base.spec.sfr.ejb.SFRaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SFRemoteHomeMethodTest (formerly WSTestSFR_HMTest)
 *
 * <dt>Test Descriptions:
 * <dd>EJB Container basic function tests:
 * <ul>
 * <li>H____ - Home Interface / EJBHome;
 * <li>HMC__ - Home Method Call;
 * <li>HMD__ - He Interface EJBMetaData;
 * <li>HMH__ - Home Interface Handle.
 * </ul>
 *
 * <dt>Command options:
 * <dd>
 * <TABLE width="100%">
 * <COL span="1" width="25%" align="left"> <COL span="1" align="left">
 * <TBODY>
 * <TR> <TH>Option</TH> <TH>Description</TH> </TR>
 * <TR> <TD>None</TD>
 * <TD></TD>
 * </TR>
 * </TBODY>
 * </TABLE>
 *
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>hmc01 - testHomeMethod - HomeMethod()
 * <li>hmc02 - testHomeMethodWithArg - HomeMethod( one-arg );
 * <li>hmc03 - testHomeMethodWithMultiArgs - HomeMethod( long-args );
 * <li>hmd01 - testHomeGetEJBMetaData - getEJBMetaData()
 * <li>hmd02 - testHomeEJBMetaDataGetEJBHome - getEJBMetaData().getEJBHome()
 * <li>hmd03 - testHomeEJBMetaDataGetHomeInterfaceClass - getEJBMetaData().getHomeInterfaceClass()
 * <li>hmd04 - testHomeEJBMetaDataGetRemoteInterfaceClass - getEJBMetaData().getRemoteInterfaceClass()
 * <li>hmd05 - testHomeEJBMetaDataGetPrimaryKeyClass - getEJBMetaData().getPrimaryKeyClass()
 * <li>hmd06 - testHomeEJBMetaDataIsSession - getEJBMetaData().isSession()
 * <li>hmd07 - testHomeEJBMetaDataIsStatefulSession - getEJBMetaData().isStatefulSession()
 * <li>hmh01 - testHomeGetHomeHandle - getHomeHandle()
 * <li>hmh02 - testHomeGetHomeHandleGetEJBHome - getHomeHandle().getEJBHome()
 * <li>hmh03 - testHomeHandleSerialization - Remote home handle serialize -> deserialize ->access
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/SFRemoteHomeMethodServlet")
public class SFRemoteHomeMethodServlet extends FATServlet {
    private final static String CLASS_NAME = SFRemoteHomeMethodServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb1x/base/spec/sfr/ejb/SFRaBMTHome";
    private static SFRaHome fhome1;

    @PostConstruct
    public void initializeHomes() {
        try {

            fhome1 = FATHelper.lookupRemoteHomeBinding(ejbJndiName1, SFRaHome.class);

        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (hmc01) Test Stateful remote home method (no parameters)
     */
    //@Test
    public void test1XSFHomeMethod() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (hmc02) Test Stateful remote home method (one parameter)
     */
    //@Test
    public void test1XSFHomeMethodWithArg() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (hmc03) Test Stateful remote home method (multi parameter)
     */
    //@Test
    public void test1XSFHomeMethodWithMultiArgs() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (hmd01) Test Stateful remote home getEJBMetaData()
     */
    @Test
    public void test1XSFHomeGetEJBMetaData() throws Exception {
        EJBMetaData md = fhome1.getEJBMetaData();
        assertNotNull("Get EJB Metadata from home was null.", md);
    }

    /**
     * (hmd02) Test Stateful remote home getEJBMetaData().getEJBHome()
     */
    @Test
    public void test1XSFHomeEJBMetaDataGetEJBHome() throws Exception {
        EJBMetaData md = fhome1.getEJBMetaData();
        assertNotNull("Get EJB Metadata from home was null.", md);
        EJBHome ejbHome = md.getEJBHome();
        assertNotNull("Get EJB Home from MetaData was null.", ejbHome);
        assertEquals("Test EJB Home name returned unexpected value.", fhome1.getClass().getName(), ejbHome.getClass().getName());
    }

    /**
     * (hmd03) Test Stateful remote home getEJBMetaData().getHomeInterfaceClass()
     */
    @Test
    public void test1XSFHomeEJBMetaDataGetHomeInterfaceClass() throws Exception {
        EJBMetaData md = fhome1.getEJBMetaData();
        assertNotNull("Get EJB Metadata from home was null.", md);
        Class<?> homeClass = md.getHomeInterfaceClass();
        assertNotNull("Get EJB home interface class from MetaData was null.", homeClass);
        assertEquals("Test EJB home interface name returned unexpected value.", homeClass.getName(), SFRaHome.class.getName());
    }

    /**
     * (hmd04) Test Stateful remote home getEJBMetaData().getRemoteInterfaceClass()
     */
    @Test
    public void test1XSFHomeEJBMetaDataGetRemoteInterfaceClass() throws Exception {
        EJBMetaData md = fhome1.getEJBMetaData();
        assertNotNull("Get EJB Metadata from home was null.", md);
        Class<?> businessClass = md.getRemoteInterfaceClass();
        assertNotNull("Get EJB business interface class from MetaData was null.", businessClass);
        assertEquals("Test EJB business interface name returned unexpected value.", businessClass.getName(), SFRa.class.getName());
    }

    /**
     * (hmd05) Test Stateful remote home getEJBMetaData().getPrimaryKeyClass()
     */
    @Test
    public void test1XSFHomeEJBMetaDataGetPrimaryKeyClass() throws Exception {
        try {
            EJBMetaData md = fhome1.getEJBMetaData();
            assertNotNull("Get EJB Metadata from home was null.", md);
            Class<?> keyClass = md.getPrimaryKeyClass();
            fail("Unexpected return from getPrimaryKeyClass() : " + keyClass);
        } catch (RuntimeException t) {
            svLogger.info("Caught expected " + t.getClass().getName());
        }
    }

    /**
     * (hmd06) Test Stateful remote home getEJBMetaData().isSession()
     */
    @Test
    public void test1XSFHomeEJBMetaDataIsSession() throws Exception {
        EJBMetaData md = fhome1.getEJBMetaData();
        assertNotNull("Get EJB Metadata from home was null.", md);
        boolean isSessBean = md.isSession();
        assertTrue("Test EJB isSession from MetaData was false.", isSessBean);
    }

    /**
     * (hmd07) Test Stateful remote home getEJBMetaData().isStatefulSession()
     */
    @Test
    public void test1XSFHomeEJBMetaDataIsStatefulSession() throws Exception {
        EJBMetaData md = fhome1.getEJBMetaData();
        assertNotNull("Get EJB Metadata from home was null.", md);
        boolean isStatelessSessBean = md.isStatelessSession();
        assertFalse("Test EJB isStatefulSession from MetaData was true.", isStatelessSessBean);
    }

    /**
     * (hmh01) Test Stateful remote home getHomeHandle()
     */
    @Test
    public void test1XSFHomeGetHomeHandle() throws Exception {
        HomeHandle handle = fhome1.getHomeHandle();
        assertNotNull("Get EJB home handle from home was null.", handle);
    }

    /**
     * (hmh02) Test Stateful remote home getHomeHandle().getEJBHome()
     */
    @Test
    public void test1XSFHomeGetHomeHandleGetEJBHome() throws Exception {
        HomeHandle handle = fhome1.getHomeHandle();
        assertNotNull("Get EJB home handle from home was null.", handle);
        // Narrow needed in Liberty since not using IBM ORB
        EJBHome ejbHome = (SFRaHome) PortableRemoteObject.narrow(handle.getEJBHome(), SFRaHome.class);
        // EJBHome ejbHome = handle.getEJBHome();
        assertNotNull("Get EJB Home from handle was null.", ejbHome);
        assertEquals("Test EJB Home name returned unexpected value.", fhome1.getClass().getName(), ejbHome.getClass().getName());
    }

    /**
     * (hmh03) Test Stateful remote home handle serialization
     */
    @Test
    public void test1XSFHomeHandleSerialization() throws Exception {
        HomeHandle handle = fhome1.getHomeHandle();
        assertNotNull("Get EJB home handle from home was null.", handle);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(handle);
        os.close();

        ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        handle = (HomeHandle) is.readObject();
        is.close();
        assertNotNull("Deserialized home handle from stream was null.", handle);
        // Narrow needed in Liberty since not using IBM ORB
        EJBHome ejbHome = (SFRaHome) PortableRemoteObject.narrow(handle.getEJBHome(), SFRaHome.class);
        // EJBHome ejbHome = handle.getEJBHome();
        assertNotNull("Get EJB Home from handle was null.", ejbHome);
        assertEquals("Test EJB Home name was unexpected value.", fhome1.getClass().getName(), ejbHome.getClass().getName());
    }
}

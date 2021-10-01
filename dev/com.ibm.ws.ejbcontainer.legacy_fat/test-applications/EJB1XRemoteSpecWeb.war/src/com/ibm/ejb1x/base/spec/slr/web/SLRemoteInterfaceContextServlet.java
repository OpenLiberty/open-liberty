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

package com.ibm.ejb1x.base.spec.slr.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb1x.base.spec.slr.ejb.SLRa;
import com.ibm.ejb1x.base.spec.slr.ejb.SLRaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SLRemoteInterfaceContextTest (formerly WSTestSLR_BXTest)
 *
 * <dt>Test Descriptions:
 * <dd>EJB Container basic function tests:
 * <ul>
 * <li>B____ - Business Interface / EJBObject;
 * <li>BXI__ - Identity Test;
 * <li>BXH__ - Business interface Handle;
 * <li>BXJ__ - Business Interface Home;
 * <li>BXK__ - Business Interface Primary Key;
 * <li>BXZ__ - Serializing Session Bean method.
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
 * <li>bxi01 - testEJBObjectIsIdenticalTrue - isIdentical() - same object
 * <li>bxi02 - testEJBObjectIsIdenticalFalse - isIdentical() - different objects
 * <li>bxh01 - testEJBObjectGetHandle - getHandle()
 * <li>bxh02 - testEJBObjectGetHandleSerialize - Remote handle serialize -> deserialize ->access
 * <li>bxj01 - testEJBObjectGetEJBHome - getEJBHome()
 * <li>bxj02 - testEJBObjectGetEJBHomeGetHomeHandleGetEJBHome - getEJBHome().getHomeHandle().getEJBHome()
 * <li>bxj03 - testEJBLocalObjectGetEJBLocalHome - getEJBLocalHome()
 * <li>bxk01 - testEJBObjectGetPrimaryKey - getPrimaryKey()
 * <li>bxz01 - testEJBMethodSerialization - Serializing Session Bean
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/SLRemoteInterfaceContextServlet")
public class SLRemoteInterfaceContextServlet extends FATServlet {
    private final static String CLASS_NAME = SLRemoteInterfaceContextServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb1x/base/spec/slr/ejb/SLRaBMTHome";
    private static SLRaHome fhome1;

    private static SLRa fejb1;
    private static SLRa fejb2;

    @PostConstruct
    public void initializeHomes() {
        try {

            fhome1 = FATHelper.lookupRemoteHomeBinding(ejbJndiName1, SLRaHome.class);
            fejb1 = fhome1.create();
            fejb2 = fhome1.create();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void destroyBeans() {
        try {

            if (fejb1 != null)
                fejb1.remove();
            if (fejb2 != null)
                fejb2.remove();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (bxi01) Test Stateless remote interface EJBObject.isIdentical; same object.
     */
    @Test
    public void test1XSLEJBObjectIsIdenticalTrue() throws Exception {
        assertTrue("Identity true test returned false (same object) but should be true", fejb1.isIdentical(fejb1));
    }

    /**
     * (bxi02) Test Stateless remote interface EJBObject.isIdentical; different object.
     */
    @Test
    public void test1XSLEJBObjectIsIdenticalFalse() throws Exception {
        assertTrue("Identity false test 1 returned false (different object) but should be true for SLSB", fejb1.isIdentical(fejb2));
        assertTrue("Identity false test 2 returned false (different object) but should be true for SLSB", fejb2.isIdentical(fejb1));
    }

    /**
     * (bxh01) Test Stateless remote interface EJBObject.getHandle.
     */
    @Test
    public void test1XSLEJBObjectGetHandle() throws Exception {
        Handle handle = fejb1.getHandle();
        assertNotNull("getHandle test was null", handle);
        EJBObject eo = handle.getEJBObject();
        assertNotNull("getEJBObject from handle was null", eo);
        SLRa ejb1 = (SLRa) javax.rmi.PortableRemoteObject.narrow(eo, SLRa.class);
        assertNotNull("SLRa from narrowing was null", ejb1);
        assertTrue("isIdentical test (SLRa and Handle) should have returned true.", fejb1.isIdentical(ejb1));
    }

    /**
     * (bxh02) Test Stateless remote interface EJBObject.getHandle; serialize->deserialize.
     */
    @Test
    public void test1XSLEJBObjectGetHandleSerialize() throws Exception {
        Handle handle1 = fejb1.getHandle();
        assertNotNull("getHandle test was null", handle1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(handle1);
        baos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Object o = ois.readObject();
        assertNotNull("Test deserialize object was null", o);
        assertTrue("Test deserialize object class was not instance of Handle", o instanceof Handle);
        Handle handle2 = (Handle) o;
        EJBObject eo = handle2.getEJBObject();
        assertNotNull("getEjbObject from handle was null", eo);
        SLRa ejb1 = (SLRa) javax.rmi.PortableRemoteObject.narrow(eo, SLRa.class);
        assertNotNull("SLRa from narrowing was null", ejb1);
        assertTrue("isIdentical test (SLRa and Handle) should have returned true.", fejb1.isIdentical(ejb1));
    }

    /**
     * (bxj01) Test Stateless remote interface EJBObject.getEJBHome.
     */
    @Test
    public void test1XSLEJBObjectGetEJBHome() throws Exception {
        Object tempHome = fejb1.getEJBHome();
        assertNotNull("getEJBHome from ejb was null.", tempHome);
        SLRaHome home1 = (SLRaHome) javax.rmi.PortableRemoteObject.narrow(tempHome, SLRaHome.class);
        assertNotNull("Narrow home to SLRahome.class was null", home1);
    }

    /**
     * (bxj02) Test Stateless remote interface EJBObject.getEJBHome.getHomeHandle.getEJBHome.
     */
    @Test
    public void test1XSLEJBObjectGetEJBHomeGetHomeHandleGetEJBHome() throws Exception {
        Object tempHome1 = fejb1.getEJBHome();
        assertNotNull("getEJBHome from ejb was null.", tempHome1);
        SLRaHome home1 = (SLRaHome) javax.rmi.PortableRemoteObject.narrow(tempHome1, SLRaHome.class);
        assertNotNull("Narrow home2 to SLRahome.class was null", home1);
        HomeHandle homeHandle = home1.getHomeHandle();
        assertNotNull("getHomeHandle from home was null.", homeHandle);
        Object tempHome2 = homeHandle.getEJBHome();
        assertNotNull("getEJBHome from handle was null.", tempHome2);
        SLRaHome home2 = (SLRaHome) javax.rmi.PortableRemoteObject.narrow(tempHome2, SLRaHome.class);
        assertNotNull("Narrow home2 to SLRahome.class was null", home2);
        assertEquals("Start -> End home were not equal", home1, home2);
    }

    /**
     * (bxj03) Test Stateless remote interface EJBLocalObject.getEJBLocalHome.
     */
    //@Test
    public void test1XSLEJBLocalObjectGetEJBLocalHome() throws Exception {
        svLogger.info("This test does not apply to remote beans.");
    }

    /**
     * (bxk01) Test Stateless remote interface EJBObject.getPrimaryKey.
     */
    @Test
    public void test1XSLEJBObjectGetPrimaryKey() throws Exception {
        try {
            Object pk1 = fejb1.getPrimaryKey();
            fail("Unexpected return from ejb.getPrimaryKey() : " + pk1);
        } catch (java.rmi.RemoteException re) {
            svLogger.info("Caught expected " + re.getClass().getName());
        }
    }

    /**
     * (bxz01) Test Stateless remote interface Serializing Session Bean calls. <p>
     *
     * See EJB 2.0 spec section 7.5.6
     */
    //@Test
    public void test1XSLEJBMethodSerialization() throws Exception {
        svLogger.info("This test has not yet been implemeted! Oops :-)");
    }
}

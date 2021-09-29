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
import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
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
 * <dd>SFRemoteInterfaceContextTest (formerly WSTestSFR_BXTest)
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
@WebServlet("/SFRemoteInterfaceContextServlet")
public class SFRemoteInterfaceContextServlet extends FATServlet {
    private final static String CLASS_NAME = SFRemoteInterfaceContextServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb1x/base/spec/sfr/ejb/SFRaBMTHome";
    private static SFRaHome fhome1;

    private static SFRa fejb1;
    private static SFRa fejb2;

    @PostConstruct
    public void initializeHomes() {
        try {

            fhome1 = FATHelper.lookupRemoteHomeBinding(ejbJndiName1, SFRaHome.class);
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
     * (bxi01) Test Stateful remote interface EJBObject.isIdentical; same object.
     */
    @Test
    public void test1XSFEJBObjectIsIdenticalTrue() throws Exception {
        assertTrue("Identity test (Same Object) should have been true for SFSB", fejb1.isIdentical(fejb1));
    }

    /**
     * (bxi02) Test Stateful remote interface EJBObject.isIdentical; different object.
     */
    @Test
    public void test1XSFEJBObjectIsIdenticalFalse() throws Exception {
        assertFalse("Identity test (Different Object) 1 should have been false for SFSB", fejb1.isIdentical(fejb2));
        assertFalse("Identity test (Different Object) 2 should have been false for SFSB", fejb2.isIdentical(fejb1));
    }

    /**
     * (bxh01) Test Stateful remote interface EJBObject.getHandle.
     */
    @Test
    public void test1XSFEJBObjectGetHandle() throws Exception {
        Handle handle = fejb1.getHandle();
        assertNotNull("getHandle test was null", handle);
        EJBObject eo = handle.getEJBObject();
        assertNotNull("getEJBObject from handle was null", eo);
        SFRa ejb1 = (SFRa) PortableRemoteObject.narrow(eo, SFRa.class);
        assertNotNull("SFRa from narrowing was null", ejb1);
        assertTrue("isIdentical test (Handle and SFRa) should have been true.", fejb1.isIdentical(ejb1));
    }

    /**
     * (bxh02) Test Stateful remote interface EJBObject.getHandle; serialize->deserialize.
     */
    @Test
    public void test1XSFEJBObjectGetHandleSerialize() throws Exception {
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
        assertNotNull("GetEjbObject from handle was null", eo);
        SFRa ejb1 = (SFRa) PortableRemoteObject.narrow(eo, SFRa.class);
        assertNotNull("SFRa from narrowing was null", ejb1);
        assertTrue("isIdentical test (SFRa and Handle) was false.", fejb1.isIdentical(ejb1));
    }

    /**
     * (bxj01) Test Stateful remote interface EJBObject.getEJBHome.
     */
    @Test
    public void test1XSFEJBObjectGetEJBHome() throws Exception {
        Object tempHome = fejb1.getEJBHome();
        assertNotNull("getEJBHome from ejb was null.", tempHome);
        SFRaHome home1 = (SFRaHome) PortableRemoteObject.narrow(tempHome, SFRaHome.class);
        assertNotNull("Narrow home to SFRahome.class was null", home1);
    }

    /**
     * (bxj02) Test Stateful remote interface EJBObject.getEJBHome.getHomeHandle.getEJBHome.
     */
    @Test
    public void test1XSFEJBObjectGetEJBHomeGetHomeHandleGetEJBHome() throws Exception {
        Object tempHome1 = fejb1.getEJBHome();
        assertNotNull("getEJBHome from ejb was null.", tempHome1);
        SFRaHome home1 = (SFRaHome) PortableRemoteObject.narrow(tempHome1, SFRaHome.class);
        assertNotNull("Narrow home2 to SFRahome.class was null", home1);
        HomeHandle homeHandle = home1.getHomeHandle();
        assertNotNull("getHomeHandle from home was null.", homeHandle);
        Object tempHome2 = homeHandle.getEJBHome();
        assertNotNull("getEJBHome from handle was null.", tempHome2);
        SFRaHome home2 = (SFRaHome) PortableRemoteObject.narrow(tempHome2, SFRaHome.class);
        assertNotNull("narrow home2 to SFRahome.class was null", home2);
        assertEquals("Start -> End home was not equal", home1, home2);
    }

    /**
     * (bxj03) Test Stateful remote interface EJBLocalObject.getEJBLocalHome.
     */
    //@Test
    public void test1XSFEJBLocalObjectGetEJBLocalHome() throws Exception {
        svLogger.info("This test does not apply to remote beans.");
    }

    /**
     * (bxk01) Test Stateful remote interface EJBObject.getPrimaryKey.
     */
    @Test
    public void test1XSFEJBObjectGetPrimaryKey() throws Exception {
        try {
            Object pk1 = fejb1.getPrimaryKey();
            fail("Unexpected return from ejb.getPrimaryKey() : " + pk1);
        } catch (RemoteException re) {
            svLogger.info("Caught expected " + re.getClass().getName());
        }
    }

    /**
     * (bxz01) Test Stateful remote interface Serializing Session Bean calls. <p>
     *
     * See EJB 2.0 spec section 7.5.6
     */
    //@Test
    public void test1XSFEJBMethodSerialization() throws Exception {
        svLogger.info("This test has not yet been implemeted! Oops :-)");
    }
}

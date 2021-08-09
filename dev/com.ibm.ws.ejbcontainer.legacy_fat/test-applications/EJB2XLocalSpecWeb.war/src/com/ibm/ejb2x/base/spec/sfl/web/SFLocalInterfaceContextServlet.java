/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb2x.base.spec.sfl.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.base.spec.sfl.ejb.SFLa;
import com.ibm.ejb2x.base.spec.sfl.ejb.SFLaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SFLocalInterfaceContextTest (formerly WSTestSFL_BXTest)
 *
 * <dt>Test Descriptions:
 * <dd>EJB Container basic function tests:
 * <ul>
 * <li>B____ - Business Interface / EJBObject / EJBLocalObject;
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
 * <li>bxh02 - testEJBObjectGetHandleSerialize - handle serialize -> deserialize ->access
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
@WebServlet("/SFLocalInterfaceContextServlet")
public class SFLocalInterfaceContextServlet extends FATServlet {

    private final static String CLASS_NAME = SFLocalInterfaceContextServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb2x/base/spec/sfl/ejb/SFLaBMTHome";
    private SFLaHome fhome1;

    @PostConstruct
    private void initializeHomes() {
        try {
            fhome1 = (SFLaHome) FATHelper.lookupLocalHome(ejbJndiName1);
            //fhome1 = (SFLaHome) new InitialContext().lookup("java:app/EJB2XSFLocalSpecEJB/SFLaBMT");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (bxi01) Test Stateful local interface EJBObject.isIdentical; same object.
     */
    @Test
    public void testSFLocalEJBObjectIsIdenticalSameObject() throws Exception {
        SFLa ejb1 = fhome1.create();
        assertTrue("Identity true test was false (same object) but should be true", ejb1.isIdentical(ejb1));
    }

    /**
     * (bxi02) Test Stateful local interface EJBObject.isIdentical; different object.
     */
    @Test
    public void testSFLocalEJBObjectIsIdenticalDifferentObject() throws Exception {
        SFLa ejb1 = fhome1.create();
        SFLa ejb2 = fhome1.create();
        assertFalse("Identity false test 1 was true (different object) but should be false for SFSB", ejb1.isIdentical(ejb2));
        assertFalse("Identity false test 2 was true (different object) but should be false for SFSB", ejb2.isIdentical(ejb1));
        ejb1.remove();
        ejb2.remove();
    }

    /**
     * (bxh01) Test Stateful local interface EJBObject.getHandle.
     */
    //@Test
    public void testSFLocalEJBObjectGetHandle() throws Exception {
        svLogger.info("This test does not apply to local beans.");
    }

    /**
     * (bxh02) Test Stateful local interface EJBObject.getHandle; serialize->deserialize.
     */
    //@Test
    public void testSFLocalEJBObjectGetHandleSerialize() throws Exception {
        svLogger.info("This test does not apply to local beans.");
    }

    /**
     * (bxj01) Test Stateful local interface EJBObject.getEJBHome.
     */
    //@Test
    public void testSFLocalEJBObjectGetEJBHome() throws Exception {
        svLogger.info("This test does not apply to local beans.");
    }

    /**
     * (bxj02) Test Stateful local interface EJBObject.getEJBHome.getHomeHandle.getEJBHome.
     */
    //@Test
    public void testSFLocalEJBObjectGetEJBHomeGetHomeHandleGetEJBHome() throws Exception {
        svLogger.info("This test does not apply to local beans.");
    }

    /**
     * (bxj03) Test Stateful local interface EJBLocalObject.getEJBLocalHome.
     */
    @Test
    public void testSFLocalEJBLocalObjectGetEJBLocalHome() throws Exception {
        SFLa ejb1 = fhome1.create();
        Object tempHome = ejb1.getEJBLocalHome();
        assertNotNull("getEJBLocalHome from ejb was null.", tempHome);
        SFLaHome home1 = (SFLaHome) tempHome;
        assertNotNull("Cast home to SFLaHome.class was null", home1);
        ejb1.remove();
    }

    /**
     * (bxk01) Test Stateful local interface EJBObject.getPrimaryKey.
     */
    @Test
    public void testSFLocalEJBObjectGetPrimaryKey() throws Exception {
        SFLa ejb1 = fhome1.create();
        try {
            Object pk1 = ejb1.getPrimaryKey();
            fail("Unexpected return from ejb.getPrimaryKey(): " + pk1);
        } catch (EJBException ejbex) {
            svLogger.info("Caught expected " + ejbex.getClass().getName());
        }
        ejb1.remove();
    }

    /**
     * (bxz01) Test Stateful local interface Serializing Session Bean calls. <p>
     *
     * See EJB 2.0 spec section 7.5.6
     */
    //@Test
    public void testSFLocalEJBMethodSerialization() throws Exception {
        svLogger.info("This test has not yet been implemeted! Oops :-)");
    }
}

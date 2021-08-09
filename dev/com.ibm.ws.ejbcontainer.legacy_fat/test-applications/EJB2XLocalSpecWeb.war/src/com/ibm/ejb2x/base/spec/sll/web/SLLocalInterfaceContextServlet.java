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

package com.ibm.ejb2x.base.spec.sll.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.base.spec.sll.ejb.SLLa;
import com.ibm.ejb2x.base.spec.sll.ejb.SLLaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SLLocalInterfaceContextTest (formerly WSTestSLL_BXTest)
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
@WebServlet("/SLLocalInterfaceContextServlet")
public class SLLocalInterfaceContextServlet extends FATServlet {

    private final static String CLASS_NAME = SLLocalInterfaceContextServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb2x/base/spec/sll/ejb/SLLaBMTHome";
    private SLLaHome fhome1;

    @PostConstruct
    private void initializeHomes() {
        try {
            fhome1 = (SLLaHome) FATHelper.lookupLocalHome(ejbJndiName1);
            //fhome1 = (SLLaHome) new InitialContext().lookup("java:app/EJB2XSLLocalSpecEJB/SLLaBMT");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (bxi01) Test Stateless local interface EJBObject.isIdentical; same object.
     */
    @Test
    public void testSLLocalEJBObjectIsIdenticalSameObject() throws Exception {
        SLLa ejb1 = fhome1.create();
        assertTrue("Identity true test returned false (same object) but should be true", ejb1.isIdentical(ejb1));
    }

    /**
     * (bxi02) Test Stateless local interface EJBObject.isIdentical; different object.
     */
    @Test
    public void testSLLocalEJBObjectIsIdenticalDifferentObject() throws Exception {
        SLLa ejb1 = fhome1.create();
        SLLa ejb2 = fhome1.create();
        assertTrue("Identity false test 1 returned false (different object) but should be true for SLSB", ejb1.isIdentical(ejb2));
        assertTrue("Identity false test 2 returned false (different object) but should be true for SLSB", ejb2.isIdentical(ejb1));
    }

    /**
     * (bxh01) Test Stateless local interface EJBObject.getHandle.
     */
    //@Test
    public void testSLLocalEJBObjectGetHandle() throws Exception {
        svLogger.info("This test does not apply to local beans.");
    }

    /**
     * (bxh02) Test Stateless local interface EJBObject.getHandle; serialize->deserialize.
     */
    //@Test
    public void testSLLocalEJBObjectGetHandleSerialize() throws Exception {
        svLogger.info("This test does not apply to local beans.");
    }

    /**
     * (bxj01) Test Stateless local interface EJBObject.getEJBHome.
     */
    //@Test
    public void testSLLocalEJBObjectGetEJBHome() throws Exception {
        svLogger.info("This test does not apply to local beans.");
    }

    /**
     * (bxj02) Test Stateless local interface EJBObject.getEJBHome.getHomeHandle.getEJBHome.
     */
    //@Test
    public void testSLLocalEJBObjectGetEJBHomeGetHomeHandleGetEJBHome() throws Exception {
        svLogger.info("This test does not apply to local beans.");
    }

    /**
     * (bxj03) Test Stateless local interface EJBLocalObject.getEJBLocalHome.
     */
    @Test
    public void testSLLocalEJBLocalObjectGetEJBLocalHome() throws Exception {
        SLLa ejb1 = fhome1.create();
        Object tempHome = ejb1.getEJBLocalHome();
        assertNotNull("getEJBLocalHome from ejb returned null.", tempHome);
        SLLaHome home1 = (SLLaHome) tempHome;
        assertNotNull("Cast home to SLLaHome.class returned null", home1);
    }

    /**
     * (bxk01) Test Stateless local interface EJBLocalObject.getPrimaryKey.
     */
    @Test
    public void testSLLocalEJBObjectGetPrimaryKey() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            Object pk1 = ejb1.getPrimaryKey();
            fail("Unexpected return from ejb.getPrimaryKey() : " + pk1);
        } catch (EJBException ejbex) {
            svLogger.info("Caught expected " + ejbex.getClass().getName());
        }
    }

    /**
     * (bxz01) Test Stateless local interface Serializing Session Bean calls. <p>
     *
     * See EJB 2.0 spec section 7.5.6
     */
    //@Test
    public void testSLLocalEJBMethodSerialization() throws Exception {
        svLogger.info("This test has not yet been implemeted! Oops :-)");
    }
}

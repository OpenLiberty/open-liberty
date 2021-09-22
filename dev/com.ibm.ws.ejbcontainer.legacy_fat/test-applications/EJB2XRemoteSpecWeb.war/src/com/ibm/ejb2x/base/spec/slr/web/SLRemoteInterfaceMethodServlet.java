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
package com.ibm.ejb2x.base.spec.slr.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.base.spec.slr.ejb.SLRTestReentrance;
import com.ibm.ejb2x.base.spec.slr.ejb.SLRTestReentranceHome;
import com.ibm.ejb2x.base.spec.slr.ejb.SLRa;
import com.ibm.ejb2x.base.spec.slr.ejb.SLRaHome;
import com.ibm.ejb2x.base.spec.slr.ejb.SLRaPassBy;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SLRemoteInterfaceMethodTest (formerly WSTestSLR_BMTest)
 *
 * <dt>Test Descriptions:
 * <dd>EJB Container basic function tests:
 * <ul>
 * <li>B____ - Business Interface / EJBObject / EJBLocalObject;
 * <li>BMG__ - Business Getter/Setter Method Call;
 * <li>BMC__ - Business Method Call.
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
 * <li>bmg01 - Boolean getter/setter
 * <li>bmg02 - Byte getter/setter
 * <li>bmg03 - Char getter/setter
 * <li>bmg04 - Short getter/setter
 * <li>bmg05 - Int getter/setter
 * <li>bmg06 - Long getter/setter
 * <li>bmg07 - Float getter/setter
 * <li>bmg08 - Double getter/setter
 * <li>bmg09 - String getter/setter
 * <li>bmg10 - Object getter/setter
 * <li>bmc01 - Simple Method Call
 * <li>bmc02 - Method Call to non-exist EJB
 * <li>bmc03 - Pass-by-reference test
 * <li>bmc04 - Pass-by-value test
 * <li>bmc05 - Invoke method in different transaction
 * <li>bmc06 - Non-reentrant recursive call
 * <li>bmc07 - Reentrant recursive call
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/SLRemoteInterfaceMethodServlet")
public class SLRemoteInterfaceMethodServlet extends FATServlet {
    private final static String CLASS_NAME = SLRemoteInterfaceMethodServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static SLRaHome fhome1;

    private final static String ejbJndiName1 = "com/ibm/ejb2x/base/spec/slr/ejb/SLRaBMTHome";
    private final static String ejbJndiName2 = "com/ibm/ejb2x/base/spec/slr/ejb/SLRNonReentranceHome";
    private static SLRa fejb1;
    private static SLRTestReentranceHome rhome1;

    @PostConstruct
    private void initializeBeans() {
        try {
            fhome1 = FATHelper.lookupRemoteHomeBinding(ejbJndiName1, SLRaHome.class);
            rhome1 = FATHelper.lookupRemoteHomeBinding(ejbJndiName2, SLRTestReentranceHome.class);

            fejb1 = fhome1.create();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    private void removeBeans() {
        try {
            if (fejb1 != null) {
                fejb1.remove();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (bmg01) Test Stateless remote get/set methods for boolean value.
     */
    //@Test
    public void testSLRemoteInterface_get_set_boolean() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg02) Test Stateless remote get/set methods for byte value.
     */
    //@Test
    public void testSLRemoteInterface_get_set_byte() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg03) Test Stateless remote get/set methods for char value.
     */
    //@Test
    public void testSLRemoteInterface_get_set_char() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg04) Test Stateless remote get/set methods for short value.
     */
    //@Test
    public void testSLRemoteInterface_get_set_short() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg05) Test Stateless remote get/set methods for int value.
     */
    //@Test
    public void testSLRemoteInterface_get_set_int() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg06) Test Stateless remote get/set methods for long value.
     */
    //@Test
    public void testSLRemoteInterface_get_set_long() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg07) Test Stateless remote get/set methods for float value.
     */
    //@Test
    public void testSLRemoteInterface_get_set_float() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg08) Test Stateless remote get/set methods for double value.
     */
    //@Test
    public void testSLRemoteInterface_get_set_double() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg09) Test Stateless remote get/set methods for String value.
     */
    //@Test
    public void testSLRemoteInterface_get_set_String() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg10) Test Stateless remote get/set methods for Object value.
     */
    //@Test
    public void testSLRemoteInterface_get_set_Object() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmc01) Test Stateless remote simple method call.
     */
    @Test
    public void testSLRemoteInterfaceMethod() throws Exception {
        String testStr = "Test string.";
        String buf = fejb1.method1(testStr);
        assertEquals("Method call (method1) test returned unexpected value.", buf, testStr);
    }

    /**
     * (bmc02) Test Stateless remote method on non-existing EJB.
     */
    @Test
    public void testSLRemoteInterfaceMethod_NonExistEJB() throws Exception {
        SLRa ejb1 = null;
        svLogger.info("First create the EJB instance.");
        ejb1 = fhome1.create();
        assertNotNull("Find/create the EJB instance was null.", ejb1);
        svLogger.info("Remove the instance while still holding on the reference..");
        ejb1.remove();
        svLogger.info("Instance removed.");
        svLogger.info("Now execute a method to the object that doesn't exist.");
        String testStr = "Test string.";
        String buf = ejb1.method1(testStr);
        assertEquals("Method call (method1) test returned unexpected value.", buf, testStr);
    }

    /**
     * (bmc03) Test Stateless remote method with pass-by-reference.
     */
    @Test
    public void testSLRemoteInterfaceMethod_PassByReference() throws Exception {
        String inKeyStr = "inKeyStr";
        String outKeyStr = "outKeyStr";

        SLRaPassBy inPbr = new SLRaPassBy(inKeyStr, outKeyStr, 3);
        SLRaPassBy outPbr = fejb1.changePassByParm(inPbr);
        assertEquals("Test inPbr key was unexpected value.", inPbr.getKey(), inKeyStr);
        assertEquals("Test inPbr key2 was unexpected value.", inPbr.getKey2(), outKeyStr);
        assertEquals("Test inPbr value was unexpected value.", inPbr.getValue(), 3);
        assertEquals("Test outPbr key was unexpected value.", outPbr.getKey(), outKeyStr);
        assertEquals("Test outPbr key2 was unexpected value.", outPbr.getKey2(), outKeyStr);
        assertEquals("Test outPbr value was unexpected value.", outPbr.getValue(), 4);
    }

    /**
     * (bmc04) Test Stateless remote method with pass-by-value.
     */
    //@Test
    public void testSLRemoteInterfaceMethod_PassByValue() throws Exception {
        svLogger.info("This test does not apply to remote beans; remote is pass-by-reference.");
    }

    /**
     * (bmc05) Test Stateless remote method in different transaction.
     */
    //@Test
    public void testSLRemoteInterfaceMethod_DifferentTransaction() throws Exception {
        svLogger.info("This test does not apply to stateless beans; not enlisted in transactions.");
    }

    /**
     * (bmc06) Test Stateless remote non-reentrant recursive method call. <p>
     *
     * See EJB 2.0 Spec section 12.1.11.
     */
    @Test
    public void testSLRemoteInterfaceMethod_NonReentrantRecursive() throws Exception {
        SLRTestReentrance ejb1 = null;
        try {
            ejb1 = rhome1.create();
            ejb1.callNonRecursiveSelf(5, ejb1);
            assertNotNull("Stateless Session always uses a new instance for method call, therefore should never callback, but was null.", ejb1);
        } finally {
            if (ejb1 != null) {
                ejb1.remove();
            }
        }
    }

    /**
     * (bmc07) Test Stateless remote reentrant recursive method call.
     */
    //@Test
    public void testSLRemoteInterfaceMethod_ReentrantRecursive() throws Exception {
        svLogger.info("This test does not apply to stateless beans; not reentrant.");
    }
}
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.base.spec.sll.ejb.SLLTestReentrance;
import com.ibm.ejb2x.base.spec.sll.ejb.SLLTestReentranceHome;
import com.ibm.ejb2x.base.spec.sll.ejb.SLLa;
import com.ibm.ejb2x.base.spec.sll.ejb.SLLaHome;
import com.ibm.ejb2x.base.spec.sll.ejb.SLLaPassBy;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SLLocalInterfaceMethodTest (formerly WSTestSLL_BMTest)
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
@WebServlet("/SLLocalInterfaceMethodServlet")
public class SLLocalInterfaceMethodServlet extends FATServlet {

    private final static String CLASS_NAME = SLLocalInterfaceMethodServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb2x/base/spec/sll/ejb/SLLaBMTHome";
    private final static String ejbJndiName2 = "com/ibm/ejb2x/base/spec/sll/ejb/SLLNonReentranceHome";
    private SLLaHome fhome1;
    private SLLTestReentranceHome rhome1;

    @PostConstruct
    private void initializeHomes() {
        try {
            fhome1 = (SLLaHome) FATHelper.lookupLocalHome(ejbJndiName1);
            rhome1 = (SLLTestReentranceHome) FATHelper.lookupLocalHome(ejbJndiName2);
            //InitialContext cntx = new InitialContext();
            //fhome1 = (SLLaHome) cntx.lookup("java:app/EJB2XSLLocalSpecEJB/SLLaBMT");
            //rhome1 = (SLLTestReentranceHome) cntx.lookup("java:app/EJB2XSLLocalSpecEJB/SLLNonReentranceEJB");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (bmg01) Test Stateless local get/set methods for boolean value.
     */
    //@Test
    public void testSLLocalInterface_get_set_boolean() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg02) Test Stateless local get/set methods for byte value.
     */
    //@Test
    public void testSLLocalInterface_get_set_byte() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg03) Test Stateless local get/set methods for char value.
     */
    //@Test
    public void testSLLocalInterface_get_set_char() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg04) Test Stateless local get/set methods for short value.
     */
    //@Test
    public void testSLLocalInterface_get_set_short() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg05) Test Stateless local get/set methods for int value.
     */
    //@Test
    public void testSLLocalInterface_get_set_int() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg06) Test Stateless local get/set methods for long value.
     */
    //@Test
    public void testSLLocalInterface_get_set_long() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg07) Test Stateless local get/set methods for float value.
     */
    //@Test
    public void testSLLocalInterface_get_set_float() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg08) Test Stateless local get/set methods for double value.
     */
    //@Test
    public void testSLLocalInterface_get_set_double() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg09) Test Stateless local get/set methods for String value.
     */
    //@Test
    public void testSLLocalInterface_get_set_String() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmg10) Test Stateless local get/set methods for Object value.
     */
    //@Test
    public void testSLLocalInterface_get_set_Object() throws Exception {
        svLogger.info("This test does not apply to stateless beans; state is not guranteed.");
    }

    /**
     * (bmc01) Test Stateless local simple method call.
     */
    @Test
    public void testSLLocalInterfaceMethod() throws Exception {
        SLLa ejb1 = fhome1.create();
        String testStr = "Test string.";
        String buf = ejb1.method1(testStr);
        assertEquals("Method call (method1) test returned unexpected value.", buf, testStr);
    }

    /**
     * (bmc02) Test Stateless local method on non-existing EJB.
     */
    @Test
    public void testSLLocalInterfaceMethod_NonExistEJB() throws Exception {
        SLLa ejb1 = null;
        svLogger.info("First create the EJB instance.");
        ejb1 = fhome1.create();
        assertNotNull("Find/create the EJB instance was null.", ejb1);
        svLogger.info("Remove the instance while still holding on the reference..");
        ejb1.remove();
        assertNotNull("Instance removed is now null.", ejb1);
        svLogger.info("Now execute a method to the object that doesn't exist.");
        String testStr = "Test string.";
        String buf = ejb1.method1(testStr);
        assertEquals("Method call (method1) test returned unexpected value.", buf, testStr);
    }

    /**
     * (bmc03) Test Stateless local method with pass-by-reference.
     */
    //@Test
    public void testSLLocalInterfaceMethod_PassByReference() throws Exception {
        svLogger.info("This test does not apply to local beans; local is pass-by-value.");
    }

    /**
     * (bmc04) Test Stateless local method with pass-by-value.
     */
    @Test
    public void testSLLocalInterfaceMethod_PassByValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        String inKeyStr = "inKeyStr";
        String outKeyStr = "outKeyStr";

        SLLaPassBy inPbr = new SLLaPassBy(inKeyStr, outKeyStr, 3);
        SLLaPassBy outPbr = ejb1.changePassByParm(inPbr);
        assertEquals("Test inPbr key returned unexpected value.", inPbr.getKey(), outKeyStr);
        assertEquals("Test inPbr key2 returned unexpected value.", inPbr.getKey2(), outKeyStr);
        assertEquals("Test inPbr value returned unexpected value.", inPbr.getValue(), 4);
        assertEquals("Test outPbr key returned unexpected value.", outPbr.getKey(), outKeyStr);
        assertEquals("Test outPbr key2 returned unexpected value.", outPbr.getKey2(), outKeyStr);
        assertEquals("Test outPbr value returned unexpected value.", outPbr.getValue(), 4);
    }

    /**
     * (bmc05) Test Stateless local method in different transaction.
     */
    //@Test
    public void testSLLocalInterfaceMethod_DifferentTransaction() throws Exception {
        svLogger.info("This test does not apply to stateless beans; not enlisted in transactions.");
    }

    /**
     * (bmc06) Test Stateless local non-reentrant recursive method call. <p>
     *
     * See EJB 2.0 Spec section 12.1.11.
     */
    @Test
    public void testSLLocalInterfaceMethod_NonReentrantRecursive() throws Exception {
        SLLTestReentrance ejb1 = null;
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
     * (bmc07) Test Stateless local reentrant recursive method call.
     */
    //@Test
    public void testSLLocalInterfaceMethod_ReentrantRecursive() throws Exception {
        svLogger.info("This test does not apply to stateless beans; not reentrant.");
    }
}

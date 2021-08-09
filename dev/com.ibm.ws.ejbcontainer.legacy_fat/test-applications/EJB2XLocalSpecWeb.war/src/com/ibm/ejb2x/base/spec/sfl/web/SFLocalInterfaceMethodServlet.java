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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.NoSuchObjectLocalException;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.base.spec.sfl.ejb.SFLApplException;
import com.ibm.ejb2x.base.spec.sfl.ejb.SFLTestReentrance;
import com.ibm.ejb2x.base.spec.sfl.ejb.SFLTestReentranceHome;
import com.ibm.ejb2x.base.spec.sfl.ejb.SFLa;
import com.ibm.ejb2x.base.spec.sfl.ejb.SFLaHome;
import com.ibm.ejb2x.base.spec.sfl.ejb.SFLaPassBy;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SFLocalInterfaceMethodTest (formerly WSTestSFL_BMTest)
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
@WebServlet("/SFLocalInterfaceMethodServlet")
public class SFLocalInterfaceMethodServlet extends FATServlet {

    private final static String CLASS_NAME = SFLocalInterfaceMethodServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static final double DDELTA = 0.0D;

    private final static String ejbJndiName1 = "com/ibm/ejb2x/base/spec/sfl/ejb/SFLaBMTHome";
    private final static String ejbJndiName2 = "com/ibm/ejb2x/base/spec/sfl/ejb/SFLNonReentranceHome";
    private SFLaHome fhome1;
    private SFLTestReentranceHome rhome1;

    @PostConstruct
    private void initializeHomes() {
        try {
            fhome1 = (SFLaHome) FATHelper.lookupLocalHome(ejbJndiName1);
            rhome1 = (SFLTestReentranceHome) FATHelper.lookupLocalHome(ejbJndiName2);
            //InitialContext cntx = new InitialContext();
            //fhome1 = (SFLaHome) cntx.lookup("java:app/EJB2XSFLocalSpecEJB/SFLaBMT");
            //rhome1 = (SFLTestReentranceHome) cntx.lookup("java:app/EJB2XSFLocalSpecEJB/SFLRNonReentranceEJB");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (bmg01) Test Stateful local get/set methods for boolean value.
     */
    @Test
    public void testSFLocalInterface_get_set_boolean() throws Exception {
        SFLa ejb1 = fhome1.create();
        boolean originalValue = ejb1.getBooleanValue();
        ejb1.setBooleanValue(!originalValue);
        assertTrue("Set value test should have returned true", ejb1.getBooleanValue() == !originalValue);
        ejb1.setBooleanValue(originalValue);
        assertTrue("Restore value test should have returned true", ejb1.getBooleanValue() == originalValue);
        ejb1.remove();
    }

    /**
     * (bmg02) Test Stateful local get/set methods for byte value.
     */
    @Test
    public void testSFLocalInterface_get_set_byte() throws Exception {
        SFLa ejb1 = fhome1.create();
        byte originalValue = ejb1.getByteValue();
        ejb1.setByteValue((byte) (originalValue + 1));
        assertEquals("Set value test did not return expected value.", ejb1.getByteValue(), (byte) (originalValue + 1));
        ejb1.setByteValue(originalValue);
        assertEquals("Restore value test did not return expected value.", ejb1.getByteValue(), originalValue);
        ejb1.remove();
    }

    /**
     * (bmg03) Test Stateful local get/set methods for char value.
     */
    @Test
    public void testSFLocalInterface_get_set_char() throws Exception {
        SFLa ejb1 = fhome1.create();
        char originalValue = ejb1.getCharValue();
        ejb1.setCharValue((char) (originalValue + 1));
        assertEquals("Set value test did not return expected value.", ejb1.getCharValue(), (char) (originalValue + 1));
        ejb1.setCharValue(originalValue);
        assertEquals("Restore value test did not return expected value.", ejb1.getCharValue(), originalValue);
        ejb1.remove();
    }

    /**
     * (bmg04) Test Stateful local get/set methods for short value.
     */
    @Test
    public void testSFLocalInterface_get_set_short() throws Exception {
        SFLa ejb1 = fhome1.create();
        short originalValue = ejb1.getShortValue();
        ejb1.setShortValue((short) (originalValue + 1));
        assertEquals("Set value test did not return expected value.", ejb1.getShortValue(), (short) (originalValue + 1));
        ejb1.setShortValue(originalValue);
        assertEquals("Restore value test did not return expected value.", ejb1.getShortValue(), originalValue);
        ejb1.remove();
    }

    /**
     * (bmg05) Test Stateful local get/set methods for int value.
     */
    @Test
    public void testSFLocalInterface_get_set_int() throws Exception {
        SFLa ejb1 = fhome1.create();
        int originalValue = ejb1.getIntValue();
        ejb1.setIntValue((originalValue + 1));
        assertEquals("Set value test did not return expected value.", ejb1.getIntValue(), (originalValue + 1));
        ejb1.setIntValue(originalValue);
        assertEquals("Restore value test did not return expected value.", ejb1.getIntValue(), originalValue);
        ejb1.remove();
    }

    /**
     * (bmg06) Test Stateful local get/set methods for long value.
     */
    @Test
    public void testSFLocalInterface_get_set_long() throws Exception {
        SFLa ejb1 = fhome1.create();
        long originalValue = ejb1.getLongValue();
        ejb1.setLongValue((originalValue + 1));
        assertEquals("Set value test did not return expected value.", ejb1.getLongValue(), (originalValue + 1));
        ejb1.setLongValue(originalValue);
        assertEquals("Restore value test did not return expected value.", ejb1.getLongValue(), originalValue);
        ejb1.remove();
    }

    /**
     * (bmg07) Test Stateful local get/set methods for float value.
     */
    @Test
    public void testSFLocalInterface_get_set_float() throws Exception {
        SFLa ejb1 = fhome1.create();
        float originalValue = ejb1.getFloatValue();
        ejb1.setFloatValue((float) (originalValue + 1.0));
        assertEquals("Set value test did not return expected value.", ejb1.getFloatValue(), (float) (originalValue + 1.0), DDELTA);
        ejb1.setFloatValue(originalValue);
        assertEquals("Restore value test did not return expected value.", ejb1.getFloatValue(), originalValue, DDELTA);
        ejb1.remove();
    }

    /**
     * (bmg08) Test Stateful local get/set methods for double value.
     */
    @Test
    public void testSFLocalInterface_get_set_double() throws Exception {
        SFLa ejb1 = fhome1.create();
        double originalValue = ejb1.getDoubleValue();
        ejb1.setDoubleValue((originalValue + 1.0));
        assertEquals("Set value test did not return expected value.", ejb1.getDoubleValue(), (originalValue + 1.0), DDELTA);
        ejb1.setDoubleValue(originalValue);
        assertEquals("Restore value test did not return expected value.", ejb1.getDoubleValue(), originalValue, DDELTA);
        ejb1.remove();
    }

    /**
     * (bmg09) Test Stateful local get/set methods for String value.
     */
    @Test
    public void testSFLocalInterface_get_set_String() throws Exception {
        SFLa ejb1 = fhome1.create();
        String originalValue = ejb1.getStringValue();
        ejb1.setStringValue(originalValue + "One More");
        assertEquals("Set value test did not return expected value.", ejb1.getStringValue(), originalValue + "One More");
        ejb1.setStringValue(originalValue);
        assertEquals("Restore value test did not return expected value.", ejb1.getStringValue(), originalValue);
        ejb1.remove();
    }

    /**
     * (bmg10) Test Stateful local get/set methods for Object value.
     */
    @Test
    public void testSFLocalInterface_get_set_Object() throws Exception {
        SFLa ejb1 = fhome1.create();
        Integer originalValue = ejb1.getIntegerValue();
        Integer newValue = new Integer(2010);
        ejb1.setIntegerValue(newValue);
        assertEquals("Set value test did not return expected value.", ejb1.getIntegerValue(), newValue);
        ejb1.setIntegerValue(originalValue);
        assertEquals("Restore value test did not return expected value.", ejb1.getIntegerValue(), originalValue);
        ejb1.remove();
    }

    /**
     * (bmc01) Test Stateful local simple method call.
     */
    @Test
    public void testSFLocalInterfaceMethod() throws Exception {
        SFLa ejb1 = fhome1.create();
        String testStr = "Test string.";
        String buf = ejb1.method1(testStr);
        assertEquals("Method call (method1) test did not return expected value.", buf, testStr);
        ejb1.remove();
    }

    /**
     * (bmc02) Test Stateful local method on non-existing EJB.
     */
    @Test
    public void testSFLocalInterfaceMethod_NonExistEJB() throws Exception {
        SFLa ejb1 = null;
        svLogger.info("First create the EJB instance.");
        ejb1 = fhome1.create();
        assertNotNull("First find/create the EJB instance was null.", ejb1);
        svLogger.info("Remove the instance while still holding on the reference..");
        ejb1.remove();
        assertNotNull("Instance removed is now null.", ejb1);
        svLogger.info("Now execute a method to the object that doesn't exist.");
        String testStr = "Test string.";
        try {
            String buf = ejb1.method1(testStr);
            fail("Unexpected successful method call : " + buf);
        } catch (NoSuchObjectLocalException ex) {
            svLogger.info("Caught expected " + ex.getClass().getName());
        }
    }

    /**
     * (bmc03) Test Stateful local method with pass-by-reference.
     */
    //@Test
    public void testSFLocalInterfaceMethod_PassByReference() throws Exception {
        svLogger.info("This test does not apply to local beans; local is pass-by-value.");
    }

    /**
     * (bmc04) Test Stateful local method with pass-by-value.
     */
    @Test
    public void testSFLocalInterfaceMethod_PassByValue() throws Exception {
        SFLa ejb1 = fhome1.create();
        String inKeyStr = "inKeyStr";
        String outKeyStr = "outKeyStr";

        SFLaPassBy inPbr = new SFLaPassBy(inKeyStr, outKeyStr, 3);
        SFLaPassBy outPbr = ejb1.changePassByParm(inPbr);
        assertEquals("Test inPbr key was unexpected value.", inPbr.getKey(), outKeyStr);
        assertEquals("Test inPbr key2 was unexpected value.", inPbr.getKey2(), outKeyStr);
        assertEquals("Test inPbr value was unexpected value.", inPbr.getValue(), 4);
        assertEquals("Test outPbr key was unexpected value.", outPbr.getKey(), outKeyStr);
        assertEquals("Test outPbr key2 was unexpected value.", outPbr.getKey2(), outKeyStr);
        assertEquals("Test outPbr value was unexpected value.", outPbr.getValue(), 4);
        ejb1.remove();
    }

    /**
     * (bmc05) Test Stateful local method in different transaction.
     */
    //@Test
    public void testSFLocalInterfaceMethod_DifferentTransaction() throws Exception {
        svLogger.info("Tested elsewhere: see stateful_concurrency test suite.");
    }

    /**
     * (bmc06) Test Stateful local non-reentrant recursive method call. <p>
     *
     * See EJB 2.0 Spec section 12.1.11.
     */
    @Test
    @ExpectedFFDC("com.ibm.ejs.container.BeanNotReentrantException")
    public void testSFLocalInterfaceMethod_NonReentrantRecursive() throws Exception {
        SFLTestReentrance ejb1 = null;
        try {
            ejb1 = rhome1.create();
            ejb1.callNonRecursiveSelf(5, ejb1);
            fail("Unexpected return from callNonRecursiveSelf().");
        } catch (SFLApplException bae) {
            String className = bae.getClass().getName();
            assertTrue("Did not Catch expected exception for " + className + ": " + bae, bae.passed);
        } finally {
            if (ejb1 != null) {
                ejb1.remove();
            }
        }
    }

    /**
     * (bmc07) Test Stateful local reentrant recursive method call.
     */
    //@Test
    public void testSFLocalInterfaceMethod_ReentrantRecursive() throws Exception {
        svLogger.info("This test does not apply to stateful beans; not reentrant.");
    }
}

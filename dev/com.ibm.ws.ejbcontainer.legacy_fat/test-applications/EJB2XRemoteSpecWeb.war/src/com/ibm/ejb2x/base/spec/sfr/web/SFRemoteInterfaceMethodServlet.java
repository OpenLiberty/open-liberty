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
package com.ibm.ejb2x.base.spec.sfr.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.rmi.NoSuchObjectException;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.annotation.WebServlet;

import org.junit.Ignore;
import org.junit.Test;

import com.ibm.ejb2x.base.spec.sfr.ejb.SFRApplException;
import com.ibm.ejb2x.base.spec.sfr.ejb.SFRTestReentrance;
import com.ibm.ejb2x.base.spec.sfr.ejb.SFRTestReentranceHome;
import com.ibm.ejb2x.base.spec.sfr.ejb.SFRa;
import com.ibm.ejb2x.base.spec.sfr.ejb.SFRaHome;
import com.ibm.ejb2x.base.spec.sfr.ejb.SFRaPassBy;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SFRemoteInterfaceMethodTest (formerly WSTestSFR_BMTest)
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
@WebServlet("/SFRemoteInterfaceMethodServlet")
public class SFRemoteInterfaceMethodServlet extends FATServlet {
    private final static String CLASS_NAME = SFRemoteInterfaceMethodServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static final double DDELTA = 0.0D;

    private final static String ejbJndiName1 = "com/ibm/ejb2x/base/spec/sfr/ejb/SFRaBMTHome";
    private final static String ejbJndiName2 = "com/ibm/ejb2x/base/spec/sfr/ejb/SFRNonReentranceHome";
    private static SFRaHome fhome1;
    private static SFRTestReentranceHome rhome1;

    private static SFRa fejb1;

    @PostConstruct
    private void initializeBeans() {
        try {
            fhome1 = FATHelper.lookupRemoteHomeBinding(ejbJndiName1, SFRaHome.class);
            rhome1 = FATHelper.lookupRemoteHomeBinding(ejbJndiName2, SFRTestReentranceHome.class);

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
     * (bmg01) Test Stateful remote get/set methods for boolean value.
     */
    @Test
    public void testSFRemoteInterface_get_set_boolean() throws Exception {
        boolean originalValue = fejb1.getBooleanValue();
        fejb1.setBooleanValue(!originalValue);
        assertTrue("Set value test was unexpected value", fejb1.getBooleanValue() == !originalValue);
        fejb1.setBooleanValue(originalValue);
        assertTrue("Restore value test was unexpected value", fejb1.getBooleanValue() == originalValue);
    }

    /**
     * (bmg02) Test Stateful remote get/set methods for byte value.
     */
    @Test
    public void testSFRemoteInterface_get_set_byte() throws Exception {
        byte originalValue = fejb1.getByteValue();
        fejb1.setByteValue((byte) (originalValue + 1));
        assertEquals("Set value test was unexpected value", fejb1.getByteValue(), (byte) (originalValue + 1));
        fejb1.setByteValue(originalValue);
        assertEquals("Restore value test was unexpected value", fejb1.getByteValue(), originalValue);
    }

    /**
     * (bmg03) Test Stateful remote get/set methods for char value.
     */
    @Test
    public void testSFRemoteInterface_get_set_char() throws Exception {
        char originalValue = fejb1.getCharValue();
        fejb1.setCharValue((char) (originalValue + 1));
        assertEquals("Set value test was unexpected value", fejb1.getCharValue(), (char) (originalValue + 1));
        fejb1.setCharValue(originalValue);
        assertEquals("Restore value test was unexpected value", fejb1.getCharValue(), originalValue);
    }

    /**
     * (bmg04) Test Stateful remote get/set methods for short value.
     */
    @Test
    public void testSFRemoteInterface_get_set_short() throws Exception {
        short originalValue = fejb1.getShortValue();
        fejb1.setShortValue((short) (originalValue + 1));
        assertEquals("Set value test was unexpected value", fejb1.getShortValue(), (short) (originalValue + 1));
        fejb1.setShortValue(originalValue);
        assertEquals("Restore value test was unexpected value", fejb1.getShortValue(), originalValue);
    }

    /**
     * (bmg05) Test Stateful remote get/set methods for int value.
     */
    @Test
    public void testSFRemoteInterface_get_set_int() throws Exception {
        int originalValue = fejb1.getIntValue();
        fejb1.setIntValue((originalValue + 1));
        assertEquals("Set value test was unexpected value", fejb1.getIntValue(), (originalValue + 1));
        fejb1.setIntValue(originalValue);
        assertEquals("Restore value test was unexpected value", fejb1.getIntValue(), originalValue);
    }

    /**
     * (bmg06) Test Stateful remote get/set methods for long value.
     */
    @Test
    public void testSFRemoteInterface_get_set_long() throws Exception {
        long originalValue = fejb1.getLongValue();
        fejb1.setLongValue((originalValue + 1));
        assertEquals("Set value test was unexpected value", fejb1.getLongValue(), (originalValue + 1));
        fejb1.setLongValue(originalValue);
        assertEquals("Restore value test was unexpected value", fejb1.getLongValue(), originalValue);
    }

    /**
     * (bmg07) Test Stateful remote get/set methods for float value.
     */
    @Test
    public void testSFRemoteInterface_get_set_float() throws Exception {
        float originalValue = fejb1.getFloatValue();
        fejb1.setFloatValue((float) (originalValue + 1.0));
        assertEquals("Set value test was unexpected value", fejb1.getFloatValue(), (float) (originalValue + 1.0), DDELTA);
        fejb1.setFloatValue(originalValue);
        assertEquals("Restore value test was unexpected value", fejb1.getFloatValue(), originalValue, DDELTA);
    }

    /**
     * (bmg08) Test Stateful remote get/set methods for double value.
     */
    @Test
    public void testSFRemoteInterface_get_set_double() throws Exception {
        double originalValue = fejb1.getDoubleValue();
        fejb1.setDoubleValue((originalValue + 1.0));
        assertEquals("Set value test was unexpected value", fejb1.getDoubleValue(), (originalValue + 1.0), DDELTA);
        fejb1.setDoubleValue(originalValue);
        assertEquals("Restore value test was unexpected value", fejb1.getDoubleValue(), originalValue, DDELTA);
    }

    /**
     * (bmg09) Test Stateful remote get/set methods for String value.
     */
    @Test
    public void testSFRemoteInterface_get_set_String() throws Exception {
        String originalValue = fejb1.getStringValue();
        fejb1.setStringValue(originalValue + "One More");
        assertEquals("Set value test was unexpected value", fejb1.getStringValue(), originalValue + "One More");
        fejb1.setStringValue(originalValue);
        assertEquals("Restore value test was unexpected value", fejb1.getStringValue(), originalValue);
    }

    /**
     * (bmg10) Test Stateful remote get/set methods for Object value.
     */
    @Test
    public void testSFRemoteInterface_get_set_Object() throws Exception {
        Integer originalValue = fejb1.getIntegerValue();
        Integer newValue = new Integer(2010);
        fejb1.setIntegerValue(newValue);
        assertEquals("Set value test was unexpected value", fejb1.getIntegerValue(), newValue);
        fejb1.setIntegerValue(originalValue);
        assertEquals("Restore value test was unexpected value", fejb1.getIntegerValue(), originalValue);
    }

    /**
     * (bmc01) Test Stateful remote simple method call.
     */
    @Test
    public void testSFRemoteInterfaceMethod() throws Exception {
        String testStr = "Test string.";
        String buf = fejb1.method1(testStr);
        assertEquals("Method call (method1) test was unexpected value.", buf, testStr);
    }

    /**
     * (bmc02) Test Stateful remote method on non-existing EJB.
     */
    @Test
    public void testSFRemoteInterfaceMethod_NonExistEJB() throws Exception {
        SFRa ejb1 = null;
        svLogger.info("First create the EJB instance.");
        ejb1 = fhome1.create();
        assertNotNull("First find/create the EJB instance was null.", ejb1);
        svLogger.info("Remove the instance while still holding on the reference..");
        ejb1.remove();
        svLogger.info("Instance removed.");
        svLogger.info("Now execute a method to the object that doesn't exist.");
        String testStr = "Test string.";
        try {
            String buf = ejb1.method1(testStr);
            fail("Unexpected successful method call after remove : " + buf);
        } catch (NoSuchObjectException ex) {
            svLogger.info("Caught expected " + ex.getClass().getName());
        }
    }

    /**
     * (bmc03) Test Stateful remote method with pass-by-reference.
     */
    @Test
    public void testSFRemoteInterfaceMethod_PassByReference() throws Exception {
        String inKeyStr = "inKeyStr";
        String outKeyStr = "outKeyStr";

        SFRaPassBy inPbr = new SFRaPassBy(inKeyStr, outKeyStr, 3);
        SFRaPassBy outPbr = fejb1.changePassByParm(inPbr);
        assertEquals("Test inPbr key was unexpected value.", inPbr.getKey(), inKeyStr);
        assertEquals("Test inPbr key2 was unexpected value.", inPbr.getKey2(), outKeyStr);
        assertEquals("Test inPbr value was unexpected value.", inPbr.getValue(), 3);
        assertEquals("Test outPbr key was unexpected value.", outPbr.getKey(), outKeyStr);
        assertEquals("Test outPbr key2 was unexpected value.", outPbr.getKey2(), outKeyStr);
        assertEquals("Test outPbr value was unexpected value.", outPbr.getValue(), 4);
    }

    /**
     * (bmc04) Test Stateful remote method with pass-by-value.
     */
    //@Test
    public void testSFRemoteInterfaceMethod_PassByValue() throws Exception {
        svLogger.info("This test does not apply to remote beans; remote is pass-by-reference.");
    }

    /**
     * (bmc05) Test Stateful remote method in different transaction.
     */
    //@Test
    public void testSFRemoteInterfaceMethod_DifferentTransaction() throws Exception {
        svLogger.info("Tested elsewhere: see stateful_concurrency test suite.");
    }

    /**
     * (bmc06) Test Stateful remote non-reentrant recursive method call. <p>
     *
     * See EJB 2.0 Spec section 12.1.11.
     */
    @Test
    @Ignore
    // TODO: #8511 - Recursive call hangs waiting for lock instead of throwing BeanNotReentrant exception
    public void testSFRemoteInterfaceMethod_NonReentrantRecursive() throws Exception {
        SFRTestReentrance ejb1 = null;
        try {
            ejb1 = rhome1.create();
            ejb1.callNonRecursiveSelf(5, ejb1);
            fail("Unexpected return from callNonRecursiveSelf().");
        } catch (SFRApplException bae) {
            String className = bae.getClass().getName();
            assertTrue("Did not catch expected  " + className + ": " + bae, bae.passed);
        } finally {
            if (ejb1 != null) {
                ejb1.remove();
            }
        }
    }

    /**
     * (bmc07) Test Stateful remote reentrant recursive method call.
     */
    //@Test
    public void testSFRemoteInterfaceMethod_ReentrantRecursive() throws Exception {
        svLogger.info("This test does not apply to stateful beans; not reentrant.");
    }
}
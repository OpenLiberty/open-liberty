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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.base.spec.sll.ejb.SLLa;
import com.ibm.ejb2x.base.spec.sll.ejb.SLLaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SLLocalImplEnvEntryTest (formerly WSTestSLL_IVTest)
 *
 * <dt>Test Descriptions:
 * <dd>EJB Container basic function tests:
 * <ul>
 * <li>I____ - Bean Implementation;
 * <li>IVE__ - Environment - java:comp/env.
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
 * <li>ive01 - Env - Boolean
 * <li>ive02 - Env - Byte
 * <li>ive03 - Env - Character
 * <li>ive04 - Env - Short
 * <li>ive05 - Env - Integer
 * <li>ive06 - Env - Long
 * <li>ive07 - Env - Float
 * <li>ive08 - Env - Double
 * <li>ive09 - Env - String
 * <li>ive10 - Env - Boolean - no env-entry-value
 * <li>ive11 - Env - Byte - no env-entry-value
 * <li>ive12 - Env - Character - no env-entry-value
 * <li>ive13 - Env - Short - no env-entry-value
 * <li>ive14 - Env - Integer - no env-entry-value
 * <li>ive15 - Env - Long - no env-entry-value
 * <li>ive16 - Env - Float - no env-entry-value
 * <li>ive17 - Env - Double - no env-entry-value
 * <li>ive18 - Env - String - no env-entry-value
 * <li>ive19 - Env - Boolean - invalid env-entry-value
 * <li>ive29 - Env - Byte - invalid env-entry-value
 * <li>ive21 - Env - Character - invalid env-entry-value
 * <li>ive22 - Env - Short - invalid env-entry-value
 * <li>ive23 - Env - Integer - invalid env-entry-value
 * <li>ive24 - Env - Long - invalid env-entry-value
 * <li>ive25 - Env - Float - invalid env-entry-value
 * <li>ive26 - Env - Double - invalid env-entry-value
 * <li>ive27 - Env - String - invalid env-entry-value
 * <li>ive28 - Env - Boolean - no env-entry
 * <li>ive29 - Env - Byte - no env-entry
 * <li>ive30 - Env - Character - no env-entry
 * <li>ive31 - Env - Short - no env-entry
 * <li>ive32 - Env - Integer - no env-entry
 * <li>ive33 - Env - Long - no env-entry
 * <li>ive34 - Env - Float - no env-entry
 * <li>ive35 - Env - Double - no env-entry
 * <li>ive36 - Env - String - no env-entry
 * <li>ive37 - Modify Env - Boolean
 * <li>ive38 - Modify Env - Byte
 * <li>ive39 - Modify Env - Character
 * <li>ive40 - Modify Env - Short
 * <li>ive41 - Modify Env - Integer
 * <li>ive42 - Modify Env - Long
 * <li>ive43 - Modify Env - Float
 * <li>ive44 - Modify Env - Double
 * <li>ive45 - Modify Env - String
 * </ul>
 * <br>Data Sources
 * </dl>
 */

@SuppressWarnings("serial")
@WebServlet("/SLLocalImplEnvEntryServlet")
@AllowedFFDC({ "java.lang.StringIndexOutOfBoundsException", "java.lang.NumberFormatException" })
public class SLLocalImplEnvEntryServlet extends FATServlet {

    private final static String CLASS_NAME = SLLocalImplEnvEntryServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb2x/base/spec/sll/ejb/SLLaEnvEntryHome";
    private SLLaHome fhome1;

    private static final double DDELTA = 0.0D;

    @PostConstruct
    private void initializeHomes() {
        try {
            fhome1 = (SLLaHome) FATHelper.lookupLocalHome(ejbJndiName1);
            //fhome1 = (SLLaHome) new InitialContext().lookup("java:app/EJB2XSLLocalSpecEJB/SLLaEnvEntry");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (ive01) Test and env-entry of type Boolean with a valid value.
     */
    @Test
    public void testSLLocalEnvEntry_Boolean() throws Exception {
        SLLa ejb1 = fhome1.create();
        // The test case looks for a environment variable named "envBoolean".
        Boolean tempBoolean = ejb1.getBooleanEnvVar("envBoolean");
        assertNotNull("Get environment boolean object returned null.", tempBoolean);
        assertTrue("Test content of environment was not true", tempBoolean.booleanValue());

        // Also check for a environment variable with a blank value

        tempBoolean = ejb1.getBooleanEnvVar("envBooleanBlankValue");
        assertNotNull("Get environment boolean object returned null.", tempBoolean);
        assertFalse("Test content of environment was not true", tempBoolean.booleanValue());
    }

    /**
     * (ive02) Test and env-entry of type Byte with a valid value.
     */
    @Test
    public void testSLLocalEnvEntry_Byte() throws Exception {
        SLLa ejb1 = fhome1.create();
        // The test case looks for a environment variable named "envByte".
        Byte tempByte = ejb1.getByteEnvVar("envByte");
        assertNotNull("Get environment byte object returned null.", tempByte);
        assertEquals("Test content of environment was not expected value", tempByte.byteValue(), (byte) 9);
    }

    /**
     * (ive03) Test and env-entry of type Character with a valid value.
     */
    @Test
    public void testSLLocalEnvEntry_Character() throws Exception {
        SLLa ejb1 = fhome1.create();
        // The test case looks for a environment variable named "envCharacter".
        // The value should be specified in the deployment descriptor before the test.
        Character tempCharacter = ejb1.getCharacterEnvVar("envCharacter");
        assertNotNull("Get environment character object returned null.", tempCharacter);
        assertEquals("Test content of environment was not expected value", tempCharacter.charValue(), 'C');
    }

    /**
     * (ive04) Test and env-entry of type Short with a valid value.
     */
    @Test
    public void testSLLocalEnvEntry_Short() throws Exception {
        SLLa ejb1 = fhome1.create();
        // The test case looks for a environment variable named "envShort".
        // The value should be specified in the deployment descriptor before the test.
        Short tempShort = ejb1.getShortEnvVar("envShort");
        assertNotNull("Get environment short object returned null.", tempShort);
        assertEquals("Test content of environment was not expected value", tempShort.shortValue(), (short) 7006);
    }

    /**
     * (ive05) Test and env-entry of type Integer with a valid value.
     */
    @Test
    public void testSLLocalEnvEntry_Integer() throws Exception {
        SLLa ejb1 = fhome1.create();
        // The test case looks for a environment variable named "envInteger".
        // The value should be specified in the deployment descriptor before the test.
        Integer tempInt = ejb1.getIntegerEnvVar("envInteger");
        assertNotNull("Get environment integer object returned null.", tempInt);
        assertEquals("Test content of environment was not expected value", tempInt.intValue(), 7004);
    }

    /**
     * (ive06) Test and env-entry of type Long with a valid value.
     */
    @Test
    public void testSLLocalEnvEntry_Long() throws Exception {
        SLLa ejb1 = fhome1.create();
        // The test case looks for a environment variable named "envLong".
        // The value should be specified in the deployment descriptor before the test.
        Long tempLong = ejb1.getLongEnvVar("envLong");
        assertNotNull("Get environment long object returned null.", tempLong);
        assertEquals("Test content of environment was not expected value", tempLong.longValue(), 7005);
    }

    /**
     * (ive07) Test and env-entry of type Float with a valid value.
     */
    @Test
    public void testSLLocalEnvEntry_Float() throws Exception {
        SLLa ejb1 = fhome1.create();
        // The test case looks for a environment variable named "envFloat".
        Float tempFloat = ejb1.getFloatEnvVar("envFloat");
        assertNotNull("Get environment float object returned null.", tempFloat);
        assertEquals("Test content of environment was not expected value", tempFloat.floatValue(), (float) 7007.0, DDELTA);

        tempFloat = ejb1.getFloatEnvVar("envFloat2");
        assertNotNull("Get environment float object returned null.", tempFloat);
        assertEquals("Test content of environment was not expected value", tempFloat.floatValue(), (float) 7007.0, DDELTA);

        tempFloat = ejb1.getFloatEnvVar("envFloat3");
        assertNotNull("Get environment float object returned null.", tempFloat);
        assertEquals("Test content of environment was not expected value", tempFloat.floatValue(), (float) 7.0070e3, DDELTA);
    }

    /**
     * (ive08) Test and env-entry of type Double with a valid value.
     */
    @Test
    public void testSLLocalEnvEntry_Double() throws Exception {
        SLLa ejb1 = fhome1.create();
        // The test case looks for a environment variable named "envDouble".
        Double tempDouble = ejb1.getDoubleEnvVar("envDouble");
        assertNotNull("Get environment double object returned null.", tempDouble);
        assertEquals("Test content of environment was not expected value", tempDouble.doubleValue(), 7003.0, DDELTA);

        tempDouble = ejb1.getDoubleEnvVar("envDouble2");
        assertNotNull("Get environment double object returned null.", tempDouble);
        assertEquals("Test content of environment was not expected value", tempDouble.doubleValue(), 7003.0, DDELTA);

        tempDouble = ejb1.getDoubleEnvVar("envDouble3");
        assertNotNull("Get environment double object returned null.", tempDouble);
        assertEquals("Test content of environment was not expected value", tempDouble.doubleValue(), 7.0030e3d, DDELTA);
    }

    /**
     * (ive09) Test and env-entry of type String with a valid value.
     */
    @Test
    public void testSLLocalEnvEntry_String() throws Exception {
        SLLa ejb1 = fhome1.create();
        // The test case looks for a environment variable named "envString".
        // The value should be specified in the deployment descriptor before the test.
        String tempStr = ejb1.getStringEnvVar("envString");
        assertNotNull("Get environment string object returned null.", tempStr);
        assertEquals("Test content of environment was not expected value", tempStr, "IEV09");

        // The test case looks for a environment variable named "envStringBlankValue".
        tempStr = ejb1.getStringEnvVar("envStringBlankValue");
        assertNotNull("Get environment blank string object returned null.", tempStr);
        assertEquals("Test content of environment was not expected value", tempStr, "");
    }

    /**
     * (ive10) Test an env-entry of type Boolean with no env-entry-value.
     */
    @Test
    public void testSLLocalEnvEntry_Boolean_NoValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envBooleanNoValue".
            // No value should be specified in the deployment descriptor before the test.
            Boolean tempBoolean = ejb1.getBooleanEnvVar("envBooleanNoValue");
            fail("Get environment no env-entry-value boolean object should have failed, instead got: " + tempBoolean);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive11) Test an env-entry of type Byte with no env-entry-value.
     */
    @Test
    public void testSLLocalEnvEntry_Byte_NoValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envByteNoValue".
            // No value should be specified in the deployment descriptor before the test.
            Byte tempByte = ejb1.getByteEnvVar("envByteNoValue");
            fail("Get environment blank byte object should have failed, instead got: " + tempByte);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive12) Test an env-entry of type Character with no env-entry-value.
     */
    @Test
    public void testSLLocalEnvEntry_Character_NoValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envCharacterNoValue".
            // No value should be specified in the deployment descriptor before the test.
            Character tempCharacter = ejb1.getCharacterEnvVar("envCharacterNoValue");
            fail("Get environment blank character object should have failed, instead got: " + tempCharacter);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive13) Test an env-entry of type Short with no env-entry-value.
     */
    @Test
    public void testSLLocalEnvEntry_Short_NoValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envShortBlank".
            // No value should be specified in the deployment descriptor before the test.
            Short tempShort = ejb1.getShortEnvVar("envShortNoValue");
            fail("Get environment blank short object should have failed, instead got: " + tempShort);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive14) Test an env-entry of type Integer with no env-entry-value.
     */
    @Test
    public void testSLLocalEnvEntry_Integer_NoValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envIntegerNoValue".
            // No value should be specified in the deployment descriptor before the test.
            Integer tempInt = ejb1.getIntegerEnvVar("envIntegerNoValue");
            fail("Get environment blank integer object should have failed, instead got: " + tempInt);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive15) Test an env-entry of type Long with no env-entry-value.
     */
    @Test
    public void testSLLocalEnvEntry_Long_NoValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envLongNoValue".
            // No value should be specified in the deployment descriptor before the test.
            Long tempLong = ejb1.getLongEnvVar("envLongNoValue");
            fail("Get environment blank long object should have failed, instead got: " + tempLong);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive16) Test an env-entry of type Float with no env-entry-value.
     */
    @Test
    public void testSLLocalEnvEntry_Float_NoValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envFloatNoValue".
            // No value should be specified in the deployment descriptor before the test.
            Float tempFloat = ejb1.getFloatEnvVar("envFloatNoValue");
            fail("Get environment blank float object should have failed, instead got: " + tempFloat);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive17) Test an env-entry of type Double with no env-entry-value.
     */
    @Test
    public void testSLLocalEnvEntry_Double_NoValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envDoubleNoValue".
            // No value should be specified in the deployment descriptor before the test.
            Double tempDouble = ejb1.getDoubleEnvVar("envDoubleNoValue");
            fail("Get environment blank double object should have failed, instead got: " + tempDouble);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive18) Test an env-entry of type String with no env-entry-value.
     */
    @Test
    public void testSLLocalEnvEntry_String_NoValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envStringNoValue".
            // No value should be specified in the deployment descriptor before the test.
            String tempStr = ejb1.getStringEnvVar("envStringNoValue");
            fail("Get environment blank string object should have failed, instead got: " + tempStr);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive19) Test an env-entry of type Boolean with an invalid value.
     */
    @Test
    public void testSLLocalEnvEntry_Boolean_InvalidValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        // The test case looks for an environment variable named "envBooleanInvalid".
        Boolean tempBoolean = ejb1.getBooleanEnvVar("envBooleanInvalid");
        assertNotNull("Get environment boolean object was null.", tempBoolean);
        assertFalse("Test content of environment, expected false but was true", tempBoolean.booleanValue());
    }

    /**
     * (ive20) Test an env-entry of type Byte with an invalid value.
     */
    @Test
    public void testSLLocalEnvEntry_Byte_InvalidValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envByteBlankValue".
            Byte tempByte = ejb1.getByteEnvVar("envByteBlankValue");
            fail("Get environment invalid byte object should have failed, instead got: " + tempByte);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envByteInvalid".
            Byte tempByte = ejb1.getByteEnvVar("envByteInvalid");
            fail("Get environment invalid byte object should have failed, instead got: " + tempByte);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envByteGT8bit".
            Byte tempByte = ejb1.getByteEnvVar("envByteGT8bit");
            fail("Get environment invalid byte object should have failed, instead got: " + tempByte);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive21) Test an env-entry of type Character with an invalid value.
     */
    @Test
    public void testSLLocalEnvEntry_Character_InvalidValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envCharacterBlankValue".
            Character tempCharacter = ejb1.getCharacterEnvVar("envCharacterBlankValue");
            fail("Get environment invalid character object =" + tempCharacter);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        // The test case looks for a environment variable named "envCharacterInvalid".
        Character tempCharacter = ejb1.getCharacterEnvVar("envCharacterInvalid");
        assertEquals("Get environment invalid character object.", tempCharacter.charValue(), 'I');
    }

    /**
     * (ive22) Test an env-entry of type Short with an invalid value.
     */
    @Test
    public void testSLLocalEnvEntry_Short_InvalidValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envShortInvalid".
            Short tempShort = ejb1.getShortEnvVar("envShortBlankValue");
            fail("Get environment invalid short object should have failed, instead got: " + tempShort);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envShortInvalid".
            Short tempShort = ejb1.getShortEnvVar("envShortInvalid");
            fail("Get environment invalid short object should have failed, instead got: " + tempShort);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envShortInvalid".
            Short tempShort = ejb1.getShortEnvVar("envShortGT16bit");
            fail("Get environment invalid short object should have failed, instead got: " + tempShort);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive23) Test an env-entry of type Integer with an invalid value.
     */
    @Test
    public void testSLLocalEnvEntry_Integer_InvalidValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envIntegerInvalid".
            Integer tempInt = ejb1.getIntegerEnvVar("envIntegerBlankValue");
            fail("Get environment invalid int object should have failed, instead got: " + tempInt);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envIntegerInvalid".
            Integer tempInt = ejb1.getIntegerEnvVar("envIntegerInvalid");
            fail("Get environment invalid int object should have failed, instead got: " + tempInt);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envIntegerInvalid".
            Integer tempInt = ejb1.getIntegerEnvVar("envIntegerGT32bit");
            fail("Get environment invalid int object should have failed, instead got: " + tempInt);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive24) Test an env-entry of type Long with an invalid value.
     */
    @Test
    public void testSLLocalEnvEntry_Long_InvalidValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envLongInvalid".
            Long tempLong = ejb1.getLongEnvVar("envLongBlankValue");
            fail("Get environment invalid long object should have failed, instead got: " + tempLong);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envLongInvalid".
            Long tempLong = ejb1.getLongEnvVar("envLongInvalid");
            fail("Get environment invalid long object should have failed, instead got: " + tempLong);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envLongInvalid".
            Long tempLong = ejb1.getLongEnvVar("envLongGT64bit");
            fail("Get environment invalid long object should have failed, instead got: " + tempLong);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive25) Test an env-entry of type Float with an invalid value.
     */
    @Test
    public void testSLLocalEnvEntry_Float_InvalidValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envFloatInvalid".
            Float tempFloat = ejb1.getFloatEnvVar("envFloatBlankValue");
            fail("Get environment invalid float object should have failed, instead got: " + tempFloat);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envFloatInvalid".
            Float tempFloat = ejb1.getFloatEnvVar("envFloatInvalid");
            fail("Get environment invalid float object should have failed, instead got: " + tempFloat);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        // The test case looks for a environment variable named "envFloatInvalid".
        Float tempFloat = ejb1.getFloatEnvVar("envFloatGT32bit");
        assertTrue("Get environment invalid float object was not infinity (Float.isInifinite)", Float.isInfinite(tempFloat.floatValue()));
    }

    /**
     * (ive26) Test an env-entry of type Double with an invalid value.
     */
    @Test
    public void testSLLocalEnvEntry_Double_InvalidValue() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envDoubleInvalid".
            Double tempDouble = ejb1.getDoubleEnvVar("envDoubleBlankdValue");
            fail("Get environment invalid double object should have failed, instead got: " + tempDouble);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envDoubleInvalid".
            Double tempDouble = ejb1.getDoubleEnvVar("envDoubleInvalid");
            fail("Get environment invalid double object should have failed, instead got: " + tempDouble);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        // The test case looks for a environment variable named "envDoubleInvalid".
        Double tempDouble = ejb1.getDoubleEnvVar("envDoubleGT64bit");
        assertTrue("Get environment invalid double object was not infinity (Double.isInifinite)", Double.isInfinite(tempDouble.doubleValue()));
    }

    /**
     * (ive27) Test an env-entry of type String with an invalid value.
     */
    //@Test
    public void testSLLocalEnvEntry_String_InvalidValue() throws Exception {
        svLogger.info("No invalid condition for String.");
    }

    /**
     * (ive28) Test an env-entry of type Boolean where there is no env-entry.
     */
    @Test
    public void testSLLocalEnvEntry_Boolean_NotExist() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envBooleanNotExist".
            Boolean tempBoolean = ejb1.getBooleanEnvVar("envBooleanNotExist");
            fail("Get environment not exist for boolean object should have failed, instead got: " + tempBoolean);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive29) Test an env-entry of type Byte where there is no env-entry.
     */
    @Test
    public void testSLLocalEnvEntry_Byte_NotExist() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envByteNotExist".
            Byte tempByte = ejb1.getByteEnvVar("envByteNotExist");
            fail("Get environment not exist byte object should have failed, instead got: " + tempByte);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive30) Test an env-entry of type Character where there is no env-entry.
     */
    @Test
    public void testSLLocalEnvEntry_Character_NotExist() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envCharacterNotExist".
            Character tempCharacter = ejb1.getCharacterEnvVar("envCharacterNotExist");
            fail("Get environment not exist character object should have failed, instead got: " + tempCharacter);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive31) Test an env-entry of type Short where there is no env-entry.
     */
    @Test
    public void testSLLocalEnvEntry_Short_NotExist() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envShortNotExist".
            Short tempShort = ejb1.getShortEnvVar("envShortNotExist");
            fail("Get environment not exist short object should have failed, instead got: " + tempShort);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive32) Test an env-entry of type Integer where there is no env-entry.
     */
    @Test
    public void testSLLocalEnvEntry_Integer_NotExist() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envIntegerNotExist".
            Integer tempInt = ejb1.getIntegerEnvVar("envIntegerNotExist");
            fail("Get environment not exist int object should have failed, instead got: " + tempInt);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive33) Test an env-entry of type Long where there is no env-entry.
     */
    @Test
    public void testSLLocalEnvEntry_Long_NotExist() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envLongNotExist".
            Long tempLong = ejb1.getLongEnvVar("envLongNotExist");
            fail("Get environment not exist long object should have failed, instead got: " + tempLong);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive34) Test an env-entry of type Float where there is no env-entry.
     */
    @Test
    public void testSLLocalEnvEntry_Float_NotExist() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envFloatNotExist".
            Float tempFloat = ejb1.getFloatEnvVar("envFloatNotExist");
            fail("Get environment not exist float object should have failed, instead got: " + tempFloat);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive35) Test an env-entry of type Double where there is no env-entry.
     */
    @Test
    public void testSLLocalEnvEntry_Double_NotExist() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envDoubleNotExist".
            Double tempDouble = ejb1.getDoubleEnvVar("envDoubleNotExist");
            fail("Get environment not exist double object should have failed, instead got: " + tempDouble);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive36) Test an env-entry of type String where there is no env-entry.
     */
    @Test
    public void testSLLocalEnvEntry_String_NotExist() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            // The test case looks for a environment variable named "envStringNotExist".
            String tempStr = ejb1.getStringEnvVar("envStringNotExist");
            fail("Get environment not exist string object should have failed, instead got: " + tempStr);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive37) Test that an env-entry of type Boolean cannot be modified.
     */
    @Test
    public void testSLLocalEnvEntry_Boolean_Modify() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            ejb1.bindEnvVar("envBoolean", new Boolean(true));
            fail("Unexpected return from bind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            ejb1.rebindEnvVar("envBoolean", new Boolean(true));
            fail("Unexpected return from rebind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive38) Test that an env-entry of type Byte cannot be modified.
     */
    @Test
    public void testSLLocalEnvEntry_Byte_Modify() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            ejb1.bindEnvVar("envByte", new Byte((byte) 10));
            fail("Unexpected return from bind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            ejb1.rebindEnvVar("envBoolean", new Byte((byte) 11));
            fail("Unexpected return from rebind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive39) Test that an env-entry of type Character cannot be modified.
     */
    @Test
    public void testSLLocalEnvEntry_Character_Modify() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            ejb1.bindEnvVar("envCharacter", new Character('X'));
            fail("Unexpected return from bind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            ejb1.rebindEnvVar("envCharacter", new Character('Y'));
            fail("Unexpected return from rebind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive40) Test that an env-entry of type Short cannot be modified.
     */
    @Test
    public void testSLLocalEnvEntry_Short_Modify() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            ejb1.bindEnvVar("envShort", new Short((short) 111));
            fail("Unexpected return from bind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            ejb1.rebindEnvVar("envShort", new Short((short) 112));
            fail("Unexpected return from rebind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive41) Test that an env-entry of type Integer cannot be modified.
     */
    @Test
    public void testSLLocalEnvEntry_Integer_Modify() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            ejb1.bindEnvVar("envInteger", new Integer(111));
            fail("Unexpected return from bind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            ejb1.rebindEnvVar("envInteger", new Integer(112));
            fail("Unexpected return from rebind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive42) Test that an env-entry of type Long cannot be modified.
     */
    @Test
    public void testSLLocalEnvEntry_Long_Modify() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            ejb1.bindEnvVar("envLong", new Long(111));
            fail("Unexpected return from bind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            ejb1.rebindEnvVar("envLong", new Long(112));
            fail("Unexpected return from rebind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive43) Test that an env-entry of type Float cannot be modified.
     */
    @Test
    public void testSLLocalEnvEntry_Float_Modify() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            ejb1.bindEnvVar("envFloat", new Float(111.0));
            fail("Unexpected return from bind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            ejb1.rebindEnvVar("envFloat", new Float(112.0));
            fail("Unexpected return from rebind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive44) Test that an env-entry of type Double cannot be modified.
     */
    @Test
    public void testSLLocalEnvEntry_Double_Modify() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            ejb1.bindEnvVar("envDouble", new Double(111.0));
            fail("Unexpected return from bind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            ejb1.rebindEnvVar("envDouble", new Double(112.0));
            fail("Unexpected return from rebind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive45) Test that an env-entry of type String cannot be modified.
     */
    @Test
    public void testSLLocalEnvEntry_String_Modify() throws Exception {
        SLLa ejb1 = fhome1.create();
        try {
            ejb1.bindEnvVar("envString", new String("111.0"));
            fail("Unexpected return from bind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            ejb1.rebindEnvVar("envString", new String("112.0"));
            fail("Unexpected return from rebind(), it should have failed.");
        } catch (javax.naming.OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }
}

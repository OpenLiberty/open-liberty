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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.base.spec.slr.ejb.SLRa;
import com.ibm.ejb2x.base.spec.slr.ejb.SLRaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SLRemoteImplEnvEntryTest (formerly WSTestSLR_IVTest)
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
@WebServlet("/SLRemoteImplEnvEntryServlet")
@AllowedFFDC({ "java.lang.StringIndexOutOfBoundsException", "java.lang.NumberFormatException" })
public class SLRemoteImplEnvEntryServlet extends FATServlet {
    private final static String CLASS_NAME = SLRemoteImplEnvEntryServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb2x/base/spec/slr/ejb/SLRaEnvEntryHome";
    private static SLRaHome fhome1;
    private static SLRa fejb1;
    private static final double DDELTA = 0.0D;

    @PostConstruct
    private void initializeBeans() {
        try {
            fhome1 = FATHelper.lookupRemoteHomeBinding(ejbJndiName1, SLRaHome.class);

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
     * (ive01) Test and env-entry of type Boolean with a valid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Boolean() throws Exception {
        // The test case looks for a environment variable named "envBoolean".
        Boolean tempBoolean = fejb1.getBooleanEnvVar("envBoolean");
        assertNotNull("Get environment boolean object was null.", tempBoolean);
        assertTrue("       test content of environment, expecting true", tempBoolean.booleanValue());

        // Also check for a environment variable with a blank value
        tempBoolean = fejb1.getBooleanEnvVar("envBooleanBlankValue");
        assertNotNull("Get environment boolean object was null.", tempBoolean);
        assertFalse("       test content of environment, expecting true", tempBoolean.booleanValue());
    }

    /**
     * (ive02) Test and env-entry of type Byte with a valid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Byte() throws Exception {
        // The test case looks for a environment variable named "envByte".
        Byte tempByte = fejb1.getByteEnvVar("envByte");
        assertNotNull("Get environment byte object was null.", tempByte);
        assertEquals("Test content of environment was unexpected value", tempByte.byteValue(), (byte) 9);
    }

    /**
     * (ive03) Test and env-entry of type Character with a valid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Character() throws Exception {
        // The test case looks for a environment variable named "envCharacter".
        // The value should be specified in the deployment descriptor before the test.
        Character tempCharacter = fejb1.getCharacterEnvVar("envCharacter");
        assertNotNull("Get environment character object was null.", tempCharacter);
        assertEquals("Test content of environment was unexpected value", tempCharacter.charValue(), 'C');
    }

    /**
     * (ive04) Test and env-entry of type Short with a valid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Short() throws Exception {
        // The test case looks for a environment variable named "envShort".
        // The value should be specified in the deployment descriptor before the test.
        Short tempShort = fejb1.getShortEnvVar("envShort");
        assertNotNull("Get environment short object was null.", tempShort);
        assertEquals("Test content of environment was unexpected value", tempShort.shortValue(), (short) 7006);
    }

    /**
     * (ive05) Test and env-entry of type Integer with a valid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Integer() throws Exception {
        // The test case looks for a environment variable named "envInteger".
        // The value should be specified in the deployment descriptor before the test.
        Integer tempInt = fejb1.getIntegerEnvVar("envInteger");
        assertNotNull("Get environment integer object was null.", tempInt);
        assertEquals("Test content of environment was unexpected value", tempInt.intValue(), 7004);
    }

    /**
     * (ive06) Test and env-entry of type Long with a valid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Long() throws Exception {
        // The test case looks for a environment variable named "envLong".
        // The value should be specified in the deployment descriptor before the test.
        Long tempLong = fejb1.getLongEnvVar("envLong");
        assertNotNull("Get environment long object was null.", tempLong);
        assertEquals("Test content of environment was unexpected value", tempLong.longValue(), 7005);
    }

    /**
     * (ive07) Test and env-entry of type Float with a valid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Float() throws Exception {
        // The test case looks for a environment variable named "envFloat".
        Float tempFloat = fejb1.getFloatEnvVar("envFloat");
        assertNotNull("Get environment float object was null.", tempFloat);
        assertEquals("Test content of environment was unexpected value", tempFloat.floatValue(), (float) 7007.0, DDELTA);

        tempFloat = fejb1.getFloatEnvVar("envFloat2");
        assertNotNull("Get environment float object was null.", tempFloat);
        assertEquals("Test content of environment was unexpected value", tempFloat.floatValue(), (float) 7007.0, DDELTA);

        tempFloat = fejb1.getFloatEnvVar("envFloat3");
        assertNotNull("Get environment float object was null.", tempFloat);
        assertEquals("Test content of environment was unexpected value", tempFloat.floatValue(), (float) 7.0070e3, DDELTA);
    }

    /**
     * (ive08) Test and env-entry of type Double with a valid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Double() throws Exception {
        // The test case looks for a environment variable named "envDouble".
        Double tempDouble = fejb1.getDoubleEnvVar("envDouble");
        assertNotNull("Get environment double object was null.", tempDouble);
        assertEquals("Test content of environment was unexpected value", tempDouble.doubleValue(), 7003.0, DDELTA);

        tempDouble = fejb1.getDoubleEnvVar("envDouble2");
        assertNotNull("Get environment double object was null.", tempDouble);
        assertEquals("Test content of environment was unexpected value", tempDouble.doubleValue(), 7003.0, DDELTA);

        tempDouble = fejb1.getDoubleEnvVar("envDouble3");
        assertNotNull("Get environment double object was null.", tempDouble);
        assertEquals("Test content of environment was unexpected value", tempDouble.doubleValue(), 7.0030e3d, DDELTA);
    }

    /**
     * (ive09) Test and env-entry of type String with a valid value.
     */
    @Test
    public void testSLRemoteEnvEntry_String() throws Exception {
        // The test case looks for a environment variable named "envString".
        // The value should be specified in the deployment descriptor before the test.
        String tempStr = fejb1.getStringEnvVar("envString");
        assertNotNull("Get environment string object was null.", tempStr);
        assertEquals("Test content of environment was unexpected value", tempStr, "IEV09");

        // The test case looks for a environment variable named "envStringBlankValue".
        tempStr = fejb1.getStringEnvVar("envStringBlankValue");
        assertNotNull("Get environment blank string object was null.", tempStr);
        assertEquals("Test content of environment was unexpected value", tempStr, "");
    }

    /**
     * (ive10) Test an env-entry of type Boolean with no env-entry-value.
     */
    @Test
    public void testSLRemoteEnvEntry_Boolean_NoValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envBooleanNoValue".
            // No value should be specified in the deployment descriptor before the test.
            Boolean tempBoolean = fejb1.getBooleanEnvVar("envBooleanNoValue");
            fail("Get environment no env-entry-value boolean object = " + tempBoolean);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive11) Test an env-entry of type Byte with no env-entry-value.
     */
    @Test
    public void testSLRemoteEnvEntry_Byte_NoValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envByteNoValue".
            // No value should be specified in the deployment descriptor before the test.
            Byte tempByte = fejb1.getByteEnvVar("envByteNoValue");
            fail("Get environment blank byte lookup should have failed, instead got " + tempByte);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive12) Test an env-entry of type Character with no env-entry-value.
     */
    @Test
    public void testSLRemoteEnvEntry_Character_NoValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envCharacterNoValue".
            // No value should be specified in the deployment descriptor before the test.
            Character tempCharacter = fejb1.getCharacterEnvVar("envCharacterNoValue");
            fail("Get environment blank character lookup should have failed, instead got " + tempCharacter);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive13) Test an env-entry of type Short with no env-entry-value.
     */
    @Test
    public void testSLRemoteEnvEntry_Short_NoValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envShortBlank".
            // No value should be specified in the deployment descriptor before the test.
            Short tempShort = fejb1.getShortEnvVar("envShortNoValue");
            fail("Get environment blank short lookup should have failed, instead got " + tempShort);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive14) Test an env-entry of type Integer with no env-entry-value.
     */
    @Test
    public void testSLRemoteEnvEntry_Integer_NoValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envIntegerNoValue".
            // No value should be specified in the deployment descriptor before the test.
            Integer tempInt = fejb1.getIntegerEnvVar("envIntegerNoValue");
            fail("Get environment blank integer lookup should have failed, instead got " + tempInt);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive15) Test an env-entry of type Long with no env-entry-value.
     */
    @Test
    public void testSLRemoteEnvEntry_Long_NoValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envLongNoValue".
            // No value should be specified in the deployment descriptor before the test.
            Long tempLong = fejb1.getLongEnvVar("envLongNoValue");
            fail("Get environment blank long lookup should have failed, instead got " + tempLong);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive16) Test an env-entry of type Float with no env-entry-value.
     */
    @Test
    public void testSLRemoteEnvEntry_Float_NoValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envFloatNoValue".
            // No value should be specified in the deployment descriptor before the test.
            Float tempFloat = fejb1.getFloatEnvVar("envFloatNoValue");
            fail("Get environment blank float lookup should have failed, instead got " + tempFloat);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive17) Test an env-entry of type Double with no env-entry-value.
     */
    @Test
    public void testSLRemoteEnvEntry_Double_NoValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envDoubleNoValue".
            // No value should be specified in the deployment descriptor before the test.
            Double tempDouble = fejb1.getDoubleEnvVar("envDoubleNoValue");
            fail("Get environment blank double lookup should have failed, instead got " + tempDouble);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive18) Test an env-entry of type String with no env-entry-value.
     */
    @Test
    public void testSLRemoteEnvEntry_String_NoValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envStringNoValue".
            // No value should be specified in the deployment descriptor before the test.
            String tempStr = fejb1.getStringEnvVar("envStringNoValue");
            fail("Get environment blank string lookup should have failed, instead got " + tempStr);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive19) Test an env-entry of type Boolean with an invalid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Boolean_InvalidValue() throws Exception {
        // The test case looks for an environment variable named "envBooleanInvalid".
        Boolean tempBoolean = fejb1.getBooleanEnvVar("envBooleanInvalid");
        assertNotNull("Get environment boolean object was null.", tempBoolean);
        assertFalse("Ttest content of environment should have been false", tempBoolean.booleanValue());
    }

    /**
     * (ive20) Test an env-entry of type Byte with an invalid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Byte_InvalidValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envByteBlankValue".
            Byte tempByte = fejb1.getByteEnvVar("envByteBlankValue");
            fail("Get environment invalid byte lookup should have failed, instead got " + tempByte);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envByteInvalid".
            Byte tempByte = fejb1.getByteEnvVar("envByteInvalid");
            fail("Get environment invalid byte lookup should have failed, instead got " + tempByte);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envByteGT8bit".
            Byte tempByte = fejb1.getByteEnvVar("envByteGT8bit");
            fail("Get environment invalid byte lookup should have failed, instead got " + tempByte);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive21) Test an env-entry of type Character with an invalid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Character_InvalidValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envCharacterBlankValue".
            Character tempCharacter = fejb1.getCharacterEnvVar("envCharacterBlankValue");
            fail("Get environment invalid character lookup should have failed, instead got " + tempCharacter);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        // The test case looks for a environment variable named "envCharacterInvalid".
        Character tempCharacter = fejb1.getCharacterEnvVar("envCharacterInvalid");
        assertEquals("Get environment invalid character object was unexpected value.", tempCharacter.charValue(), 'I');
    }

    /**
     * (ive22) Test an env-entry of type Short with an invalid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Short_InvalidValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envShortInvalid".
            Short tempShort = fejb1.getShortEnvVar("envShortBlankValue");
            fail("Get environment invalid short lookup should have failed, instead got " + tempShort);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envShortInvalid".
            Short tempShort = fejb1.getShortEnvVar("envShortInvalid");
            fail("Get environment invalid short lookup should have failed, instead got " + tempShort);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envShortInvalid".
            Short tempShort = fejb1.getShortEnvVar("envShortGT16bit");
            fail("Get environment invalid short lookup should have failed, instead got " + tempShort);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive23) Test an env-entry of type Integer with an invalid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Integer_InvalidValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envIntegerInvalid".
            Integer tempInt = fejb1.getIntegerEnvVar("envIntegerBlankValue");
            fail("Get environment invalid int lookup should have failed, instead got " + tempInt);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envIntegerInvalid".
            Integer tempInt = fejb1.getIntegerEnvVar("envIntegerInvalid");
            fail("Get environment invalid int lookup should have failed, instead got " + tempInt);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envIntegerInvalid".
            Integer tempInt = fejb1.getIntegerEnvVar("envIntegerGT32bit");
            fail("Get environment invalid int lookup should have failed, instead got " + tempInt);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive24) Test an env-entry of type Long with an invalid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Long_InvalidValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envLongInvalid".
            Long tempLong = fejb1.getLongEnvVar("envLongBlankValue");
            fail("Get environment invalid long lookup should have failed, instead got " + tempLong);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envLongInvalid".
            Long tempLong = fejb1.getLongEnvVar("envLongInvalid");
            fail("Get environment invalid long lookup should have failed, instead got " + tempLong);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envLongInvalid".
            Long tempLong = fejb1.getLongEnvVar("envLongGT64bit");
            fail("Get environment invalid long lookup should have failed, instead got " + tempLong);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive25) Test an env-entry of type Float with an invalid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Float_InvalidValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envFloatInvalid".
            Float tempFloat = fejb1.getFloatEnvVar("envFloatBlankValue");
            fail("Get environment invalid float lookup should have failed, instead got " + tempFloat);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envFloatInvalid".
            Float tempFloat = fejb1.getFloatEnvVar("envFloatInvalid");
            fail("Get environment invalid float lookup should have failed, instead got " + tempFloat);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        // The test case looks for a environment variable named "envFloatInvalid".
        Float tempFloat = fejb1.getFloatEnvVar("envFloatGT32bit");
        assertTrue("Get environment invalid float object was not infinite (Float.isInfinite)", Float.isInfinite(tempFloat.floatValue()));
    }

    /**
     * (ive26) Test an env-entry of type Double with an invalid value.
     */
    @Test
    public void testSLRemoteEnvEntry_Double_InvalidValue() throws Exception {
        try {
            // The test case looks for a environment variable named "envDoubleInvalid".
            Double tempDouble = fejb1.getDoubleEnvVar("envDoubleBlankdValue");
            fail("Get environment invalid double lookup should have failed, instead got " + tempDouble);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        try {
            // The test case looks for a environment variable named "envDoubleInvalid".
            Double tempDouble = fejb1.getDoubleEnvVar("envDoubleInvalid");
            fail("Get environment invalid double object lookup should have failed, instead got " + tempDouble);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }

        // The test case looks for a environment variable named "envDoubleInvalid".
        Double tempDouble = fejb1.getDoubleEnvVar("envDoubleGT64bit");
        assertTrue("Get environment invalid double object was not infinite (Double.isInifinite)", Double.isInfinite(tempDouble.doubleValue()));
    }

    /**
     * (ive27) Test an env-entry of type String with an invalid value.
     */
    //@Test
    public void testSLRemoteEnvEntry_String_InvalidValue() throws Exception {
        svLogger.info("No invalid condition for String.");
    }

    /**
     * (ive28) Test an env-entry of type Boolean where there is no env-entry.
     */
    @Test
    public void testSLRemoteEnvEntry_Boolean_NotExist() throws Exception {
        try {
            // The test case looks for a environment variable named "envBooleanNotExist".
            Boolean tempBoolean = fejb1.getBooleanEnvVar("envBooleanNotExist");
            fail("Get environment not exist should have failed, instead got boolean object =" + tempBoolean);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive29) Test an env-entry of type Byte where there is no env-entry.
     */
    @Test
    public void testSLRemoteEnvEntry_Byte_NotExist() throws Exception {
        try {
            // The test case looks for a environment variable named "envByteNotExist".
            Byte tempByte = fejb1.getByteEnvVar("envByteNotExist");
            fail("Get environment not exist should have failed, instead got byte object = " + tempByte);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive30) Test an env-entry of type Character where there is no env-entry.
     */
    @Test
    public void testSLRemoteEnvEntry_Character_NotExist() throws Exception {
        try {
            // The test case looks for a environment variable named "envCharacterNotExist".
            Character tempCharacter = fejb1.getCharacterEnvVar("envCharacterNotExist");
            fail("Get environment not exist should have failed, instead got character object = " + tempCharacter);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive31) Test an env-entry of type Short where there is no env-entry.
     */
    @Test
    public void testSLRemoteEnvEntry_Short_NotExist() throws Exception {
        try {
            // The test case looks for a environment variable named "envShortNotExist".
            Short tempShort = fejb1.getShortEnvVar("envShortNotExist");
            fail("Get environment not exist should have failed, instead got short object = " + tempShort);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive32) Test an env-entry of type Integer where there is no env-entry.
     */
    @Test
    public void testSLRemoteEnvEntry_Integer_NotExist() throws Exception {
        try {
            // The test case looks for a environment variable named "envIntegerNotExist".
            Integer tempInt = fejb1.getIntegerEnvVar("envIntegerNotExist");
            fail("Get environment not exist should have failed, instead got int object = " + tempInt);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive33) Test an env-entry of type Long where there is no env-entry.
     */
    @Test
    public void testSLRemoteEnvEntry_Long_NotExist() throws Exception {
        try {
            // The test case looks for a environment variable named "envLongNotExist".
            Long tempLong = fejb1.getLongEnvVar("envLongNotExist");
            fail("Get environment not exist should have failed, instead got long object = " + tempLong);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive34) Test an env-entry of type Float where there is no env-entry.
     */
    @Test
    public void testSLRemoteEnvEntry_Float_NotExist() throws Exception {
        try {
            // The test case looks for a environment variable named "envFloatNotExist".
            Float tempFloat = fejb1.getFloatEnvVar("envFloatNotExist");
            fail("Get environment not exist should have failed, instead got float object = " + tempFloat);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive35) Test an env-entry of type Double where there is no env-entry.
     */
    @Test
    public void testSLRemoteEnvEntry_Double_NotExist() throws Exception {
        try {
            // The test case looks for a environment variable named "envDoubleNotExist".
            Double tempDouble = fejb1.getDoubleEnvVar("envDoubleNotExist");
            fail("Get environment not exist should have failed, instead got double object = " + tempDouble);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive36) Test an env-entry of type String where there is no env-entry.
     */
    @Test
    public void testSLRemoteEnvEntry_String_NotExist() throws Exception {
        try {
            // The test case looks for a environment variable named "envStringNotExist".
            String tempStr = fejb1.getStringEnvVar("envStringNotExist");
            fail("Get environment not exist should have failed, instead got string object = " + tempStr);
        } catch (NamingException ne) {
            svLogger.info("Caught expected " + ne.getClass().getName());
        }
    }

    /**
     * (ive37) Test that an env-entry of type Boolean cannot be modified.
     */
    @Test
    public void testSLRemoteEnvEntry_Boolean_Modify() throws Exception {
        try {
            fejb1.bindEnvVar("envBoolean", new Boolean(true));
            fail("Unexpected return from bind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            fejb1.rebindEnvVar("envBoolean", new Boolean(true));
            fail("Unexpected return from rebind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive38) Test that an env-entry of type Byte cannot be modified.
     */
    @Test
    public void testSLRemoteEnvEntry_Byte_Modify() throws Exception {
        try {
            fejb1.bindEnvVar("envByte", new Byte((byte) 10));
            fail("Unexpected return from bind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            fejb1.rebindEnvVar("envBoolean", new Byte((byte) 11));
            fail("Unexpected return from rebind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive39) Test that an env-entry of type Character cannot be modified.
     */
    @Test
    public void testSLRemoteEnvEntry_Character_Modify() throws Exception {
        try {
            fejb1.bindEnvVar("envCharacter", new Character('X'));
            fail("Unexpected return from bind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            fejb1.rebindEnvVar("envCharacter", new Character('Y'));
            fail("Unexpected return from rebind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive40) Test that an env-entry of type Short cannot be modified.
     */
    @Test
    public void testSLRemoteEnvEntry_Short_Modify() throws Exception {
        try {
            fejb1.bindEnvVar("envShort", new Short((short) 111));
            fail("Unexpected return from bind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            fejb1.rebindEnvVar("envShort", new Short((short) 112));
            fail("Unexpected return from rebind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive41) Test that an env-entry of type Integer cannot be modified.
     */
    @Test
    public void testSLRemoteEnvEntry_Integer_Modify() throws Exception {
        try {
            fejb1.bindEnvVar("envInteger", new Integer(111));
            fail("Unexpected return from bind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            fejb1.rebindEnvVar("envInteger", new Integer(112));
            fail("Unexpected return from rebind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive42) Test that an env-entry of type Long cannot be modified.
     */
    @Test
    public void testSLRemoteEnvEntry_Long_Modify() throws Exception {
        try {
            fejb1.bindEnvVar("envLong", new Long(111));
            fail("Unexpected return from bind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            fejb1.rebindEnvVar("envLong", new Long(112));
            fail("Unexpected return from rebind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive43) Test that an env-entry of type Float cannot be modified.
     */
    @Test
    public void testSLRemoteEnvEntry_Float_Modify() throws Exception {
        try {
            fejb1.bindEnvVar("envFloat", new Float(111.0));
            fail("Unexpected return from bind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            fejb1.rebindEnvVar("envFloat", new Float(112.0));
            fail("Unexpected return from rebind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive44) Test that an env-entry of type Double cannot be modified.
     */
    @Test
    public void testSLRemoteEnvEntry_Double_Modify() throws Exception {
        try {
            fejb1.bindEnvVar("envDouble", new Double(111.0));
            fail("Unexpected return from bind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            fejb1.rebindEnvVar("envDouble", new Double(112.0));
            fail("Unexpected return from rebind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }

    /**
     * (ive45) Test that an env-entry of type String cannot be modified.
     */
    @Test
    public void testSLRemoteEnvEntry_String_Modify() throws Exception {
        try {
            fejb1.bindEnvVar("envString", new String("111.0"));
            fail("Unexpected return from bind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }

        try {
            fejb1.rebindEnvVar("envString", new String("112.0"));
            fail("Unexpected return from rebind().");
        } catch (OperationNotSupportedException onse) {
            svLogger.info("Caught expected " + onse.getClass().getName());
        }
    }
}
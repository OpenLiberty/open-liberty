/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.ann.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.injection.ann.ejb.EnvInjectionEJBRemote;
import com.ibm.ws.ejbcontainer.injection.ann.ejb.EnvInjectionEJBRemoteHome;
import com.ibm.ws.ejbcontainer.injection.ann.ejb.EnvInjectionRemote;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> SFRemoteEnvInjectionTest .
 *
 * <dt><b>Test Author:</b> Jeremy Bauer <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the Basic EJB 3.0
 * Injection of Environment values on Stateful Session beans
 * with no XML (and therefore no env values). <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testSFREnvObjFldInjection - Business Interface: Object Field Injection
 * <li>testSFREnvPrimFldInjection - Business Interface: Primitive Field Injection
 * <li>testSFREnvObjMthdInjection - Business Interface: Object Method Injection
 * <li>testSFREnvPrimMthdInjection - Business Interface: Primitive Method Injection
 * <li>testSFRCompEnvObjFldInjection - Component Interface: Object Field Injection
 * <li>testSFRCompEnvPrimFldInjection - Component Interface: Primitive Field Injection
 * <li>testSFRCompEnvObjMthdInjection - Component Interface: Object Method Injection
 * <li>testSFRCompEnvPrimMthdInjection - Component Interface: Primitive Method Injection
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/SFRemoteEnvInjectionServlet")
public class SFRemoteEnvInjectionServlet extends FATServlet {
    private static final String CLASS_NAME = SFRemoteEnvInjectionServlet.class.getName();
    @SuppressWarnings("unused")
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String PASSED = "Passed";

    // Names of application and module... for lookup.
    private static final String Application = "EJB3INJSATestApp";
    private static final String Module = "EJB3INJSABean";

    // Names of the beans used for the test... for lookup.
    private static final String ObjFldBean = "SFEnvInjectObjFld";
    private static final String PrimFldBean = "SFEnvInjectPrimFld";
    private static final String ObjMthdBean = "SFEnvInjectObjMthd";
    private static final String PrimMthdBean = "SFEnvInjectPrimMthd";
    private static final String CompObjFldBean = "CompSFEnvInjectObjFld";
    private static final String CompPrimFldBean = "CompSFEnvInjectPrimFld";
    private static final String CompObjMthdBean = "CompSFEnvInjectObjMthd";
    private static final String CompPrimMthdBean = "CompSFEnvInjectPrimMthd";

    // Names of the interfaces used for the test
    private static final String EnvInjectionRemoteInterface = EnvInjectionRemote.class.getName();
    private static final String EnvInjectionRemoteHomeInterface = EnvInjectionEJBRemoteHome.class.getName();

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB that has no XML, with Business Interfaces only,
     * for fields that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * All of the fields should be null as no injection occurs when a
     * value is not specified in XML. <p>
     *
     * Also test that the values injected into the fields may not be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateful Session bean with injection may be accessed.
     * <li> String field is null.
     * <li> Character field is null.
     * <li> Byte field is null.
     * <li> Short field is null.
     * <li> Integer field is null.
     * <li> Long field is null.
     * <li> Boolean field is null.
     * <li> Double field is null.
     * <li> Float field is null.
     * <li> String value may not be looked up from global namespace - NameNotFound.
     * <li> Character value may not be looked up from global namespace - NameNotFound.
     * <li> Byte value may not be looked up from global namespace - NameNotFound.
     * <li> Short value may not be looked up from global namespace - NameNotFound.
     * <li> Integer value may not be looked up from global namespace - NameNotFound.
     * <li> Long value may not be looked up from global namespace - NameNotFound.
     * <li> Boolean value may not be looked up from global namespace - NameNotFound.
     * <li> Double value may not be looked up from global namespace - NameNotFound.
     * <li> Float value may not be looked up from global namespace - NameNotFound.
     * <li> String value may not be looked up from session context - IllegalArgument.
     * <li> Character value may not be looked up from session context - IllegalArgument.
     * <li> Byte value may not be looked up from session context - IllegalArgument.
     * <li> Short value may not be looked up from session context - IllegalArgument.
     * <li> Integer value may not be looked up from session context - IllegalArgument.
     * <li> Long value may not be looked up from session context - IllegalArgument.
     * <li> Boolean value may not be looked up from session context - IllegalArgument.
     * <li> Double value may not be looked up from session context - IllegalArgument.
     * <li> Float value may not be looked up from session context - IllegalArgument.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the pooled state. <p>
     */
    @Test
    public void testSFREnvObjFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionRemote bean = (EnvInjectionRemote) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         ObjFldBean);
        assertNotNull("1 ---> SFRSB accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 29;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.finish();
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB that has no XML, with Business Interfaces only,
     * for fields that are the primitives (char, int, float, etc.). <p>
     *
     * All of the fields should be defaulted as no injection occurs when a
     * value is not specified in XML. <p>
     *
     * Also test that the values injected into the fields may not be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateful Session bean with injection may be accessed.
     * <li> char value is \u0000.
     * <li> byte value is 0.
     * <li> short value is 0.
     * <li> int value is 0.
     * <li> long value is 0L.
     * <li> boolean value is false.
     * <li> double value is 0.0D.
     * <li> float value is 0.0F.
     * <li> char value may not be looked up from global namespace - NameNotFound.
     * <li> byte value may not be looked up from global namespace - NameNotFound.
     * <li> short value may not be looked up from global namespace - NameNotFound.
     * <li> int value may not be looked up from global namespace - NameNotFound.
     * <li> long value may not be looked up from global namespace - NameNotFound.
     * <li> boolean value may not be looked up from global namespace - NameNotFound.
     * <li> double value may not be looked up from global namespace - NameNotFound.
     * <li> float value may not be looked up from global namespace - NameNotFound.
     * <li> char value may not be looked up from session context - IllegalArgument.
     * <li> byte value may not be looked up from session context - IllegalArgument.
     * <li> short value may not be looked up from session context - IllegalArgument.
     * <li> int value may not be looked up from session context - IllegalArgument.
     * <li> long value may not be looked up from session context - IllegalArgument.
     * <li> boolean value may not be looked up from session context - IllegalArgument.
     * <li> double value may not be looked up from session context - IllegalArgument.
     * <li> float value may not be looked up from session context - IllegalArgument.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the pooled state. <p>
     */
    @Test
    public void testSFREnvPrimFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionRemote bean = (EnvInjectionRemote) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         PrimFldBean);
        assertNotNull("1 ---> SFRSB accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 26;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.finish();
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB with no XML, with Business Interfaces only,
     * for methods that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * All of the values should be null as no injection occurs when a
     * value is not specified in XML. Likewise, none of the injection
     * methods should be called.<p>
     *
     * Also test that the values injected into the methods may not be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateful Session bean with injection may be accessed.
     * <li> Injection methods are not called.
     * <li> String field is null.
     * <li> Character field is null.
     * <li> Byte field is null.
     * <li> Short field is null.
     * <li> Integer field is null.
     * <li> Long field is null.
     * <li> Boolean field is null.
     * <li> Double field is null.
     * <li> Float field is null.
     * <li> String value may not be looked up from global namespace - NameNotFound.
     * <li> Character value may not be looked up from global namespace - NameNotFound.
     * <li> Byte value may not be looked up from global namespace - NameNotFound.
     * <li> Short value may not be looked up from global namespace - NameNotFound.
     * <li> Integer value may not be looked up from global namespace - NameNotFound.
     * <li> Long value may not be looked up from global namespace - NameNotFound.
     * <li> Boolean value may not be looked up from global namespace - NameNotFound.
     * <li> Double value may not be looked up from global namespace - NameNotFound.
     * <li> Float value may not be looked up from global namespace - NameNotFound.
     * <li> String value may not be looked up from session context - IllegalArgument.
     * <li> Character value may not be looked up from session context - IllegalArgument.
     * <li> Byte value may not be looked up from session context - IllegalArgument.
     * <li> Short value may not be looked up from session context - IllegalArgument.
     * <li> Integer value may not be looked up from session context - IllegalArgument.
     * <li> Long value may not be looked up from session context - IllegalArgument.
     * <li> Boolean value may not be looked up from session context - IllegalArgument.
     * <li> Double value may not be looked up from session context - IllegalArgument.
     * <li> Float value may not be looked up from session context - IllegalArgument.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the pooled state. <p>
     */
    @Test
    public void testSFREnvObjMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionRemote bean = (EnvInjectionRemote) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         ObjMthdBean);
        assertNotNull("1 ---> SFRSB accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 30;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.finish();
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB with no XML, with Business Interfaces only,
     * for methods that are the primitives (char, int, float, etc.). <p>
     *
     * All of the values should be defaulted as no injection occurs when a
     * value is not specified in XML. Likewise, none of the injection
     * methods should be called.<p>
     *
     * Also test that the values injected into the methods may not be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateful Session bean with injection may be accessed.
     * <li> Injection methods are not called.
     * <li> char value is \u0000.
     * <li> byte value is 0.
     * <li> short value is 0.
     * <li> int value is 0.
     * <li> long value is 0L.
     * <li> boolean value is false.
     * <li> double value is 0.0D.
     * <li> float value is 0.0F.
     * <li> char value may not be looked up from global namespace - NameNotFound.
     * <li> byte value may not be looked up from global namespace - NameNotFound.
     * <li> short value may not be looked up from global namespace - NameNotFound.
     * <li> int value may not be looked up from global namespace - NameNotFound.
     * <li> long value may not be looked up from global namespace - NameNotFound.
     * <li> boolean value may not be looked up from global namespace - NameNotFound.
     * <li> double value may not be looked up from global namespace - NameNotFound.
     * <li> float value may not be looked up from global namespace - NameNotFound.
     * <li> char value may not be looked up from session context - IllegalArgument.
     * <li> byte value may not be looked up from session context - IllegalArgument.
     * <li> short value may not be looked up from session context - IllegalArgument.
     * <li> int value may not be looked up from session context - IllegalArgument.
     * <li> long value may not be looked up from session context - IllegalArgument.
     * <li> boolean value may not be looked up from session context - IllegalArgument.
     * <li> double value may not be looked up from session context - IllegalArgument.
     * <li> float value may not be looked up from session context - IllegalArgument.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the pooled state. <p>
     */
    @Test
    public void testSFREnvPrimMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionRemote bean = (EnvInjectionRemote) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         PrimMthdBean);
        assertNotNull("1 ---> SFRSB accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 27;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.finish();
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB with no XML, with Component Interfaces,
     * for fields that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * All of the fields should be null as no injection occurs when a
     * value is not specified in XML. <p>
     *
     * Also test that the values injected into the fields may not be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateful Session bean with injection may be accessed.
     * <li> String field is null.
     * <li> Character field is null.
     * <li> Byte field is null.
     * <li> Short field is null.
     * <li> Integer field is null.
     * <li> Long field is null.
     * <li> Boolean field is null.
     * <li> Double field is null.
     * <li> Float field is null.
     * <li> String value may not be looked up from global namespace - NameNotFound.
     * <li> Character value may not be looked up from global namespace - NameNotFound.
     * <li> Byte value may not be looked up from global namespace - NameNotFound.
     * <li> Short value may not be looked up from global namespace - NameNotFound.
     * <li> Integer value may not be looked up from global namespace - NameNotFound.
     * <li> Long value may not be looked up from global namespace - NameNotFound.
     * <li> Boolean value may not be looked up from global namespace - NameNotFound.
     * <li> Double value may not be looked up from global namespace - NameNotFound.
     * <li> Float value may not be looked up from global namespace - NameNotFound.
     * <li> String value may not be looked up from session context - IllegalArgument.
     * <li> Character value may not be looked up from session context - IllegalArgument.
     * <li> Byte value may not be looked up from session context - IllegalArgument.
     * <li> Short value may not be looked up from session context - IllegalArgument.
     * <li> Integer value may not be looked up from session context - IllegalArgument.
     * <li> Long value may not be looked up from session context - IllegalArgument.
     * <li> Boolean value may not be looked up from session context - IllegalArgument.
     * <li> Double value may not be looked up from session context - IllegalArgument.
     * <li> Float value may not be looked up from session context - IllegalArgument.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the pooled state. <p>
     */
    @Test
    public void testSFRCompEnvObjFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBRemoteHome slHome = (EnvInjectionEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteHomeInterface,
                                                                                                                         Application,
                                                                                                                         Module,
                                                                                                                         CompObjFldBean);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SFRSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 29;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB with no XML, with Component Interfaces,
     * for fields that are the primitives (char, int, float, etc.). <p>
     *
     * All of the fields should be defaulted as no injection occurs when a
     * value is not specified in XML. <p>
     *
     * Also test that the values injected into the fields may not be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateful Session bean with injection may be accessed.
     * <li> char value is \u0000.
     * <li> byte value is 0.
     * <li> short value is 0.
     * <li> int value is 0.
     * <li> long value is 0L.
     * <li> boolean value is false.
     * <li> double value is 0.0D.
     * <li> float value is 0.0F.
     * <li> char value may not be looked up from global namespace - NameNotFound.
     * <li> byte value may not be looked up from global namespace - NameNotFound.
     * <li> short value may not be looked up from global namespace - NameNotFound.
     * <li> int value may not be looked up from global namespace - NameNotFound.
     * <li> long value may not be looked up from global namespace - NameNotFound.
     * <li> boolean value may not be looked up from global namespace - NameNotFound.
     * <li> double value may not be looked up from global namespace - NameNotFound.
     * <li> float value may not be looked up from global namespace - NameNotFound.
     * <li> char value may not be looked up from session context - IllegalArgument.
     * <li> byte value may not be looked up from session context - IllegalArgument.
     * <li> short value may not be looked up from session context - IllegalArgument.
     * <li> int value may not be looked up from session context - IllegalArgument.
     * <li> long value may not be looked up from session context - IllegalArgument.
     * <li> boolean value may not be looked up from session context - IllegalArgument.
     * <li> double value may not be looked up from session context - IllegalArgument.
     * <li> float value may not be looked up from session context - IllegalArgument.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the pooled state. <p>
     */
    @Test
    public void testSFRCompEnvPrimFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBRemoteHome slHome = (EnvInjectionEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteHomeInterface,
                                                                                                                         Application,
                                                                                                                         Module,
                                                                                                                         CompPrimFldBean);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SFRSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 26;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB with no XML, with Component Interfaces,
     * for methods that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * All of the values should be null as no injection occurs when a
     * value is not specified in XML. Likewise, none of the injection
     * methods should be called.<p>
     *
     * Also test that the values injected into the methods may not be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateful Session bean with injection may be accessed.
     * <li> Injection methods are not called.
     * <li> String field is null.
     * <li> Character field is null.
     * <li> Byte field is null.
     * <li> Short field is null.
     * <li> Integer field is null.
     * <li> Long field is null.
     * <li> Boolean field is null.
     * <li> Double field is null.
     * <li> Float field is null.
     * <li> String value may not be looked up from global namespace - NameNotFound.
     * <li> Character value may not be looked up from global namespace - NameNotFound.
     * <li> Byte value may not be looked up from global namespace - NameNotFound.
     * <li> Short value may not be looked up from global namespace - NameNotFound.
     * <li> Integer value may not be looked up from global namespace - NameNotFound.
     * <li> Long value may not be looked up from global namespace - NameNotFound.
     * <li> Boolean value may not be looked up from global namespace - NameNotFound.
     * <li> Double value may not be looked up from global namespace - NameNotFound.
     * <li> Float value may not be looked up from global namespace - NameNotFound.
     * <li> String value may not be looked up from session context - IllegalArgument.
     * <li> Character value may not be looked up from session context - IllegalArgument.
     * <li> Byte value may not be looked up from session context - IllegalArgument.
     * <li> Short value may not be looked up from session context - IllegalArgument.
     * <li> Integer value may not be looked up from session context - IllegalArgument.
     * <li> Long value may not be looked up from session context - IllegalArgument.
     * <li> Boolean value may not be looked up from session context - IllegalArgument.
     * <li> Double value may not be looked up from session context - IllegalArgument.
     * <li> Float value may not be looked up from session context - IllegalArgument.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the pooled state. <p>
     */
    @Test
    public void testSFRCompEnvObjMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBRemoteHome slHome = (EnvInjectionEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteHomeInterface,
                                                                                                                         Application,
                                                                                                                         Module,
                                                                                                                         CompObjMthdBean);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SFRSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 30;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB with no XML, with Component Interfaces,
     * for methods that are the primitives (char, int, float, etc.). <p>
     *
     * All of the values should be defaulted as no injection occurs when a
     * value is not specified in XML. Likewise, none of the injection
     * methods should be called.<p>
     *
     * Also test that the values injected into the methods may not be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateful Session bean with injection may be accessed.
     * <li> Injection methods are not called.
     * <li> char value is \u0000.
     * <li> byte value is 0.
     * <li> short value is 0.
     * <li> int value is 0.
     * <li> long value is 0L.
     * <li> boolean value is false.
     * <li> double value is 0.0D.
     * <li> float value is 0.0F.
     * <li> char value may not be looked up from global namespace - NameNotFound.
     * <li> byte value may not be looked up from global namespace - NameNotFound.
     * <li> short value may not be looked up from global namespace - NameNotFound.
     * <li> int value may not be looked up from global namespace - NameNotFound.
     * <li> long value may not be looked up from global namespace - NameNotFound.
     * <li> boolean value may not be looked up from global namespace - NameNotFound.
     * <li> double value may not be looked up from global namespace - NameNotFound.
     * <li> float value may not be looked up from global namespace - NameNotFound.
     * <li> char value may not be looked up from session context - IllegalArgument.
     * <li> byte value may not be looked up from session context - IllegalArgument.
     * <li> short value may not be looked up from session context - IllegalArgument.
     * <li> int value may not be looked up from session context - IllegalArgument.
     * <li> long value may not be looked up from session context - IllegalArgument.
     * <li> boolean value may not be looked up from session context - IllegalArgument.
     * <li> double value may not be looked up from session context - IllegalArgument.
     * <li> float value may not be looked up from session context - IllegalArgument.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the pooled state. <p>
     */
    @Test
    public void testSFRCompEnvPrimMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBRemoteHome slHome = (EnvInjectionEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteHomeInterface,
                                                                                                                         Application,
                                                                                                                         Module,
                                                                                                                         CompPrimMthdBean);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SFRSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 27;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

}

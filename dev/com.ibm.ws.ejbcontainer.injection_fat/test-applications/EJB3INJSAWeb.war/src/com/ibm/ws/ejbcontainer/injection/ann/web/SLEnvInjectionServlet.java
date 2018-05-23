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

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.injection.ann.ejb.EnvInjectionEJBLocal;
import com.ibm.ws.ejbcontainer.injection.ann.ejb.EnvInjectionEJBLocalHome;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> SLEnvInjectionTest .
 *
 * <dt><b>Test Author:</b> Brian Decker <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the Basic EJB 3.0
 * Injection of Environment values on Stateless Session beans
 * with no XML (and therefore no env values). <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testSLLCompEnvObjFldInjection - Component Interface: Object Field Injection
 * <li>testSLLCompEnvPrimFldInjection - Component Interface: Primitive Field Injection
 * <li>testSLLCompEnvObjMthdInjection - Component Interface: Object Method Injection
 * <li>testSLLCompEnvPrimMthdInjection - Component Interface: Primitive Method Injection
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/SLEnvInjectionServlet")
public class SLEnvInjectionServlet extends FATServlet {

    private static final String PASSED = "Passed";

    // Names of application and module... for lookup.
    private static final String Application = "EJB3INJSATestApp";
    private static final String Module = "EJB3INJSABean";

    // Names of the beans used for the test... for lookup.
    private static final String CompObjFldBean = "CompSLEnvInjectObjFld";
    private static final String CompPrimFldBean = "CompSLEnvInjectPrimFld";
    private static final String CompObjMthdBean = "CompSLEnvInjectObjMthd";
    private static final String CompPrimMthdBean = "CompSLEnvInjectPrimMthd";

    // Names of the interfaces used for the test
    private static final String EnvInjectionLocalHomeInterface = EnvInjectionEJBLocalHome.class.getName();

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateless Session EJB with no XML, with Component Interfaces,
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
     * <li> Stateless Session bean with injection may be accessed.
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
    public void testSLLCompEnvObjFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBLocalHome slHome = (EnvInjectionEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(EnvInjectionLocalHomeInterface,
                                                                                                                 Application,
                                                                                                                 Module,
                                                                                                                 CompObjFldBean);
        EnvInjectionEJBLocal bean = slHome.create();
        assertNotNull("1 ---> SLLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 29;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateless Session EJB with no XML, with Component Interfaces,
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
     * <li> Stateless Session bean with injection may be accessed.
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
    public void testSLLCompEnvPrimFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBLocalHome slHome = (EnvInjectionEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(EnvInjectionLocalHomeInterface,
                                                                                                                 Application,
                                                                                                                 Module,
                                                                                                                 CompPrimFldBean);
        EnvInjectionEJBLocal bean = slHome.create();
        assertNotNull("1 ---> SLLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 26;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateless Session EJB with no XML, with Component Interfaces,
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
     * <li> Stateless Session bean with injection may be accessed.
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
    public void testSLLCompEnvObjMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBLocalHome slHome = (EnvInjectionEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(EnvInjectionLocalHomeInterface,
                                                                                                                 Application,
                                                                                                                 Module,
                                                                                                                 CompObjMthdBean);
        EnvInjectionEJBLocal bean = slHome.create();
        assertNotNull("1 ---> SLLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 30;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateless Session EJB with no XML, with Component Interfaces,
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
     * <li> Stateless Session bean with injection may be accessed.
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
    public void testSLLCompEnvPrimMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBLocalHome slHome = (EnvInjectionEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(EnvInjectionLocalHomeInterface,
                                                                                                                 Application,
                                                                                                                 Module,
                                                                                                                 CompPrimMthdBean);
        EnvInjectionEJBLocal bean = slHome.create();
        assertNotNull("1 ---> SLLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 27;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

}

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

package com.ibm.ws.ejbcontainer.injection.xml.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.injection.xml.ejb.EnvInjectionEJBLocal;
import com.ibm.ws.ejbcontainer.injection.xml.ejb.EnvInjectionEJBLocalHome;
import com.ibm.ws.ejbcontainer.injection.xml.ejb.EnvInjectionLocal;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> BasicSFEnvInjectionTest .
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the Basic EJB 3.0
 * Injection of Environment values on Stateful Session beans. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testSFLEnvObjFldInjection - Business Interface: Object Field Injection
 * <li>testSFLEnvPrimFldInjection - Business Interface: Primitive Field Injection
 * <li>testSFLEnvObjMthdInjection - Business Interface: Object Method Injection
 * <li>testSFLEnvPrimMthdInjection - Business Interface: Primitive Method Injection
 * <li>testSFLCompEnvObjFldInjection - Component Interface: Object Field Injection
 * <li>testSFLCompEnvPrimFldInjection - Component Interface: Primitive Field Injection
 * <li>testSFLCompEnvObjMthdInjection - Component Interface: Object Method Injection
 * <li>testSFLCompEnvPrimMthdInjection - Component Interface: Primitive Method Injection
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/BasicSFEnvInjectionServlet")
public class BasicSFEnvInjectionServlet extends FATServlet {

    private static final String PASSED = "Passed";

    // SimpleBinding name was used for the bean used by this test
    private static final String SimpleBindingPreFix = "com/ibm/ws/ejbcontainer/injection/xml/ejb/";

    // Names of the beans used for the test... for lookup.
    private static final String BasicObjFldBean = "BasicSFEnvInjectObjFld";
    private static final String BasicPrimFldBean = "BasicSFEnvInjectPrimFld";
    private static final String BasicObjMthdBean = "BasicSFEnvInjectObjMthd";
    private static final String BasicPrimMthdBean = "BasicSFEnvInjectPrimMthd";
    private static final String CompObjFldBean = "CompSFEnvInjectObjFld";
    private static final String CompPrimFldBean = "CompSFEnvInjectPrimFld";
    private static final String CompObjMthdBean = "CompSFEnvInjectObjMthd";
    private static final String CompPrimMthdBean = "CompSFEnvInjectPrimMthd";

    // Names of the interfaces used for the test
    private static final String EnvInjectionEJBLocalHomeInterface = EnvInjectionEJBLocalHome.class.getName();

    /** Jndi Names of the Bean Homes to use for the test. **/
    private String ivJNDI_SFLEnvInjectObjFld = SimpleBindingPreFix + CompObjFldBean +
                                               "#" + EnvInjectionEJBLocalHomeInterface;
    private String ivJNDI_SFLEnvInjectPrimFld = SimpleBindingPreFix + CompPrimFldBean +
                                                "#" + EnvInjectionEJBLocalHomeInterface;
    private String ivJNDI_SFLEnvInjectObjMthd = SimpleBindingPreFix + CompObjMthdBean +
                                                "#" + EnvInjectionEJBLocalHomeInterface;
    private String ivJNDI_SFLEnvInjectPrimMthd = SimpleBindingPreFix + CompPrimMthdBean +
                                                 "#" + EnvInjectionEJBLocalHomeInterface;

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB, with Business Interfaces only, for fields
     * that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * Also test that the values injected into the fields may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be accessed.
     * <li> String field injected properly from environment entry.
     * <li> Character field injected properly from environment entry.
     * <li> Byte field injected properly from environment entry.
     * <li> Short field injected properly from environment entry.
     * <li> Integer field injected properly from environment entry.
     * <li> Long field injected properly from environment entry.
     * <li> Boolean field injected properly from environment entry.
     * <li> Double field injected properly from environment entry.
     * <li> Float field injected properly from environment entry.
     * <li> String value may be looked up from global namespace.
     * <li> Character value may be looked up from global namespace.
     * <li> Byte value may be looked up from global namespace.
     * <li> Short value may be looked up from global namespace.
     * <li> Integer value may be looked up from global namespace.
     * <li> Long value may be looked up from global namespace.
     * <li> Boolean value may be looked up from global namespace.
     * <li> Double value may be looked up from global namespace.
     * <li> Float value may be looked up from global namespace.
     * <li> String value may be looked up from session context.
     * <li> Character value may be looked up from session context.
     * <li> Byte value may be looked up from session context.
     * <li> Short value may be looked up from session context.
     * <li> Integer value may be looked up from session context.
     * <li> Long value may be looked up from session context.
     * <li> Boolean value may be looked up from session context.
     * <li> Double value may be looked up from session context.
     * <li> Float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the cached state. <p>
     */
    @Test
    public void testSFLEnvObjFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionLocal bean = (EnvInjectionLocal) FATHelper.lookupLocalBinding(SimpleBindingPreFix + BasicObjFldBean);
        assertNotNull("1 ---> SFLSB was not accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 29;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB, with Business Interfaces only, for fields
     * that are the primitives (char, int, float, etc.). <p>
     *
     * Also test that the values injected into the fields may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be accessed.
     * <li> char field injected properly from environment entry.
     * <li> byte field injected properly from environment entry.
     * <li> short field injected properly from environment entry.
     * <li> int field injected properly from environment entry.
     * <li> long field injected properly from environment entry.
     * <li> boolean field injected properly from environment entry.
     * <li> double field injected properly from environment entry.
     * <li> float field injected properly from environment entry.
     * <li> char value may be looked up from global namespace.
     * <li> byte value may be looked up from global namespace.
     * <li> short value may be looked up from global namespace.
     * <li> int value may be looked up from global namespace.
     * <li> long value may be looked up from global namespace.
     * <li> boolean value may be looked up from global namespace.
     * <li> double value may be looked up from global namespace.
     * <li> float value may be looked up from global namespace.
     * <li> char value may be looked up from session context.
     * <li> byte value may be looked up from session context.
     * <li> short value may be looked up from session context.
     * <li> int value may be looked up from session context.
     * <li> long value may be looked up from session context.
     * <li> boolean value may be looked up from session context.
     * <li> double value may be looked up from session context.
     * <li> float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does not occur from the cached state. <p>
     */
    @Test
    public void testSFLEnvPrimFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionLocal bean = (EnvInjectionLocal) FATHelper.lookupLocalBinding(SimpleBindingPreFix + BasicPrimFldBean);
        assertNotNull("1 ---> SFLSB was not accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 26;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB, with Business Interfaces only, for methods
     * that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * Also test that the values injected into the methods may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be accessed.
     * <li> Injection methods are called (not injected into fields directly).
     * <li> String method injected properly from environment entry.
     * <li> Character method injected properly from environment entry.
     * <li> Byte method injected properly from environment entry.
     * <li> Short method injected properly from environment entry.
     * <li> Integer method injected properly from environment entry.
     * <li> Long method injected properly from environment entry.
     * <li> Boolean method injected properly from environment entry.
     * <li> Double method injected properly from environment entry.
     * <li> Float method injected properly from environment entry.
     * <li> String value may be looked up from global namespace.
     * <li> Character value may be looked up from global namespace.
     * <li> Byte value may be looked up from global namespace.
     * <li> Short value may be looked up from global namespace.
     * <li> Integer value may be looked up from global namespace.
     * <li> Long value may be looked up from global namespace.
     * <li> Boolean value may be looked up from global namespace.
     * <li> Double value may be looked up from global namespace.
     * <li> Float value may be looked up from global namespace.
     * <li> String value may be looked up from session context.
     * <li> Character value may be looked up from session context.
     * <li> Byte value may be looked up from session context.
     * <li> Short value may be looked up from session context.
     * <li> Integer value may be looked up from session context.
     * <li> Long value may be looked up from session context.
     * <li> Boolean value may be looked up from session context.
     * <li> Double value may be looked up from session context.
     * <li> Float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does no occur from the cached state. <p>
     */
    @Test
    public void testSFLEnvObjMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionLocal bean = (EnvInjectionLocal) FATHelper.lookupLocalBinding(SimpleBindingPreFix + BasicObjMthdBean);
        assertNotNull("1 ---> SFLSB was not accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 30;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB, with Business Interfaces only, for methods
     * that are the primitives (char, int, float, etc.). <p>
     *
     * Also test that the values injected into the methods may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be accessed.
     * <li> Injection methods are called (not injected into fields directly).
     * <li> char method injected properly from environment entry.
     * <li> byte method injected properly from environment entry.
     * <li> short method injected properly from environment entry.
     * <li> int method injected properly from environment entry.
     * <li> long method injected properly from environment entry.
     * <li> boolean method injected properly from environment entry.
     * <li> double method injected properly from environment entry.
     * <li> float method injected properly from environment entry.
     * <li> char value may be looked up from global namespace.
     * <li> byte value may be looked up from global namespace.
     * <li> short value may be looked up from global namespace.
     * <li> int value may be looked up from global namespace.
     * <li> long value may be looked up from global namespace.
     * <li> boolean value may be looked up from global namespace.
     * <li> double value may be looked up from global namespace.
     * <li> float value may be looked up from global namespace.
     * <li> char value may be looked up from session context.
     * <li> byte value may be looked up from session context.
     * <li> short value may be looked up from session context.
     * <li> int value may be looked up from session context.
     * <li> long value may be looked up from session context.
     * <li> boolean value may be looked up from session context.
     * <li> double value may be looked up from session context.
     * <li> float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does no occur from the cached state. <p>
     */
    @Test
    public void testSFLEnvPrimMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionLocal bean = (EnvInjectionLocal) FATHelper.lookupLocalBinding(SimpleBindingPreFix + BasicPrimMthdBean);
        assertNotNull("1 ---> SFLSB was not accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 27;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB, with Component Interfaces, for fields
     * that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * Also test that the values injected into the fields may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be created.
     * <li> String field injected properly from environment entry.
     * <li> Character field injected properly from environment entry.
     * <li> Byte field injected properly from environment entry.
     * <li> Short field injected properly from environment entry.
     * <li> Integer field injected properly from environment entry.
     * <li> Long field injected properly from environment entry.
     * <li> Boolean field injected properly from environment entry.
     * <li> Double field injected properly from environment entry.
     * <li> Float field injected properly from environment entry.
     * <li> String value may be looked up from global namespace.
     * <li> Character value may be looked up from global namespace.
     * <li> Byte value may be looked up from global namespace.
     * <li> Short value may be looked up from global namespace.
     * <li> Integer value may be looked up from global namespace.
     * <li> Long value may be looked up from global namespace.
     * <li> Boolean value may be looked up from global namespace.
     * <li> Double value may be looked up from global namespace.
     * <li> Float value may be looked up from global namespace.
     * <li> String value may be looked up from session context.
     * <li> Character value may be looked up from session context.
     * <li> Byte value may be looked up from session context.
     * <li> Short value may be looked up from session context.
     * <li> Integer value may be looked up from session context.
     * <li> Long value may be looked up from session context.
     * <li> Boolean value may be looked up from session context.
     * <li> Double value may be looked up from session context.
     * <li> Float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does not occur from the cached state. <p>
     */
    @Test
    public void testSFLCompEnvObjFldInjection_BasicSFEnvInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 updated lookup for the new binding format
        EnvInjectionEJBLocalHome sfHome = (EnvInjectionEJBLocalHome) FATHelper.lookupLocalBinding(ivJNDI_SFLEnvInjectObjFld);
        EnvInjectionEJBLocal bean = sfHome.create();
        assertNotNull("1 ---> SFLSB was not created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 29;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB, with Component Interfaces, for fields
     * that are the primitives (char, int, float, etc.). <p>
     *
     * Also test that the values injected into the fields may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be created.
     * <li> char field injected properly from environment entry.
     * <li> byte field injected properly from environment entry.
     * <li> short field injected properly from environment entry.
     * <li> int field injected properly from environment entry.
     * <li> long field injected properly from environment entry.
     * <li> boolean field injected properly from environment entry.
     * <li> double field injected properly from environment entry.
     * <li> float field injected properly from environment entry.
     * <li> char value may be looked up from global namespace.
     * <li> byte value may be looked up from global namespace.
     * <li> short value may be looked up from global namespace.
     * <li> int value may be looked up from global namespace.
     * <li> long value may be looked up from global namespace.
     * <li> boolean value may be looked up from global namespace.
     * <li> double value may be looked up from global namespace.
     * <li> float value may be looked up from global namespace.
     * <li> char value may be looked up from session context.
     * <li> byte value may be looked up from session context.
     * <li> short value may be looked up from session context.
     * <li> int value may be looked up from session context.
     * <li> long value may be looked up from session context.
     * <li> boolean value may be looked up from session context.
     * <li> double value may be looked up from session context.
     * <li> float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does not occur from the cached state. <p>
     */
    @Test
    public void testSFLCompEnvPrimFldInjection_BasicSFEnvInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 updated lookup for the new binding format
        EnvInjectionEJBLocalHome sfHome = (EnvInjectionEJBLocalHome) FATHelper.lookupLocalBinding(ivJNDI_SFLEnvInjectPrimFld);
        EnvInjectionEJBLocal bean = sfHome.create();
        assertNotNull("1 ---> SFLSB was not created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 26;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB, with Component Interfaces, for methods
     * that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * Also test that the values injected into the methods may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be created.
     * <li> Injection methods are called (not injected into fields directly).
     * <li> String method injected properly from environment entry.
     * <li> Character method injected properly from environment entry.
     * <li> Byte method injected properly from environment entry.
     * <li> Short method injected properly from environment entry.
     * <li> Integer method injected properly from environment entry.
     * <li> Long method injected properly from environment entry.
     * <li> Boolean method injected properly from environment entry.
     * <li> Double method injected properly from environment entry.
     * <li> Float method injected properly from environment entry.
     * <li> String value may be looked up from global namespace.
     * <li> Character value may be looked up from global namespace.
     * <li> Byte value may be looked up from global namespace.
     * <li> Short value may be looked up from global namespace.
     * <li> Integer value may be looked up from global namespace.
     * <li> Long value may be looked up from global namespace.
     * <li> Boolean value may be looked up from global namespace.
     * <li> Double value may be looked up from global namespace.
     * <li> Float value may be looked up from global namespace.
     * <li> String value may be looked up from session context.
     * <li> Character value may be looked up from session context.
     * <li> Byte value may be looked up from session context.
     * <li> Short value may be looked up from session context.
     * <li> Integer value may be looked up from session context.
     * <li> Long value may be looked up from session context.
     * <li> Boolean value may be looked up from session context.
     * <li> Double value may be looked up from session context.
     * <li> Float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does not occur from the cached state. <p>
     */
    @Test
    public void testSFLCompEnvObjMthdInjection_BasicSFEnvInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionEJBLocalHome sfHome = (EnvInjectionEJBLocalHome) FATHelper.lookupLocalBinding(ivJNDI_SFLEnvInjectObjMthd);
        EnvInjectionEJBLocal bean = sfHome.create();
        assertNotNull("1 ---> SFLSB was not created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 30;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB, with Component Interfaces, for methods
     * that are the primitives (char, int, float, etc.). <p>
     *
     * Also test that the values injected into the methods may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be created.
     * <li> Injection methods are called (not injected into fields directly).
     * <li> char method injected properly from environment entry.
     * <li> byte method injected properly from environment entry.
     * <li> short method injected properly from environment entry.
     * <li> int method injected properly from environment entry.
     * <li> long method injected properly from environment entry.
     * <li> boolean method injected properly from environment entry.
     * <li> double method injected properly from environment entry.
     * <li> float method injected properly from environment entry.
     * <li> char value may be looked up from global namespace.
     * <li> byte value may be looked up from global namespace.
     * <li> short value may be looked up from global namespace.
     * <li> int value may be looked up from global namespace.
     * <li> long value may be looked up from global namespace.
     * <li> boolean value may be looked up from global namespace.
     * <li> double value may be looked up from global namespace.
     * <li> float value may be looked up from global namespace.
     * <li> char value may be looked up from session context.
     * <li> byte value may be looked up from session context.
     * <li> short value may be looked up from session context.
     * <li> int value may be looked up from session context.
     * <li> long value may be looked up from session context.
     * <li> boolean value may be looked up from session context.
     * <li> double value may be looked up from session context.
     * <li> float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does not occur from the cached state. <p>
     */
    @Test
    public void testSFLCompEnvPrimMthdInjection_BasicSFEnvInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 updated lookup for the new binding format
        EnvInjectionEJBLocalHome sfHome = (EnvInjectionEJBLocalHome) FATHelper.lookupLocalBinding(ivJNDI_SFLEnvInjectPrimMthd);
        EnvInjectionEJBLocal bean = sfHome.create();
        assertNotNull("1 ---> SFLSB was not created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 27;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

}

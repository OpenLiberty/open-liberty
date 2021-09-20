/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.mix.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.injection.mix.ejb.EnvInjectionEJBRemote;
import com.ibm.ws.ejbcontainer.injection.mix.ejb.EnvInjectionEJBRemoteHome;
import com.ibm.ws.ejbcontainer.injection.mix.ejb.EnvInjectionRemote;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> BasicSLRemoteEnvInjectionTest .
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the Basic EJB 3.0
 * Injection of Environment values on Stateless Session beans. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testSLREnvObjFldInjection - Business Interface: Object Field Injection
 * <li>testSLREnvPrimFldInjection - Business Interface: Primitive Field Injection
 * <li>testSLREnvObjMthdInjection - Business Interface: Object Method Injection
 * <li>testSLREnvPrimMthdInjection - Business Interface: Primitive Method Injection
 * <li>testSLRCompEnvObjFldInjection - Component Interface: Object Field Injection
 * <li>testSLRCompEnvPrimFldInjection - Component Interface: Primitive Field Injection
 * <li>testSLRCompEnvObjMthdInjection - Component Interface: Object Method Injection
 * <li>testSLRCompEnvPrimMthdInjection - Component Interface: Primitive Method Injection
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/BasicSLRemoteEnvInjectionServlet")
public class BasicSLRemoteEnvInjectionServlet extends FATServlet {

    private static final String PASSED = "Passed";

    // SimpleBinding name was used for the bean used by this test
    private static final String SimpleBindingPreFix = "com/ibm/ws/ejbcontainer/injection/mix/ejb/";

    // Names of the beans used for the test... for lookup.
    private static final String BasicObjFldBean = "BasicSLEnvInjectObjFld";
    private static final String BasicPrimFldBean = "BasicSLEnvInjectPrimFld";
    private static final String BasicObjMthdBean = "BasicSLEnvInjectObjMthd";
    private static final String BasicPrimMthdBean = "BasicSLEnvInjectPrimMthd";
    private static final String CompObjFldBean = "CompSLEnvInjectObjFld";
    private static final String CompPrimFldBean = "CompSLEnvInjectPrimFld";
    private static final String CompObjMthdBean = "CompSLEnvInjectObjMthd";
    private static final String CompPrimMthdBean = "CompSLEnvInjectPrimMthd";

    // Names of the interfaces used for the test
    private static final String EnvInjectionEJBRemoteHomeInterface = EnvInjectionEJBRemoteHome.class.getName();

    /** Jndi Names of the Bean Homes to use for the test. **/
    private String ivJNDI_SLREnvInjectObjFld = SimpleBindingPreFix + CompObjFldBean +
                                               "#" + EnvInjectionEJBRemoteHomeInterface;
    private String ivJNDI_SLREnvInjectPrimFld = SimpleBindingPreFix + CompPrimFldBean +
                                                "#" + EnvInjectionEJBRemoteHomeInterface;
    private String ivJNDI_SLREnvInjectObjMthd = SimpleBindingPreFix + CompObjMthdBean +
                                                "#" + EnvInjectionEJBRemoteHomeInterface;
    private String ivJNDI_SLREnvInjectPrimMthd = SimpleBindingPreFix + CompPrimMthdBean +
                                                 "#" + EnvInjectionEJBRemoteHomeInterface;

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateless Session EJB, with Business Interfaces only, for fields
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
     * does NOT occur from the pooled state. <p>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLREnvObjFldInjection_BasicSLRemoteEnvInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + BasicObjFldBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SLLSB accessed successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (EJBException ex) {
            assertEquals("Expected EJBException test is wrong",
                         "discardInstance", ex.getMessage());
        }

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
     * Stateless Session EJB, with Business Interfaces only, for fields
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
     * does NOT occur from the pooled state. <p>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLREnvPrimFldInjection_BasicSLRemoteEnvInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + BasicPrimFldBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SLLSB accessed successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (EJBException ex) {
            assertEquals("Expected EJBException test is wrong",
                         "discardInstance", ex.getMessage());
        }

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
     * Stateless Session EJB, with Business Interfaces only, for methods
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
     * does NOT occur from the pooled state. <p>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLREnvObjMthdInjection_BasicSLRemoteEnvInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + BasicObjMthdBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SLLSB accessed successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (EJBException ex) {
            assertEquals("Expected EJBException test is wrong",
                         "discardInstance", ex.getMessage());
        }

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
     * Stateless Session EJB, with Business Interfaces only, for methods
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
     * does NOT occur from the pooled state. <p>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLREnvPrimMthdInjection_BasicSLRemoteEnvInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + BasicPrimMthdBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SLLSB accessed successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (EJBException ex) {
            assertEquals("Expected EJBException test is wrong",
                         "discardInstance", ex.getMessage());
        }

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 27;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateless Session EJB, with Component Interfaces, for fields
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
     * does NOT occur from the pooled state. <p>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLRCompEnvObjFldInjection_BasicSLRemoteEnvInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 updated lookup for the new binding format
        EnvInjectionEJBRemoteHome slHome = FATHelper.lookupRemoteHomeBinding(ivJNDI_SLREnvInjectObjFld,
                                                                             EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLLSB created successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (RemoteException rex) {
            Throwable cause = rex.getCause(); // a nested RemoteException
            cause = cause.getCause(); // a nested EJBException
            assertEquals("Expected RemoteException test is wrong",
                         "discardInstance", cause.getMessage());
        }

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
     * Stateless Session EJB, with Component Interfaces, for fields
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
     * does NOT occur from the pooled state. <p>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLRCompEnvPrimFldInjection_BasicSLRemoteEnvInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 updated lookup for the new binding format
        EnvInjectionEJBRemoteHome slHome = FATHelper.lookupRemoteHomeBinding(ivJNDI_SLREnvInjectPrimFld,
                                                                             EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLLSB created successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (RemoteException rex) {
            Throwable cause = rex.getCause(); // a nested RemoteException
            cause = cause.getCause(); // a nested EJBException
            assertEquals("Expected RemoteException test is wrong",
                         "discardInstance", cause.getMessage());
        }

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
     * Stateless Session EJB, with Component Interfaces, for methods
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
     * does NOT occur from the pooled state. <p>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLRCompEnvObjMthdInjection_BasicSLRemoteEnvInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionEJBRemoteHome slHome = FATHelper.lookupRemoteHomeBinding(ivJNDI_SLREnvInjectObjMthd,
                                                                             EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLLSB created successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (RemoteException rex) {
            Throwable cause = rex.getCause(); // a nested RemoteException
            cause = cause.getCause(); // a nested EJBException
            assertEquals("Expected RemoteException test is wrong",
                         "discardInstance", cause.getMessage());
        }

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
     * Stateless Session EJB, with Component Interfaces, for methods
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
     * does NOT occur from the pooled state. <p>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLRCompEnvPrimMthdInjection_BasicSLRemoteEnvInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507
        EnvInjectionEJBRemoteHome slHome = FATHelper.lookupRemoteHomeBinding(ivJNDI_SLREnvInjectPrimMthd,
                                                                             EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLLSB created successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (RemoteException rex) {
            Throwable cause = rex.getCause(); // a nested RemoteException
            cause = cause.getCause(); // a nested EJBException
            assertEquals("Expected RemoteException test is wrong",
                         "discardInstance", cause.getMessage());
        }

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 27;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

}

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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.injection.ann.ejb.EnvInjectionEJBRemote;
import com.ibm.ws.ejbcontainer.injection.ann.ejb.EnvInjectionEJBRemoteHome;
import com.ibm.ws.ejbcontainer.injection.ann.ejb.EnvInjectionRemote;
import com.ibm.ws.ejbcontainer.injection.ann.ejb.SLInjectRemote;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> SLRmtServEnvInjectionTest .
 *
 * <dt><b>Test Author:</b> Brian Decker / Jeremy Bauer / Urrvano Gamez, Jr. <p>
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
 * <li>testSLREnvObjFldInjection - Business Interface: Object Field Injection
 * <li>testSLREnvPrimFldInjection - Business Interface: Primitive Field Injection
 * <li>testSLREnvObjMthdInjection - Business Interface: Object Method Injection
 * <li>testSLREnvPrimMthdInjection - Business Interface: Primitive Method Injection
 * <li>testSLRCompEnvObjFldInjection - Component Interface: Object Field Injection
 * <li>testSLRCompEnvPrimFldInjection - Component Interface: Primitive Field Injection
 * <li>testSLRCompEnvObjMthdInjection - Component Interface: Object Method Injection
 * <li>testSLRCompEnvPrimMthdInjection - Component Interface: Primitive Method Injection
 * <li>testSLREJBClassLevelInjection - Business Interface: EJB class level ENC injection
 * <li>testSLREJBFieldLevelInjection - Business Interface: EJB field level ENC injection
 * <li>testSLREJBMethodLevelInjection - Business Interface: EJB method level ENC injection
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/SLRmtServEnvInjectionServlet")
public class SLRmtServEnvInjectionServlet extends FATServlet {

    private static final String PASSED = "Passed";
    private static final String DISCARD = "discardInstance";

    // Names of application and module... for lookup.
    private static final String Application = "EJB3INJSATestApp";
    private static final String Module = "EJB3INJSABean";

    // Names of the beans used for the test... for lookup.
    private static final String ObjFldBean = "SLEnvInjectObjFld";
    private static final String PrimFldBean = "SLEnvInjectPrimFld";
    private static final String ObjMthdBean = "SLEnvInjectObjMthd";
    private static final String PrimMthdBean = "SLEnvInjectPrimMthd";
    private static final String CompObjFldBean = "CompSLEnvInjectObjFld";
    private static final String CompPrimFldBean = "CompSLEnvInjectPrimFld";
    private static final String CompObjMthdBean = "CompSLEnvInjectObjMthd";
    private static final String CompPrimMthdBean = "CompSLEnvInjectPrimMthd";
    private static final String EJBClsBean = "SLEnvInjectEJBClsRmt";
    private static final String EJBFldBean = "SLEnvInjectEJBFldRmt";
    private static final String EJBMthdBean = "SLEnvInjectEJBMthdRmt";

    // Names of the interfaces used for the test
    private static final String EnvInjectionRemoteInterface = EnvInjectionRemote.class.getName();
    private static final String EnvInjectionRemoteHomeInterface = EnvInjectionEJBRemoteHome.class.getName();
    private static final String SLInjectRemoteInterface = SLInjectRemote.class.getName();

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateless Session EJB that has no XML, with Business Interfaces only,
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
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLREnvObjFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionRemote bean = (EnvInjectionRemote) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         ObjFldBean);
        assertNotNull("1 ---> SLLSB accessed successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (EJBException ejbex) {
            // If call goes through ORB, exception text will include the server stack
            if (ejbex.getMessage().contains(">>")) {
                assertTrue("1a --> discardInstance expected exception has wrong text : ",
                           ejbex.getMessage().contains(DISCARD));
            } else {
                assertEquals("1a --> discardInstance expected exception has wrong text : ",
                             DISCARD, ejbex.getMessage());
            }
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
     * Stateless Session EJB that has no XML, with Business Interfaces only,
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
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLREnvPrimFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionRemote bean = (EnvInjectionRemote) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         PrimFldBean);
        assertNotNull("1 ---> SLLSB accessed successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (EJBException ejbex) {
            // If call goes through ORB, exception text will include the server stack
            if (ejbex.getMessage().contains(">>")) {
                assertTrue("1a --> discardInstance expected exception has wrong text : ",
                           ejbex.getMessage().contains(DISCARD));
            } else {
                assertEquals("1a --> discardInstance expected exception has wrong text : ",
                             DISCARD, ejbex.getMessage());
            }
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
     * Stateless Session EJB with no XML, with Business Interfaces only,
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
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLREnvObjMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionRemote bean = (EnvInjectionRemote) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         ObjMthdBean);
        assertNotNull("1 ---> SLLSB accessed successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (EJBException ejbex) {
            // If call goes through ORB, exception text will include the server stack
            if (ejbex.getMessage().contains(">>")) {
                assertTrue("1a --> discardInstance expected exception has wrong text : ",
                           ejbex.getMessage().contains(DISCARD));
            } else {
                assertEquals("1a --> discardInstance expected exception has wrong text : ",
                             DISCARD, ejbex.getMessage());
            }
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
     * Stateless Session EJB with no XML, with Business Interfaces only,
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
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLREnvPrimMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionRemote bean = (EnvInjectionRemote) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         PrimMthdBean);
        assertNotNull("1 ---> SLLSB accessed successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (EJBException ejbex) {
            // If call goes through ORB, exception text will include the server stack
            if (ejbex.getMessage().contains(">>")) {
                assertTrue("1a --> discardInstance expected exception has wrong text : ",
                           ejbex.getMessage().contains(DISCARD));
            } else {
                assertEquals("1a --> discardInstance expected exception has wrong text : ",
                             DISCARD, ejbex.getMessage());
            }
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
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLRCompEnvObjFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBRemoteHome slHome = (EnvInjectionEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteHomeInterface,
                                                                                                                         Application,
                                                                                                                         Module,
                                                                                                                         CompObjFldBean);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLLSB created successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (RemoteException rex) {
            assertTrue("1a --> discardInstance expected exception has wrong text : ",
                       rex.getMessage().contains(DISCARD));
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
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLRCompEnvPrimFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBRemoteHome slHome = (EnvInjectionEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteHomeInterface,
                                                                                                                         Application,
                                                                                                                         Module,
                                                                                                                         CompPrimFldBean);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLLSB created successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (RemoteException rex) {
            assertTrue("1a --> discardInstance expected exception has wrong text : ",
                       rex.getMessage().contains(DISCARD));
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
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLRCompEnvObjMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBRemoteHome slHome = (EnvInjectionEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteHomeInterface,
                                                                                                                         Application,
                                                                                                                         Module,
                                                                                                                         CompObjMthdBean);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLLSB created successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (RemoteException rex) {
            assertTrue("1a --> discardInstance expected exception has wrong text : ",
                       rex.getMessage().contains(DISCARD));
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
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLRCompEnvPrimMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBRemoteHome slHome = (EnvInjectionEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(EnvInjectionRemoteHomeInterface,
                                                                                                                         Application,
                                                                                                                         Module,
                                                                                                                         CompPrimMthdBean);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLLSB created successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (RemoteException rex) {
            assertTrue("1a --> discardInstance expected exception has wrong text : ",
                       rex.getMessage().contains(DISCARD));
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
     * This test calls EJB A which has EJB B injected into its environment
     * at a class level. EJB A looks up EJB B from its session context and
     * calls a method on the bean.
     */
    @Test
    public void testSLREJBClassLevelInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        SLInjectRemote bean = (SLInjectRemote) FATHelper.lookupDefaultBindingsEJBRemoteInterface(SLInjectRemoteInterface,
                                                                                                 Application,
                                                                                                 Module,
                                                                                                 EJBClsBean);
        assertNotNull("1 ---> SLSB accessed successfully.", bean);

        int testpoint = 2;
        // This calls a bean which has a class level EJB annotation, injecting another
        // bean into its ENC.
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.callInjectedEJB(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.finish();
    }

    /**
     * This test calls a EJB A which has EJB B injected into its environment
     * at a field level. EJB A uses the injected field to call a method
     * on the bean. Also EJB A looks up EJB B from the session context that
     * should have been created via the field injection and calls a method
     * on the bean.
     */
    @Test
    public void testSLREJBFieldLevelInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        SLInjectRemote bean = (SLInjectRemote) FATHelper.lookupDefaultBindingsEJBRemoteInterface(SLInjectRemoteInterface,
                                                                                                 Application,
                                                                                                 Module,
                                                                                                 EJBFldBean);
        assertNotNull("1 ---> SLSB accessed successfully.", bean);

        int testpoint = 2;
        // This calls a bean which has a field level EJB annotation, injecting another
        // bean into its ENC.
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.callInjectedEJB(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.finish();
    }

    /**
     * This test calls EJB A which has EJB B injected into its environment
     * at the method level. EJB A calls a method on the bean using the ref
     * from the setter method. Also EJB A looks up EJB B from the session context that
     * should have been created via the method injection and calls a method
     * on the bean.
     */
    @Test
    public void testSLREJBMethodLevelInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        SLInjectRemote bean = (SLInjectRemote) FATHelper.lookupDefaultBindingsEJBRemoteInterface(SLInjectRemoteInterface,
                                                                                                 Application,
                                                                                                 Module,
                                                                                                 EJBMthdBean);
        assertNotNull("1 ---> SLSB accessed successfully.", bean);

        int testpoint = 2;
        // This calls a bean which has a field level EJB annotation, injecting another
        // bean into its ENC.
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.callInjectedEJB(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.finish();
    }
}

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
import com.ibm.ws.ejbcontainer.injection.fat.tests.repeataction.RepeatWithCDI;
import com.ibm.ws.ejbcontainer.injection.fat.tests.repeataction.RepeatWithEE9CDI;
import com.ibm.ws.ejbcontainer.injection.mix.ejb.EnvInjectionEJBRemote;
import com.ibm.ws.ejbcontainer.injection.mix.ejb.EnvInjectionEJBRemoteHome;
import com.ibm.ws.ejbcontainer.injection.mix.ejb.EnvInjectionRemote;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;

/**
 * <dl>
 * <dt><b>Test Name:</b> AdvSLRemoteEnvInjectionTest .
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the EJB 3.0
 * Injection of Environment values on Stateless Session beans. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testSLRAdvEnvObjFldInjection - Business Interface: Object Field Injection
 * <li>testSLRAdvEnvPrimFldInjection - Business Interface: Primitive Field Injection
 * <li>testSLRAdvEnvObjMthdInjection - Business Interface: Object Method Injection
 * <li>testSLRAdvEnvPrimMthdInjection - Business Interface: Primitive Method Injection
 * <li>testSLRAdvCompEnvObjFldInjection - Component Interface: Object Field Injection
 * <li>testSLRAdvCompEnvPrimFldInjection - Component Interface: Primitive Field Injection
 * <li>testSLRAdvCompEnvObjMthdInjection - Component Interface: Object Method Injection
 * <li>testSLRAdvCompEnvPrimMthdInjection - Component Interface: Primitive Method Injection
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/AdvSLRemoteEnvInjectionServlet")
public class AdvSLRemoteEnvInjectionServlet extends FATServlet {

    private static final String PASSED = "Passed";

    // SimpleBinding name was used for the bean used by this test
    private static final String SimpleBindingPreFix = "com/ibm/ws/ejbcontainer/injection/mix/ejb/";

    // Names of the beans used for the test... for lookup.
    private static final String AdvObjFldBean = "AdvSLEnvInjectObjFld";
    private static final String AdvPrimFldBean = "AdvSLEnvInjectPrimFld";
    private static final String AdvObjMthdBean = "AdvSLEnvInjectObjMthd";
    private static final String AdvPrimMthdBean = "AdvSLEnvInjectPrimMthd";
    private static final String AdvCompObjFldBean = "AdvCompSLEnvInjectObjFld";
    private static final String AdvCompPrimFldBean = "AdvCompSLEnvInjectPrimFld";
    private static final String AdvCompObjMthdBean = "AdvCompSLEnvInjectObjMthd";
    private static final String AdvCompPrimMthdBean = "AdvCompSLEnvInjectPrimMthd";

    // Names of the interfaces used for the test
    private static final String EnvInjectionEJBRemoteHomeInterface = EnvInjectionEJBRemoteHome.class.getName();

    /** Jndi Names of the Bean Homes to use for the test. **/
    private String ivJNDI_SLREnvInjectObjFld = SimpleBindingPreFix + AdvCompObjFldBean +
                                               "#" + EnvInjectionEJBRemoteHomeInterface;
    private String ivJNDI_SLREnvInjectPrimFld = SimpleBindingPreFix + AdvCompPrimFldBean +
                                                "#" + EnvInjectionEJBRemoteHomeInterface;
    private String ivJNDI_SLREnvInjectObjMthd = SimpleBindingPreFix + AdvCompObjMthdBean +
                                                "#" + EnvInjectionEJBRemoteHomeInterface;
    private String ivJNDI_SLREnvInjectPrimMthd = SimpleBindingPreFix + AdvCompPrimMthdBean +
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
     * <li> String with default value and XML value is the XML value.
     * <li> String with default value and no XML value is the default value.
     * <li> String with no default value and no XML value is null.
     * <li> String with no default value and misnamed XML value is null.
     * <li> Renamed String with no default value and XML value is the XML value.
     * <li> Object with no default value and an XML-defined Integer is the XML value.
     * <li> String with default value and XML value may be looked up from global namespace.
     * <li> String with default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> String of misnamed XML value may be looked up from global namespace.
     * <li> Renamed String with no default value and XML value may be looked up from global namespace.
     * <li> String of original name may be looked up from global namespace.
     * <li> Object with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> Class level resource with name and type may be looked up from global namespace.
     * <li> Second class level resource with name only may be looked up from global namespace.
     * <li> String with default value and XML value may be looked up from session context.
     * <li> String with default value and no XML value may not be looked up from session context.
     * <li> String with no default value and no XML value may not be looked up from session context.
     * <li> String with no default value and misnamed XML value may not be looked up from session context.
     * <li> String of misnamed XML value may be looked up from session context.
     * <li> Renamed String with no default value and XML value may be looked up from session context.
     * <li> String of original name may be looked up from session context.
     * <li> Object with no default value and an XML-defined Integer may be looked up from session context.
     * <li> Class level resource with name and type may be looked up from session context.
     * <li> Second class level resource with name only may be looked up from session context.
     * </ol>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLRAdvEnvObjFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + AdvObjFldBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SLRSB accessed successfully.", bean);

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
     * <li> int with default value and XML value is the XML value.
     * <li> int with default value and no XML value is the default value.
     * <li> int with no default value and no XML value is 0.
     * <li> int with no default value and misnamed XML value is 0.
     * <li> Renamed int with no default value and XML value is the XML value.
     * <li> int with no default value and invalid XML value is 0.
     * <li> float with no default value and an XML-defined Integer is the XML value.
     * <li> int with default value and XML value may be looked up from global namespace.
     * <li> int with default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> int of misnamed XML value may be looked up from global namespace.
     * <li> Renamed int with no default value and XML value may be looked up from global namespace.
     * <li> int of original name may be looked up from global namespace.
     * <li> int with no default value and invalid XML value may not be looked up from global namespace.
     * <li> float with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> int with default value and XML value may be looked up from session context.
     * <li> int with default value and no XML value may not be looked up from session context.
     * <li> int with no default value and no XML value may not be looked up from session context.
     * <li> int with no default value and misnamed XML value may not be looked up from session context.
     * <li> int of misnamed XML value may be looked up from session context.
     * <li> Renamed int with no default value and XML value may be looked up from session context.
     * <li> int of original name may be looked up from session context.
     * <li> int with no default value and invalid XML value may not be looked up from session context.
     * <li> float with no default value and an XML-defined Integer may be looked up from session context.
     * </ol>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLRAdvEnvPrimFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + AdvPrimFldBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SLRSB accessed successfully.", bean);

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
     * <li> Injection methods with values are called (not injected into fields directly).
     * <li> String with default value and XML value is the XML value.
     * <li> String with default value and no XML value is the default value.
     * <li> String with no default value and no XML value is null.
     * <li> String with no default value and misnamed XML value is null.
     * <li> Renamed String with no default value and XML value is the XML value.
     * <li> Object with no default value and an XML-defined Integer is the XML value.
     * <li> String with default value and XML value may be looked up from global namespace.
     * <li> String with default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> String of misnamed XML value may be looked up from global namespace.
     * <li> Renamed String with no default value and XML value may be looked up from global namespace.
     * <li> String of original name may be looked up from global namespace.
     * <li> Object with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> Class level resource with name and type may be looked up from global namespace.
     * <li> Second class level resource with name only may be looked up from global namespace.
     * <li> String with default value and XML value may be looked up from session context.
     * <li> String with default value and no XML value may not be looked up from session context.
     * <li> String with no default value and no XML value may not be looked up from session context.
     * <li> String with no default value and misnamed XML value may not be looked up from session context.
     * <li> String of misnamed XML value may be looked up from session context.
     * <li> Renamed String with no default value and XML value may be looked up from session context.
     * <li> String of original name may be looked up from session context.
     * <li> Object with no default value and an XML-defined Integer may be looked up from session context.
     * <li> Class level resource with name and type may be looked up from session context.
     * <li> Second class level resource with name only may be looked up from session context.
     * </ol>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLRAdvEnvObjMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + AdvObjMthdBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SLRSB accessed successfully.", bean);

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
     * <li> Injection methods with values are called (not injected into fields directly).
     * <li> int with default value and XML value is the XML value.
     * <li> int with default value and no XML value is the default value.
     * <li> int with no default value and no XML value is 0.
     * <li> int with no default value and misnamed XML value is 0.
     * <li> Renamed int with no default value and XML value is the XML value.
     * <li> int with no default value and invalid XML value is 0.
     * <li> float with no default value and an XML-defined Integer is the XML value.
     * <li> int with default value and XML value may be looked up from global namespace.
     * <li> int with default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> int of misnamed XML value may be looked up from global namespace.
     * <li> Renamed int with no default value and XML value may be looked up from global namespace.
     * <li> int of original name may be looked up from global namespace.
     * <li> int with no default value and invalid XML value may not be looked up from global namespace.
     * <li> float with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> int with default value and XML value may be looked up from session context.
     * <li> int with default value and no XML value may not be looked up from session context.
     * <li> int with no default value and no XML value may not be looked up from session context.
     * <li> int with no default value and misnamed XML value may not be looked up from session context.
     * <li> int of misnamed XML value may be looked up from session context.
     * <li> Renamed int with no default value and XML value may be looked up from session context.
     * <li> int of original name may be looked up from session context.
     * <li> int with no default value and invalid XML value may not be looked up from session context.
     * <li> float with no default value and an XML-defined Integer may be looked up from session context.
     * </ol>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLRAdvEnvPrimMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + AdvPrimMthdBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SLRSB accessed successfully.", bean);

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
     * <li> String with default value and XML value is the XML value.
     * <li> String with default value and no XML value is the default value.
     * <li> String with no default value and no XML value is null.
     * <li> String with no default value and misnamed XML value is null.
     * <li> Renamed String with no default value and XML value is the XML value.
     * <li> Object with no default value and an XML-defined Integer is the XML value.
     * <li> String with default value and XML value may be looked up from global namespace.
     * <li> String with default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> String of misnamed XML value may be looked up from global namespace.
     * <li> Renamed String with no default value and XML value may be looked up from global namespace.
     * <li> String of original name may be looked up from global namespace.
     * <li> Object with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> Class level resource with name and type may be looked up from global namespace.
     * <li> Second class level resource with name only may be looked up from global namespace.
     * <li> String with default value and XML value may be looked up from session context.
     * <li> String with default value and no XML value may not be looked up from session context.
     * <li> String with no default value and no XML value may not be looked up from session context.
     * <li> String with no default value and misnamed XML value may not be looked up from session context.
     * <li> String of misnamed XML value may be looked up from session context.
     * <li> Renamed String with no default value and XML value may be looked up from session context.
     * <li> String of original name may be looked up from session context.
     * <li> Object with no default value and an XML-defined Integer may be looked up from session context.
     * <li> Class level resource with name and type may be looked up from session context.
     * <li> Second class level resource with name only may be looked up from session context.
     * </ol>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLRAdvCompEnvObjFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 updated lookup for the new binding format
        EnvInjectionEJBRemoteHome slHome = FATHelper.lookupRemoteHomeBinding(ivJNDI_SLREnvInjectObjFld,
                                                                             EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLRSB created successfully.", bean);

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
     * <li> int with default value and XML value is the XML value.
     * <li> int with default value and no XML value is the default value.
     * <li> int with no default value and no XML value is 0.
     * <li> int with no default value and misnamed XML value is 0.
     * <li> Renamed int with no default value and XML value is the XML value.
     * <li> int with no default value and invalid XML value is 0.
     * <li> float with no default value and an XML-defined Integer is the XML value.
     * <li> int with default value and XML value may be looked up from global namespace.
     * <li> int with default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> int of misnamed XML value may be looked up from global namespace.
     * <li> Renamed int with no default value and XML value may be looked up from global namespace.
     * <li> int of original name may be looked up from global namespace.
     * <li> int with no default value and invalid XML value may not be looked up from global namespace.
     * <li> float with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> int with default value and XML value may be looked up from session context.
     * <li> int with default value and no XML value may not be looked up from session context.
     * <li> int with no default value and no XML value may not be looked up from session context.
     * <li> int with no default value and misnamed XML value may not be looked up from session context.
     * <li> int of misnamed XML value may be looked up from session context.
     * <li> Renamed int with no default value and XML value may be looked up from session context.
     * <li> int of original name may be looked up from session context.
     * <li> int with no default value and invalid XML value may not be looked up from session context.
     * <li> float with no default value and an XML-defined Integer may be looked up from session context.
     * </ol>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLRAdvCompEnvPrimFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 updated lookup for the new binding format
        EnvInjectionEJBRemoteHome slHome = FATHelper.lookupRemoteHomeBinding(ivJNDI_SLREnvInjectPrimFld,
                                                                             EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLRSB created successfully.", bean);

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
     * <li> Injection methods with values are called (not injected into fields directly).
     * <li> String with default value and XML value is the XML value.
     * <li> String with default value and no XML value is the default value.
     * <li> String with no default value and no XML value is null.
     * <li> String with no default value and misnamed XML value is null.
     * <li> Renamed String with no default value and XML value is the XML value.
     * <li> Object with no default value and an XML-defined Integer is the XML value.
     * <li> String with default value and XML value may be looked up from global namespace.
     * <li> String with default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> String of misnamed XML value may be looked up from global namespace.
     * <li> Renamed String with no default value and XML value may be looked up from global namespace.
     * <li> String of original name may be looked up from global namespace.
     * <li> Object with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> Class level resource with name and type may be looked up from global namespace.
     * <li> Second class level resource with name only may be looked up from global namespace.
     * <li> String with default value and XML value may be looked up from session context.
     * <li> String with default value and no XML value may not be looked up from session context.
     * <li> String with no default value and no XML value may not be looked up from session context.
     * <li> String with no default value and misnamed XML value may not be looked up from session context.
     * <li> String of misnamed XML value may be looked up from session context.
     * <li> Renamed String with no default value and XML value may be looked up from session context.
     * <li> String of original name may be looked up from session context.
     * <li> Object with no default value and an XML-defined Integer may be looked up from session context.
     * <li> Class level resource with name and type may be looked up from session context.
     * <li> Second class level resource with name only may be looked up from session context.
     * </ol>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    @SkipForRepeat({ RepeatWithCDI.ID, RepeatWithEE9CDI.ID })
    public void testSLRAdvCompEnvObjMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionEJBRemoteHome slHome = FATHelper.lookupRemoteHomeBinding(ivJNDI_SLREnvInjectObjMthd,
                                                                             EnvInjectionEJBRemoteHome.class);

        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLRSB created successfully.", bean);

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
    }

    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    @SkipForRepeat({ EmptyAction.ID, EE7FeatureReplacementAction.ID, EE8FeatureReplacementAction.ID, JakartaEE9Action.ID })
    public void testSLRAdvCompEnvObjMthdInjectionCDIEnabled() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionEJBRemoteHome slHome = FATHelper.lookupRemoteHomeBinding(ivJNDI_SLREnvInjectObjMthd,
                                                                             EnvInjectionEJBRemoteHome.class);

        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLRSB created successfully.", bean);

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
                     PASSED, bean.verifyEnvInjectionCDIEnabled(testpoint++));
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
     * <li> Injection methods with values are called (not injected into fields directly).
     * <li> int with default value and XML value is the XML value.
     * <li> int with default value and no XML value is the default value.
     * <li> int with no default value and no XML value is 0.
     * <li> int with no default value and misnamed XML value is 0.
     * <li> Renamed int with no default value and XML value is the XML value.
     * <li> int with no default value and invalid XML value is 0.
     * <li> float with no default value and an XML-defined Integer is the XML value.
     * <li> int with default value and XML value may be looked up from global namespace.
     * <li> int with default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> int of misnamed XML value may be looked up from global namespace.
     * <li> Renamed int with no default value and XML value may be looked up from global namespace.
     * <li> int of original name may be looked up from global namespace.
     * <li> int with no default value and invalid XML value may not be looked up from global namespace.
     * <li> float with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> int with default value and XML value may be looked up from session context.
     * <li> int with default value and no XML value may not be looked up from session context.
     * <li> int with no default value and no XML value may not be looked up from session context.
     * <li> int with no default value and misnamed XML value may not be looked up from session context.
     * <li> int of misnamed XML value may be looked up from session context.
     * <li> Renamed int with no default value and XML value may be looked up from session context.
     * <li> int of original name may be looked up from session context.
     * <li> int with no default value and invalid XML value may not be looked up from session context.
     * <li> float with no default value and an XML-defined Integer may be looked up from session context.
     * </ol>
     */
    @Test
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSLRAdvCompEnvPrimMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507
        EnvInjectionEJBRemoteHome slHome = FATHelper.lookupRemoteHomeBinding(ivJNDI_SLREnvInjectPrimMthd,
                                                                             EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLRSB created successfully.", bean);

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
    }

}

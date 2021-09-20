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

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.injection.mix.ejb.EnvInjectionEJBRemote;
import com.ibm.ws.ejbcontainer.injection.mix.ejb.EnvInjectionEJBRemoteHome;
import com.ibm.ws.ejbcontainer.injection.mix.ejb.EnvInjectionRemote;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> AdvSFRemoteEnvInjectionTest .
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the EJB 3.0
 * Injection of Environment values on Stateful Session beans. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testSFRAdvEnvObjFldInjection - Business Interface: Object Field Injection
 * <li>testSFRAdvEnvPrimFldInjection - Business Interface: Primitive Field Injection
 * <li>testSFRAdvEnvObjMthdInjection - Business Interface: Object Method Injection
 * <li>testSFRAdvEnvPrimMthdInjection - Business Interface: Primitive Method Injection
 * <li>testSFRAdvCompEnvObjFldInjection - Component Interface: Object Field Injection
 * <li>testSFRAdvCompEnvPrimFldInjection - Component Interface: Primitive Field Injection
 * <li>testSFRAdvCompEnvObjMthdInjection - Component Interface: Object Method Injection
 * <li>testSFRAdvCompEnvPrimMthdInjection - Component Interface: Primitive Method Injection
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/AdvSFRemoteEnvInjectionServlet")
public class AdvSFRemoteEnvInjectionServlet extends FATServlet {

    private static final String PASSED = "Passed";

    // SimpleBinding name was used for the bean used by this test
    private static final String SimpleBindingPreFix = "com/ibm/ws/ejbcontainer/injection/mix/ejb/";

    // Names of the beans used for the test... for lookup.
    private static final String AdvObjFldBean = "AdvSFEnvInjectObjFld";
    private static final String AdvPrimFldBean = "AdvSFEnvInjectPrimFld";
    private static final String AdvObjMthdBean = "AdvSFEnvInjectObjMthd";
    private static final String AdvPrimMthdBean = "AdvSFEnvInjectPrimMthd";
    private static final String AdvCompObjFldBean = "AdvCompSFEnvInjectObjFld";
    private static final String AdvCompPrimFldBean = "AdvCompSFEnvInjectPrimFld";
    private static final String AdvCompObjMthdBean = "AdvCompSFEnvInjectObjMthd";
    private static final String AdvCompPrimMthdBean = "AdvCompSFEnvInjectPrimMthd";

    // Names of the interfaces used for the test
    private static final String EnvInjectionEJBRemoteHomeInterface = EnvInjectionEJBRemoteHome.class.getName();

    /** Jndi Names of the Bean Homes to use for the test. **/
    private String ivJNDI_SFREnvInjectObjFld = SimpleBindingPreFix + AdvCompObjFldBean +
                                               "#" + EnvInjectionEJBRemoteHomeInterface;
    private String ivJNDI_SFREnvInjectPrimFld = SimpleBindingPreFix + AdvCompPrimFldBean +
                                                "#" + EnvInjectionEJBRemoteHomeInterface;
    private String ivJNDI_SFREnvInjectObjMthd = SimpleBindingPreFix + AdvCompObjMthdBean +
                                                "#" + EnvInjectionEJBRemoteHomeInterface;
    private String ivJNDI_SFREnvInjectPrimMthd = SimpleBindingPreFix + AdvCompPrimMthdBean +
                                                 "#" + EnvInjectionEJBRemoteHomeInterface;

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
     * <li> Stateful Session bean with injection may be accessed.
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
    public void testSFRAdvEnvObjFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + AdvObjFldBean,
                                                                EnvInjectionRemote.class);

        assertNotNull("1 ---> SFRSB accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
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
     * <li> Stateful Session bean with injection may be accessed.
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
    public void testSFRAdvEnvPrimFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + AdvPrimFldBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SFRSB accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
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
     * <li> Stateful Session bean with injection may be accessed.
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
    public void testSFRAdvEnvObjMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + AdvObjMthdBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SFRSB accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
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
     * <li> Stateful Session bean with injection may be accessed.
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
    public void testSFRAdvEnvPrimMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + AdvPrimMthdBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SFRSB accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
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
     * <li> Stateful Session bean with injection may be created.
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
    public void testSFRAdvCompEnvObjFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 updated lookup for the new binding format
        EnvInjectionEJBRemoteHome sfHome = FATHelper.lookupRemoteHomeBinding(ivJNDI_SFREnvInjectObjFld,
                                                                             EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = sfHome.create();
        assertNotNull("1 ---> SFRSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
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
     * <li> Stateful Session bean with injection may be created.
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
    public void testSFRAdvCompEnvPrimFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 updated lookup for the new binding format
        EnvInjectionEJBRemoteHome sfHome = FATHelper.lookupRemoteHomeBinding(ivJNDI_SFREnvInjectPrimFld,
                                                                             EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = sfHome.create();
        assertNotNull("1 ---> SFRSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
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
     * <li> Stateful Session bean with injection may be created.
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
    public void testSFRAdvCompEnvObjMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionEJBRemoteHome sfHome = FATHelper.lookupRemoteHomeBinding(ivJNDI_SFREnvInjectObjMthd,
                                                                             EnvInjectionEJBRemoteHome.class);

        EnvInjectionEJBRemote bean = sfHome.create();
        assertNotNull("1 ---> SFRSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
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
     * <li> Stateful Session bean with injection may be created.
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
    public void testSFRAdvCompEnvPrimMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBRemoteHome sfHome = FATHelper.lookupRemoteHomeBinding(ivJNDI_SFREnvInjectPrimMthd,
                                                                             EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = sfHome.create();
        assertNotNull("1 ---> SFRSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }

}

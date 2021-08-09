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
import com.ibm.ws.ejbcontainer.injection.xml.ejbx.EJBInjectionLocal;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> AdvSLEJBInjectionTest .
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the Advanced EJB 3.0
 * Injection of EJBs into Stateless Session beans. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testSLEJBFldInjection - Business Interface: EJB Field Injection
 * <li>testSLEJBMthdInjection - Business Interface: EJB Method Injection
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/AdvSLEJBInjectionServlet")
public class AdvSLEJBInjectionServlet extends FATServlet {

    private static final String PASSED = "Passed";

    // Names of application and module... for lookup.
    private static final String Application = "EJB3INJSXTestApp";
    private static final String Module = "EJB3INJEJBXBean.jar";

    // Names of the beans used for the test... for lookup.
    private static final String SLSTestFieldBean = "SLSTestFieldBean";
    private static final String SLSTestMethodBean = "SLSTestMethodBean";

    // Names of the interfaces used for the test
    private static final String EJBInjectionLocalInterface = EJBInjectionLocal.class.getName();

    /**
     * Test Field injection of EJBs on an EJB 3.0 Stateless Session EJB,
     * with advanced configurations. <p>
     *
     * Also test that the values injected into the fields may be looked
     * up through both the global namespace and the session context. <p>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the pooled state. <p>
     */
    @Test
    public void testSLEJBFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        EJBInjectionLocal bean = (EJBInjectionLocal) FATHelper.lookupDefaultBindingEJBLocalInterface(EJBInjectionLocalInterface, Application, Module,
                                                                                                     SLSTestFieldBean);
        assertNotNull("1 ---> SLLSB was not accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyAdvEJB30Injection(testpoint++));
    }

    /**
     * Test Method injection of EJBs on an EJB 3.0 Stateless Session EJB,
     * with advanced configurations. <p>
     *
     * Also test that the values injected into the fields may be looked
     * up through both the global namespace and the session context. <p>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the pooled state. <p>
     */
    @Test
    public void testSLEJBMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        EJBInjectionLocal bean = (EJBInjectionLocal) FATHelper.lookupDefaultBindingEJBLocalInterface(EJBInjectionLocalInterface, Application, Module,
                                                                                                     SLSTestMethodBean);
        assertNotNull("1 ---> SLLSB was not accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyAdvEJB30Injection(testpoint++));
    }

}

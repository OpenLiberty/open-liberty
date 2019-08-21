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
import com.ibm.ws.ejbcontainer.injection.xml.ejb.SuperEnvInjectionEJBLocal;
import com.ibm.ws.ejbcontainer.injection.xml.ejb.SuperEnvInjectionEJBLocalHome;
import com.ibm.ws.ejbcontainer.injection.xml.ejb.SuperEnvInjectionLocal;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> SuperEnvInjectionTest .
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the Basic EJB 3.0
 * Injection of Environment values on superclasses of beans. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testSLSuperClassInjection - Test superclasses of SL Session Bean
 * <li>testSFSuperClassInjection - Test superclasses of SF Session Bean
 * <li>testSLCompSuperClassInjection - Test superclasses of Component SL Session Bean
 * <li>testSFCompSuperClassInjection - Test superclasses of Component SF Session Bean
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/SuperEnvInjectionServlet")
public class SuperEnvInjectionServlet extends FATServlet {

    private static final String PASSED = "Passed";

    // Names of application and module... for lookup.
    private static final String Application = "EJB3INJSXTestApp";
    private static final String Module = "EJB3INJSXBean.jar";

    // SimpleBinding name was used for the bean used by this test
    private static final String SimpleBindingPreFix = "com/ibm/ws/ejbcontainer/injection/xml/ejb/";

    // Names of the beans used for the test... for lookup.
    private static final String SuperSLBean = "SuperSLEnvInject";
    private static final String SuperSFBean = "SuperSFEnvInject";
    private static final String SuperCompSLBean = "SuperCompSLEnvInject";
    private static final String SuperCompSFBean = "SuperCompSFEnvInject";

    // Names of the interfaces used for the test
    private static final String SuperInjectionLocalInterface = SuperEnvInjectionLocal.class.getName();

    /** Jndi Names of the Bean Homes to use for the test. **/
    private String ivJNDI_SuperCompSLEnvInject = SimpleBindingPreFix + SuperCompSLBean;
    private String ivJNDI_SuperCompSFEnvInject = SimpleBindingPreFix + SuperCompSFBean;

    /**
     * Test injection to a superclass of a SL Session Bean using both
     * xml and annotations. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Environment entry injection will occur in the superclass of a Stateless Session bean.
     * <li> Injection will occur in a superclass's public, private, or protected fields or methods.
     * <li> Injection will occur in the superclass of a superclass of a bean.
     * </ol>
     *
     */
    @Test
    public void testSLSuperClassInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        SuperEnvInjectionLocal bean = (SuperEnvInjectionLocal) FATHelper.lookupDefaultBindingEJBLocalInterface(SuperInjectionLocalInterface, Application, Module,
                                                                                                               SuperSLBean);
        assertNotNull("1 ---> SLLSB was not accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }

    /**
     * Test injection to a superclass of a SF Session Bean using both
     * xml and annotations. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Environment entry injection will occur in the superclass of a Stateful Session bean.
     * <li> Injection will occur in a superclass's public, private, or protected fields or methods.
     * <li> Injection will occur in the superclass of a superclass of a bean.
     * </ol>
     *
     */
    @Test
    public void testSFSuperClassInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        SuperEnvInjectionLocal bean = (SuperEnvInjectionLocal) FATHelper.lookupDefaultBindingEJBLocalInterface(SuperInjectionLocalInterface, Application, Module,
                                                                                                               SuperSFBean);
        assertNotNull("1 ---> SFLSB was not accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }

    /**
     * Test injection to a superclass of a SL Session Bean with Component Interfaces using both
     * xml and annotations. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Environment entry injection will occur in the superclass of a Stateless Session bean.
     * <li> Injection will occur in a superclass's public, private, or protected fields or methods.
     * <li> Injection will occur in the superclass of a superclass of a bean.
     * </ol>
     *
     */
    @Test
    public void testSLCompSuperClassInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        SuperEnvInjectionEJBLocalHome slHome = (SuperEnvInjectionEJBLocalHome) FATHelper.lookupLocalBinding(ivJNDI_SuperCompSLEnvInject);
        SuperEnvInjectionEJBLocal bean = slHome.create();
        assertNotNull("1 ---> SLLSB was not created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }

    /**
     * Test injection to a superclass of a SF Session Bean with Component Interfaces using both
     * xml and annotations. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Environment entry injection will occur in the superclass of a Stateful Session bean.
     * <li> Injection will occur in a superclass's public, private, or protected fields or methods.
     * <li> Injection will occur in the superclass of a superclass of a bean.
     * </ol>
     *
     */
    @Test
    public void testSFCompSuperClassInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        SuperEnvInjectionEJBLocalHome slHome = (SuperEnvInjectionEJBLocalHome) FATHelper.lookupLocalBinding(ivJNDI_SuperCompSFEnvInject);
        SuperEnvInjectionEJBLocal bean = slHome.create();
        assertNotNull("1 ---> SFLSB was not created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }
}

/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.cdi.jcdi.web;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.cdi.jcdi.ejb.BeanManagerLocal;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> BeanManagerInjectionTest .
 *
 * <dt><b>Test Author:</b> Tracy Burroughs <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support of CDI BeanManager access through
 * injection and lookup in component environment context (java:comp). <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li> testJavaColonCompLookup
 * - verifies that the CDI BeanManager may be looked up at
 * java:comp/BeanManager for a basic EJB in a CDI enabled module.
 * <li> testResourceAnnotationInjection
 * - verifies that the CDI BeanManager may be injected using the
 *
 * @Resource annotation and looked up for a basic EJB in a CDI
 *           enabled module.
 *           <li> testResourceMappedNameInjection
 *           - verifies that the CDI BeanManager may be injected using the
 * @Resource annotation with a mappedName looked up for a basic
 *           EJB in a CDI enabled module.
 *           <li> testResourceEnvRefXMLInjection
 *           - verifies that the CDI BeanManager may be injected using the
 *           <resoruce-env-ref> XML stanza and looked up for a basic EJB in
 *           a CDI enabled module.
 *           <li> testInjectAnnotationInjection
 *           - verifies that the CDI BeanManager may be injected using the
 * @Inject annotation and may NOT be looked up in java:comp/env
 *         for a basic EJB in a CDI enabled module.
 *         <li> testJavaColonCompLookupWhenNotJCDIEnabled
 *         - verifies that the CDI BeanManager may NOT be looked up at
 *         java:comp/BeanManager for a basic EJB in a non-CDI enabled module.
 *         </ul>
 *         <br>Data Sources - None
 *         </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/BeanManagerInjectionServlet")
public class BeanManagerInjectionServlet extends FATServlet {
    private static final String EJB_CDI_MODULE_NAME = "EJB31JCDIBean";
    private static final String EJB_NON_CDI_MODULE_NAME = "EJB31NonJCDIBean";
    private static final String JNDI_BMSTATELESSXML_NAME = "java:app/" + EJB_CDI_MODULE_NAME + "/BeanManagerStatelessXML";

    /**
     * Tests that the CDI BeanManager may be looked up at java:comp/BeanManager
     * for a basic EJB in a CDI enabled module. <p>
     *
     * This is verified by performing the following:
     * <ul>
     * <li> Performing a lookup of java:comp/BeanManager
     * </ul>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testJavaColonCompLookup() throws Exception {
        // Locate Stateless local bean
        BeanManagerLocal bean = (BeanManagerLocal) FATHelper.lookupDefaultBindingEJBJavaApp(BeanManagerLocal.class.getName(),
                                                                                            EJB_CDI_MODULE_NAME,
                                                                                            "BasicStateless");

        // Verify that the BeanManager may be looked up at java:comp/BeanManager
        bean.verifyBeanMangerInjectionAndLookup();
    }

    /**
     * Tests that the CDI BeanManager may be injected using the @Resource
     * annotation and looked up for a basic EJB in a CDI enabled module. <p>
     *
     * This is verified by performing the following:
     * <ul>
     * <li> verify the instance variable is not null.
     * <li> lookup the reference in java:comp/env/<ref-name>
     * <li> lookup of java:comp/BeanManager
     * </ul>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testResourceAnnotationInjection() throws Exception {
        // Locate Stateless local bean
        BeanManagerLocal bean = (BeanManagerLocal) FATHelper.lookupDefaultBindingEJBJavaApp(BeanManagerLocal.class.getName(),
                                                                                            EJB_CDI_MODULE_NAME,
                                                                                            "BeanManagerStatelessAnnotation");

        // Verify that the BeanManager may be looked up at java:comp/BeanManager
        bean.verifyBeanMangerInjectionAndLookup();
    }

    /**
     * Tests that the CDI BeanManager may be injected using the @Resource
     * annotation with the mappedName element and looked up for a basic EJB
     * in a CDI enabled module. <p>
     *
     * This is verified by performing the following:
     * <ul>
     * <li> verify the instance variable is not null.
     * <li> lookup the reference in java:comp/env/<ref-name>
     * <li> lookup of java:comp/BeanManager
     * </ul>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testResourceMappedNameInjection() throws Exception {
        // Locate Stateless local bean
        BeanManagerLocal bean = (BeanManagerLocal) FATHelper.lookupDefaultBindingEJBJavaApp(BeanManagerLocal.class.getName(),
                                                                                            EJB_CDI_MODULE_NAME,
                                                                                            "BeanManagerStatelessMappedName");

        // Verify that the BeanManager may be looked up at java:comp/BeanManager
        bean.verifyBeanMangerInjectionAndLookup();
    }

    /**
     * Tests that the CDI BeanManager may be injected using the <resoruce-env-ref>
     * XML stanza and looked up for a basic EJB in a CDI enabled module. <p>
     *
     * This is verified by performing the following:
     * <ul>
     * <li> verify the instance variable is not null.
     * <li> lookup the reference in java:comp/env/<ref-name>
     * <li> lookup of java:comp/BeanManager
     * </ul>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testResourceEnvRefXMLInjection() throws Exception {
        // Locate Stateless local bean
        // Locate Stateless local bean
        BeanManagerLocal bean = (BeanManagerLocal) FATHelper.lookupDefaultBindingEJBJavaApp(BeanManagerLocal.class.getName(),
                                                                                            EJB_CDI_MODULE_NAME,
                                                                                            "BeanManagerStatelessXML");
        // BeanManagerRemote bean = FATHelper.lookupRemoteBinding(JNDI_BMSTATELESSXML_NAME, BeanManagerRemote.class );

        // Verify that the BeanManager may be looked up at java:comp/BeanManager
        bean.verifyBeanMangerInjectionAndLookup();
    }

    /**
     * Tests that the CDI BeanManager may be injected using the @Inject
     * annotation and may NOT be looked up in java:comp/env for a basic
     * EJB in a CDI enabled module. <p>
     *
     * This is verified by performing the following:
     * <ul>
     * <li> verify the instance variable is not null.
     * <li> lookup the reference in java:comp/env/<ref-name>
     * <li> lookup of java:comp/BeanManager
     * </ul>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testInjectAnnotationInjection() throws Exception {
        // Locate Stateless local bean
        BeanManagerLocal bean = (BeanManagerLocal) FATHelper.lookupDefaultBindingEJBJavaApp(BeanManagerLocal.class.getName(),
                                                                                            EJB_CDI_MODULE_NAME,
                                                                                            "BeanManagerStatelessInject");

        // Verify that the BeanManager injection and lookup
        bean.verifyBeanMangerInjectionAndLookup();
    }

    /**
     * Tests that the CDI BeanManager may NOT be looked up at
     * java:comp/BeanManager for a basic EJB in a non-CDI enabled module. <p>
     *
     * This is verified by performing the following:
     * <ul>
     * <li> Performing a lookup of java:comp/BeanManager
     * </ul>
     *
     * @throws Exception when an assertion failure occurs.
     */
    //@Test
    public void testJavaColonCompLookupWhenNotJCDIEnabled() throws Exception {
        // Locate Stateless local bean
        BeanManagerLocal bean = (BeanManagerLocal) FATHelper.lookupDefaultBindingEJBJavaApp(BeanManagerLocal.class.getName(),
                                                                                            EJB_NON_CDI_MODULE_NAME,
                                                                                            "BasicStatelessNonJcdi");

        // Verify that the BeanManager may not be looked up at java:comp/BeanManager
        bean.verifyBeanMangerInjectionAndLookup();
    }

}

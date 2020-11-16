/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
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
import com.ibm.ws.ejbcontainer.cdi.jcdi.ejb.BeanManagerRemote;

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
        BeanManagerLocal bean = (BeanManagerLocal) FATHelper.lookupLocalBinding("ejblocal:BasicStateless");

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
        BeanManagerLocal bean = (BeanManagerLocal) FATHelper.lookupLocalBinding("ejblocal:BeanManagerStatelessAnnotation");

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
        BeanManagerLocal bean = (BeanManagerLocal) FATHelper.lookupLocalBinding("ejblocal:BeanManagerStatelessMappedName");

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
        BeanManagerRemote bean = FATHelper.lookupRemoteBinding("ejb/BeanManagerStatelessXML", BeanManagerRemote.class);

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
        BeanManagerLocal bean = (BeanManagerLocal) FATHelper.lookupLocalBinding("ejblocal:BeanManagerStatelessInject");

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
        BeanManagerLocal bean = (BeanManagerLocal) FATHelper.lookupLocalBinding("ejblocal:BasicStatelessNonJcdi");

        // Verify that the BeanManager may not be looked up at java:comp/BeanManager
        bean.verifyBeanMangerInjectionAndLookup();
    }
}

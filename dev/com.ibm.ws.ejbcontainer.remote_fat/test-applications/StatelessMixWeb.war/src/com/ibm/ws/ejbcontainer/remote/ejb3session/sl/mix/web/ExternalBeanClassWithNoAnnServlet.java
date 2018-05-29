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
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web;

import static org.junit.Assert.assertNotNull;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.sc2.CMTStatelessLocal;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> ExternalBeanClassWithNoAnnTest .
 *
 * <dt><b>Test Author:</b> Urrvano Gamez, Jr.
 * <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests that the bean class (CMTStatelessLocalBean.java) can be located in
 * a different module (jar file) than the defining ejb-jar.xml. This particular
 * bean implementation contains no annotations.
 * <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>
 * Sub-tests
 * <ul>
 * <li>testLookupXMLBasedName - (local) Business Interface: Look up XML-based
 * name and verify all methods
 * </ul>
 * <br>
 * Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/ExternalBeanClassWithNoAnnServlet")
public class ExternalBeanClassWithNoAnnServlet extends FATServlet {
    /** Strings to use in the lookups for the test. **/
    final String businessInterface = CMTStatelessLocal.class.getName();
    final String module = "StatelessMixSCEJB";
    final String beanName1 = "CMTStatelessLocalBean";

    /**
     * Test calling method on a local EJB 3.0 CMT Stateless Session EJB that was
     * looked up by its XML-specified name. The bean class is packaged in a
     * different module (jar file) than the ejb-jar.xml file.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Stateless Session bean may be created.
     * <li>SLSB method with NotSupported tx attribute may be called.
     * </ol>
     */
    @Test
    public void testLookupXMLBasedName() throws Exception {
        // --------------------------------------------------------------------
        // Lookup SLRSB by XML name and execute the test
        // --------------------------------------------------------------------
        CMTStatelessLocal bean1 = (CMTStatelessLocal) FATHelper.lookupDefaultBindingEJBJavaApp(businessInterface, module, beanName1);
        assertNotNull("1 ---> CMTStatelessLocal obtained successfully.", bean1);
        bean1.tx_NotSupported();
    }
}
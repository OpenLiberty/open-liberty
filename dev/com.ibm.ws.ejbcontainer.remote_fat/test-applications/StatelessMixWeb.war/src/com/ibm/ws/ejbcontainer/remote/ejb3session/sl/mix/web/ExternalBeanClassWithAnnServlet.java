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
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.sc2.CMTStatelessRemote;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> ExternalBeanClassWithAnnTest .
 *
 * <dt><b>Test Author:</b> Urrvano Gamez, Jr.
 * <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests that the bean class (CMTStatelessRemoteBean.java) can be located in
 * a different module (jar file) than the defining ejb-jar.xml. This particular
 * bean implementation contains annotations.
 * <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>
 * Sub-tests
 * <ul>
 * <li>testLookupXMLBasedName - (remote) Business Interface: Look up XML-based
 * name and verify all methods
 * </ul>
 * <br>
 * Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/ExternalBeanClassWithAnnServlet")
public class ExternalBeanClassWithAnnServlet extends FATServlet {
    /** Strings to use in the lookups for the test. **/
    final String businessInterface = CMTStatelessRemote.class.getName();
    final String module = "StatelessMixSCEJB";
    final String beanName1 = "CMTStatelessRemote";

    /**
     * Test calling a method on a remote EJB 3.0 CMT Stateless Session EJB that
     * was looked up by its XML-specified name. The bean class is packaged in a
     * different module (jar file) than the ejb-jar.xml file.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Stateless Session bean may be created.
     * <li>SLSB method with RequiresNew tx attribute may be called.
     * </ol>
     *
     * @throws Exception
     */
    @Test
    public void testLookupXMLBasedName() throws Exception {
        // --------------------------------------------------------------------
        // Lookup SLRSB by XML name and execute the test
        // --------------------------------------------------------------------
        CMTStatelessRemote bean1 = (CMTStatelessRemote) FATHelper.lookupJavaBinding("java:global/StatelessMixTest/StatelessMixSCEJB/CMTStatelessRemote!com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.sc2.CMTStatelessRemote"); //FATHelper.lookupDefaultBindingEJBJavaApp(businessInterface, module, beanName1);
        assertNotNull("1 ---> CMTStatelessRemote obtained successfully.", bean1);
        bean1.tx_RequiresNew();
    }
}
/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.ejb.NoSuchEJBException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.RemoteInterface1;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.RemoteInterface2;

import componenttest.app.FATServlet;

/**
 * A SFSB defined in XML (no annotations used in the bean class)
 * with no business interfaces (BI) defined with at least one class on the implements
 * clause of the bean. At least one of the classes listed on the implements clause
 * will be a business interface for the bean and the interface itself will be
 * annotated with either the Local or Remote annotation.
 *
 * The bean should be able to find and use the business interface(s).<p>
 *
 * Sub-tests
 * <ul>
 * <li>test01 - SFSB no annotations, no BIs defined, implements clause contains one class,
 * RemoteInterface1.class, which will be annotated with the Remote annotation.
 * The remote interface should be found and used.
 * <li>test02 - SFSB no annotations, no BIs defined, implements clause contains multiple classes,
 * which will be annotated with the Local or Remote annotation. The remote interfaces
 * should be found and used.
 * </ul>
 */
@WebServlet("/InterfaceMixRemoteServlet")
public class InterfaceMixRemoteServlet extends FATServlet {
    private static final long serialVersionUID = -6446216863793492997L;
    private final static String CLASSNAME = InterfaceMixRemoteServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Strings to use in the lookups for the test. **/
    final String businessInterface1 = "com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.RemoteInterface1";
    final String businessInterface2 = "com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.RemoteInterface2";
    final String module = "StatefulMixRemoteEJB";
    final String beanName1 = "SFNoBizInterfaceDefined2";
    final String beanName2 = "SFNoBizInterfaceDefined3";

    /**
     * Very simple bean with no annotations at all and the XML does NOT define
     * any business interfaces. The bean's implements clause contains one class,
     * RemoteInterface1.class, which will be annotated with the Remote annotation.
     * The remote interface should be found and used.
     *
     * @throws Exception
     *
     */
    @Test
    public void testInterfaceMixRemoteServlet_test01() throws Exception {
        // --------------------------------------------------------------------
        // Lookup SFSB by annotation name and execute the test
        // --------------------------------------------------------------------
        RemoteInterface1 bean1 = (RemoteInterface1) FATHelper.lookupDefaultBindingEJBJavaApp(businessInterface1, module, beanName1);
        assertNotNull("1 ---> RemoteInterface1 obtained successfully.", bean1);

        // call bean1.remoteBizInterface1()
        svLogger.info("Info: executing bean1.remoteBizInterface1().");
        String actual = bean1.remoteBizInterface1();
        String expected = "Used RemoteInterface1";
        svLogger.info("Expected: " + expected);
        svLogger.info("Actual: " + actual);
        assertEquals("2 --->Compare the actual and expected result string.", actual, expected);

        // Destroys the bean
        bean1.finish();

        // Make sure we can't call the bean after it is destroyed and check the exception.
        try {
            bean1.remoteBizInterface1();
            fail("3 --> expected NoSuchEJBException");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("3 ---> finish: expected NoSuchEJBException");
        }
    }

    /**
     * Very simple bean with no annotations at all and the XML does NOT define
     * any business interfaces. The bean's implements clause contains multiple classes
     * each of which are annotated with either the Remote annotation or Local annotation.
     *
     * This test verifies that all of the remote interfaces are found and used.
     *
     * @throws Exception
     *
     */
    @Test
    public void testInterfaceMixRemoteServlet_test02() throws Exception {
        // --------------------------------------------------------------------
        // Lookup SFSB by annotation name and execute the test
        // --------------------------------------------------------------------
        RemoteInterface1 bean1 = (RemoteInterface1) FATHelper.lookupDefaultBindingEJBJavaApp(businessInterface1, module, beanName2);
        assertNotNull("1 ---> RemoteInterface1 obtained successfully.", bean1);

        RemoteInterface2 bean2 = (RemoteInterface2) FATHelper.lookupDefaultBindingEJBJavaApp(businessInterface2, module, beanName2);
        assertNotNull("2 ---> RemoteInterface2 obtained successfully.", bean2);

        // call bean1.remoteBizInterface1()
        svLogger.info("Info: executing bean1.remoteBizInterface1().");
        String actual = bean1.remoteBizInterface1();
        String expected = "Used RemoteInterface1";
        svLogger.info("Expected: " + expected);
        svLogger.info("Actual: " + actual);
        assertEquals("3 --->Compare the actual and expected result string for bean1.", actual, expected);

        // call bean2.remoteBizInterface2()
        svLogger.info("Info: executing bean2.remoteBizInterface2().");
        String actual2 = bean2.remoteBizInterface2();
        String expected2 = "Used RemoteInterface2";
        svLogger.info("Expected2: " + expected2);
        svLogger.info("Actual2: " + actual2);
        assertEquals("4 --->Compare the actual and expected result string for bean2.", actual, expected);

        // Destroys bean1
        bean1.finish();

        // Make sure we can't call the bean after it is destroyed and check the exception.
        try {
            bean1.remoteBizInterface1();
            fail("5 --> expected NoSuchEJBException");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("5 ---> finish() for bean1: expected NoSuchEJBException");
        }

        // Destroys bean2
        bean2.finish();

        // Make sure we can't call the bean after it is destroyed and check the exception.
        try {
            bean2.remoteBizInterface2();
            fail("6 --> expected NoSuchEJBException");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("6 ---> finish() for bean2: expected NoSuchEJBException");
        }
    }
}

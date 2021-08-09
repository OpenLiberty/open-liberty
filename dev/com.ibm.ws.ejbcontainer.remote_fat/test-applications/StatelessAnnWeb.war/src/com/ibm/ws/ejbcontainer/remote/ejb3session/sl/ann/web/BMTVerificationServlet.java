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
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.BMTRemote;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>TxAttrTest
 *
 * <dt>Test Descriptions:
 * <dd>Tests whether a bean which uses @TransactionManagement (BEAN) to declare itself
 * a bean-managed transaction bean is actually a BMT bean.
 *
 * <dt>Author:
 * <dd>Urrvano Gamez, Jr.
 *
 *
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testTransactionManagementAnn verifies that bean using @TransactionManagment (BEAN) is actually a BMT bean.
 * <li>testTransactionManagementAnnRemote verifies that bean using @TransactionManagment (BEAN) is actually a BMT bean - using a remote bean.
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/BMTVerificationServlet")
public class BMTVerificationServlet extends FATServlet {
    /**
     * Definitions for the logger
     */
    private final static String CLASSNAME = BMTVerificationServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Name of module... for lookup.
    private static final String Module = "StatelessAnnEJB";

    private BMTRemote bean2;

    @PostConstruct
    private void setUp() {
        String beanName = "BMTBean";
        String remoteInterfaceName = BMTRemote.class.getName();

        try {
            bean2 = (BMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(remoteInterfaceName, Module, beanName); // F379-549fvtFrw2
        } catch (NamingException ne) {
            fail("lookup of remote interface failed: " + remoteInterfaceName);
        }
    }

    /**
     * This test is the same as testTransactionManagementAnn() except that it
     * will use a remote interface of the BMTBean.
     *
     * The bean, BMTBean, uses @TransactionManagement (BEAN) to declare itself a
     * bean-managed transaction bean.
     *
     * Verify that the bean is a bean-managed transaction bean by calling a
     * method in the bean that simply does a userTran.begin() and a
     * userTran.commit().
     */
    @Test
    public void testTransactionManagementAnnRemote() throws Exception {
        assertNotNull("Remote bean, bean2, not null", bean2);

        try {
            bean2.bmtMethod();
            svLogger.info("The bmtMethod ran successfully. This verifies that the @TransactionManagement (BEAN)worked to declare this bean a BMT bean.");
        } catch (IllegalStateException ie) {
            fail("The method threw an IllegalStateException: " + ie);
        }
    }
}
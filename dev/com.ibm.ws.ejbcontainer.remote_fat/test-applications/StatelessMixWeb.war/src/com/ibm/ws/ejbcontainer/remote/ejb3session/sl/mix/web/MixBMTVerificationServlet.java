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

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.BMTLocal;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.BMTRemote;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>BMTverificationTest
 *
 * <dt>Test Descriptions:
 * <dd>Tests whether a bean which uses both XML (<transaction-type> Bean </transaction-type)
 * and @TransactionManagement (BEAN)to declare itself a bean-managed transaction bean is
 * actually a BMT bean. This test class tests both the Local and Remote versions.
 *
 * <dt>Author:
 * <dd>Urrvano Gamez, Jr.
 *
 *
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testIsBMT verifies that bean using both XML and annotations to declare itself a BMT bean is actually a BMT bean.
 * <li>testIsBMTRemote verifies that bean using both XML and annotations to declare itself a BMT bean is actually a BMT bean (remote version).
 * <li>testIsBMTTransactionTypeNotSpecified verifies that bean using annotations to declare itself a BMT bean is actually a BMT bean even if <transaction-type> is not specified in
 * XML.
 * <li>testIsBMTTransactionTypeNotSpecifiedRemote verifies that bean using annotations to declare itself a BMT bean is actually a BMT bean even if <transaction-type> is not
 * specified in XML (remote version).
 *
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/BMTVerificationServlet")
public class MixBMTVerificationServlet extends FATServlet {
    private static final String CLASS_NAME = MixBMTVerificationServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @EJB(beanName = "BMTBean")
    private BMTLocal bean1;

    @EJB(beanName = "BMTBean")
    private BMTRemote bean2;

    @EJB(beanName = "BMTNoXMLTranType")
    private BMTLocal bean;

    @EJB(beanName = "BMTNoXMLTranType")
    private BMTRemote rbean;

    /**
     * The bean, BMTBean, uses both XML (<transaction-type> Bean
     * </transaction-type>) and @TransactionManagement (BEAN) to declare itself
     * a bean-managed transaction bean.
     *
     * Verify that the bean is a bean-managed transaction bean by calling a
     * method in the bean that simply does a userTran.begin() and a
     * userTran.commit().
     */
    @Test
    public void testIsBMT() throws Exception {
        assertNotNull("Local bean, bean1, not null", bean1);
        bean1.bmtMethod();
        svLogger.info("The bmtMethod ran successfully. This verifies that using both XML and annotations worked to declare this bean a BMT bean.");
    }

    /**
     * The bean, BMTBean, uses both XML (<transaction-type> Bean
     * </transaction-type>) and @TransactionManagement (BEAN) to declare itself
     * a bean-managed transaction bean. This is the Remote version.
     *
     * Verify that the bean is a bean-managed transaction bean by calling a
     * method in the bean that simply does a userTran.begin() and a
     * userTran.commit().
     */
    @Test
    public void testIsBMTRemote() throws Exception {
        assertNotNull("Remote bean, bean2, not null", bean2);
        bean2.bmtMethod();
        svLogger.info("The bmtMethod ran successfully for the remote version of this test. This verifies that using both XML and annotations worked to declare this bean a BMT bean.");
    }

    /**
     * The bean, BMTNoXMLTranType, uses the TransactionManagement Annotation
     * with a value = (BEAN) to declare itself a bean-managed transaction bean.
     * The ejb-jar.xml does not contain the <transaction-type> for this bean.
     * The annotation value should be used.
     *
     * Verify that the bean is a bean-managed transaction bean by calling a
     * method in the bean that simply does a userTran.begin() and a
     * userTran.commit().
     */
    @Test
    public void testIsBMTTransactionTypeNotSpecified() throws Exception {
        assertNotNull("Local bean, bean, not null", bean);
        bean.bmtMethod();
        svLogger.info("The bmtMethod ran successfully. This verifies that the annotation worked to declare this bean a BMT bean.");
    }

    /**
     * Remote version.
     *
     * The bean, BMTNoXMLTranType, uses the TransactionManagement Annotation
     * with a value = (BEAN) to declare itself a bean-managed transaction bean.
     * The ejb-jar.xml does not contain the <transaction-type> for this bean.
     * The annotation value should be used.
     *
     * Verify that the bean is a bean-managed transaction bean by calling a
     * method in the bean that simply does a userTran.begin() and a
     * userTran.commit().
     */
    @Test
    public void testIsBMTTransactionTypeNotSpecifiedRemote() throws Exception {
        assertNotNull("Remote bean, rbean, not null", rbean);
        rbean.bmtMethod();
        svLogger.info("The bmtMethod ran successfully for the remote version of this test. This verifies that the annotation worked to declare this bean a BMT bean.");
    }
}
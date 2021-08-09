/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.repeatable.transaction.web;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

@Resource(name = "com.ibm.ws.injection.repeatable.transaction.web.BasicRepeatableUserTransactionServlet/JNDI_Ann_UserTransaction", type = javax.transaction.UserTransaction.class)
@WebServlet("/BasicRepeatableUserTransactionServlet")
public class BasicRepeatableUserTransactionServlet extends FATServlet {
    private static final String CLASS_NAME = BasicRepeatableUserTransactionServlet.class.getName();
    private static final long serialVersionUID = 1L;

    // Annotation targets
    @Resource
    UserTransaction UserTranFldAnn;
    UserTransaction UserTranMthdAnn;

    // XML injection targets
    UserTransaction UserTranFldXML;
    UserTransaction UserTranMthdXML;

    @Resource
    public void setUserTranMthdAnnMethod(UserTransaction utx) {
        UserTranMthdAnn = utx;
    }

    public void setUserTranMthdXMLMethod(UserTransaction utx) {
        UserTranMthdXML = utx;
    }

    /**
     * Test injection of a User Transaction into a field via annotation
     */
    @Test
    public void testRepeatableUserTransactionFldAnnInjection() {
        RepeatableTransactionTestHelper.testRepeatableUserTransaction(UserTranFldAnn, "UserTranMthdXML");
    }

    /**
     * Test injection of a User Transaction via an annotated setter method
     */
    @Test
    public void testRepeatableUserTransactionMthdAnnInjection() {
        RepeatableTransactionTestHelper.testRepeatableUserTransaction(UserTranFldXML, "UserTranFldXML");
    }

    /**
     * Test injection of a User Transaction into a field via XML
     */
    @Test
    public void testRepeatableUserTransactionFldXMLInjection() {
        RepeatableTransactionTestHelper.testRepeatableUserTransaction(UserTranMthdAnn, "UserTranMthdAnn");
        RepeatableTransactionTestHelper.testJNDILookup(CLASS_NAME + "/UserTranFldXML");
    }

    /**
     * Test injection of a User Transaction via a setter method with XML
     */
    @Test
    public void testRepeatableUserTransactionMthdXMLInjection() {
        RepeatableTransactionTestHelper.testRepeatableUserTransaction(UserTranMthdXML, "UserTranMthdXML");
        RepeatableTransactionTestHelper.testJNDILookup(CLASS_NAME + "/UserTranMthdXML");
    }

    /**
     * Test declaration of class-level @Resource annotation to ensure it
     * correctly creates a JNDI resource
     */
    @Test
    public void testRepeatableUserTransactionClassLevelResourceInjection() {
        RepeatableTransactionTestHelper.testJNDILookup(CLASS_NAME + "/JNDI_Ann_UserTransaction");
    }
}
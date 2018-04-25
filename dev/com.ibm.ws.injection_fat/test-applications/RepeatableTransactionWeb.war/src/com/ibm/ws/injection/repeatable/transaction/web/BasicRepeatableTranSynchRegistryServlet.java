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
import javax.transaction.TransactionSynchronizationRegistry;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@Resource(name = "com.ibm.ws.injection.repeatable.transaction.web.BasicRepeatableTranSynchRegistryServlet/JNDI_Class_Ann_TranSynchReg", type = TransactionSynchronizationRegistry.class)
@WebServlet("/BasicRepeatableTranSynchRegistryServlet")
public class BasicRepeatableTranSynchRegistryServlet extends FATServlet {
    private static final String CLASS_NAME = BasicRepeatableTranSynchRegistryServlet.class.getName();

    // Annotation injection targets
    @Resource
    TransactionSynchronizationRegistry TranSynchRegFldAnn;
    TransactionSynchronizationRegistry TranSynchRegMthdAnn;

    // XML injection targets
    TransactionSynchronizationRegistry TranSynchRegFldXML;
    TransactionSynchronizationRegistry TranSynchRegMthdXML;

    @Resource
    public void setTranSynchRegMthdAnnMethod(TransactionSynchronizationRegistry tsr) {
        TranSynchRegMthdAnn = tsr;
    }

    public void setTranSynchRegMthdXMLMethod(TransactionSynchronizationRegistry tsr) {
        TranSynchRegMthdXML = tsr;
    }

    /**
     * Test injection of a TranSynchRegistry into a field via annotation
     */
    @Test
    public void testRepeatableTranSynchRegistryFldAnnInjection() {
        RepeatableTransactionTestHelper.testRepeatableTranSynchRegistry(TranSynchRegFldAnn, "TranSynchRegFldAnn");
    }

    /**
     * Test injection of a TranSynchRegistry via an annotated setter method
     */
    @Test
    public void testRepeatableTranSynchRegistryMthdAnnInjection() {
        RepeatableTransactionTestHelper.testRepeatableTranSynchRegistry(TranSynchRegMthdAnn, "TranSynchRegMthdAnn");
    }

    /**
     * Test injection of a TranSynchRegistry into a field via XML
     */
    @Test
    public void testRepeatableTranSynchRegistryFldXMLInjection() {
        RepeatableTransactionTestHelper.testRepeatableTranSynchRegistry(TranSynchRegFldXML, "TranSynchRegFldXML");
        RepeatableTransactionTestHelper.testJNDILookup(CLASS_NAME + "/TranSynchRegFldXML");
    }

    /**
     * Test injection of a TranSynchRegistry via a setter method with XML
     */
    @Test
    public void testRepeatableTranSynchRegistryMthdXMLInjection() {
        RepeatableTransactionTestHelper.testRepeatableTranSynchRegistry(TranSynchRegMthdXML, "TranSynchRegMthdXML");
        RepeatableTransactionTestHelper.testJNDILookup(CLASS_NAME + "/TranSynchRegMthdXML");
    }

    /**
     * Test declaration of class-level @Resource annotation to ensure it
     * correctly creates a JNDI resource
     */
    @Test
    public void testRepeatableTranSynchRegistyClassLevelResourceInjection() {
        RepeatableTransactionTestHelper.testJNDILookup(CLASS_NAME + "/JNDI_Class_Ann_TranSynchReg");
    }
}
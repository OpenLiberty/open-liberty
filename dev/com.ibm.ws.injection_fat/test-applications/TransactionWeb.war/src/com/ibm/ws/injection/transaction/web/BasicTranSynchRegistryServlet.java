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
package com.ibm.ws.injection.transaction.web;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.transaction.TransactionSynchronizationRegistry;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@Resource(name = "com.ibm.ws.injection.transaction.web.BasicTranSynchRegistryServlet/JNDI_Class_Ann_TranSynchReg", type = TransactionSynchronizationRegistry.class)
@WebServlet("/BasicTranSynchRegistryServlet")
public class BasicTranSynchRegistryServlet extends FATServlet {
    private static final String CLASS_NAME = BasicTranSynchRegistryServlet.class.getName();

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
    public void testTranSynchRegistryFldAnnInjection() {
        TransactionTestHelper.testTranSynchRegistry(TranSynchRegFldAnn, "TranSynchRegFldAnn");
    }

    /**
     * Test injection of a TranSynchRegistry via an annotated setter method
     */
    @Test
    public void testTranSynchRegistryMthdAnnInjection() {
        TransactionTestHelper.testTranSynchRegistry(TranSynchRegMthdAnn, "TranSynchRegMthdAnn");
    }

    /**
     * Test injection of a TranSynchRegistry into a field via XML
     */
    @Test
    public void testTranSynchRegistryFldXMLInjection() {
        TransactionTestHelper.testTranSynchRegistry(TranSynchRegFldXML, "TranSynchRegFldXML");
        TransactionTestHelper.testJNDILookup(CLASS_NAME + "/TranSynchRegFldXML");
    }

    /**
     * Test injection of a TranSynchRegistry via a setter method with XML
     */
    @Test
    public void testTranSynchRegistryMthdXMLInjection() {
        TransactionTestHelper.testTranSynchRegistry(TranSynchRegMthdXML, "TranSynchRegMthdXML");
        TransactionTestHelper.testJNDILookup(CLASS_NAME + "/TranSynchRegMthdXML");
    }

    /**
     * Test declaration of class-level @Resource annotation to ensure it
     * correctly creates a JNDI resource
     */
    @Test
    public void testTranSynchRegistyClassLevelResourceInjection() {
        TransactionTestHelper.testJNDILookup(CLASS_NAME + "/JNDI_Class_Ann_TranSynchReg");
    }
}
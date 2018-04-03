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

import java.util.HashMap;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

@Resources(value = {
                     @Resource(name = "com.ibm.ws.injection.transaction.web.AdvTransactionServletContextAttributeListener/JNDI_Class_Ann_UserTransaction",
                               type = javax.transaction.UserTransaction.class),
                     @Resource(name = "com.ibm.ws.injection.transaction.web.AdvTransactionServletContextAttributeListener/JNDI_Class_Ann_TranSynchReg",
                               type = TransactionSynchronizationRegistry.class)
})
public class AdvTransactionServletContextAttributeListener implements ServletContextAttributeListener {
    private static final String CLASS_NAME = AdvTransactionServletContextAttributeListener.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    // A map of the UserTransactions and TransactionSynchronizationRegistries to be tested
    HashMap<String, Object> map;

    // The JNDI Names that will need to be looked up
    String[] JNDI_NAMES = {
                            CLASS_NAME + "/UserTranFldXML",
                            CLASS_NAME + "/UserTranMthdXML",
                            CLASS_NAME + "/TranSynchRegFldXML",
                            CLASS_NAME + "/TranSynchRegMthdXML",
                            CLASS_NAME + "/JNDI_Class_Ann_UserTransaction",
                            CLASS_NAME + "/JNDI_Class_Ann_TranSynchReg",
    };

    // Annotation targets
    @Resource
    UserTransaction UserTranFldAnn;
    UserTransaction UserTranMthdAnn;

    @Resource
    TransactionSynchronizationRegistry TranSynchRegFldAnn;
    TransactionSynchronizationRegistry TranSynchRegMthdAnn;

    // XML targets
    UserTransaction UserTranFldXML;
    UserTransaction UserTranMthdXML;

    TransactionSynchronizationRegistry TranSynchRegFldXML;
    TransactionSynchronizationRegistry TranSynchRegMthdXML;

    public AdvTransactionServletContextAttributeListener() {
        map = new HashMap<String, Object>();
    }

    @Override
    public void attributeAdded(ServletContextAttributeEvent arg0) {
        svLogger.info("AdvTransactionHttpContextAtrributeListener: Attribute added");
        doWork(WCEventTracker.KEY_LISTENER_ADD_AdvTransactionContextAttributeListener);
    }

    @Override
    public void attributeRemoved(ServletContextAttributeEvent arg0) {
        svLogger.info("AdvTransactionHttpContextAtrributeListener: Attribute Removed");
        doWork(WCEventTracker.KEY_LISTENER_DEL_AdvTransactionContextAttributeListener);
    }

    @Override
    public void attributeReplaced(ServletContextAttributeEvent arg0) {
        svLogger.info("AdvTransactionHttpContextAtrributeListener: Attribute Replaced");
        doWork(WCEventTracker.KEY_LISTENER_REP_AdvTransactionContextAttributeListener);
    }

    /**
     * This will populate the map of object to test. It
     * will then send that map off to be tested. Lastly, the list JNDI names are
     * handed of to the test framework to ensure they can be looked up and
     * tested.
     */
    public void doWork(String key) {
        populateMap();
        TransactionTestHelper.processRequest(key, map);
        TransactionTestHelper.testJNDILookupWrapper(key, JNDI_NAMES);
    }

    /**
     * Clear the map to avoid duplicates. Then add all the object that are to be
     * tested
     */
    public void populateMap() {
        map.clear();
        map.put("UserTranFldAnn", UserTranFldAnn);
        map.put("UserTranMthdAnn", UserTranMthdAnn);
        map.put("UserTranFldXML", UserTranFldXML);
        map.put("UserTranMthdXML", UserTranMthdXML);

        map.put("TranSynchRegFldAnn", TranSynchRegFldAnn);
        map.put("TranSynchRegMthdAnn", TranSynchRegMthdAnn);
        map.put("TranSynchRegFldXML", TranSynchRegFldXML);
        map.put("TranSynchRegMthdXML", TranSynchRegMthdXML);
    }

    @Resource
    public void setUserTranMthdAnnMethod(UserTransaction userTranMthdAnn) {
        UserTranMthdAnn = userTranMthdAnn;
    }

    public void setUserTranMthdXMLMethod(UserTransaction userTranMthdXML) {
        UserTranMthdXML = userTranMthdXML;
    }

    @Resource
    public void setTranSynchRegMthdAnnMethod(TransactionSynchronizationRegistry tranSynchRegMthdAnn) {
        TranSynchRegMthdAnn = tranSynchRegMthdAnn;
    }

    public void setTranSynchRegMthdXMLMethod(TransactionSynchronizationRegistry tranSynchRegMthdXML) {
        TranSynchRegMthdXML = tranSynchRegMthdXML;
    }
}
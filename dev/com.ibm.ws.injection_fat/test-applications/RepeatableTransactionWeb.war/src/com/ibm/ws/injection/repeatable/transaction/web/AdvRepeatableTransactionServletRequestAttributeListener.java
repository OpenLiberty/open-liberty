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

import java.util.HashMap;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

@Resource(name = "com.ibm.ws.injection.repeatable.transaction.web.AdvRepeatableTransactionServletRequestAttributeListener/JNDI_Class_Ann_UserTransaction",
          type = javax.transaction.UserTransaction.class)
@Resource(name = "com.ibm.ws.injection.repeatable.transaction.web.AdvRepeatableTransactionServletRequestAttributeListener/JNDI_Class_Ann_TranSynchReg",
          type = TransactionSynchronizationRegistry.class)
public class AdvRepeatableTransactionServletRequestAttributeListener implements ServletRequestAttributeListener {
    private static final String CLASS_NAME = AdvRepeatableTransactionServletRequestAttributeListener.class.getName();
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

    public AdvRepeatableTransactionServletRequestAttributeListener() {
        map = new HashMap<String, Object>();
    }

    @Override
    public void attributeAdded(ServletRequestAttributeEvent sre) {
        svLogger.info("AdvRepeatableTransactionServletRequestAtrributeListener: Attribute added");
        doWork(WCEventTracker.KEY_LISTENER_ADD_AdvRepeatableTransactionServletRequestAttributeListener);
    }

    @Override
    public void attributeRemoved(ServletRequestAttributeEvent sre) {
        svLogger.info("AdvRepeatableTransactionServletRequestAtrributeListener: Attribute Removed");
        doWork(WCEventTracker.KEY_LISTENER_DEL_AdvRepeatableTransactionServletRequestAttributeListener);
    }

    @Override
    public void attributeReplaced(ServletRequestAttributeEvent sre) {
        svLogger.info("AdvRepeatableTransactionServletRequestAtrributeListener: Attribute Replaced");
        doWork(WCEventTracker.KEY_LISTENER_REP_AdvRepeatableTransactionServletRequestAttributeListener);
    }

    /**
     * This will populate the map of object to test. It
     * will then send that map off to be tested. Lastly, the list JNDI names are
     * handed of to the test framework to ensure they can be looked up and
     * tested.
     */
    public void doWork(String key) {
        populateMap();
        RepeatableTransactionTestHelper.processRequest(key, map);
        RepeatableTransactionTestHelper.testJNDILookupWrapper(key, JNDI_NAMES);
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
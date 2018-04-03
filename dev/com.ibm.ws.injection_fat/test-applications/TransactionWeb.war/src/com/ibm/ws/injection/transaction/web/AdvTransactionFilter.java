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

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

@Resources(
           value = {
                     @Resource(name = "com.ibm.ws.injection.transaction.web.AdvTransactionFilter/JNDI_Class_Ann_UserTransaction",
                               type = javax.transaction.UserTransaction.class),
                     @Resource(name = "com.ibm.ws.injection.transaction.web.AdvTransactionFilter/JNDI_Class_Ann_TranSynchReg", type = TransactionSynchronizationRegistry.class)
           })
public class AdvTransactionFilter implements Filter {
    private static final String CLASS_NAME = AdvTransactionFilter.class.getName();
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

    // UserTransaction setter methods
    @Resource
    public void setUserTranMthdAnnMethod(UserTransaction userTranMthdAnn) {
        UserTranMthdAnn = userTranMthdAnn;
    }

    public void setUserTranMthdXMLMethod(UserTransaction userTranMthdXML) {
        UserTranMthdXML = userTranMthdXML;
    }

    // TransactionSynchronizationRegistry setter methods
    @Resource
    public void setTranSynchRegMthdAnnMethod(TransactionSynchronizationRegistry tranSynchRegMthdAnn) {
        TranSynchRegMthdAnn = tranSynchRegMthdAnn;
    }

    public void setTranSynchRegMthdXMLMethod(TransactionSynchronizationRegistry tranSynchRegMthdXML) {
        TranSynchRegMthdXML = tranSynchRegMthdXML;
    }

    // Constructor
    public AdvTransactionFilter() {
        map = new HashMap<String, Object>();
    }

    /* Filter Methods */
    @Override
    public void destroy() {}

    @Override
    public void init(FilterConfig fc) throws ServletException {}

    /**
     * Inherited from Filter. This will populate the map of object to test. It
     * will then send that map off to be tested. Lastly, the list JNDI names are
     * handed of to the test framework to ensure they can be looked up and
     * tested.
     */
    @Override
    public void doFilter(ServletRequest sr, ServletResponse sp, FilterChain fc) throws IOException, ServletException {
        String key = WCEventTracker.KEY_FILTER_DOFILTER_AdvTransactionFilter;
        if (sr.getParameter("testMethod").equals("testTransactionServletFilter")) {
            svLogger.info("Testing in doFilter...");
            populateMap();
            TransactionTestHelper.processRequest(key, map);
            TransactionTestHelper.testJNDILookupWrapper(key, JNDI_NAMES);
        }

        fc.doFilter(sr, sp);
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
}
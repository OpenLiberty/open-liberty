/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class PassivationTracker {
    private final static String CLASSNAME = PassivationTracker.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    public static final String[] SF_EMPTY_EXPECTED_RESULTS_REMOTE = new String[] {
                                                                                   "StatefulEmptyBean.postConstruct",
                                                                                   "CLEmptyInterceptor.prePassivate",
                                                                                   "StatefulEmptyBean.passivate",
                                                                                   "CLEmptyInterceptor.postActivate",
                                                                                   "StatefulEmptyBean.activate",
                                                                                   "CLEmptyInterceptor.aroundInvoke:Remote",
                                                                                   "CLEmptyInterceptor.prePassivate",
                                                                                   "StatefulEmptyBean.passivate",
                                                                                   "CLEmptyInterceptor.postActivate",
                                                                                   "StatefulEmptyBean.activate",
                                                                                   "CLEmptyInterceptor.aroundInvoke:Remote" };

    public static final String[] SF_EMPTY_EXPECTED_RESULTS_LOCAL = new String[] {
                                                                                  "StatefulEmptyBean.postConstruct",
                                                                                  "CLEmptyInterceptor.prePassivate",
                                                                                  "StatefulEmptyBean.passivate",
                                                                                  "CLEmptyInterceptor.postActivate",
                                                                                  "StatefulEmptyBean.activate",
                                                                                  "CLEmptyInterceptor.aroundInvoke:Local",
                                                                                  "CLEmptyInterceptor.prePassivate",
                                                                                  "StatefulEmptyBean.passivate",
                                                                                  "CLEmptyInterceptor.postActivate",
                                                                                  "StatefulEmptyBean.activate",
                                                                                  "CLEmptyInterceptor.aroundInvoke:Local" };

    private static ArrayList<String> messageTracker = new ArrayList<String>();

    public synchronized static void clearAll() {
        messageTracker.clear();
    }

    public synchronized static void addMessage(String message) {
        svLogger.info("PassivationTracker.addMessage: " + message);
        messageTracker.add(message);
    }

    public synchronized static String[] getMessages() {
        String[] strArr = new String[messageTracker.size()];
        messageTracker.toArray(strArr);
        return strArr;
    }

    public synchronized static void compareMessages(String[] expMessages) {
        List<String> expectedList = Arrays.asList(expMessages);
        svLogger.info("expected event sequence: " + expectedList);
        svLogger.info("actual   event sequence: " + messageTracker);
        assertEquals("3 ---> method1 around invoke", expectedList, messageTracker);
    }
}
/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mdb.jms.xml.ejb;

import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;

import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.TransactionAttribute;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

public class SLRaBean implements SessionBean {
    private final static String CLASSNAME = SLRaBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final long serialVersionUID = 1181647011303722826L;
    private SessionContext mySessionCtx;
    final static String BeanName = "SLRaBean";

    String sendQueueName = "java:comp/env/jms/CommonDestinationQ";
    String qcfName = "java:comp/env/jms/TestQCF";

    String tcfName = "jms/TestTCF";
    String commonDestinationTopic = "jms/CommonDestinationTopic";

    /**
     * getSessionContext
     */
    public SessionContext getSessionContext() {
        printMsg(BeanName, "(getSessionContext)");
        return mySessionCtx;
    }

    /**
     * setSessionContext
     */
    @Override
    public void setSessionContext(SessionContext ctx) {
        printMsg(BeanName, "(setSessionContext)");
        mySessionCtx = ctx;
    }

    /**
     * ejbCreate
     */
    public void ejbCreate() throws CreateException {
        printMsg(BeanName, "(ejbCreate)");
    }

    /**
     * ejbActivate
     */
    @Override
    public void ejbActivate() {
        printMsg(BeanName, "(ejbActivate)");
    }

    /**
     * ejbPassivate
     */
    @Override
    public void ejbPassivate() {
        printMsg(BeanName, "(ejbPassivate)");
    }

    /**
     * ejbRemove
     */
    @Override
    public void ejbRemove() {
        printMsg(BeanName, "(ejbRemove)");
    }

    /**
     * Insert the method's description here.
     */
    public void printMsg(String beanName, String msg) {
        svLogger.info("       " + beanName + " : " + msg);
    }

    /**
     * method1
     */
    @TransactionAttribute(NOT_SUPPORTED)
    public String method1(String arg1) {
        printMsg(BeanName, "----->method1 arg = " + arg1);
        return arg1;
    }

    // new for message-destination-ref
    public void sendMessage(String message) throws Exception {
        svLogger.info("SLRaBean.sendMessage: sending message: " + message);
        FATMDBHelper.putQueueMessage(message, qcfName, sendQueueName);
        svLogger.info("SLRaBean Message: " + message + " sent.");
    }

    public void putTopicMessage(String message1, String message2) throws Exception {
        svLogger.info("SLRaBean.putTopicMessage: put message: " + message1 + message2);
        FATMDBHelper.putTopicMessage(message1, message2, tcfName, commonDestinationTopic);
        svLogger.info("SLRaBean Message: " + message1 + message2 + " sent.");
    }
}
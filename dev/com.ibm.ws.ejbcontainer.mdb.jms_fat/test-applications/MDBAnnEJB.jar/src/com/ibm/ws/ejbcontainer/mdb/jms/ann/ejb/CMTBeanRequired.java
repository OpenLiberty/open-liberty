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
package com.ibm.ws.ejbcontainer.mdb.jms.ann.ejb;

import static javax.annotation.Resource.AuthenticationType.APPLICATION;
import static javax.ejb.TransactionAttributeType.REQUIRED;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.ejb.TransactionAttribute;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;
import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * This is a Message Driven Bean that on receipt of a message writes that message to a Queue
 */

// add annotation for activationConfig props
@MessageDriven(activationConfig = {
                                    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                                    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
                                    @ActivationConfigProperty(propertyName = "destination", propertyValue = "CMTRequiredReqQueue")
},
               name = "CMTBeanRequired")
public class CMTBeanRequired implements MessageListener {
    private final static String CLASSNAME = CMTBeanRequired.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @Resource
    private MessageDrivenContext myMessageDrivenCtx;

    // d459309, add authenticationType=APPLICATION
    @Resource(name = "jms/TestQCF", authenticationType = APPLICATION, shareable = true)
    private QueueConnectionFactory replyQueueFactory;

    @Resource(name = "jms/TestResultQueue")
    private Queue replyQueue;

    @EJB
    private SLLa slla;

    private final String jndiSFL = "java:global/MDBAnnApp/MDBAnnEJB/SFLBean!com.ibm.ws.ejbcontainer.mdb.jms.ann.ejb.SFLocal";

    final static String BeanName = "CMTBeanRequired";

    public static SFLocal commitBean;
    public static SFLocal rollbackBean;

    // Test points
    String results = null;
    static int svCount = 0;

    /**
     * The onMessage method extracts the text and message id of the message and print the text to the
     * Application Server standard out and calls put message with the message id and text.
     *
     * @param msg This should be a TextMessage.
     */
    @Override
    @TransactionAttribute(REQUIRED)
    public void onMessage(Message msg) {
        String text = null;
        String messageID = null;

        results = "";

        try {
            text = ((TextMessage) msg).getText();

            svLogger.info("senderBean.onMessage(), msg text ->: " + text);

            if (text.equalsIgnoreCase("CMT COMMIT")) {
                testRequiredTx("onMessage()");
                testSLLaObjectAccess();
                ctx_getRollbackOnly("onMessage()");
                testCMTTxCommit();

                messageID = msg.getJMSMessageID();
                svLogger.info("Message ID :" + messageID);
                FATMDBHelper.putQueueMessage(results, replyQueueFactory, replyQueue);
                svLogger.info("Test results are sent.");
            } else if (text.equalsIgnoreCase("CMT ROLLBACK")) {
                testCMTTxRollback();
                FATMDBHelper.putQueueMessage(results, replyQueueFactory, replyQueue);
                svLogger.info("Test results are sent.");
            } else {
                svLogger.info("*Error : Received unknown message -> " + text);
            }
        } catch (Exception e) {
            svLogger.info("Caught exception: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * To verify the thread is associated with a global transaction when the transaction attribute of onMessage() is 'Required'
     *
     * @param results test point results
     * @param invokeLoc location MDB is invoked from
     */
    public void testRequiredTx(String invokeLoc) {
        try {
            if (FATTransactionHelper.isTransactionGlobal()) {
                results = results + " Thread is associated with a global transaction correctly which is called by " + invokeLoc + " in CMT MDB. ";
            } else {
                results = results + " FAIL: Thread is not be associated with a global transaction ";
            }
        } catch (Exception ise) {
            results = results + " FAIL: IllegalStateException is generated when " + invokeLoc + " in MDB: " + ise.toString() + ". ";
        }
    }

    /**
     * onMsg with Tx attribute 'Required' access a CMTD SLL with T attribute 'supports', check the Tx context is passed to SL
     */
    public void testSLLaObjectAccess() {
        try {
            byte[] SLLaTXID = slla.method2("testSLLaObjectAccess");

            if (FATTransactionHelper.isSameTransactionId(SLLaTXID)) {
                results = results + " testSLLaObjectAccess passed. ";
            } else {
                results = results + " FAIL: testSLLaObjectAccess failed. ";
            }
        } catch (Exception e) {
            results = results + " FAIL: Exception when accessing SLLaObject: " + e.toString() + ". ";
        }
    }

    /**
     * Execute getRollbackOnly
     *
     * @param results test point results
     * @param invokeLoc location MDB is invoked from
     */
    public void ctx_getRollbackOnly(String invokeLoc) {
        try {
            myMessageDrivenCtx.getRollbackOnly();
            results = results + " CMTD MDB can access getRollbackOnly() when " + invokeLoc + ". ";
        } catch (IllegalStateException ise) {
            results = results + " FAIL: IllegalStateException should not be generated when " + invokeLoc + "in CMTD MDB accesses getRollbackOnly() ";
        } catch (Throwable e) {
            results = results + " FAIL: Unexpected Exception when invoking getRollbackOnly from " + invokeLoc + " in CMT MDB: " + e.toString() + ". ";
        }
    }

    /**
     * Access TxSyncSFBean to begin transaction
     *
     */
    public void testCMTTxCommit() {
        try {
            svLogger.info(" testCMTTxCommit looking up local bean ...");
            commitBean = (SFLocal) new InitialContext().lookup(jndiSFL);
            svLogger.info(" done");
        } catch (Exception ne) {
            results = results + " FAIL: Unable to lookup bean local for " + jndiSFL + ": " + ne.toString() + ". ";
            return;
        }

        try {
            commitBean.setIntValue(0);
            commitBean.incrementInt();
        } catch (Throwable t) {
            results = results + " FAIL: Unexpected exception while manipulating SF: " + t.toString() + ". ";
        }
    }

    /**
     * Access TxSyncSFBean to begin transaction. Then call setRollbackOnly
     *
     */
    public void testCMTTxRollback() {
        try {
            svLogger.info(" testCMTTxRollback looking up local bean ...");
            rollbackBean = (SFLocal) new InitialContext().lookup(jndiSFL);
            svLogger.info(" done");
        } catch (Exception ne) {
            results = results + " FAIL: Unable to lookup bean local for " + jndiSFL + ": " + ne.toString() + ". ";
            return;
        }

        try {
            if (svCount == 0) {
                rollbackBean.setIntValue(0);
                rollbackBean.incrementInt();
                myMessageDrivenCtx.setRollbackOnly();
                svCount++;
                results = results + " FAIL: Message should not be received as transaction was rolled back. ";
            } else {
                results = results + " Message should be received as transaction was not rolled back. ";
            }
        } catch (Throwable t) {
            results = results + " FAIL: Unexpected exception while manipulating SF: " + t.toString() + ". ";
        }
    }
}
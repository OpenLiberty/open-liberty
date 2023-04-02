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
package com.ibm.ws.ejbcontainer.mdb.jms.ejb21;

import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;
import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * This is a Message Driven Bean that on receipt of a message writes that message to a Queue
 */
@SuppressWarnings("serial")
public class CMTBeanRequired implements MessageDrivenBean, MessageListener {
    private final static String CLASSNAME = CMTBeanRequired.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private MessageDrivenContext myMessageDrivenCtx = null;

    private final String replyQueueFactoryName = "jms/TestQCF";
    private final String replyQueueName = "jms/TestResultQueue";

    // JNDI for session beans
    private static final String ejbJndiName1 = "java:global/MDB21TestApp/MDB21TestEJB/MDBSLL!com.ibm.ws.ejbcontainer.mdb.jms.ejb21.SLLaHome";
    private final String jndiSFLocalHome = "java:global/MDB21TestApp/MDB21TestEJB/MDBSF!com.ibm.ws.ejbcontainer.mdb.jms.ejb21.SFLocalHome";

    final static String BeanName = "CMTBeanRequired";

    public static SFLocal commitBean;
    public static SFLocal rollbackBean;

    //Test points
    String results = null;
    static int svCount = 0;

    /**
     * This method is called when the Message Driven Bean is created. It currently does nothing.
     *
     * @exception javax.ejb.CreateException
     * @exception javax.ejb.EJBException
     */
    public void ejbCreate() throws CreateException {
    }

    /**
     * This method is called when the Message Driven Bean is removed from the server.
     *
     * @exception javax.ejb.EJBException
     */
    @Override
    public void ejbRemove() {
    }

    /**
     * This method returns the MessageDrivenContext for this Message Driven Bean. The object returned
     * is the same object that is passed in when setMessageDrivenContext is called
     *
     * @return javax.ejb.MessageDrivenContext
     */
    public MessageDrivenContext getMessageDrivenContext() {
        return myMessageDrivenCtx;
    }

    /**
     * This message stores the MessageDrivenContext in case it is needed later, or the getMessageDrivenContext
     * method is called.
     *
     * @param ctx javax.ejb.MessageDrivenContext
     * @exception javax.ejb.EJBException The exception description.
     */
    @Override
    public void setMessageDrivenContext(MessageDrivenContext ctx) {
        myMessageDrivenCtx = ctx;
    }

    /**
     * The onMessage method extracts the text and message id of the message and print the text to the
     * Application Server standard out and calls put message with the message id and text.
     *
     * @param msg This should be a TextMessage.
     */
    @Override
    public void onMessage(Message msg) {
        String text = null;
        String messageID = null;

        results = "";

        try {
            text = ((TextMessage) msg).getText();

            svLogger.info("senderBean.onMessage(), msg text ->: " + text);

            if (text.equalsIgnoreCase("CMT COMMIT")) {
                testRequiredTx("onMessage()");
                testSLLaObjectAccess(ejbJndiName1);
                ctx_getRollbackOnly("onMessage()");
                testCMTTxCommit();

                messageID = msg.getJMSMessageID();
                svLogger.info("Message ID :" + messageID);
                FATMDBHelper.putQueueMessage(results, replyQueueFactoryName, replyQueueName);
                svLogger.info("Test results are sent.");
            } else if (text.equalsIgnoreCase("CMT ROLLBACK")) {
                testCMTTxRollback();
                FATMDBHelper.putQueueMessage(results, replyQueueFactoryName, replyQueueName);
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
     *
     * @param jndiName JNDI Name of Local Home
     */
    public void testSLLaObjectAccess(String jndiName) {
        SLLaHome fhome1 = null;
        SLLa fejb1 = null;

        try {
            fhome1 = (SLLaHome) new InitialContext().lookup(jndiName);
            fejb1 = fhome1.create();

            byte[] SLLaTXID = fejb1.method2("testSLLaObjectAccess");

            if (FATTransactionHelper.isSameTransactionId(SLLaTXID)) {
                results = results + " testSLLaObjectAccess passed. ";
            } else {
                results = results + " FAIL: testSLLaObjectAccess failed. ";
            }
        } catch (Exception e) {
            results = results + " FAIL: Exception when accessing SLLaHome JNDI: " + e.toString() + ". ";
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
     * Access CMP's incrementInt() to update the database
     *
     */
    public void testCMTTxCommit() {
        SFLocalHome fhome1;

        try {
            svLogger.info(" testCMTTxCommit looking up local home ...");
            fhome1 = (SFLocalHome) new InitialContext().lookup(jndiSFLocalHome);
            svLogger.info("create - started.");
            commitBean = fhome1.create();
            svLogger.info("create - ended.");
            commitBean.setIntValue(0);
            commitBean.incrementInt();
        } catch (Throwable t) {
            results = results + " FAIL: Unexpected exception while manipulating SF: " + t.toString() + ". ";
        }
    }

    /**
     * Access CMP's incrementInt() to update the database. Then call setRollbackOnly
     *
     */
    public void testCMTTxRollback() {
        SFLocalHome fhome1;

        try {
            svLogger.info(" testCMTTxRollback looking up local home ...");
            fhome1 = (SFLocalHome) new InitialContext().lookup(jndiSFLocalHome);
            svLogger.info("create - started.");
            rollbackBean = fhome1.create();
            svLogger.info("create - ended.");

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
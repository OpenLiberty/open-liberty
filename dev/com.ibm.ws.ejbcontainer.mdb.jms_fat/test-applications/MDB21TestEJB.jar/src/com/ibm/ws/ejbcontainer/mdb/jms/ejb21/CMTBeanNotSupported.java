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
import javax.rmi.PortableRemoteObject;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;
import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * This is a Message Driven Bean that on receipt of a message writes that message to a Queue
 */
@SuppressWarnings("serial")
public class CMTBeanNotSupported implements MessageDrivenBean, MessageListener {
    private final static String CLASSNAME = CMTBeanNotSupported.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private MessageDrivenContext myMessageDrivenCtx = null;

    private final String replyQueueFactoryName = "jms/TestQCF";
    private final String replyQueueName = "jms/TestResultQueue";

    // JNDI for session beans
    private final static String ejbJndiName1 = "java:global/MDB21TestApp/MDB21TestEJB/MDBSLL!com.ibm.ws.ejbcontainer.mdb.jms.ejb21.SLLaHome";
    private static final String ejbJndiName2 = "java:global/MDB21TestApp/MDB21TestEJB/MDBSLR!com.ibm.ws.ejbcontainer.mdb.jms.ejb21.SLRaHome";

    final static String BeanName = "CMTBeanNotSupported";

    String results = "";

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

        // Performing test points

        testNotSupportedTx("onMessage()");
        testSLLaObjectAccess(ejbJndiName1);
        testSLRaHomeAccess(ejbJndiName2);
        testSLRaObjectAccess(ejbJndiName2);

        ctx_setRollbackOnly("onMessage()");
        ctx_getRollbackOnly("onMessage()");

        try {
            text = ((TextMessage) msg).getText();
            svLogger.info("senderBean.onMessage(), msg text ->: " + text);

            //
            // store the message id to use as the Correlator value
            //
            messageID = msg.getJMSMessageID();
            svLogger.info("Message ID :" + messageID);
            FATMDBHelper.putQueueMessage(results, replyQueueFactoryName, replyQueueName);
            svLogger.info("Test results are sent.");
        } catch (Exception e) {
            svLogger.info("Caught exception: " + e.toString());
            e.printStackTrace();
        }

        return;
    }

    /**
     * To verify the thread is not associated with a global transaction when the transaction attribute of onMessage() is 'Not Supported'
     *
     * @param results test point results
     * @param invokeLoc location MDB is invoked from
     */
    public void testNotSupportedTx(String invokeLoc) {
        svLogger.info(BeanName + "-----> testNotSupportedTx starts.");
        if (!FATTransactionHelper.isTransactionGlobal()) {
            results = results + " Thread is not associated with a global transaction correctly which is called by " + invokeLoc + " in CMT MDB. ";
        } else {
            results = results + " FAIL: Thread should not be associated with a global transaction ";
        }
    }

    /**
     * onMsg with Tx attribute 'NotSupported' access a CMTD SLL with T attribute 'supports', check no Tx context is passed to SL
     */
    public void testSLLaObjectAccess(String jndiName) {
        SLLaHome fhome1 = null;
        SLLa fejb1 = null;

        try {
            fhome1 = (SLLaHome) (new InitialContext().lookup(jndiName));
            fejb1 = fhome1.create();

            byte[] SLLaTXID = fejb1.method2("testSLLaObjectAccess");

            if (!FATTransactionHelper.isSameTransactionId(SLLaTXID)) {
                results = results + " testSLLaObjectAccess passed. ";
            } else {
                results = results + " FAIL: testSLLaObjectAccess failed. ";
            }
        } catch (Exception e) {
            results = results + " FAIL: Exception when accessing SLLaHome JNDI: " + e.toString() + ". ";
        }
    }

    /**
     * Access SL Remote interface from MDB
     *
     * @param jndiName JNDI Name of the SL Remote home
     */
    public void testSLRaHomeAccess(String jndiName) {
        try {
            SLRaHome fhome1 = (SLRaHome) PortableRemoteObject.narrow(new InitialContext().lookup(jndiName), SLRaHome.class);
            if (fhome1 == null) {
                results = results + " FAIL: The SLRaHome JNDI resource is not accessed correctly. ";
            } else {
                results = results + " The SLRaHome JNDI resource is accessed correctly. ";
            }
        } catch (Exception e) {
            results = results + " FAIL: Exception when accessing SLRaHome JNDI: " + e.toString() + ". ";
        }
    }

    /**
     * Access SL remote interface from MDB and access the methods of SL
     */
    public void testSLRaObjectAccess(String jndiName) {
        SLRaHome fhome1 = null;
        SLRa fejb1 = null;

        try {
            fhome1 = (SLRaHome) PortableRemoteObject.narrow(new InitialContext().lookup(jndiName), SLRaHome.class);
            fejb1 = fhome1.create();

            String testStr = "Test string.";
            String buf = fejb1.method1(testStr);
            if (buf.equalsIgnoreCase(testStr)) {
                svLogger.info("testSLRaObjectAccess passed.");
            } else {
                results = results + " FAIL: slra.method1 returns a wrong value: " + buf + ". ";
            }
        } catch (Exception e) {
            results = results + " FAIL: Exception caught in testSLRaObjectAccess: " + e.toString() + ". ";
        }
    }

    /**
     * Execute setRollbackOnly
     *
     * @param results test point results
     * @param invokeLoc location MDB is invoked from
     */
    public void ctx_setRollbackOnly(String invokeLoc) {
        try {
            myMessageDrivenCtx.setRollbackOnly();
            results = results + " FAIL: IllegalStateException should be thrown when " + invokeLoc + " accesses setRollbackOnly. ";
        } catch (IllegalStateException ise) {
            results = results + " IllegalStateException is generated correctly when " + invokeLoc + " accesses setRollbackOnly() ";
        } catch (Throwable e) {
            results = results + " FAIL: Unexpected Exception when invoking setRollbackOnly from " + invokeLoc + " in CMT MDB: " + e.toString() + ". ";
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
            results = results + " FAIL: IllegalStateException should be thrown when " + invokeLoc + " accesses getRollbackOnly. ";
        } catch (IllegalStateException ise) {
            results = results + " IllegalStateException is generated correctly when " + invokeLoc + " accesses getRollbackOnly() ";
        } catch (Throwable e) {
            results = results + " FAIL: Unexpected Exception when invoking getRollbackOnly from " + invokeLoc + " in CMT MDB: " + e.toString() + ". ";
        }
    }
}
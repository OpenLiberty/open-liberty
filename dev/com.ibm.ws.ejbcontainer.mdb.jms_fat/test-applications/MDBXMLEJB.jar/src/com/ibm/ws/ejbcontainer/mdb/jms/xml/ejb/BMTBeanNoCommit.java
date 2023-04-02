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

import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.transaction.UserTransaction;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

/**
 * None in the MDB. The MDB verifies the behavior of the EJB container when the BMT bean
 * leaves a user transaction open without either a commit or rollback. The test client verifies
 * the DB contents for assert the actual test point.
 */
public class BMTBeanNoCommit implements MessageDrivenBean, MessageListener {
    private final static String CLASSNAME = BMTBeanNoCommit.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final long serialVersionUID = -429864400159226288L;

    private MessageDrivenContext myMessageDrivenCtx = null;

    private final String replyQueueFactoryName = "java:comp/env/jms/TestQCF";
    private final String replyQueueName = "java:comp/env/jms/TestResultQueue";

    private static final String jndiSFLHome = "java:global/MDBXMLApp/MDBXMLEJB/MDBSF!com.ibm.ws.ejbcontainer.mdb.jms.xml.ejb.SFLocalHome";

    final static String BeanName = "BMTBeanNoCommit";

    public static SFLocal noCommitBean;

    String results = null;
    static int svCount = 0;

    /**
     * This method is called when the Message Driven Bean is created. It currently does nothing.
     *
     * @exception javax.ejb.CreateException
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
     * @param msg javax.jms.Message This should be a TextMessage.
     */
    @Override
    public void onMessage(Message msg) {
        String text = null;
        String messageID = null;

        results = "";

        // Performing test points
        testBMTTxNoCommit();

        try {
            // send the result vector through the reply queue
            text = ((TextMessage) msg).getText();

            svLogger.info("senderBean.onMessage(), msg text ->: " + text);
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
     * To verify the container's behavior when the onMessage() method leaves a transaction open
     */
    public void testBMTTxNoCommit() {
        svLogger.info(BeanName + "-----> testBMTTxNoCommit starts.");

        SFLocalHome fhome1;

        try {
            svLogger.info(" testBMTTxNoCommit looking up remote home ...");
            fhome1 = (SFLocalHome) PortableRemoteObject.narrow(new InitialContext().lookup(jndiSFLHome), SFLocalHome.class);
            results = results + " Able to lookup bean remote home for " + jndiSFLHome + ". ";
        } catch (Exception ne) {
            results = results + " FAIL: Unable to lookup bean remote home for " + jndiSFLHome + ": " + ne.toString() + ". ";
            return;
        }

        UserTransaction tmpTx = myMessageDrivenCtx.getUserTransaction();
        try {
            tmpTx.begin();

            svLogger.info("create - started.");
            noCommitBean = fhome1.create();
            svLogger.info("create - ended.");

            if (svCount == 0) {
                noCommitBean.setIntValue(0);
                noCommitBean.incrementInt();
                results = results + " FAIL: Message should not be received by client since no commit occurred. ";
                // tmpTx.commit();
                svCount++;
            } else {
                results = results + " Message should be received by client since we've now committed. ";
                tmpTx.commit();
            }
        } catch (Throwable t) {
            results = results + " FAIL: Unexpected exception thrown while processing SF: " + t.toString() + ". ";
            return;
        }

        svLogger.info(BeanName + "<----- testBMTTxNoCommit ends.");
    }
}
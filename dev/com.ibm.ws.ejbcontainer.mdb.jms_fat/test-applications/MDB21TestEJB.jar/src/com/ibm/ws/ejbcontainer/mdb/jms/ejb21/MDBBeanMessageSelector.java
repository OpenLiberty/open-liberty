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

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

/**
 * This is a Message Driven Bean (MCM04) that on receipt of a message prints the messageid and the contents
 * of the message to the standard out of the Application Server
 */
@SuppressWarnings("serial")
public class MDBBeanMessageSelector implements MessageDrivenBean, MessageListener {
    private final static String CLASSNAME = MDBBeanMessageSelector.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private javax.ejb.MessageDrivenContext myMessageDrivenCtx = null;

    private final String replyQueueFactoryName = "jms/TestQCF";
    private final String replyQueueName = "jms/TestResultQueue";

    final static String BeanName = "MDBBeanMessageSelector";
    final static String replyMessage = "testMessageSelector passed";

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
     * The onMessage method extracts the text from the message and the messageid and passes that data into
     * the handleMessage methods.
     *
     * @param msg javax.jms.Message This should be a TextMessage.
     */
    @Override
    public void onMessage(Message msg) {
        String text = null;

        try {
            svLogger.info(BeanName + " message bean onMessage() method called");

            text = ((TextMessage) msg).getText();
            svLogger.info("onMessage() text received: " + text);

            if (msg.getJMSType().equalsIgnoreCase("MCM02")) {
                FATMDBHelper.putQueueMessage(replyMessage, replyQueueFactoryName, replyQueueName);
            } else {
                FATMDBHelper.putQueueMessage("Should not get this message.  Message Selector failed.", replyQueueFactoryName, replyQueueName);
            }
        } catch (Exception e) {
            svLogger.info("Exception thrown while in onMessage: " + e.toString());
            e.printStackTrace();
        }

        return;
    }
}
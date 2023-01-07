/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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

import java.security.Principal;
import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.TextMessage;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

public class CommonMDB implements MessageDrivenBean {
    private final static String CLASSNAME = CommonMDB.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final long serialVersionUID = 2435039542438031499L;

    final static String BeanName = "CommonMDB";

    private MessageDrivenContext myMessageDrivenCtx = null;

    // Test points
    String results = "";

    //JNDI names
    private final String replyQueueFactoryName = "java:comp/env/jms/TestQCF";
    private final String replyQueueName = "java:comp/env/jms/TestResultQueue";

    /**
     * This method is called when the Message Driven Bean is created.
     *
     * @exception javax.ejb.CreateException
     * @exception javax.ejb.EJBException
     */
    public void ejbCreate() throws CreateException {
        svLogger.info(BeanName + "(ejbCreate)");
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
     * Execute getCallerPrincipal
     *
     * @param results test point results
     * @param invokeLoc location MDB is invoked from
     */
    public void ctx_getCallerPrincipal(String invokeLoc) {
        svLogger.info(BeanName + "-----> ctx_getCallerPrincipal starts.");

        try {
            Principal p = myMessageDrivenCtx.getCallerPrincipal();

            if (p == null)
                results = results + " FAIL: getCallerPrincipal() returned null when invoked from " + invokeLoc + ". ";
        } catch (Throwable t) {
            results = results + " FAIL: Unexpected exception is thrown when " + invokeLoc + " in MDB invokes getCallerPrincipal(): " + t.toString() + ". ";
            svLogger.info("Unexpected Exception:");
            t.printStackTrace();
        }

        results = results + " Positive message to inidicate success. ";

        svLogger.info(BeanName + "<----- ctx_getCallerPrincipal ends.");
    }

    /**
     * The onMessage method extracts the text and message id of the message and print the text to the
     * Application Server standard out and calls put message with the message id and text.
     *
     * @param msg This should be a TextMessage.
     */
    public void onMessage(Message msg) {
        String text = null;
        String messageID = null;

        ctx_getCallerPrincipal("onMessage()");

        // send the result vector through the reply queue
        try {
            text = ((TextMessage) msg).getText();

            svLogger.info("CommonMDB.onMessage(), msg text ->: " + text);

            messageID = msg.getJMSMessageID();
            svLogger.info("Message ID :" + messageID);
            FATMDBHelper.putQueueMessage(results, replyQueueFactoryName, replyQueueName);
            svLogger.info("CommonMDB.onMessage(): Test results are sent.");
        } catch (Exception err) {
            svLogger.info("Caught exception: " + err.toString());
            err.printStackTrace();
        }
        return;
    }
}
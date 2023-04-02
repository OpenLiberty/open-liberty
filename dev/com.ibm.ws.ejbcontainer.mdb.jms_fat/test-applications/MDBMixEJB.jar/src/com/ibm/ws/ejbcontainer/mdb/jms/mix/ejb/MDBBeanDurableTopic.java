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
package com.ibm.ws.ejbcontainer.mdb.jms.mix.ejb;

import java.util.logging.Logger;

import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

/**
 * This is a Message Driven Bean (MCM04) that on receipt of a message prints the messageid and the contents
 * of the message to the standard out of the Application Server
 */
@MessageDriven
public class MDBBeanDurableTopic implements MessageListener {
    private final static String CLASSNAME = MDBBeanDurableTopic.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private final String replyQueueFactoryName = "java:comp/env/jms/TestQCF";
    private final String replyQueueName = "java:comp/env/jms/TestResultQueue";

    final static String BeanName = "MDBBeanDurableTopic";
    final static String replyMessage = "testDurableTopic passed";

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
            FATMDBHelper.putQueueMessage(replyMessage, replyQueueFactoryName, replyQueueName);
        } catch (Exception e) {
            svLogger.info("Exception thrown while in onMessage: " + e.toString());
            e.printStackTrace();
        }
        return;
    }
}
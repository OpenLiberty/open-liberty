/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.xml.ejb;

import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.Stateful;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

@Stateful(name = "SFMsgDestinationFldInjTestBean")
public class SFMsgDestinationFldInjTestBean implements SFMsgDestinationLocalBiz {
    private static final String CLASS_NAME = SFMsgDestinationFldInjTestBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String CF_NAME = "java:comp/env/jms/WSTestQCF";
    private static final String REQUEST_QUEUE = "java:comp/env/jms/RequestQueue";

    String Qmessage = "Not set";

    // XML-based field injection
    public QueueConnectionFactory queueConnectionFactory;
    // XML-based field injection
    public Queue requestQueue;
    // XML-based field injection
    public Queue responseQueue;

    @Override
    public void setQueueMessage(String message) {
        Qmessage = message;
    }

    @Override
    public void putQueueMessage() {
        svLogger.info("*** SFMsgDestinationFldInjTestBean");
        svLogger.info("**** queueConnectionFactory: " + queueConnectionFactory);
        svLogger.info("**** requestQueue: " + requestQueue);
        svLogger.info("**** responseQueue: " + responseQueue);

        try {
            FATMDBHelper.putQueueMessage(Qmessage, queueConnectionFactory, requestQueue);
        } catch (Exception ex) {
            throw new EJBException("Caught throwable attempting to put message on queue", ex);
        }
    }

    @Override
    public String getQueueMessage() {
        try {
            // Get the message off the request queue and put another on the response queue.
            // Also, return the message
            String message = (String) FATMDBHelper.getQueueMessage(CF_NAME, REQUEST_QUEUE);
            message = "SFMsgDestinationFldInjTestBean:" + message;
            FATMDBHelper.putQueueMessage(message, queueConnectionFactory, responseQueue);
            return message;
        } catch (Exception ex) {
            throw new EJBException("Caught throwable attempting to get message from queue", ex);
        }
    }
}

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

import static javax.annotation.Resource.AuthenticationType.APPLICATION;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

@Stateless(name = "SLMsgDestinationAnnotInjTestBean")
@Local(SLMsgDestinationLocalBiz.class)
public class SLMsgDestinationAnnotInjTestBean {
    private static final String CLASS_NAME = SLMsgDestinationAnnotInjTestBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String CF_NAME = "java:comp/env/jms/WSTestQCF";
    private static final String REQUEST_QUEUE = "java:comp/env/jms/RequestQueue";

    @Resource(name = "jms/WSTestQCF", authenticationType = APPLICATION, shareable = true, description = "Queue conn factory")
    public QueueConnectionFactory qcf;

    @Resource(name = "jms/RequestQueue")
    public Queue reqQueue;

    @Resource(name = "jms/ResponseQueue")
    public Queue resQueue;

    // XML method injection of qcf
    public void setQueueConnectionFactory(QueueConnectionFactory qc) {
        qcf = qc;
    }

    // XML method injection of queue
    public void setRequestQueue(Queue queue) {
        reqQueue = queue;
    }

    // XML method injection of queue
    public void setResponseQueue(Queue queue) {
        resQueue = queue;
    }

    public void putQueueMessage(String message) {
        svLogger.info("*** SLMsgDestinationAnnotInjTestBean");
        svLogger.info("**** qcf: " + qcf);
        svLogger.info("**** reqQueue: " + reqQueue);
        svLogger.info("**** resQueue: " + resQueue);

        try {
            FATMDBHelper.putQueueMessage(message, qcf, reqQueue);
        } catch (Exception ex) {
            throw new EJBException("Caught throwable attempting to put message on queue", ex);
        }
    }

    public String getQueueMessage() {
        try {
            // Get the message off the request queue and put another on the response queue.
            // Also, return the message
            String message = (String) FATMDBHelper.getQueueMessage(CF_NAME, REQUEST_QUEUE);
            message = "SLMsgDestinationAnnotInjTestBean:" + message;
            FATMDBHelper.putQueueMessage(message, qcf, resQueue);
            return message;
        } catch (Exception ex) {
            throw new EJBException("Caught throwable attempting to get message from queue", ex);
        }
    }
}

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

import javax.ejb.EJBException;
import javax.ejb.Local;
import javax.ejb.Stateless;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

@Stateless(name = "SLMsgDestinationTestBean")
@Local(SLMsgDestinationLocalBiz.class)
public class SLMsgDestinationTestBean {
    private static final String CF_NAME = "java:comp/env/jms/WSTestQCF";
    private static final String REQUEST_QUEUE = "java:comp/env/jms/RequestQueue";
    private static final String RESPONSE_QUEUE = "java:comp/env/jms/ResponseQueue";

    public void putQueueMessage(String message) {
        try {
            FATMDBHelper.putQueueMessage(message, CF_NAME, REQUEST_QUEUE);
        } catch (Exception ex) {
            throw new EJBException("Caught throwable attempting to put message on queue", ex);
        }
    }

    public String getQueueMessage() {
        try {
            // Get the message off the request queue and put another on the response queue.
            // Also, return the message
            String message = (String) FATMDBHelper.getQueueMessage(CF_NAME, REQUEST_QUEUE);
            message = "SLMsgDestinationTestBean:" + message;
            FATMDBHelper.putQueueMessage(message, CF_NAME, RESPONSE_QUEUE);
            return message;
        } catch (Exception ex) {
            throw new EJBException("Caught throwable attempting to get message from queue", ex);
        }
    }
}

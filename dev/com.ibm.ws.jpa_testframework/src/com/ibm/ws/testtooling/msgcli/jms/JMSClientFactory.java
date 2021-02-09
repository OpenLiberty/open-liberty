/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testtooling.msgcli.jms;

import com.ibm.ws.testtooling.msgcli.MessagingClient;
import com.ibm.ws.testtooling.msgcli.MessagingException;
import com.ibm.ws.testtooling.msgcli.jms.JMSClientConfig.JMSType;

public final class JMSClientFactory {
    public final static MessagingClient createJMSMessagingClient(String identity, JMSClientConfig config) throws MessagingException {
        if (identity == null || config == null) {
            return null;
        }

        try {
            AbstractJMSClient jmsClient = null;

            if (config.getJmsType() == JMSType.QUEUE) {
                jmsClient = new JMSQueueClient(identity, config);
            } else if (config.getJmsType() == JMSType.TOPIC) {
                jmsClient = new JMSTopicClient(identity, config);
            } else {
                return null;
            }

            jmsClient.initialize();
            return jmsClient;
        } catch (MessagingException me) {
            throw me;
        } catch (Exception e) {
            throw new MessagingException(e);
        }
    }
}

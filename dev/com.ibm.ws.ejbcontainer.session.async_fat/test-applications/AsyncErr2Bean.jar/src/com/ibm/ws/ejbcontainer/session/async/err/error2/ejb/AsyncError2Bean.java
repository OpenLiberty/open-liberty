/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.session.async.err.error2.ejb;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@Asynchronous
@Local(AsyncError2.class)
@MessageDriven(activationConfig = {
                                    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                                    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
                                    @ActivationConfigProperty(propertyName = "destination", propertyValue = "AsyncError2ReqQueue") },
               name = "AsyncError2", messageListenerInterface = MessageListener.class)
public class AsyncError2Bean {
    public final static String CLASSNAME = AsyncError2Bean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    public void onMessage(javax.jms.Message message) {
        try {
            if (message instanceof TextMessage) {
                svLogger.logp(Level.WARNING, CLASSNAME, "test_fireAndForget",
                              "AsyncError2 test failed.  Asynchronous does not support Message Driven Beans.");
                svLogger.logp(Level.INFO, CLASSNAME, "test_fireAndForget",
                              "Bean got message from onMessage():" + ((TextMessage) message).getText());
            }
        } catch (JMSException e) {
            svLogger.logp(Level.WARNING, CLASSNAME, "test_fireAndForget", "Communication error", e);
        }
    }

    public AsyncError2Bean() {}
}

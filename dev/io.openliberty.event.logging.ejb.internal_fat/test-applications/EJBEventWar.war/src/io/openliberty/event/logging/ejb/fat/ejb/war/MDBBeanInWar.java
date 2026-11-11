/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.ejb.war;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 * MDB embedded in the standalone WAR, listening on queue {@code EJBEventWarQueue}.
 * Context info (standalone WAR): EJBEventWar | MDBBeanInWar | onMessage
 */
@MessageDriven(activationConfig = {
                                    @ActivationConfigProperty(propertyName = "destinationType",
                                                              propertyValue = "javax.jms.Queue"),
                                    @ActivationConfigProperty(propertyName = "destination",
                                                              propertyValue = "EJBEventWarQueue")
},
               name = "MDBBeanInWar")
public class MDBBeanInWar implements MessageListener {

    private static final Logger log = Logger.getLogger(MDBBeanInWar.class.getName());

    public static volatile CountDownLatch latch = new CountDownLatch(1);

    @Resource
    private MessageDrivenContext ctx;

    @Override
    public void onMessage(Message msg) {
        try {
            String text = (msg instanceof TextMessage) ? ((TextMessage) msg).getText() : "?";
            log.info("MDBBeanInWar.onMessage() received: " + text);
        } catch (Exception e) {
            log.warning("MDBBeanInWar.onMessage() error: " + e);
        } finally {
            latch.countDown();
        }
    }

    public static void awaitMessage(long timeoutMs) throws InterruptedException {
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Timed out waiting for MDBBeanInWar.onMessage()");
        }
    }
}

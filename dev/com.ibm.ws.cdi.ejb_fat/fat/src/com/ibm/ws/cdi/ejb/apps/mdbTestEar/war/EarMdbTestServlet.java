/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.mdbTestEar.war;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.ejb.apps.mdbTestEar.lib.EarTestMessageHolder;

import componenttest.app.FATServlet;

@WebServlet("/EarMdbTestServlet")
public class EarMdbTestServlet extends FATServlet {

    @Resource(name = "jms/qcf")
    QueueConnectionFactory qcf;
    @Resource(name = "jms/jar/jarQueue")
    Queue jarQueue;
    @Resource(name = "jms/jar/jarNoDiscoveryQueue")
    Queue jarNoDiscoveryQueue;

    @Inject
    private EarTestMessageHolder messageHolder;

    private static final long serialVersionUID = 1L;

    @Test
    public void testEarMdb() throws JMSException, InterruptedException {

        try (QueueConnection conn = qcf.createQueueConnection()) {
            conn.start();

            QueueSession session = conn.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
            QueueSender sender = session.createSender(jarQueue);

            TextMessage message = session.createTextMessage();
            message.setText("testJarMdb");
            sender.send(message);
            System.out.println("Servlet sent message: " + message);

            messageHolder.assertMessageArrives("testJarMdb");

            QueueSession jarNoDiscoverySession = conn.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
            QueueSender jarNoDiscoverySender = jarNoDiscoverySession.createSender(jarNoDiscoveryQueue);

            TextMessage jarNoDiscoveryMessage = session.createTextMessage();
            jarNoDiscoveryMessage.setText("testJarMdbNoDiscovery");
            jarNoDiscoverySender.send(jarNoDiscoveryMessage);
            System.out.println("Servlet sent message: " + jarNoDiscoveryMessage);

            messageHolder.assertMessageArrives("testJarMdbNoDiscovery");
        }

    }

}

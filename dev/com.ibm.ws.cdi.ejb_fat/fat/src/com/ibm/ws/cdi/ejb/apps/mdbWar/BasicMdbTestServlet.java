/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.mdbWar;

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

import componenttest.app.FATServlet;

@WebServlet("/BasicMdbTestServlet")
public class BasicMdbTestServlet extends FATServlet {

    @Resource(name = "jms/qcf")
    QueueConnectionFactory qcf;
    @Resource(name = "jms/basic/queue")
    Queue queue;

    @Inject
    private TestMessageHolder messageHolder;

    private static final long serialVersionUID = 1L;

    @Test
    public void testBasicMdb() throws JMSException, InterruptedException {

        try (QueueConnection conn = qcf.createQueueConnection()) {
            conn.start();

            QueueSession session = conn.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
            QueueSender sender = session.createSender(queue);

            TextMessage message = session.createTextMessage();
            message.setText("testMdb");
            sender.send(message);
            System.out.println("Servlet sent message: " + message);
        }

        messageHolder.assertMessageArrives("testMdb");
    }

}

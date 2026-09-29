/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.ejbcontainer.fat.mdb.quiesce.web;

import javax.annotation.Resource;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

/**
 * Servlet that sends test messages to all MDB queues in both EJB and Web modules.
 */
@WebServlet("/MdbQuiesceServlet")
@SuppressWarnings("serial")
public class MdbQuiesceServlet extends FATServlet {

    @Resource(lookup = "jms/MdbQuiesceQCF")
    private QueueConnectionFactory qcf;

    @Resource(lookup = "jms/MdbQuiesceEjbDefaultQueue")
    private Queue ejbDefaultQueue;

    @Resource(lookup = "jms/MdbQuiesceEjbBndQueue")
    private Queue ejbBndQueue;

    @Resource(lookup = "jms/MdbQuiesceEjbServerQueue")
    private Queue ejbServerQueue;

    @Resource(lookup = "jms/MdbQuiesceEjbInvalidQueue")
    private Queue ejbInvalidQueue;

    @Resource(lookup = "jms/MdbQuiesceEjbInvalidServerQueue")
    private Queue ejbInvalidServerQueue;

    @Resource(lookup = "jms/MdbQuiesceWebDefaultQueue")
    private Queue webDefaultQueue;

    @Resource(lookup = "jms/MdbQuiesceWebBndQueue")
    private Queue webBndQueue;

    @Resource(lookup = "jms/MdbQuiesceWebServerQueue")
    private Queue webServerQueue;

    /**
     * Test method that sends messages to all MDB queues.
     * This method is called by the FAT test framework.
     */
    public void sendMessages() throws Exception {
        sendMessagesToAllQueues();
    }

    private void sendMessagesToAllQueues() throws Exception {
        QueueConnection qc = null;
        QueueSession qs = null;

        try {
            qc = qcf.createQueueConnection();
            qs = qc.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            // Send to EJB module queues
            sendMessage(qs, ejbDefaultQueue, "Test message for EJB Default MDB");
            sendMessage(qs, ejbBndQueue, "Test message for EJB Bnd MDB");
            sendMessage(qs, ejbServerQueue, "Test message for EJB Server MDB");
            sendMessage(qs, ejbInvalidQueue, "Test message for EJB Invalid MDB");
            sendMessage(qs, ejbInvalidServerQueue, "Test message for EJB Invalid Server MDB");

            // Send to Web module queues
            sendMessage(qs, webDefaultQueue, "Test message for Web Default MDB");
            sendMessage(qs, webBndQueue, "Test message for Web Bnd MDB");
            sendMessage(qs, webServerQueue, "Test message for Web Server MDB");

        } finally {
            if (qs != null) {
                qs.close();
            }
            if (qc != null) {
                qc.close();
            }
        }
    }

    private void sendMessage(QueueSession session, Queue queue, String messageText) throws Exception {
        QueueSender sender = null;
        try {
            sender = session.createSender(queue);
            TextMessage message = session.createTextMessage(messageText);
            sender.send(message);
            System.out.println("Sent message to queue: " + messageText);
        } finally {
            if (sender != null) {
                sender.close();
            }
        }
    }
}

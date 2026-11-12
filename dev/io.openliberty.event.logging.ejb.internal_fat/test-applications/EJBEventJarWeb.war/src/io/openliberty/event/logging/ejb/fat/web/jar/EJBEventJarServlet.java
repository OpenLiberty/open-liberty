/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.web.jar;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;
import io.openliberty.event.logging.ejb.fat.ejb.jar.api.AsyncBeanInJarRemote;
import io.openliberty.event.logging.ejb.fat.ejb.jar.api.RemoteBeanInJarRemote;

/**
 * Thin driver servlet deployed alongside the standalone EJB JAR.
 * Looks up beans via {@code java:global} JNDI names using remote interfaces only —
 * no local interfaces or shared statics cross application boundaries.
 * Each method is called by a corresponding {@code @Test} method in the FAT test
 * class, which invokes the servlet and then verifies the event log entries.
 */
@SuppressWarnings("serial")
@WebServlet("/EJBEventJarServlet")
public class EJBEventJarServlet extends FATServlet {

    private static final Logger log = Logger.getLogger(EJBEventJarServlet.class.getName());

    @Resource(name = "jms/EJBEventQCF")
    private QueueConnectionFactory qcf;

    @Resource(name = "jms/EJBEventJarQueue")
    private Queue jarQueue;

    private AsyncBeanInJarRemote lookupAsync() throws Exception {
        return (AsyncBeanInJarRemote) new InitialContext().lookup("java:global/EJBEventJar/AsyncBeanInJar!io.openliberty.event.logging.ejb.fat.ejb.jar.api.AsyncBeanInJarRemote");
    }

    private RemoteBeanInJarRemote lookupRemote() throws Exception {
        return (RemoteBeanInJarRemote) new InitialContext().lookup("java:global/EJBEventJar/RemoteBeanInJar!io.openliberty.event.logging.ejb.fat.ejb.jar.api.RemoteBeanInJarRemote");
    }

    // -----------------------------------------------------------------------
    // Invocation methods (standalone JAR: 3-part contextInfo module|bean|method)
    // -----------------------------------------------------------------------

    /** Invokes the async bean and waits for the Future to complete. */
    public void testStandaloneJarAsyncEventLogged() throws Exception {
        log.info("testStandaloneJarAsyncEventLogged: invoking AsyncBeanInJar.doAsync()");
        lookupAsync().doAsync().get();
        log.info("testStandaloneJarAsyncEventLogged: complete");
    }

    /** Invokes the remote business method. */
    public void testStandaloneJarRemoteEventLogged() throws Exception {
        log.info("testStandaloneJarRemoteEventLogged: invoking RemoteBeanInJar.doRemote()");
        String result = lookupRemote().doRemote();
        if (!"remote".equals(result)) {
            throw new RuntimeException("Unexpected result: " + result);
        }
    }

    /**
     * Sends a JMS message to trigger the MDB and waits up to 10s on a temporary reply queue
     * for the MDB's response before returning.
     */
    public void testStandaloneJarMdbEventLogged() throws Exception {
        log.info("testStandaloneJarMdbEventLogged: sending message to EJBEventJarQueue");
        QueueConnection conn = qcf.createQueueConnection();
        try {
            conn.start();
            QueueSession session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            TemporaryQueue replyQueue = session.createTemporaryQueue();
            QueueReceiver receiver = session.createReceiver(replyQueue);
            try {
                TextMessage msg = session.createTextMessage("testStandaloneJarMdbEventLogged");
                msg.setJMSReplyTo(replyQueue);
                QueueSender sender = session.createSender(jarQueue);
                sender.send(msg);

                log.info("testStandaloneJarMdbEventLogged: message sent, waiting for reply from MDBBeanInJar");
                Message reply = receiver.receive(10_000);
                if (reply == null) {
                    throw new RuntimeException("Timed out waiting for reply from MDBBeanInJar");
                }
                log.info("testStandaloneJarMdbEventLogged: reply received from MDBBeanInJar");
            } finally {
                receiver.close();
                replyQueue.delete();
            }
        } finally {
            conn.close();
        }
    }
}

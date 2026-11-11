/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.web.war;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;
import io.openliberty.event.logging.ejb.fat.ejb.war.AsyncBeanInWarRemote;
import io.openliberty.event.logging.ejb.fat.ejb.war.MDBBeanInWar;
import io.openliberty.event.logging.ejb.fat.ejb.war.RemoteBeanInWarRemote;
import io.openliberty.event.logging.ejb.fat.ejb.war.TimerBeanInWar;

/**
 * Servlet embedded in the standalone WAR that drives all four EJB types for
 * event logging tests. Each method is called by a corresponding {@code @Test}
 * method in the FAT test class, which invokes the servlet and then verifies
 * the event log entries.
 */
@SuppressWarnings("serial")
@WebServlet("/EJBEventWarServlet")
public class EJBEventWarServlet extends FATServlet {

    private static final Logger log = Logger.getLogger(EJBEventWarServlet.class.getName());

    @EJB
    private AsyncBeanInWarRemote asyncBean;

    @EJB
    private RemoteBeanInWarRemote remoteBean;

    @EJB
    private TimerBeanInWar timerBean;

    @Resource(name = "jms/EJBEventQCF")
    private QueueConnectionFactory qcf;

    @Resource(name = "jms/EJBEventWarQueue")
    private Queue warQueue;

    // -----------------------------------------------------------------------
    // Invocation methods (standalone WAR: 3-part contextInfo module|bean|method)
    // -----------------------------------------------------------------------

    /** Invokes the async bean and waits for the Future to complete. */
    public void testStandaloneWarAsyncEventLogged() throws Exception {
        log.info("testStandaloneWarAsyncEventLogged: invoking AsyncBeanInWar.doAsync()");
        asyncBean.doAsync().get();
    }

    /** Invokes the remote business method. */
    public void testStandaloneWarRemoteEventLogged() throws Exception {
        log.info("testStandaloneWarRemoteEventLogged: invoking RemoteBeanInWar.doRemote()");
        String result = remoteBean.doRemote();
        if (!"remote".equals(result)) {
            throw new RuntimeException("Unexpected result: " + result);
        }
    }

    /**
     * Creates a programmatic timer; waits up to 10s for it to fire.
     * {@code triggerTimer()} creates the timer and the servlet blocks on a
     * latch, so by the time the HTTP response returns the {@code @Timeout}
     * has already fired.
     */
    public void testStandaloneWarTimerEventLogged() throws Exception {
        log.info("testStandaloneWarTimerEventLogged: triggering programmatic timer");
        timerBean.triggerTimer();
        TimerBeanInWar.awaitTimer(10_000);
    }

    /** Sends a JMS message and waits up to 10s for MDB delivery. */
    public void testStandaloneWarMdbEventLogged() throws Exception {
        log.info("testStandaloneWarMdbEventLogged: sending message to EJBEventWarQueue");
        MDBBeanInWar.latch = new CountDownLatch(1);
        QueueConnection conn = qcf.createQueueConnection();
        try {
            conn.start();
            QueueSession session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            TextMessage msg = session.createTextMessage("testStandaloneWarMdbEventLogged");
            QueueSender sender = session.createSender(warQueue);
            sender.send(msg);
        } finally {
            conn.close();
        }
        MDBBeanInWar.awaitMessage(10_000);
    }
}

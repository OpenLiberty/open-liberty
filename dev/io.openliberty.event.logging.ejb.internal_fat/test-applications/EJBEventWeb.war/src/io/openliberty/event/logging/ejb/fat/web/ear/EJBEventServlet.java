/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.web.ear;

import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Handle;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;
import io.openliberty.event.logging.ejb.fat.ejb.ear.AsyncBeanLocal;
import io.openliberty.event.logging.ejb.fat.ejb.ear.EJBEventTestException;
import io.openliberty.event.logging.ejb.fat.ejb.ear.MDBBean;
import io.openliberty.event.logging.ejb.fat.ejb.ear.RemoteBeanHome;
import io.openliberty.event.logging.ejb.fat.ejb.ear.RemoteBeanLocalHome;
import io.openliberty.event.logging.ejb.fat.ejb.ear.RemoteBeanLocalObject;
import io.openliberty.event.logging.ejb.fat.ejb.ear.RemoteBeanRemote;
import io.openliberty.event.logging.ejb.fat.ejb.ear.StatefulRemoteBeanHome;
import io.openliberty.event.logging.ejb.fat.ejb.ear.StatefulRemoteBeanLocalHome;
import io.openliberty.event.logging.ejb.fat.ejb.ear.StatefulRemoteBeanLocalObject;
import io.openliberty.event.logging.ejb.fat.ejb.ear.StatefulRemoteBeanObject;
import io.openliberty.event.logging.ejb.fat.ejb.ear.StatefulRemoteBeanRemote;
import io.openliberty.event.logging.ejb.fat.ejb.ear.StressTimerBean;
import io.openliberty.event.logging.ejb.fat.ejb.ear.TimerBean;
import io.openliberty.event.logging.ejb.fat.ejb.ear.TimerBeanLocal;

/**
 * Servlet inside the EAR that drives all four EJB types for event logging tests.
 * Each method is called by a corresponding {@code @Test} method in the FAT test
 * class, which invokes the servlet and then verifies the event log entries.
 */
@SuppressWarnings("serial")
@WebServlet("/EJBEventServlet")
public class EJBEventServlet extends FATServlet {

    private static final Logger log = Logger.getLogger(EJBEventServlet.class.getName());

    @EJB
    private AsyncBeanLocal asyncBean;

    @EJB
    private RemoteBeanRemote remoteBean;

    @EJB
    private TimerBeanLocal timerBean;

    @EJB
    private StressTimerBean stressTimerBean;

    /** Number of interval timers used by the stress test; see {@link StressTimerBean}. */
    private static final int STRESS_TIMER_COUNT = 10;

    @Resource(name = "jms/EJBEventQCF")
    private QueueConnectionFactory qcf;

    @Resource(name = "jms/EJBEventEarQueue")
    private Queue earQueue;

    // -----------------------------------------------------------------------
    // Invocation methods (EAR packaging: 4-part contextInfo app|module|bean|method)
    // -----------------------------------------------------------------------

    /** Invokes the async bean and waits for the Future to complete. */
    public void testEarAsyncEventLogged() throws Exception {
        log.info("testEarAsyncEventLogged: invoking AsyncBean.doAsync()");
        asyncBean.doAsync().get();
        log.info("testEarAsyncEventLogged: complete");
    }

    /**
     * Exercises stateless + stateful beans via EJB 3.x remote and EJB 2.x home interfaces.
     * Verifies that remote business methods on component objects are logged but that
     * home.create() and local component methods are NOT logged.
     *
     * Stateless RemoteBean: doRemote (logged) + home.create (NOT logged) + doRemoteObject (logged)
     * + localHome.create (NOT logged) + doLocalObject (NOT logged)
     * Stateful StatefulRemoteBean: doRemote (logged) + doRemove [@Remove] (logged)
     * + remoteHome.create (NOT logged)
     * + doRemoteObject (logged) + getHandle (NOT logged)
     * + remoteHome.remove(Handle) (the home method is NOT logged, but it
     * calls EJBObject.remove(), which IS logged as a remote "remove")
     * + localHome.create (NOT logged) + doLocalObject (NOT logged)
     * + localObject.remove (NOT logged)
     *
     * Every stateful bean obtained is removed before the method ends.
     */
    public void testEarRemoteEventLogged() throws Exception {
        // --- Stateless RemoteBean ---
        log.info("testEarRemoteEventLogged: invoking RemoteBean.doRemote()");
        String result = remoteBean.doRemote();
        if (!"remote".equals(result)) {
            throw new RuntimeException("Unexpected result: " + result);
        }
        log.info("testEarRemoteEventLogged: stateless remote home create + doRemoteObject");
        RemoteBeanHome home = (RemoteBeanHome) new InitialContext().lookup("java:global/EJBEventApp/EJBEventEJB/RemoteBean!io.openliberty.event.logging.ejb.fat.ejb.ear.RemoteBeanHome");
        String remoteObjectResult = home.create().doRemoteObject();
        if (!"remoteObject".equals(remoteObjectResult)) {
            throw new RuntimeException("Unexpected result: " + remoteObjectResult);
        }
        log.info("testEarRemoteEventLogged: stateless local home create + doLocalObject");
        RemoteBeanLocalHome localHome = (RemoteBeanLocalHome) new InitialContext().lookup("java:global/EJBEventApp/EJBEventEJB/RemoteBean!io.openliberty.event.logging.ejb.fat.ejb.ear.RemoteBeanLocalHome");
        RemoteBeanLocalObject localObj = localHome.create();
        String localObjResult = localObj.doLocalObject();
        if (!"localHome".equals(localObjResult)) {
            throw new RuntimeException("Unexpected result: " + localObjResult);
        }

        // --- Stateful StatefulRemoteBean ---
        log.info("testEarRemoteEventLogged: invoking StatefulRemoteBean.doRemote()");
        StatefulRemoteBeanRemote statefulBean = (StatefulRemoteBeanRemote) new InitialContext().lookup("java:global/EJBEventApp/EJBEventEJB/StatefulRemoteBean!io.openliberty.event.logging.ejb.fat.ejb.ear.StatefulRemoteBeanRemote");
        String statefulResult = statefulBean.doRemote();
        if (!"statefulRemote".equals(statefulResult)) {
            throw new RuntimeException("Unexpected result: " + statefulResult);
        }
        log.info("testEarRemoteEventLogged: stateful business remove via @Remove method");
        statefulBean.doRemove();
        log.info("testEarRemoteEventLogged: stateful remote home create + doRemoteObject");
        StatefulRemoteBeanHome statefulHome = (StatefulRemoteBeanHome) new InitialContext().lookup("java:global/EJBEventApp/EJBEventEJB/StatefulRemoteBean!io.openliberty.event.logging.ejb.fat.ejb.ear.StatefulRemoteBeanHome");
        StatefulRemoteBeanObject statefulRemoteObj = statefulHome.create();
        String statefulRemoteObjResult = statefulRemoteObj.doRemoteObject();
        if (!"statefulRemoteObject".equals(statefulRemoteObjResult)) {
            throw new RuntimeException("Unexpected result: " + statefulRemoteObjResult);
        }
        log.info("testEarRemoteEventLogged: stateful remote getHandle + home.remove(Handle)");
        Handle statefulHandle = statefulRemoteObj.getHandle();
        statefulHome.remove(statefulHandle);
        log.info("testEarRemoteEventLogged: stateful local home create + doLocalObject");
        StatefulRemoteBeanLocalHome statefulLocalHome = (StatefulRemoteBeanLocalHome) new InitialContext().lookup("java:global/EJBEventApp/EJBEventEJB/StatefulRemoteBean!io.openliberty.event.logging.ejb.fat.ejb.ear.StatefulRemoteBeanLocalHome");
        StatefulRemoteBeanLocalObject statefulLocalObj = statefulLocalHome.create();
        String statefulLocalObjResult = statefulLocalObj.doLocalObject();
        if (!"statefulLocalObject".equals(statefulLocalObjResult)) {
            throw new RuntimeException("Unexpected result: " + statefulLocalObjResult);
        }
        log.info("testEarRemoteEventLogged: stateful local object remove");
        statefulLocalObj.remove();
    }

    /**
     * Calls a remote business method that throws a checked application exception, then calls a
     * method that succeeds. The checked exception is expected to reach the caller unchanged.
     */
    public void testEarRemoteFailureEventLogged() throws Exception {
        log.info("testEarRemoteFailureEventLogged: invoking RemoteBean.doRemoteFail()");
        try {
            remoteBean.doRemoteFail();
            throw new RuntimeException("RemoteBean.doRemoteFail() did not throw");
        } catch (EJBEventTestException expected) {
            log.info("testEarRemoteFailureEventLogged: received expected " + expected);
        }
        log.info("testEarRemoteFailureEventLogged: invoking RemoteBean.doRemote()");
        String result = remoteBean.doRemote();
        if (!"remote".equals(result)) {
            throw new RuntimeException("Unexpected result: " + result);
        }
    }

    /**
     * Calls a remote business method that throws an unchecked exception (the container logs
     * CNTR0020E and an FFDC for it), then calls a method that succeeds.
     */
    public void testEarRemoteSystemFailureEventLogged() throws Exception {
        log.info("testEarRemoteSystemFailureEventLogged: invoking RemoteBean.doRemoteSystemFail()");
        boolean threw = false;
        try {
            remoteBean.doRemoteSystemFail();
        } catch (Exception expected) {
            threw = true;
            log.info("testEarRemoteSystemFailureEventLogged: received expected " + expected);
        }
        if (!threw) {
            throw new RuntimeException("RemoteBean.doRemoteSystemFail() did not throw");
        }
        log.info("testEarRemoteSystemFailureEventLogged: invoking RemoteBean.doRemote()");
        String result = remoteBean.doRemote();
        if (!"remote".equals(result)) {
            throw new RuntimeException("Unexpected result: " + result);
        }
    }

    /**
     * Calls an async method that throws a checked application exception, which the caller sees as
     * the cause of an ExecutionException, then calls an async method that succeeds.
     */
    public void testEarAsyncFailureEventLogged() throws Exception {
        log.info("testEarAsyncFailureEventLogged: invoking AsyncBean.doAsyncFail()");
        try {
            asyncBean.doAsyncFail().get();
            throw new RuntimeException("AsyncBean.doAsyncFail() did not throw");
        } catch (ExecutionException expected) {
            if (!(expected.getCause() instanceof EJBEventTestException)) {
                throw new RuntimeException("Unexpected cause: " + expected.getCause(), expected);
            }
            log.info("testEarAsyncFailureEventLogged: received expected " + expected.getCause());
        }
        log.info("testEarAsyncFailureEventLogged: invoking AsyncBean.doAsync()");
        asyncBean.doAsync().get();
    }

    /**
     * Creates a programmatic timer; waits up to 10s for it to fire.
     * {@code triggerTimer()} creates the timer and the servlet blocks on a
     * latch, so by the time the HTTP response returns the {@code @Timeout}
     * has already fired.
     */
    public void testEarTimerEventLogged() throws Exception {
        log.info("testEarTimerEventLogged: triggering programmatic timer");
        timerBean.triggerTimer();
        TimerBean.awaitTimer(10_000);
        log.info("testEarTimerEventLogged: timer fired");
    }

    /** Sends a JMS message and waits up to 10s for MDB delivery. */
    public void testEarMdbEventLogged() throws Exception {
        log.info("testEarMdbEventLogged: sending message to EJBEventEarQueue");
        MDBBean.latch = new java.util.concurrent.CountDownLatch(1);
        QueueConnection conn = qcf.createQueueConnection();
        try {
            conn.start();
            QueueSession session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            TextMessage msg = session.createTextMessage("testEarMdbEventLogged");
            QueueSender sender = session.createSender(earQueue);
            sender.send(msg);
        } finally {
            conn.close();
        }
        MDBBean.awaitMessage(10_000);
        log.info("testEarMdbEventLogged: MDB onMessage delivered");
    }

    // -----------------------------------------------------------------------
    // Interval timers used while eventLogging is added and removed at runtime
    // -----------------------------------------------------------------------

    /** Starts the interval timers that keep EJB timer invocations continuously in flight. */
    public void startStressTimers() throws Exception {
        log.info("startStressTimers: creating " + STRESS_TIMER_COUNT + " interval timers");
        stressTimerBean.startTimers(STRESS_TIMER_COUNT);
    }

    /** Cancels the stress timers. */
    public void stopStressTimers() throws Exception {
        log.info("stopStressTimers: cancelling the interval timers");
        stressTimerBean.stopTimers();
    }

    /**
     * Fails unless all of the stress timers exist. The timers are not persistent,
     * so they would be gone if the application had been restarted.
     */
    public void verifyStressTimersActive() throws Exception {
        int count = stressTimerBean.getTimerCount();
        if (count != STRESS_TIMER_COUNT) {
            throw new RuntimeException("Expected " + STRESS_TIMER_COUNT + " active stress timers but found " + count);
        }
    }

    // -----------------------------------------------------------------------
    // Helper invoked by EJBEventLoggingTest to exercise all EJB types at once
    // -----------------------------------------------------------------------

    /**
     * Exercises all seven EJB invocation types in a single call: async, local,
     * local home, MDB, remote, remote home and programmatic timer. Used by test
     * methods in {@code EJBEventLoggingTest} that reconfigure {@code <eventLogging>}
     * and then verify which events were logged. Returns normally on success.
     */
    public void testAllEjbTypes() throws Exception {
        log.info("testAllEjbTypes: invoking AsyncBean.doAsync()");
        asyncBean.doAsync().get();

        log.info("testAllEjbTypes: invoking AsyncBean.doLocal()");
        String localResult = asyncBean.doLocal();
        if (!"local".equals(localResult)) {
            throw new RuntimeException("Unexpected result: " + localResult);
        }

        log.info("testAllEjbTypes: looking up RemoteBeanLocalHome via JNDI");
        RemoteBeanLocalHome localHome = (RemoteBeanLocalHome) new InitialContext().lookup("java:global/EJBEventApp/EJBEventEJB/RemoteBean!io.openliberty.event.logging.ejb.fat.ejb.ear.RemoteBeanLocalHome");
        RemoteBeanLocalObject localObj = localHome.create();
        String localHomeResult = localObj.doLocalObject();
        if (!"localHome".equals(localHomeResult)) {
            throw new RuntimeException("Unexpected result: " + localHomeResult);
        }

        log.info("testAllEjbTypes: sending message to EJBEventEarQueue");
        MDBBean.latch = new java.util.concurrent.CountDownLatch(1);
        QueueConnection conn = qcf.createQueueConnection();
        try {
            conn.start();
            QueueSession session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            TextMessage msg = session.createTextMessage("testAllEjbTypes");
            QueueSender sender = session.createSender(earQueue);
            sender.send(msg);
        } finally {
            conn.close();
        }
        MDBBean.awaitMessage(10_000);

        log.info("testAllEjbTypes: invoking RemoteBean.doRemote()");
        String remoteResult = remoteBean.doRemote();
        if (!"remote".equals(remoteResult)) {
            throw new RuntimeException("Unexpected result: " + remoteResult);
        }

        log.info("testAllEjbTypes: stateless remote home create + doRemoteObject");
        RemoteBeanHome home = (RemoteBeanHome) new InitialContext().lookup("java:global/EJBEventApp/EJBEventEJB/RemoteBean!io.openliberty.event.logging.ejb.fat.ejb.ear.RemoteBeanHome");
        String remoteObjectResult = home.create().doRemoteObject();
        if (!"remoteObject".equals(remoteObjectResult)) {
            throw new RuntimeException("Unexpected result: " + remoteObjectResult);
        }

        log.info("testAllEjbTypes: stateful StatefulRemoteBean.doRemote()");
        StatefulRemoteBeanRemote statefulBean = (StatefulRemoteBeanRemote) new InitialContext().lookup("java:global/EJBEventApp/EJBEventEJB/StatefulRemoteBean!io.openliberty.event.logging.ejb.fat.ejb.ear.StatefulRemoteBeanRemote");
        String statefulResult = statefulBean.doRemote();
        if (!"statefulRemote".equals(statefulResult)) {
            throw new RuntimeException("Unexpected result: " + statefulResult);
        }
        log.info("testAllEjbTypes: stateful business remove via @Remove method");
        statefulBean.doRemove();

        log.info("testAllEjbTypes: stateful remote home create + doRemoteObject");
        StatefulRemoteBeanHome statefulHome = (StatefulRemoteBeanHome) new InitialContext().lookup("java:global/EJBEventApp/EJBEventEJB/StatefulRemoteBean!io.openliberty.event.logging.ejb.fat.ejb.ear.StatefulRemoteBeanHome");
        StatefulRemoteBeanObject statefulRemoteObj = statefulHome.create();
        String statefulRemoteObjResult = statefulRemoteObj.doRemoteObject();
        if (!"statefulRemoteObject".equals(statefulRemoteObjResult)) {
            throw new RuntimeException("Unexpected result: " + statefulRemoteObjResult);
        }
        log.info("testAllEjbTypes: stateful remote object remove (EJBObject.remove)");
        statefulRemoteObj.remove();

        log.info("testAllEjbTypes: stateful local home create + doLocalObject");
        StatefulRemoteBeanLocalHome statefulLocalHome = (StatefulRemoteBeanLocalHome) new InitialContext().lookup("java:global/EJBEventApp/EJBEventEJB/StatefulRemoteBean!io.openliberty.event.logging.ejb.fat.ejb.ear.StatefulRemoteBeanLocalHome");
        StatefulRemoteBeanLocalObject statefulLocalObj = statefulLocalHome.create();
        String statefulLocalObjResult = statefulLocalObj.doLocalObject();
        if (!"statefulLocalObject".equals(statefulLocalObjResult)) {
            throw new RuntimeException("Unexpected result: " + statefulLocalObjResult);
        }
        log.info("testAllEjbTypes: stateful local object remove");
        statefulLocalObj.remove();

        log.info("testAllEjbTypes: triggering programmatic timer");
        timerBean.triggerTimer();
        TimerBean.awaitTimer(10_000);

        log.info("testAllEjbTypes: complete");
    }
}

/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.calendar.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.ejbcontainer.timer.cal.web.EarlyTimeoutPersistentServlet;
import com.ibm.ws.ejbcontainer.timer.cal.web.NextTimeoutPersistentServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class NextTimeoutPersistentTest extends FATServletClient {
    private static final String NEXT_TIMEOUT_SERVLET = "TimerCalTestWeb/NextTimeoutPersistentServlet";
    private static final String EARLY_TIMEOUT_SERVLET = "TimerCalTestWeb/EarlyTimeoutPersistentServlet";
    private static final String SCHEDULE_SERVLET = "TimerCalTestWeb/ScheduleExpressionServlet";

    @Server("com.ibm.ws.ejbcontainer.timer.cal.fat.PersistentTimerServer")
    @TestServlets({ @TestServlet(servlet = NextTimeoutPersistentServlet.class, contextRoot = "TimerCalTestWeb"),
                    @TestServlet(servlet = EarlyTimeoutPersistentServlet.class, contextRoot = "TimerCalTestWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.cal.fat.PersistentTimerServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.timer.cal.fat.PersistentTimerServer")).andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.cal.fat.PersistentTimerServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### TimerCalTestApp.ear
        JavaArchive TimerCalTestEJB = ShrinkHelper.buildJavaArchive("TimerCalTestEJB.jar", "com.ibm.ws.ejbcontainer.timer.cal.ejb.");
        WebArchive TimerCalTestWeb = ShrinkHelper.buildDefaultApp("TimerCalTestWeb.war", "com.ibm.ws.ejbcontainer.timer.cal.web.");
        EnterpriseArchive TimerCalTestApp = ShrinkWrap.create(EnterpriseArchive.class, "TimerCalTestApp.ear");
        TimerCalTestApp.addAsModule(TimerCalTestEJB).addAsModule(TimerCalTestWeb);

        ShrinkHelper.exportDropinAppToServer(server, TimerCalTestApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        try {
            FATServletClient.runTest(server, NEXT_TIMEOUT_SERVLET, "cleanup");
            FATServletClient.runTest(server, EARLY_TIMEOUT_SERVLET, "cleanup");
            FATServletClient.runTest(server, SCHEDULE_SERVLET, "cleanup");
        } finally {
            // CNTR0092W: Attempted to access EnterpriseBean
            // PersistentTimerTestApp#PersistentTimerTestEJB.jar#NotSupportedTranBean, that has not been started.
            //
            // persistent.internal.InvokerTask run starts for a persistent timer during server shutdown,
            // PersistentTimerTaskHandler tries to access bean properties to run timeout but bean is gone.

            // DSRA0304E:  XAException occurred. XAException contents and details are:
            // DSRA0302E:  XAException occurred.  Error code is: XA_RBROLLBACK (100).  Exception is: null
            //
            // persistent.internal.InvokerTask run starts for a persistent timer during server shutdown,
            // but datasource has already been shutdown; can occur without any timers, just by database poll.

            // DSRA0230E: Attempt to perform operation XAResource.end() is not allowed because transaction state is TRANSACTION_FAIL.
            // DSRA0302E:  XAException occurred.  Error code is: XAER_NOTA (-4).  Exception is: null
            // DSRA0302E:  XAException occurred.  Error code is: XAER_RMFAIL (-7).  Exception is: No current connection.
            //
            // persistent.internal.InvokerTask run starts for a persistent timer during server shutdown,
            // but timer fails because server stopping, then attempt to rollback fails because transaction
            // service has already stopped.

            // J2CA0027E: An exception occurred while invoking end on an XA Resource Adapter from DataSource
            //            dataSource[DefaultDataSource], within transaction ID {XidImpl: formatId(57415344),
            //            gtrid_length(36), bqual_length(54),
            //
            // persistent.internal.InvokerTask run starts for a persistent timer during server shutdown,
            // but transaction service has already been shutdown.

            // CWWKC1501W: Persistent executor [EJBPersistentTimerExecutor] rolled back task [task id]
            //             (!EJBTimerP![j2eename]) due to failure javax.ejb.EJBException: Timeout method
            //             [method name] will not be invoked because server is stopping
            //
            // persistent.internal.InvokerTask run starts for a persistent timer during server shutdown,
            // but EJB timer service throws exception due to server stopping.

            // CWWKC1503W: Persistent executor [EJBPersistentTimerExecutor] rolled back task [task id]
            //             (!EJBTimerP![j2eename]) due to failure javax.ejb.EJBException: Timeout method
            //             [method name] will not be invoked because server is stopping
            //
            // persistent.internal.InvokerTask run starts for a persistent timer during server shutdown,
            // but EJB timer service throws exception due to server stopping.

            if (server != null && server.isStarted()) {
                server.stopServer("CNTR0092W",
                                  "DSRA0304E",
                                  "DSRA0302E",
                                  "DSRA0230E.*TRANSACTION_FAIL",
                                  "J2CA0027E",
                                  "CWWKC1501W.*server is stopping",
                                  "CWWKC1503W.*server is stopping");
            }
        }
    }

    // All other test methods on this servlet are non-persistent; call this one directly
    @Test
    public void testGetScheduleForPersistentTimer() throws Exception {
        runTest(server, SCHEDULE_SERVLET, getTestMethodSimpleName());
    }

}

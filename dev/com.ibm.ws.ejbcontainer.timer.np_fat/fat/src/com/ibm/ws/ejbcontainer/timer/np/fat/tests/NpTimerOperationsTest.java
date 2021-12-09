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
package com.ibm.ws.ejbcontainer.timer.np.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.ejbcontainer.timer.np.web.NpTimerOperationsServlet;
import com.ibm.ws.ejbcontainer.timer.np.web.PassByValueServlet;
import com.ibm.ws.ejbcontainer.timer.np.web.SFSBRefTimerServlet;
import com.ibm.ws.ejbcontainer.timer.np.web.SLSBAnnotationTxImplServlet;
import com.ibm.ws.ejbcontainer.timer.np.web.SLSBCheckTimerAPIServlet;
import com.ibm.ws.ejbcontainer.timer.np.web.SingletonAnnotationTxImplServlet;
import com.ibm.ws.ejbcontainer.timer.np.web.SingletonCheckTimerAPIServlet;
import com.ibm.ws.ejbcontainer.timer.np.web.TimeoutFailureServlet;
import com.ibm.ws.ejbcontainer.timer.np.web.TimerTxServlet;
import com.ibm.ws.ejbcontainer.timer.np.web.XMLTxServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test to exercise the EJB Container Non-Persistent Timer support using session
 * beans. Verifies the "Allowed Operations" tables in the EJB specification for
 * session bean types. <p>
 *
 * The following scenarios are covered for each session bean type:
 *
 * <ul>
 * <li>TimerService access in bean that does not implement a timeout method.
 * <li>TimerService access in setSessionContext.
 * <li>TimerService access in PostCreate.
 * <li>TimerService access in business method.
 * <li>TimerService access in ejbTimeout.
 * <li>SessionContext access in ejbTimeout - CMT.
 * <li>SessionContext access in ejbTimeout - BMT.
 * <li>createTimer() IllegalArgumentExceptions.
 * </ul>
 *
 * Test the ability to access a Timer outside of a bean. <p>
 *
 * Test that the info and schedule of a non-persistent timer use pass-by-value
 * semantics. <p>
 *
 * Tests that verify the proper behavior of non-persistent timers defined for a
 * singleton session bean using only annotations - no XML, not implementing
 * TimedObject. <p>
 *
 * Tests that verify the proper behavior of non-persistent timers defined for a
 * stateless session bean using only annotations - no XML, not implementing
 * TimedObject. <p>
 *
 * Verify the transaction quality of Timer Service and the behavior
 * of non-persistent Timer objects. <p>
 *
 * <ul>
 * <li>testCreateAndCancelATimerInACommittedTran
 * <li>testCreateATimerInARolledBackTran
 * <li>testCancelATimerInARolledBackTran
 * <li>testEjbTimeoutNeverCommits()
 * <li>testCommittedEjbTimeoutTran
 * <li>testTimoutOccursAtCreateNotAtTxCommit()
 * <li>testRolledBackEjbTimeoutTran()
 * </ul>
 */
@RunWith(FATRunner.class)
public class NpTimerOperationsTest extends FATServletClient {
    @Server("com.ibm.ws.ejbcontainer.timer.np.fat.TimerServer")
    @TestServlets({ @TestServlet(servlet = NpTimerOperationsServlet.class, contextRoot = "NpTimersWeb"),
                    @TestServlet(servlet = PassByValueServlet.class, contextRoot = "NpTimersWeb"),
                    @TestServlet(servlet = SFSBRefTimerServlet.class, contextRoot = "NpTimersWeb"),
                    @TestServlet(servlet = SingletonAnnotationTxImplServlet.class, contextRoot = "NpTimersWeb"),
                    @TestServlet(servlet = SingletonCheckTimerAPIServlet.class, contextRoot = "NpTimersWeb"),
                    @TestServlet(servlet = SLSBAnnotationTxImplServlet.class, contextRoot = "NpTimersWeb"),
                    @TestServlet(servlet = SLSBCheckTimerAPIServlet.class, contextRoot = "NpTimersWeb"),
                    @TestServlet(servlet = TimeoutFailureServlet.class, contextRoot = "NpTimersWeb"),
                    @TestServlet(servlet = TimerTxServlet.class, contextRoot = "NpTimersWeb"),
                    @TestServlet(servlet = XMLTxServlet.class, contextRoot = "NpTimersWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.np.fat.TimerServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.timer.np.fat.TimerServer")).andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.np.fat.TimerServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### NpTimersApp.ear
        JavaArchive NpTimersEJB = ShrinkHelper.buildJavaArchive("NpTimersEJB.jar", "com.ibm.ws.ejbcontainer.timer.np.ejb.");
        JavaArchive NpTimerOperationsEJB = ShrinkHelper.buildJavaArchive("NpTimerOperationsEJB.jar", "com.ibm.ws.ejbcontainer.timer.np.operations.ejb.");
        WebArchive NpTimersWeb = ShrinkHelper.buildDefaultApp("NpTimersWeb.war", "com.ibm.ws.ejbcontainer.timer.np.web.");
        EnterpriseArchive NpTimersApp = ShrinkWrap.create(EnterpriseArchive.class, "NpTimersApp.ear");
        NpTimersApp.addAsModule(NpTimersEJB).addAsModule(NpTimerOperationsEJB).addAsModule(NpTimersWeb);

        ShrinkHelper.exportDropinAppToServer(server, NpTimersApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // CNTR0020E: EJB threw an unexpected (non-declared) exception
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0020E");
        }
    }

}

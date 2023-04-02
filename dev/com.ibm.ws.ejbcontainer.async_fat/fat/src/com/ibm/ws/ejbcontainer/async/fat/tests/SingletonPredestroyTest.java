/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.async.fat.tests;

import static org.junit.Assert.assertFalse;

import java.util.List;

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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Regression tests for deadlock scenario
 *
 * During Singleton PreDestroy an intercepter or just a method call to another bean that
 * turns around and calls a method on the Singleton used to result in a deadlock. Could also happen
 * if the other bean has an asynchronous call on the Singleton that goes off at the same time.
 */
@RunWith(FATRunner.class)
public class SingletonPredestroyTest extends FATServletClient {

    @Server("com.ibm.ws.ejbcontainer.async.fat.AsyncCoreServer")
    public static LibertyServer server;
    private static final String MESSAGE_LOG = "logs/messages.log";

    private static String appName = "SingletonLifecycleDeadlockTest";

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.async.fat.AsyncCoreServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.async.fat.AsyncCoreServer")).andWith(new JakartaEE9Action().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11).forServers("com.ibm.ws.ejbcontainer.async.fat.AsyncCoreServer")).andWith(new JakartaEE10Action().forServers("com.ibm.ws.ejbcontainer.async.fat.AsyncCoreServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // cleanup from prior repeat actions
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        JavaArchive SingletonLifecycleDeadlockEJB = ShrinkHelper.buildJavaArchive("SingletonLifecycleDeadlockEJB.jar",
                                                                                  "com.ibm.ws.ejbcontainer.singletonlifecycledeadlock.ejb.");
        EnterpriseArchive SingletonLifecycleDeadlockTest = ShrinkWrap.create(EnterpriseArchive.class, appName + ".ear");
        WebArchive SingletonLifecycleDeadlockWeb = ShrinkHelper.buildDefaultApp("SingletonLifecycleDeadlockWeb.war",
                                                                                "com.ibm.ws.ejbcontainer.singletonlifecycledeadlock.web.");
        SingletonLifecycleDeadlockTest.addAsModule(SingletonLifecycleDeadlockEJB).addAsModule(SingletonLifecycleDeadlockWeb);

        ShrinkHelper.exportDropinAppToServer(server, SingletonLifecycleDeadlockTest, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            // Never ignore CWWKE1102W or CWWKE1107W it's basically what the PreDestroy test is testing.
            // CWWKE1102W: The quiesce operation did not complete. The server will now stop.
            // CWWKE1107W: 2 threads did not complete during the quiesce period.
            server.stopServer();
        }
    }

    @Test
    public void SingletonPreDestroyDeadlockTest() throws Exception {
        server.setMarkToEndOfLog();
        server.deleteAllDropinApplications();
        // CWWKZ0009I: The application SingletonLifecycleDeadlockTest has stopped successfully.
        server.waitForStringInLog("CWWKZ0009I:.*SingletonLifecycleDeadlockTest");

        List<String> messageList = server.findStringsInLogsUsingMark("SingletonPDBean: PreDestroy: done", MESSAGE_LOG);
        assertFalse("Did not find 'Singleton Predestroy done' message in log", messageList.isEmpty());
    }
}

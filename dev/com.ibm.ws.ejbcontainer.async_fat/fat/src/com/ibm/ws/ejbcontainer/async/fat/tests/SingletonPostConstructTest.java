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
import com.ibm.ws.ejbcontainer.singletonlifecycledeadlock.web.SingletonPostConstructServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Regression tests for a deadlock scenario
 *
 * During Singleton PostConstruct an intercepter or just a method call to another bean that
 * turns around and calls a method on the Singleton used to result in a deadlock. Could also happen
 * if the other bean has an asynchronous call on the Singleton that goes off at the same time.
 *
 * Singleton creation still needs to only happen once so we still synchronize/lock the creation but
 * there is a timeout now (configurable by @AcessTimeout) to fail creating the bean during the deadlock
 */
@RunWith(FATRunner.class)
public class SingletonPostConstructTest extends FATServletClient {

    @Server("com.ibm.ws.ejbcontainer.async.fat.AsyncCoreServer")
    @TestServlets({ @TestServlet(servlet = SingletonPostConstructServlet.class, contextRoot = "SingletonLifecycleDeadlockWeb") })
    public static LibertyServer server;

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
            // Never ignore CWWKE1102W or CWWKE1107W hitting it means possible deadlock for PostConstruct
            // CWWKE1102W: The quiesce operation did not complete. The server will now stop.
            // CWWKE1107W: 2 threads did not complete during the quiesce period.

            //CNTR0019E: EJB threw an unexpected (non-declared) exception during invocation of method "businessMethod". Exception data: javax.ejb.ConcurrentAccessTimeoutException: Timeout occured trying to acquire singleton session bean SingletonLifecycleDeadlockTest#SingletonLifecycleDeadlockEJB.jar#SingletonPostConstructBean.
            //CNTR0020E: EJB threw an unexpected (non-declared) exception during invocation of method "asyncMethod" on bean "BeanId(SingletonLifecycleDeadlockTest#SingletonLifecycleDeadlockEJB.jar#OtherPCBean, null)". Exception data: javax.ejb.EJBTransactionRolledbackException: nested exception is: javax.ejb.ConcurrentAccessTimeoutException: Timeout occured trying to acquire singleton session bean SingletonLifecycleDeadlockTest#SingletonLifecycleDeadlockEJB.jar#SingletonPostConstructBean.
            server.stopServer("CNTR0019E", "CNTR0020E");
        }
    }
}

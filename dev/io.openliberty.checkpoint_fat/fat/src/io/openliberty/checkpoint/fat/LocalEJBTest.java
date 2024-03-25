/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import ejbapp1.EJBEvent;
import ejbapp1.LocalEJBServlet;
import ejbapp1.LocalInterface;
import ejbapp1.TestObserver;
import ejbapp1.TimerTest;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class LocalEJBTest extends FATServletClient {

    public static final String SERVER_NAME = "checkpointEJB";
    public static final String REMOTE_EJB_APP_NAME = "ejbapp1";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = ejbapp1.LocalEJBServlet.class, contextRoot = REMOTE_EJB_APP_NAME)
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.defaultMPRepeat(SERVER_NAME);

    @Before
    public void setUp() throws Exception {
        WebArchive ejbMisc = ShrinkWrap.create(WebArchive.class, REMOTE_EJB_APP_NAME + ".war")
                        .addClass(LocalEJBServlet.class)
                        .addClass(TestObserver.class)
                        .addClass(LocalInterface.class)
                        .addClass(EJBEvent.class)
                        .addClass(TimerTest.class)
                        .addPackages(true, LocalEJBServlet.class.getPackage())
                        .add(new FileAsset(new File("test-applications/" + REMOTE_EJB_APP_NAME + "/resources/META-INF/permissions.xml")),
                             "/META-INF/permissions.xml")
                        .add(new FileAsset(new File("test-applications/" + REMOTE_EJB_APP_NAME + "/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        ShrinkHelper.exportDropinAppToServer(server, ejbMisc, DeployOptions.SERVER_ONLY);

        TestMethod testMethod = getTestMethod(TestMethod.class, testName);
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                             server -> {

                                 assertNotNull("'SRVE0169I: Loading Web Module: " + REMOTE_EJB_APP_NAME + "' message not found in log before restore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: .*" + REMOTE_EJB_APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: Application " + REMOTE_EJB_APP_NAME + " started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: .*" + REMOTE_EJB_APP_NAME, 0));
                                 if (testMethod == TestMethod.testNonPersistentTimers) {
                                     try {
                                         // Need to sleep a bit for timer test to cause
                                         // timer time to passed and force it to catchup
                                         Thread.sleep(6000);
                                     } catch (InterruptedException e) {
                                         Assert.fail();
                                     }
                                 }
                             });
        server.startServer(getTestMethodNameOnly(testName) + ".log");
    }

    @Test
    public void testAtApplicationsMultiRestore() throws Exception {
        HttpUtils.findStringInUrl(server, REMOTE_EJB_APP_NAME, "Got RemoteEJBServlet");

        server.stopServer(false);
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, REMOTE_EJB_APP_NAME, "Got RemoteEJBServlet");

        server.stopServer(false);
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, REMOTE_EJB_APP_NAME, "Got RemoteEJBServlet");
    }

    @Test
    public void testNonPersistentTimers() throws Exception {
        String result = server.waitForStringInLogUsingMark("TIMER TEST - .*");
        assertNotNull("No TIMER TEST found in log", result);
        assertTrue("Unexpected value: " + result, result.contains("PASSED"));
    }

    @After
    public void stopServer() throws Exception {
        server.stopServer();
    }

    @AfterClass
    public static void removeWebApp() throws Exception {
        ShrinkHelper.cleanAllExportedArchives();
    }

    static enum TestMethod {
        testAtApplicationsMultiRestore,
        testNonPersistentTimers,
        unknown;
    }
}

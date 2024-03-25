/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.jaxws.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.jaxws.suite.FATSuite;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class EJBWSBasicTest {

    private static final String SERVER_NAME = "EJBWSBasicServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static final String ejbwsbasicjar = "EJBWSBasic";
    private static final String ejbwsbasicclientwar = "EJBWSBasicClient";
    private static final String ejbwsbasicear = "EJBWSBasic";

    private static final String SERVLET_PATH = "/EJBWSBasicClient/EJBBasicClientServlet";

    @Rule
    public final TestName testName = new TestName();

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @BeforeClass
    public static void beforeAllTests() throws Exception {

        JavaArchive jar = ShrinkHelper.buildJavaArchive(ejbwsbasicjar + ".jar", "io.openliberty.checkpoint.testapp.jaxws.ejbbasic",
                                                        "io.openliberty.checkpoint.testapp.jaxws.ejbbasic.view");

        WebArchive war = ShrinkWrap.create(WebArchive.class, ejbwsbasicclientwar + ".war")
                        .addPackages(true, "io.openliberty.checkpoint.testapp.jaxws.ejbbasic.client", "io.openliberty.checkpoint.testapp.jaxws.ejbbasic.view.client");
        ShrinkHelper.addDirectory(war, "test-applications/EJBWSBasicClient/resources/");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ejbwsbasicear + ".ear").addAsModule(jar).addAsModule(war);

        ShrinkHelper.exportDropinAppToServer(server, ear);

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                             server -> {
                                 assertNotNull("'CWWKZ0001I: ' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I.*EJBWSBasic", 0));
                             });
        server.startServer();
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testQueryUser() throws Exception {
        runTest("PASS");
    }

    /*
     * TODO: Investigate why the correct custom exception (UserNotFoundException) is being thrown, but
     * is now being wrapped in an InvocationTargetException
     */
    @Test
    public void testUserNotFoundException() throws Exception {
        runTest("PASS");
    }

    @Test
    public void testListUsers() throws Exception {
        runTest("PASS");
    }

    @Test
    public void testQueryUserBasicAsyncHandler() throws Exception {
        runTest("PASS");
    }

    @Test
    public void testQueryUserBasicAsyncResponse() throws Exception {
        runTest("PASS");
    }

    @Test
    public void testQueryUserBasicAsyncHandler_EJB() throws Exception {
        runTest("PASS");
    }

    @Test
    public void testQueryUserBasicAsyncResponse_EJB() throws Exception {
        runTest("PASS");
    }

    @Test
    public void testInConsistentNamespace() throws Exception {
        runTest("PASS");
    }

    protected void runTest(String responseString) throws Exception {

        String testMethod = testName.getMethodName();
        if (testMethod.contains("_EE")) {
            testMethod = testMethod.substring(0, testMethod.indexOf("_EE"));
        }

        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname())
                        .append(":")
                        .append(server.getHttpDefaultPort())
                        .append(SERVLET_PATH)
                        .append("?testMethod=")
                        .append(testMethod);
        String urlStr = sBuilder.toString();
        Log.info(this.getClass(), testMethod, "Calling Application with URL=" + urlStr);

        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK, 10);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();

        assertTrue("The excepted response must contain " + responseString + " while " + line + " is received", line.contains(responseString));
    }
}

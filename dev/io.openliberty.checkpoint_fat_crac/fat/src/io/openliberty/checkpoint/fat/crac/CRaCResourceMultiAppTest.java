/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat.crac;

import static io.openliberty.checkpoint.fat.crac.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.WebApplication;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.fat.crac.app.multiple1.MultiApp1Servlet;
import io.openliberty.checkpoint.fat.crac.app.multiple2.MultiApp2Servlet;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class CRaCResourceMultiAppTest {

    public static final String APP1_NAME = "testApp1";
    public static final String APP1_PACKAGE = MultiApp1Servlet.class.getPackage().getName();
    public static final String APP2_NAME = "testApp2";
    public static final String APP2_PACKAGE = MultiApp2Servlet.class.getPackage().getName();

    @Rule
    public TestName testName = new TestName();

    @Server("cracFATServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests repeatTest = MicroProfileActions.repeat("cracFATServer", TestMode.FULL, //
                                                                      MicroProfileActions.MP41, // first test in LITE mode
                                                                      // rest are FULL mode
                                                                      MicroProfileActions.MP50, MicroProfileActions.MP60);

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        ShrinkHelper.defaultApp(server, APP1_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP1_PACKAGE);
        ShrinkHelper.defaultApp(server, APP2_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP2_PACKAGE);
    }

    @Before
    public void setUp() throws Exception {
        ServerConfiguration serverConfig = server.getServerConfiguration();
        ConfigElementList<WebApplication> webApplications = serverConfig.getWebApplications();
        webApplications.clear();

        WebApplication app1 = new WebApplication();
        app1.setLocation(APP1_NAME + ".war");
        app1.setName(APP1_NAME);
        webApplications.add(app1);

        WebApplication app2 = new WebApplication();
        app2.setLocation(APP2_NAME + ".war");
        app2.setName(APP2_NAME);
        webApplications.add(app2);
        server.updateServerConfiguration(serverConfig);

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                             server -> {
                                 assertNotNull("'SRVE0169I: ' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: Loading Web Module: " + APP1_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: ' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: Application " + APP1_NAME + " started", 0));

                                 assertNotNull("'SRVE0169I: ' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: Loading Web Module: " + APP2_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: ' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: Application " + APP2_NAME + " started", 0));

                                 assertNotNull("beforeCheckpoint not called",
                                               server.waitForStringInLogUsingMark("MultiApp1Servlet: TESTING - beforeCheckpoint " + 3, 0));
                                 assertNotNull("beforeCheckpoint not called",
                                               server.waitForStringInLogUsingMark("MultiApp1Servlet: TESTING - beforeCheckpoint " + 2, 0));
                                 assertNotNull("beforeCheckpoint not called",
                                               server.waitForStringInLogUsingMark("MultiApp1Servlet: TESTING - beforeCheckpoint " + 1, 0));

                                 assertNotNull("beforeCheckpoint not called",
                                               server.waitForStringInLogUsingMark("MultiApp2Servlet: TESTING - beforeCheckpoint " + 3, 0));
                                 assertNotNull("beforeCheckpoint not called",
                                               server.waitForStringInLogUsingMark("MultiApp2Servlet: TESTING - beforeCheckpoint " + 2, 0));
                                 assertNotNull("beforeCheckpoint not called",
                                               server.waitForStringInLogUsingMark("MultiApp2Servlet: TESTING - beforeCheckpoint " + 1, 0));
                             });

    }

    @Test
    public void testMultiAppCheckpointRestore() throws Exception {
        server.startServer(getTestMethodNameOnly(testName) + ".log");

        assertNotNull("afterRestore not called",
                      server.waitForStringInLogUsingMark("MultiApp1Servlet: TESTING - afterRestore " + 1, 0));
        assertNotNull("afterRestore not called",
                      server.waitForStringInLogUsingMark("MultiApp1Servlet: TESTING - afterRestore " + 2, 0));
        assertNotNull("afterRestore not called",
                      server.waitForStringInLogUsingMark("MultiApp1Servlet: TESTING - afterRestore " + 3, 0));

        assertNotNull("afterRestore not called",
                      server.waitForStringInLogUsingMark("MultiApp2Servlet: TESTING - afterRestore " + 1, 0));
        assertNotNull("afterRestore not called",
                      server.waitForStringInLogUsingMark("MultiApp2Servlet: TESTING - afterRestore " + 2, 0));
        assertNotNull("afterRestore not called",
                      server.waitForStringInLogUsingMark("MultiApp2Servlet: TESTING - afterRestore " + 3, 0));

        checkApplicationConnection(APP1_NAME);
        checkApplicationConnection(APP2_NAME);
    }

    public void checkApplicationConnection(String appName) throws Exception {
        URL url = createURL(appName + "/test");
        Log.info(getClass(), testName.getMethodName(), "Calling URL: " + url.toExternalForm());
        String response = HttpUtils.getHttpResponseAsString(url);
        assertNotNull(response);
        assertTrue(response.contains("TESTING - service: SUCCESS - "));
    }

    public static URL createURL(String path) throws MalformedURLException {
        if (!path.startsWith("/"))
            path = "/" + path;
        return new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + path);
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

}

/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
package io.openliberty.checkpoint.messaging.JMS20.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 *
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class JMSMDBTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("MDBTest");

    @ClassRule
    public static RepeatTests r1 = EERepeatActions.repeat(server.getServerName(), TestMode.FULL, false, EERepeatActions.EE11,
                                                          EERepeatActions.EE10, EERepeatActions.EE9, EERepeatActions.EE8);

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    boolean val = false;

    private boolean runInServlet(String test) throws IOException {

        boolean result = false;
        URL url = new URL("http://" + HOST + ":" + PORT
                          + "/MDBTestWeb?test=" + test);
        System.out.println("The Servlet URL is : " + url.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            con.connect();

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String sep = System.lineSeparator();
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            if (lines.indexOf(test + " COMPLETED SUCCESSFULLY") < 0) {
                org.junit.Assert.fail("Missing success message in output. "
                                      + lines);
                result = false;
            } else
                result = true;

            return result;

        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");
        server.copyFileToLibertyServerRoot("resources/security",
                                           "clientLTPAKeys/mykey.jks");
        ShrinkHelper.cleanAllExportedArchives();
        TestUtils.addDropinsWebApp(server, "MDBTestWeb", "mdb.test.web");
        TestUtils.addDropinsWebApp(server, "MDBTestApp", "mdb.test.app");

        final FATSuite.PortSetting JMSPortSetting = new FATSuite.PortSetting("bvt.prop.jms", 7276, "jms_port");
        final FATSuite.PortSetting JMSPortSSLSetting = new FATSuite.PortSetting("bvt.prop.jms.ssl", 7286, "jms_port_ssl");

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer("JMSMDBTest.log");
        FATSuite.addServerEnvPorts(server, Arrays.asList(JMSPortSetting, JMSPortSSLSetting));
        server.checkpointRestore();

        String waitFor = server.waitForStringInLog("CWWKZ0001I:.*MDBTestWeb", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
    }

    @AfterClass
    public static void tearDown() {
        try {
            System.out.println("Stopping server");
            server.stopServer();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        ShrinkHelper.cleanAllExportedArchives();
    }

    @Test
    public void testQueueMDB() throws Exception {
        val = runInServlet("testQueueMDB");
        assertTrue("testQueueMDB failed ", val);

        String msg = server.waitForStringInLog(
                                               "Message received on Annotated MDB: testQueueMDB", 5000);
        assertNotNull("Test testQueueMDB failed", msg);

    }

}

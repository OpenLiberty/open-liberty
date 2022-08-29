/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile6.internal.test.suite;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile6.internal.test.helloworld.HelloWorldApplication;
import io.openliberty.microprofile6.internal.test.helloworld.basic.BasicHelloWorldBean;
import io.openliberty.microprofile6.internal.test.helloworld.config.ConfiguredHelloWorldBean;

public class MPCompatibilityTestUtils {

    private static final String APP_NAME = "helloworld";

    protected static final String MESSAGE = BasicHelloWorldBean.MESSAGE;

    protected static void setUp(LibertyServer server) throws Exception {
        setUp(server, true);
    }

    protected static void setUp(LibertyServer server, boolean startServer) throws Exception {

        PropertiesAsset config = new PropertiesAsset().addProperty("message", MESSAGE);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(HelloWorldApplication.class.getPackage())
                                   .addPackage(ConfiguredHelloWorldBean.class.getPackage())
                                   .addAsResource(config, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        if (startServer) {
            server.startServer();
        }
    }

    protected static void cleanUp(LibertyServer server, String... ignoredFailuresRegExps) throws Exception {
        if ((server != null) && (server.isStarted())) {
            server.stopServer(ignoredFailuresRegExps);
        }
    }

    protected static void validateMessagesAndStop(LibertyServer server, String... expectedFailuresRegExps) throws Exception {
        if (server != null) {
            for (String msg : expectedFailuresRegExps) {
                assertServerMessage(server, msg);
            }
            if (server.isStarted()) {
                server.stopServer(expectedFailuresRegExps);
            }
        } else {
            fail("Server was null");
        }
    }

    protected static void assertServerMessage(LibertyServer server, String expectedFailuresRegExp) throws Exception {
        List<String> msgs = server.findStringsInLogs(expectedFailuresRegExp);
        assertTrue("Message not found in server logs: " + expectedFailuresRegExp, msgs != null && msgs.size() > 0);
    }

    protected static StringBuilder runGetMethod(LibertyServer server, int exprc, String requestUri, String testOut) throws IOException {
        URL url = new URL("http://" + getHost(server) + ":" + getPort(server) + requestUri);
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
            }

            if (lines.indexOf(testOut) < 0) {
                fail("Missing success message in output. " + lines);
            }

            if (retcode != exprc) {
                fail("Bad return Code from Get. Expected " + exprc + "Got"
                     + retcode);
            }

            return lines;
        } finally {
            con.disconnect();
        }
    }

    private static int getPort(LibertyServer server) {
        return server.getHttpDefaultPort();
    }

    private static String getHost(LibertyServer server) {
        return server.getHostname();
    }

}

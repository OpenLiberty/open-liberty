/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile10.fat.suite;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile10.fat.tests.helloworld.HelloWorldApplication;
import com.ibm.ws.microprofile10.fat.tests.helloworld.HelloWorldBean;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class SimpleJAXRSCDITest {

    private static final String SERVER_NAME = "MPServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static final String APP_NAME = "helloworld";

    private static final String MESSAGE = HelloWorldBean.MESSAGE;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(HelloWorldApplication.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer("CWMOT0010W"); //CWMOT0010W: OpenTracing cannot track JAX-RS requests because an OpentracingTracerFactory class was not provided or client libraries for tracing backend are not in the class path.
        }
    }

    private static final String[] MP_VERSIONS = { "1.0", "1.2", "1.3", "1.4", "2.0", "2.1", "2.2", "3.0", "3.2", "3.3", "4.0" };
    private static final String LITE_MODE = "3.3";

    @ClassRule
    public static RepeatTests r = getMPRepeat();

    private static RepeatTests getMPRepeat() {
        RepeatTests repeat = RepeatTests.with(new MicroProfile(LITE_MODE));
        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            for (String ver : MP_VERSIONS) {
                if (!ver.equals(LITE_MODE)) {
                    repeat = repeat.andWith(new MicroProfile(ver));
                }
            }
        }

        return repeat;
    }

    static class MicroProfile extends FeatureReplacementAction {
        public MicroProfile(String version) {
            for (String ver : MP_VERSIONS) {
                if (ver.equals(version)) {
                    addFeature("microProfile-" + ver);
                } else {
                    removeFeature("microProfile-" + ver);
                }
            }
            forServers(SERVER_NAME);
            withID("MP" + version);
        }
    }

    @Test
    public void testSimple() throws IOException {
        runGetMethod(200, "/helloworld/helloworld", MESSAGE);
    }

    private StringBuilder runGetMethod(int exprc, String requestUri, String testOut) throws IOException {
        URL url = new URL("http://" + getHost() + ":" + getPort() + requestUri);
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
            for (String line = br.readLine(); line != null; line = br
                                                                     .readLine())
                lines.append(line).append(sep);

            if (lines.indexOf(testOut) < 0)
                fail("Missing success message in output. " + lines);

            if (retcode != exprc)
                fail("Bad return Code from Get. Expected " + exprc + "Got"
                     + retcode);

            return lines;
        } finally {
            con.disconnect();
        }
    }

    private int getPort() {
        return server.getHttpDefaultPort();
    }

    private String getHost() {
        return server.getHostname();
    }

}

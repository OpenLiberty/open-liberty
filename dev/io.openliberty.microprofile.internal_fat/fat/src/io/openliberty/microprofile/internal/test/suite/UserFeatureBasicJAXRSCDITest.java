/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.internal.test.suite;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
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
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.internal.test.helloworld.HelloWorldApplication;
import io.openliberty.microprofile.internal.test.helloworld.basic.BasicHelloWorldBean;

@RunWith(FATRunner.class)
public class UserFeatureBasicJAXRSCDITest {

    private static final String SERVER_NAME = "MPUserFeatureServer";

    //MP41 will be run in LITE mode. The others will be run in FULL.
    private static RepeatTests repeatAll() {
        List<FeatureSet> others = new ArrayList<>(MicroProfileActions.ALL_PLUS);
        others.remove(MicroProfileActions.MP41);
        return MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.MP41, others);
    }

    @ClassRule
    public static RepeatTests r = repeatAll();

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static final String APP_NAME = "helloworld";

    private static final String MESSAGE = BasicHelloWorldBean.MESSAGE;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(HelloWorldApplication.class.getPackage())
                                   .addPackage(BasicHelloWorldBean.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
        installUserFeature(server);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer("CWMOT0010W"); //CWMOT0010W: OpenTracing cannot track JAX-RS requests because an OpentracingTracerFactory class was not provided or client libraries for tracing backend are not in the class path.
        }
        uninstallUserFeature(server);
    }

    private static void installUserFeature(LibertyServer server) throws Exception {
        server.installUserFeature("io.extension.mpTestFeature-1.0");
        server.installUserFeature("io.extension.mpTestFeature.internal-1.0");
        server.installUserFeature("io.extension.mpTestFeature.internal-1.2.7");
        server.installUserFeature("io.extension.mpTestFeature.internal-1.2.8");
        server.installUserFeature("io.extension.mpTestFeature.internal-1.3.7");
        server.installUserFeature("io.extension.mpTestFeature.internal-1.3.8");
        server.installUserFeature("io.extension.mpTestFeature.internal-1.4.7");
        server.installUserFeature("io.extension.mpTestFeature.internal-1.4.8");
        server.installUserFeature("io.extension.mpTestFeature.internal-2.0");
        server.installUserFeature("io.extension.mpTestFeature.internal-2.1");
        server.installUserFeature("io.extension.mpTestFeature.internal-2.2");
        server.installUserFeature("io.extension.mpTestFeature.internal-3.0");
        server.installUserFeature("io.extension.mpTestFeature.internal-3.2");
        server.installUserFeature("io.extension.mpTestFeature.internal-3.3");
        server.installUserFeature("io.extension.mpTestFeature.internal-4.0");
        server.installUserFeature("io.extension.mpTestFeature.internal-4.1");
        server.installUserFeature("io.extension.mpTestFeature.internal-5.0");
        server.installUserFeature("io.extension.mpTestFeature.internal-6.0");
        server.installUserFeature("io.extension.mpTestFeature.internal-6.1");
        server.installUserFeature("io.extension.mpTestFeature.internal-7.0.10");
        server.installUserFeature("io.extension.mpTestFeature.internal-7.0.11");
        server.installUserFeature("io.extension.mpTestFeature.internal-7.1.10");
        server.installUserFeature("io.extension.mpTestFeature.internal-7.1.11");
    }

    private static void uninstallUserFeature(LibertyServer server) throws Exception {
        server.uninstallUserFeature("io.extension.mpTestFeature-1.0");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-1.0");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-1.2.7");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-1.2.8");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-1.3.7");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-1.3.8");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-1.4.7");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-1.4.8");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-2.0");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-2.1");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-2.2");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-3.0");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-3.2");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-3.3");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-4.0");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-4.1");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-5.0");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-6.0");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-6.1");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-7.0.10");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-7.0.11");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-7.1.10");
        server.uninstallUserFeature("io.extension.mpTestFeature.internal-7.1.11");
    }

    @Test
    public void testBasicJAXRSCDI() throws IOException {
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

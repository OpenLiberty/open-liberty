/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile5.internal.test.suite;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile5.internal.test.helloworld.HelloWorldApplication;
import io.openliberty.microprofile5.internal.test.helloworld.basic.BasicHelloWorldBean;
import io.openliberty.microprofile5.internal.test.helloworld.config.ConfiguredHelloWorldBean;

@RunWith(FATRunner.class)
public class MP5CompatibleTest {

    private static final String SERVER_NAME = "MP5CompatibleServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static final String APP_NAME = "helloworld";

    private static final String MESSAGE = BasicHelloWorldBean.MESSAGE;

    @BeforeClass
    public static void setUp() throws Exception {

        PropertiesAsset config = new PropertiesAsset().addProperty("message", MESSAGE);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(HelloWorldApplication.class.getPackage())
                                   .addPackage(ConfiguredHelloWorldBean.class.getPackage())
                                   .addAsResource(config, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
    }

    /**
     * Just microProfile-5.0 ... Should always pass, a sanity check
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void testMicroProfile50() throws Exception {
        try {
            server.setServerConfigurationFromFilePath("MP50.xml");
            server.startServer();
            runGetMethod(200, "/helloworld/helloworld", MESSAGE);
        } finally {
            server.stopServer("CWMOT0010W"); //CWMOT0010W: OpenTracing cannot track JAX-RS requests because an OpentracingTracerFactory class was not provided or client libraries for tracing backend are not in the class path.
        }
    }

    /**
     * microProfile-5.0 plus jakartaee-9.1
     * Should always pass
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMP50andEE9() throws Exception {
        try {
            server.setServerConfigurationFromFilePath("MP50andEE9.xml");
            server.startServer();
            runGetMethod(200, "/helloworld/helloworld", MESSAGE);
        } finally {
            server.stopServer();
            //server.stopServer("CWMOT0010W"); //CWMOT0010W: OpenTracing cannot track JAX-RS requests because an OpentracingTracerFactory class was not provided or client libraries for tracing backend are not in the class path.
        }
    }

    /**
     * microProfile-5.0 plus jakartaee-8.0
     * Should fail because microProfile-5.0 is not compatible with jakartaee-8.0
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testMP50andEE8() throws Exception {
        try {
            server.setServerConfigurationFromFilePath("MP50andEE8.xml");
            server.startServerAndValidate(true, //preClean
                                          false, //cleanStart
                                          false, //validateApps
                                          false, //expectStartFailure
                                          false); //validateTimedExit
        } finally {
            server.stopServer("CWWKF0033E: The singleton features .* and .* cannot be loaded at the same time",
                              "CWWKF0044E: The .* and .* features cannot be loaded at the same time",
                              "CWWKF0046W: The configuration includes an incompatible combination of features");
        }
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

    private int getPort() {
        return server.getHttpDefaultPort();
    }

    private String getHost() {
        return server.getHostname();
    }

}

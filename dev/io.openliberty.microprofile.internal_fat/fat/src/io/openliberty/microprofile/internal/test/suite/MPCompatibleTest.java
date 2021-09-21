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
package io.openliberty.microprofile.internal.test.suite;

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
import io.openliberty.microprofile.internal.test.helloworld.HelloWorldApplication;
import io.openliberty.microprofile.internal.test.helloworld.basic.BasicHelloWorldBean;
import io.openliberty.microprofile.internal.test.helloworld.config.ConfiguredHelloWorldBean;

@RunWith(FATRunner.class)
public class MPCompatibleTest {

    private static final String SERVER_NAME = "MPCompatibleServer";

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
     * Just microProfile-4.0 ... Should always pass, a sanity check
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void testMicroProfile40() throws Exception {
        try {
            server.setServerConfigurationFromFilePath("MP40.xml");
            server.startServer();
            runGetMethod(200, "/helloworld/helloworld", MESSAGE);
        } finally {
            server.stopServer("CWMOT0010W"); //CWMOT0010W: OpenTracing cannot track JAX-RS requests because an OpentracingTracerFactory class was not provided or client libraries for tracing backend are not in the class path.
        }
    }

    /**
     * microProfile-4.0 plus concurrent-1.0
     * Should pass despite the fact the concurrent-1.0 pulls in the MP Context Propagation 1.0 API feature.
     * The MP Context Propagation 1.0 impl should not work with MP 4.0 but pulling in just the API should still be OK.
     * TBH this isn't ideal but is just the way the concurrent impl has been designed.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMP40andConcurrent10() throws Exception {
        try {
            server.setServerConfigurationFromFilePath("MP40andConcurrent10.xml");
            server.startServer();
            runGetMethod(200, "/helloworld/helloworld", MESSAGE);
        } finally {
            server.stopServer("CWMOT0010W"); //CWMOT0010W: OpenTracing cannot track JAX-RS requests because an OpentracingTracerFactory class was not provided or client libraries for tracing backend are not in the class path.
        }
    }

    /**
     * microProfile-4.0 plus concurrent-2.0
     * Should fail because concurrent-2.0 depends on eeCompatible-9.0.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testMP40andConcurrent20() throws Exception {
        try {
            server.setServerConfigurationFromFilePath("MP40andConcurrent20.xml");
            server.startServerAndValidate(true, //preClean
                                          false, //cleanStart
                                          false, //validateApps
                                          false, //expectStartFailure
                                          false); //validateTimedExit
        } finally {
            server.stopServer("CWWKF0044E: The concurrent-2.0 and .* features cannot be loaded at the same time",
                              "CWWKF0046W: The configuration includes an incompatible combination of features");
        }
    }

    /**
     * microProfile-4.0 plus jakartaee-8.0
     * Should always pass
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMP40andEE8() throws Exception {
        try {
            server.setServerConfigurationFromFilePath("MP40andEE8.xml");
            server.startServer();
            runGetMethod(200, "/helloworld/helloworld", MESSAGE);
        } finally {
            server.stopServer("CWMOT0010W"); //CWMOT0010W: OpenTracing cannot track JAX-RS requests because an OpentracingTracerFactory class was not provided or client libraries for tracing backend are not in the class path.
        }
    }

    /**
     * microProfile-4.0 plus jakartaee-9.1
     * Should fail because microProfile-4.0 is not compatible with jakartaee-9.1
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testMP40andEE9() throws Exception {
        try {
            server.setServerConfigurationFromFilePath("MP40andEE9.xml");
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

    /**
     * microProfile-4.0 plus microProfile-3.3
     * Should fail because microProfile-4.0 can not be started at the same time as microProfile-3.3
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testMP40andMP33() throws Exception {
        try {
            server.setServerConfigurationFromFilePath("MP40andMP33.xml");
            server.startServerAndValidate(true, //preClean
                                          false, //cleanStart
                                          false, //validateApps
                                          false, //expectStartFailure
                                          false); //validateTimedExit
        } finally {
            server.stopServer("CWWKF0033E: The singleton features microProfile-3.3 and microProfile-4.0 cannot be loaded at the same time.",
                              "CWWKF0046W: The configuration includes an incompatible combination of features");
        }
    }

    /**
     * microProfile-4.0 plus mpConfig-1.4
     * Should fail because microProfile-4.0 can not be started at the same time as mpConfig-1.4
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testMP40andMPConfig14() throws Exception {
        try {
            server.setServerConfigurationFromFilePath("MP40andMPConfig14.xml");
            server.startServerAndValidate(true, //preClean
                                          false, //cleanStart
                                          false, //validateApps
                                          false, //expectStartFailure
                                          false); //validateTimedExit
        } finally {
            server.stopServer("CWWKF0033E: The singleton features mpConfig-1.4 and mpConfig-2.0 cannot be loaded at the same time.",
                              "CWWKF0046W: The configuration includes an incompatible combination of features");
        }
    }

    /**
     * microProfile-4.0 plus mpContextPropagation-1.0
     * Should fail because microProfile-4.0 can not be started at the same time as mpContextPropagation-1.0
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testMP40andCtxPropagtion10() throws Exception {
        try {
            server.setServerConfigurationFromFilePath("MP40andMPCtx10.xml");
            server.startServerAndValidate(true, //preClean
                                          false, //cleanStart
                                          false, //validateApps
                                          false, //expectStartFailure
                                          false); //validateTimedExit
        } finally {
            server.stopServer("CWWKF0033E: The singleton features io.openliberty.mpCompatible-0.0 and io.openliberty.mpCompatible-4.0 cannot be loaded at the same time.",
                              "CWWKF0046W: The configuration includes an incompatible combination of features");
        }
    }

    /**
     * microProfile-4.0 plus mpContextPropagation-1.2
     * Should always pass
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void testMP40andCtxPropagtion12() throws Exception {
        try {
            server.setServerConfigurationFromFilePath("MP40andMPCtx12.xml");
            server.startServer();
            runGetMethod(200, "/helloworld/helloworld", MESSAGE);
        } finally {
            server.stopServer("CWMOT0010W"); //CWMOT0010W: OpenTracing cannot track JAX-RS requests because an OpentracingTracerFactory class was not provided or client libraries for tracing backend are not in the class path.
        }
    }

    /**
     * microProfile-3.3 plus mpGraphQL-1.0 and mpReactiveMessaging-1.0
     * Should pass.
     *
     * mpGraphQL-1.0 currently produces some warnings that need to be investigated
     * https://github.com/OpenLiberty/open-liberty/issues/15496
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMP33plusStandalone() throws Exception {
        try {
            server.setServerConfigurationFromFilePath("MP33plusStandalone.xml");
            server.startServer();
            runGetMethod(200, "/helloworld/helloworld", MESSAGE);
        } finally {
            //I think CWWWC0002W is a configuration error and should not appear
            //Should be removed by issue 15496
            server.stopServer("CWMOT0010W", //CWMOT0010W: OpenTracing cannot track JAX-RS requests because an OpentracingTracerFactory class was not provided or client libraries for tracing backend are not in the class path.
                              "CWWWC0002W");//CWWWC0002W: No servlet definition is found for the ExecutionServlet servlet name in the AuthorizationFilter filter mapping.
        }
    }

    /**
     * microProfile-4.0 plus mpGraphQL-1.0
     * mpReactiveMessaging-1.0 should be included once the following feature is completed
     * https://github.com/OpenLiberty/open-liberty/issues/15440
     *
     * mpGraphQL-1.0 currently produces some warnings that need to be investigated
     * https://github.com/OpenLiberty/open-liberty/issues/15496
     *
     * Should pass.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMP40plusStandalone() throws Exception {
        try {
            server.setServerConfigurationFromFilePath("MP40plusStandalone.xml");
            server.startServer();
            runGetMethod(200, "/helloworld/helloworld", MESSAGE);
        } finally {
            //I think CWWWC0002W is a configuration error and should not appear
            //Should be removed by issue 15496
            server.stopServer("CWMOT0010W", //CWMOT0010W: OpenTracing cannot track JAX-RS requests because an OpentracingTracerFactory class was not provided or client libraries for tracing backend are not in the class path.
                              "CWWWC0002W");//CWWWC0002W: No servlet definition is found for the ExecutionServlet servlet name in the AuthorizationFilter filter mapping.
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

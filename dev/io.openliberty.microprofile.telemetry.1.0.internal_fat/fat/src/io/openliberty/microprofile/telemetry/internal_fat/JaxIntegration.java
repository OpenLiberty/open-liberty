/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertEquals;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;

import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxpropagation.JaxEndpoints;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxpropagation.InMemorySpanExporterProvider;

import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;

@RunWith(FATRunner.class)
public class JaxIntegration extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10Jax";
    public static final String APP_NAME = "JaxPropagation";

    private static int[] dontRetryOnFail = {500}; //The server will return a 500 response code if an Assert fails during the test. Passing 500 in as an allowed response code will prevent wasting time  and more importantly prevent extra spans from confusing anyone trying to debug.

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(JaxEndpoints.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=in-memory\notel.bsp.schedule.delay=100"),
                                                       "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        server.startServer();
    }

    @Test
    public void testIntegrationWithJaxClient() throws Exception {
        HttpRequest pokeJax = new HttpRequest(server, "/" + APP_NAME + "/endpoints/jaxclient");
        assertEquals("Test Passed", pokeJax.run(java.lang.String.class));

        Thread.sleep(1000);

        HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans");
        assertEquals("Test Passed", readspans.run(java.lang.String.class));
    }

    @Test
    @Mode(TestMode.EXPERIMENTAL)
    public void testIntegrationWithJaxClientAsync() throws Exception {
        HttpRequest pokeJax = new HttpRequest(server, "/" + APP_NAME + "/endpoints/jaxclientasync");
        assertEquals("Test Passed", pokeJax.run(java.lang.String.class));

        Thread.sleep(1000);

        HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans");
        assertEquals("Test Passed", readspans.run(java.lang.String.class));
    }

    @Test
    public void testIntegrationWithMpClient() throws Exception {
        HttpRequest pokeMp = new HttpRequest(server, "/" + APP_NAME + "/endpoints/jaxclient");
        assertEquals("Test Passed", pokeMp.run(java.lang.String.class));

        Thread.sleep(1000);

        HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans");
        assertEquals("Test Passed", readspans.run(java.lang.String.class));
    }

    @Test
    @Mode(TestMode.EXPERIMENTAL)
    public void testIntegrationWithMpClientAsync() throws Exception {
        HttpRequest pokeMp = new HttpRequest(server, "/" + APP_NAME + "/endpoints/mpclientasync");
        assertEquals("Test Passed", pokeMp.run(java.lang.String.class));

        Thread.sleep(1000);

        HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans");
        assertEquals("Test Passed", readspans.run(java.lang.String.class));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}

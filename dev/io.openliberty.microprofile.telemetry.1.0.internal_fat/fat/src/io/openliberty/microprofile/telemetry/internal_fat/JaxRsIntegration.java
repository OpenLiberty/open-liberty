/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.JaxRsEndpoints;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;

@RunWith(FATRunner.class)
public class JaxRsIntegration extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10Jax";
    public static final String APP_NAME = "JaxPropagation";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(JaxRsEndpoints.class.getPackage())
                        .addPackage(InMemorySpanExporter.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(new StringAsset("otel.sdk.disabled=false\notel.traces.exporter=in-memory\notel.bsp.schedule.delay=100"),
                                       "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        server.startServer();
    }

    @Test
    public void testIntegrationWithJaxRsClient() throws Exception {
        HttpRequest pokeJax = new HttpRequest(server, "/" + APP_NAME + "/endpoints/jaxrsclient");
        assertEquals("Test Passed", pokeJax.run(String.class));

        Thread.sleep(1000);

        HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans");
        assertEquals("Test Passed", readspans.run(String.class));
    }

    @Test
    public void testIntegrationWithJaxRsClientAsync() throws Exception {
        HttpRequest pokeJax = new HttpRequest(server, "/" + APP_NAME + "/endpoints/jaxrsclientasync");
        assertEquals("Test Passed", pokeJax.run(String.class));

        Thread.sleep(1000);

        HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans");
        assertEquals("Test Passed", readspans.run(String.class));
    }

    @Test
    public void testIntegrationWithMpClient() throws Exception {
        HttpRequest pokeMp = new HttpRequest(server, "/" + APP_NAME + "/endpoints/jaxrsclient");
        assertEquals("Test Passed", pokeMp.run(String.class));

        Thread.sleep(1000);

        HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans");
        assertEquals("Test Passed", readspans.run(String.class));
    }

    @Test
    public void testIntegrationWithMpClientAsync() throws Exception {
        HttpRequest pokeMp = new HttpRequest(server, "/" + APP_NAME + "/endpoints/mpclientasync");
        assertEquals("Test Passed", pokeMp.run(String.class));

        Thread.sleep(1000);

        HttpRequest readspans = new HttpRequest(server, "/" + APP_NAME + "/endpoints/readspans");
        assertEquals("Test Passed", readspans.run(String.class));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}

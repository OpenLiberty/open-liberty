/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;

import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.PropertiesAsset;
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

import io.openliberty.microprofile.telemetry.internal_fat.apps.clientnocdi.ClientTriggeringServlet;
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ClientWithNoCdi {

    public static final String SERVER_NAME = "Telemetry10JaxWithLogging";
    public static final String NO_CDI_APP_NAME = "clientNoCDI";

    @TestServlets({
                    @TestServlet(contextRoot = NO_CDI_APP_NAME, servlet = ClientTriggeringServlet.class),
    })
    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        PropertiesAsset appConfig = new PropertiesAsset()
                        .addProperty("otel.sdk.disabled", "false")
                        .addProperty("otel.traces.exporter", "in-memory")
                        .addProperty("otel.bsp.schedule.delay", "100");

        WebArchive noCDIApp = ShrinkWrap.create(WebArchive.class, NO_CDI_APP_NAME + ".war")
                        .addPackage(ClientTriggeringServlet.class.getPackage());
        CDIArchiveHelper.addBeansXML(noCDIApp, com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode.NONE);//Not strictly needed unless we expand this test to cover MP rest client.


        ShrinkHelper.exportAppToServer(server, noCDIApp, SERVER_ONLY);
        server.startServer();
    }

    //This test does not use open telemetry directly, but it does trigger io.openliberty.microprofile.telemetry.internal.rest.TelemetryClientFilter.java for an app with no CDI and ensures it does not throw an exception
    @Test
    public void testClientWithNoCDi() throws Exception {
        HttpRequest httpRequest = new HttpRequest(server, "/" + NO_CDI_APP_NAME + "/ClientTriggeringServlet");
        assertEquals(io.openliberty.microprofile.telemetry.internal_fat.apps.clientnocdi.ClientInvokedServlet.TEST_PASSED, httpRequest.run(String.class));

        assertFalse("The test did not use a TelemetryClientFilter object", 
                     server.findStringsInLogsUsingMark("microprofile.telemetry\\d*.internal.rest.TelemetryClientFilter < isEnabled Exit", server.getDefaultTraceFile())
                     .isEmpty());

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}

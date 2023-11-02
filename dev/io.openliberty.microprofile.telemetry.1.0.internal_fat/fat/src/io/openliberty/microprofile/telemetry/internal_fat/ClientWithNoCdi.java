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
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.microprofile.telemetry.internal_fat.apps.clientnocdi.ClientTriggeringServlet;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ClientWithNoCdi {

    public static final String SERVER_NAME = "Telemetry10JaxWithLogging";
    public static final String NO_CDI_APP_NAME = "clientNoCDI";

    @ClassRule
    public static RepeatTests r = FATSuite.allMPRepeats(SERVER_NAME);

    @TestServlets({
                    @TestServlet(contextRoot = NO_CDI_APP_NAME, servlet = ClientTriggeringServlet.class),
    })
    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive noCDIApp = ShrinkWrap.create(WebArchive.class, NO_CDI_APP_NAME + ".war")
                        .addPackage(ClientTriggeringServlet.class.getPackage());
        CDIArchiveHelper.addBeansXML(noCDIApp, com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode.NONE);//Not strictly needed unless we expand this test to cover MP rest client.

        ShrinkHelper.exportAppToServer(server, noCDIApp, SERVER_ONLY);
        server.startServer();
    }

    //This test does not use open telemetry directly, but it does trigger io.openliberty.microprofile.telemetry.internal.rest.TelemetryClientFilter.java for an app with no CDI and ensures it does not throw an exception

    //This test also ensures that we actually call the started and shutdown methods in OpenTelemetryInfoFactoryImpl
    //I'm putting it in here to avoid creating an extra server that enables trace. And in an existing method because
    //Our version of junit doesn't allow fixed test ordering.
    @Test
    public void testClientWithNoCDIAndFactoryStarupShutdownCalled() throws Exception {
        HttpRequest httpRequest = new HttpRequest(server, "/" + NO_CDI_APP_NAME + "/ClientTriggeringServlet");
        assertEquals(io.openliberty.microprofile.telemetry.internal_fat.apps.clientnocdi.ClientInvokedServlet.TEST_PASSED, httpRequest.run(String.class));

        assertNotNull("The test did not use a TelemetryClientFilter object",
                      server.waitForStringInLog("microprofile.telemetry\\d*.internal.rest.TelemetryClientFilter < isEnabled Exit", server.getDefaultTraceFile()));

        server.deleteDirectoryFromLibertyServerRoot("apps");

        assertNotNull("OpenTelemetryInfoFactoryImpl.applicationStarting was not invoked",
                      server.waitForStringInLog("OpenTelemetryInfoFactoryImpl > applicationStarting Entry", server.getDefaultTraceFile()));

        assertNotNull("OpenTelemetryInfoFactoryImpl.applicationStopped was not invoked",
                      server.waitForStringInLog("OpenTelemetryInfoFactoryImpl > applicationStopped", server.getDefaultTraceFile()));

        assertNotNull("OpenTelemetryInfoImpl was not disposed",
                      server.waitForStringInLog("EnabledOpenTelemetryInfo > dispose Entry", server.getDefaultTraceFile()));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKZ0059E", "CWWKZ0014W"); //CWWKZ0059E is complaining that we're deleting an app while its still configured. This is needed to test the shutdown code works properly. (Actually stopping the server archives the trace log). CWWKZ0014W is saying the application cannot be found because we just deleted it.
        }
    }

}

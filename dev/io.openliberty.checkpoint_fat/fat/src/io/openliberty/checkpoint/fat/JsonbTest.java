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
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.HttpURLConnection;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import jsonb.JsonbApplication;

@RunWith(FATRunner.class)
@CheckpointTest
public class JsonbTest extends FATServletClient {

    private static final String SERVER_NAME = "checkpointJsonb";
    
    public static final String APP_NAME = "jsonb";

    @Server(SERVER_NAME)
    public static LibertyServer server;
    
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification() //
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly()) //
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, APP_NAME);
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Before
    public void setUp() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                             server -> {
                                 assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
                             });
        server.startServer(getTestMethodNameOnly(testName) + ".log");
    }

    @Test
    public void testJsonbApplication() throws Exception {
        Jsonb JSONB = JsonbBuilder.create();
        Client client = ClientBuilder.newClient();
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/jsonb/app/properties";

        WebTarget target = client.target(url);
        Response response = target.request().get();

        assertEquals("Incorrect response code from " + url, HttpURLConnection.HTTP_OK, response.getStatus());

        String json = response.readEntity(String.class);
        Properties props = JSONB.fromJson(json, Properties.class);

        assertEquals("The property 'name' did not match", "JsonbApplication", props.getProperty("name"));
        assertEquals("The property 'path' did not match", "app/properties", props.getProperty("path"));
        response.close();
        client.close();
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }
}

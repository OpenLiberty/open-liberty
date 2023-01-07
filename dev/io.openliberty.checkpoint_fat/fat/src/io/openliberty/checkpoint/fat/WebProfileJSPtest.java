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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class WebProfileJSPtest {

    public static final String WAR_APP_NAME = "JSPapp";
    public static final String SERVER_NAME = "webProfileJSP";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void exportWebApp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_APP_NAME + ".war")
                        .addAsWebInfResource(new File(server.getInstallRoot() + "/usr/servers/" + server.getServerName() + "/web.xml"))
                        .addAsWebResource(new File(server.getInstallRoot() + "/usr/servers/" + server.getServerName() + "/JSPfile.jsp"))
                        .addAsResource(new File(server.getInstallRoot() + "/usr/servers/" + server.getServerName() + "/alternateJSPdir/alternateJSPfile.jsp"));
        ShrinkHelper.exportAppToServer(server, war, DeployOptions.OVERWRITE);
    }

    @Test
    public void testJSPwithUpdate() throws Exception {
        server.setCheckpoint(CheckpointPhase.APPLICATIONS);
        server.startServer();

        URL url1 = new URL("http://localhost:" + server.getHttpDefaultPort() + "/JSPapp/alternateJSPfile.jsp");
        int responseCode = HttpUtils.getHttpConnection(url1, 5000, HTTPRequestMethod.GET).getResponseCode();

        if (responseCode < 400) {
            fail("request did not return an HTTP error response code");
        }

        server.stopServer("SRVE0190E");

        ServerConfiguration preConfig = server.getServerConfiguration();
        preConfig.getJspEngine().setExtraAttribute("extendedDocumentRoot", server.getInstallRoot() + "/usr/servers/" + server.getServerName() + "/alternateJSPdir");
        server.updateServerConfiguration(preConfig);

        server.checkpointRestore();

        URL url2 = new URL("http://localhost:" + server.getHttpDefaultPort() + "/JSPapp");
        int responseCode2 = HttpUtils.getHttpConnection(url2, 5000, HTTPRequestMethod.GET).getResponseCode();

        assertEquals("Incorrect response code from " + url2, 200, responseCode2);

        assertNotNull(server.findStringsInLogs("jsp servlet"));

        URL url3 = new URL("http://localhost:" + server.getHttpDefaultPort() + "/JSPapp/alternateJSPfile.jsp");
        int responseCode3 = HttpUtils.getHttpConnection(url3, 5000, HTTPRequestMethod.GET).getResponseCode();

        assertEquals("Incorrect response code from " + url3, 200, responseCode3);

        assertNotNull(server.findStringsInLogs("alternate jsp dir"));
    }

    @After
    public void stopServer() throws Exception {
        server.stopServer();
    }

}

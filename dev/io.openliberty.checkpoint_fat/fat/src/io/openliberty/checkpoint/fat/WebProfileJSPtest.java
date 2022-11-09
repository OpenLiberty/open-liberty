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
package io.openliberty.checkpoint.fat;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class WebProfileJSPtest {

    public static final String WAR_APP_NAME = "JSPapp";

    @Server("webProfileJSP")
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
        server.stopServer();

        ServerConfiguration preConfig = server.getServerConfiguration();
        preConfig.getJspEngine().setExtraAttribute("extendedDocumentRoot", server.getInstallRoot() + "/usr/servers/" + server.getServerName() + "/alternateJSPdir");
        server.updateServerConfiguration(preConfig);

        server.checkpointRestore();

        URL url = new URL("http://localhost:" + server.getHttpDefaultPort() + "/JSPapp");
        int responseCode = HttpUtils.getHttpConnection(url, 5000, HTTPRequestMethod.GET).getResponseCode();

        if (responseCode < 200 || responseCode >= 300) {
            fail("request did not return a 200 HTTP response code");
        }

        server.findStringsInLogs("jsp servlet");

        URL url2 = new URL("http://localhost:" + server.getHttpDefaultPort() + "/JSPapp/alternateJSPfile.jsp");
        int responseCode2 = HttpUtils.getHttpConnection(url2, 5000, HTTPRequestMethod.GET).getResponseCode();

        if (responseCode2 < 200 || responseCode2 >= 300) {
            fail("request did not return a 200 HTTP response code");
        }

        server.findStringsInLogs("alternate jsp dir");
    }

    @After
    public void stopServer() throws Exception {
        server.stopServer();
    }

}

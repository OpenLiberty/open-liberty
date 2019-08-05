/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.clientcontainer.fat;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;

/**
 * test SSL & BasicAuth in client container
 */
@RunWith(FATRunner.class)
public class ClientSecurityTest {

    @Server("JaxWsTransportSecurityServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {

        String appName = "TransportSecurityProvider";

        WebArchive war = ShrinkWrap.create(WebArchive.class, appName + ".war")
                        .addPackages(true, "com.ibm.ws.jaxws.transport.server.security")
                        .add(new FileAsset(new File("test-applications/TransportSecurityProvider/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, appName + ".ear").addAsModule(war);

        server.copyFileToLibertyServerRoot("JaxWsTransportSecurityServer/resources");

        ShrinkHelper.exportAppToServer(server, ear);

        server.startServer("JaxwsTransSecTest.log");
        server.waitForStringInLog("CWWKZ0001I.*TransportSecurityProvider");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server == null) {
            return;
        }

        if (server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testBasicAuthWithSSL() throws Exception {

        String appName = "JaxWsTransportSecurityClient";
        LibertyClient client = LibertyClientFactory.getLibertyClient(appName);

        client.addIgnoreErrors("CWWKS9702W");

        JavaArchive jar = ShrinkHelper.buildJavaArchive(appName + ".jar", "com.ibm.ws.jaxws.transport.security", "com.ibm.ws.jaxws.test.transsecurity");

        ShrinkHelper.addDirectory(jar, "test-applications/JaxWsTransportSecurityClient/resources/");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, appName + ".ear")
                        .addAsManifestResource(new File("lib/LibertyFATTestFiles/Ear/" + appName + "/META-INF/application.xml"))
                        .addAsModule(jar);

        ShrinkHelper.exportAppToClient(client, ear);

        List<String> args = new ArrayList<String>();
        args.add(server.getHostname());
        args.add(Integer.toString(server.getHttpDefaultSecurePort()));
        args.add("employee");
        args.add(0, "--");
        client.startClientWithArgs(true, true, true, false, "run", args, true);
        List<String> response = new ArrayList<String>();
        response.add("Hello, employee from SayHelloPojoService");

        assertNotNull("FAIL: Client should report installed features: " + client.waitForStringInCopiedLog("CWWKF0034I:.*" + "client"));
        for (String s : response) {
            assertNotNull("FAIL: Did not receive response from server: " + s, client.waitForStringInCopiedLog(s));
        }

    }

}

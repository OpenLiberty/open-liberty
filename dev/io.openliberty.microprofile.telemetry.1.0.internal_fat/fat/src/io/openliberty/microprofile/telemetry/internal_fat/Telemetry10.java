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

import java.util.Collections;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.annotation.TestServlet;
import componenttest.topology.utils.FATServletClient;

import jakarta.servlet.annotation.WebServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.hotadd.Telemetry10Servlet;
import java.io.File;

@RunWith(FATRunner.class)
public class Telemetry10 extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10";
    public static final String APP_NAME = "TelemetryApp";

    @Server(SERVER_NAME)
    @TestServlet(servlet = Telemetry10Servlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        DeployOptions[] deployOptions = { DeployOptions.SERVER_ONLY };
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
        .addAsManifestResource(new File("publish/resources/permissions.xml"), "permissions.xml")
        .addAsManifestResource(new File("publish/resources/META-INF/microprofile-config.properties"),"microprofile-config.properties");
        app.addClasses(Telemetry10Servlet.class);
        ShrinkHelper.exportDropinAppToServer(server, app, deployOptions);
        server.startServerAndValidate(true, true, false); //don't validate because the app won't have started properly
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWNEN0047W.*Telemetry10Servlet");
    }

    /**
     * Copy a server config file to the server root and wait for notification that the server config has been updated
     *
     * @param filename
     * @throws Exception
     */
    private static void copyConfigFileToLibertyServerRoot(String filename, String appName) throws Exception {
        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot(filename);

        if (appName == null) {
            server.waitForConfigUpdateInLogUsingMark(null, false);
        } else {
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(appName), false);
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.fat.extended;

import java.io.File;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jaxrs21.fat.jsonb.JsonBTestServlet;

@SkipForRepeat("RESTEasy") // failures when comparing JSON
@RunWith(FATRunner.class)
public class PackageJsonBTestNoFeature extends FATServletClient {

    private static final String appName = "jsonbapp";
    public static final String JOHNZON_IMPL = "publish/shared/resources/johnzon/";
    public static final String JSONB_API = "publish/files/jsonbapi/";

    @Server("jaxrs21.fat.packageJsonBNoFeature")
    @TestServlet(servlet = JsonBTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(appName, "jaxrs21.fat.jsonb");
        app.addAsLibraries(new File(JOHNZON_IMPL).listFiles());
        app.addAsLibraries(new File(JSONB_API).listFiles());
        ShrinkHelper.exportDropinAppToServer(server, app);
        server.addInstalledAppForValidation(appName);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CWWKC0044W"); // TODO: remove CWWKC004W once OL issue #1635 is resolved
    }
}
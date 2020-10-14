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
package com.ibm.ws.jaxrs20.fat.jackson1x;

import java.io.File;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs20.fat.jackson.JacksonBaseTest;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class JacksonPOJOwithUserJacksonLib1xTest extends JacksonBaseTest {

    @Server("com.ibm.ws.jaxrs.fat.jackson1x")
    public static LibertyServer server;

    // for comparing json objects on the test servlet
    private static final String databind = "publish/shared/resources/jackson2x/";
    private static final String jackson = "publish/shared/resources/jackson1x/";
    private static final String jacksonwar = "jackson1x";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(jacksonwar, "com.ibm.ws.jaxrs.fat.jackson",
                                                      "com.ibm.ws.jaxrs.fat.jackson1x");
        app.addAsLibraries(new File(databind).listFiles());
        app.addAsLibraries(new File(jackson).listFiles());
        ShrinkHelper.exportAppToServer(server, app);
        server.addInstalledAppForValidation(jacksonwar);

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        target = jacksonwar + "/TestServlet";
        params.put("jacksonwar", jacksonwar);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }
}
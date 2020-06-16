/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.options;

import java.io.File;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs.fat.options.OptionsTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;


@RunWith(FATRunner.class)
public class OptionsTest {
    private static final String CONTEXT_ROOT = "options";
    private static final String HTTPCLIENT = "publish/shared/resources/httpclient/";

    @Server("com.ibm.ws.jaxrs.fat.options")
    @TestServlet(servlet = OptionsTestServlet.class, contextRoot = CONTEXT_ROOT)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(CONTEXT_ROOT, "com.ibm.ws.jaxrs.fat.options");
        app.addAsLibraries(new File(HTTPCLIENT).listFiles());
        ShrinkHelper.exportDropinAppToServer(server, app);
        server.addInstalledAppForValidation(CONTEXT_ROOT);


        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }
}
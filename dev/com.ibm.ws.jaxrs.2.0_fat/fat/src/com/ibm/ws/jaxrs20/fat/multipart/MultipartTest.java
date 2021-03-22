/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.multipart;

import java.io.File;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs.fat.multipart.MultipartTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

//@SkipForRepeat("EE9_FEATURES") // multipart impl currently depends on CXF-classes - will need separate impl for RESTEasy
@RunWith(FATRunner.class)
public class MultipartTest extends FATServletClient {
    private static final String war = "multipart";

    @Server("com.ibm.ws.jaxrs.fat.multipart")
    @TestServlet(servlet = MultipartTestServlet.class, contextRoot = war)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, war, "com.ibm.ws.jaxrs.fat.multipart")
                    .addAsWebInfResource(new StringAsset("This is a resource file that is part of the app"),
                                         "resource.txt")//;
                    .as(ZipExporter.class).exportTo(new File("/tmp/multipart.war"), true);

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
        server.stopServer(/*"CWWKW0101W", "CWWKW0102W", "SRVE8052E", "SRVE0276E", "SRVE0190E"*/);
    }
}

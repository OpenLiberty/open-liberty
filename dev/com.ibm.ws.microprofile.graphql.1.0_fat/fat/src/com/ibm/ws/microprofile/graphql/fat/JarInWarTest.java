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
package com.ibm.ws.microprofile.graphql.fat;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpGraphQL10.jarInWar.EntityInWar;
import mpGraphQL10.jarInWar.JarInWarTestServlet;
import mpGraphQL10.jarInWar.inJar.EntityInJar;

@RunWith(FATRunner.class)
public class JarInWarTest extends FATServletClient {

    private static final String SERVER = "mpGraphQL10.jarInWar";
    private static final String APP_NAME = "jarInWarApp";

    @Server(SERVER)
    @TestServlet(servlet = JarInWarTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jarInWar.jar");
        jar.addPackage(EntityInJar.class.getPackage());
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        war.addAsLibrary(jar);
        war.addPackage(EntityInWar.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }
}

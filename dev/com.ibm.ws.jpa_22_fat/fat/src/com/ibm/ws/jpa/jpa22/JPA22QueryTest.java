/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.jpa22;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jpa.FATSuite;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.PrivHelper;
import jpa22query.web.JPAQueryTestServlet;

/**
 * Test cases that verify JPA Query support for the Stream API.
 *
 */
@RunWith(FATRunner.class)
public class JPA22QueryTest extends FATServletClient {
    public static final String APP_NAME = "jpa22query";
    public static final String SERVLET = "TestJPA22Query";

    @Server("JPA22QueryServer")
    @TestServlet(servlet = JPAQueryTestServlet.class, path = APP_NAME + "/" + SERVLET)
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        final String resPath = "test-applications/jpa22/" + APP_NAME + "/resources/";

        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        app.addPackage("jpa22query.web");
        app.addPackage("jpa22query.entity");
        ShrinkHelper.addDirectory(app, resPath);
        ShrinkHelper.exportAppToServer(server1, app);
        server1.addInstalledAppForValidation(APP_NAME);

        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.dumpServer("query");
        server1.stopServer("CWWJP9991W"); // From Eclipselink drop-and-create tables option
    }
}

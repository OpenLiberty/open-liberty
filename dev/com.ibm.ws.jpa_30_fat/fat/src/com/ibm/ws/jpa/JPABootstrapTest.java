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
package com.ibm.ws.jpa;

import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.PrivHelper;
import jpabootstrap.web.TestJPABootstrapServlet;

/**
 * Verify that the JPA Runtime Integration can parse the supported JPA Spec levels of persistence.xml.
 *
 */
@RunWith(FATRunner.class)
public class JPABootstrapTest extends FATServletClient {
    public static final String APP_NAME = "jpabootstrap";
    public static final String SERVLET = "TestJPABootstrap";

    @Server("JPABootstrapFATServer")
    @TestServlets({
                    @TestServlet(servlet = TestJPABootstrapServlet.class, path = APP_NAME + "_3.0/" + SERVLET)
    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1, PrivHelper.JAXB_PERMISSION);
        createApplication("3.0");
        server1.startServer();
    }

    private static void createApplication(String specLevel) throws Exception {
        final String resPath = "test-applications/" + APP_NAME + "/resources/jpa-" + specLevel + "/";

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + "_" + specLevel + ".war");
        app.addPackage("jpabootstrap.web");
        app.addPackage("jpabootstrap.entity");
        app.merge(ShrinkWrap.create(GenericArchive.class).as(ExplodedImporter.class).importDirectory(resPath).as(GenericArchive.class),
                  "/",
                  Filters.includeAll());
        ShrinkHelper.exportDropinAppToServer(server1, app);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWJP9991W");
    }

    @Test
    public void testJPA30Bootstrap() throws Exception {
        runTest("3.0");
    }

    private void runTest(String spec) throws Exception {
        FATServletClient.runTest(server1, APP_NAME + "_" + spec + "/TestJPABootstrap", "testPersistenceUnitBootstrap");

    }
}

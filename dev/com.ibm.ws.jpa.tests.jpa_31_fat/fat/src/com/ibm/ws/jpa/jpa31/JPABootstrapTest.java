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
package com.ibm.ws.jpa.jpa31;

import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jpa.FATSuite;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.PrivHelper;
import jpabootstrap.web.TestJPABootstrapServlet;

/**
 * Verify that the JPA Runtime Integration can parse the supported JPA Spec levels of persistence.xml.
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@MinimumJavaLevel(javaLevel = 11)
public class JPABootstrapTest extends FATServletClient {
    public static final String APP_NAME = "jpabootstrap";
    public static final String SERVLET = "TestJPABootstrap";
    public static final String SPECLEVEL = "3.1";

    @Server("JPABootstrapFATServer")
    @TestServlets({
                    @TestServlet(servlet = TestJPABootstrapServlet.class, path = APP_NAME + "_" + SPECLEVEL + "/" + SERVLET)
    })
    public static LibertyServer server1;

    public static final JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1, PrivHelper.JAXB_PERMISSION);

        //Get driver name
        server1.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server1, testContainer);

        createApplication(SPECLEVEL);
        server1.startServer();
    }

    private static void createApplication(String specLevel) throws Exception {
        final String resPath = "test-applications/" + APP_NAME + "/resources/jpa-" + specLevel + "/web/";

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
        server1.stopServer("CWWJP9991W",
                           "Missing PostgreSQL10JsonPlatform"); // Generated with postgres db, since we don't include the postgres plugin);
    }

    @Test
    public void testJPA31Bootstrap() throws Exception {
        runTest(SPECLEVEL);
    }

    private void runTest(String spec) throws Exception {
        FATServletClient.runTest(server1, APP_NAME + "_" + spec + "/TestJPABootstrap", "testPersistenceUnitBootstrap");

    }
}
